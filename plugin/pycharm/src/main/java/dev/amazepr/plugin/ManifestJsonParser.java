package dev.amazepr.plugin;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

final class ManifestJsonParser {
    ParsedWalkthrough parse(String json) {
        List<String> diagnostics = new ArrayList<>();
        try {
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            String title = stringAt(objectAt(root, "change", diagnostics), "title", "Untitled change", diagnostics);
            String intent = stringAt(objectAt(root, "change", diagnostics), "intent", "", diagnostics);
            Set<String> filePaths = manifestFilePaths(root, diagnostics);
            List<WalkthroughStep> steps = workflowSteps(root, filePaths, diagnostics);
            return new ParsedWalkthrough(title, intent, diagnostics, steps);
        } catch (IllegalStateException | JsonSyntaxException exception) {
            diagnostics.add("Manifest JSON could not be parsed: " + exception.getMessage());
            return new ParsedWalkthrough("Invalid manifest", "", diagnostics, List.of());
        }
    }

    private Set<String> manifestFilePaths(JsonObject root, List<String> diagnostics) {
        Set<String> paths = new HashSet<>();
        JsonArray files = arrayAt(root, "files", diagnostics);
        if (files == null) {
            return paths;
        }

        for (JsonElement element : files) {
            if (!element.isJsonObject()) {
                diagnostics.add("files contains a non-object item.");
                continue;
            }
            String path = stringAt(element.getAsJsonObject(), "path", "", diagnostics);
            if (!path.isBlank() && !paths.add(path)) {
                diagnostics.add("Duplicate file entry: " + path);
            }
        }
        return paths;
    }

    private List<WalkthroughStep> workflowSteps(
        JsonObject root,
        Set<String> filePaths,
        List<String> diagnostics
    ) {
        List<WalkthroughStep> steps = new ArrayList<>();
        JsonArray workflow = arrayAt(root, "workflow", diagnostics);
        if (workflow == null) {
            return steps;
        }

        for (JsonElement element : workflow) {
            if (!element.isJsonObject()) {
                diagnostics.add("workflow contains a non-object item.");
                continue;
            }

            JsonObject item = element.getAsJsonObject();
            int number = intAt(item, "step", steps.size() + 1, diagnostics);
            String title = stringAt(item, "title", "Step " + number, diagnostics);
            String description = stringAt(item, "description", "", diagnostics);
            List<WorkflowFileReference> stepFiles = stepFiles(item, filePaths, diagnostics);
            steps.add(new WalkthroughStep(number, title, description, stepFiles));
        }
        return steps;
    }

    private List<WorkflowFileReference> stepFiles(
        JsonObject item,
        Set<String> filePaths,
        List<String> diagnostics
    ) {
        List<WorkflowFileReference> references = new ArrayList<>();
        JsonArray files = arrayAt(item, "files", diagnostics);
        if (files == null) {
            return references;
        }

        for (JsonElement element : files) {
            WorkflowFileReference reference = workflowFileReference(element, diagnostics);
            if (reference == null) {
                continue;
            }
            references.add(reference);
            if (!filePaths.contains(reference.path())) {
                diagnostics.add("Workflow references file not listed in files: " + reference.path());
            }
        }
        return references;
    }

    private WorkflowFileReference workflowFileReference(
        JsonElement element,
        List<String> diagnostics
    ) {
        if (element.isJsonPrimitive()) {
            return new WorkflowFileReference(element.getAsString(), null, null, null);
        }

        if (!element.isJsonObject()) {
            diagnostics.add("workflow file reference is not a string or object.");
            return null;
        }

        JsonObject object = element.getAsJsonObject();
        String path = stringAt(object, "path", "", diagnostics);
        if (path.isBlank()) {
            return null;
        }
        Integer line = optionalIntAt(object, "line", diagnostics);
        Integer endLine = optionalIntAt(object, "end_line", diagnostics);
        String label = optionalStringAt(object, "label");
        return new WorkflowFileReference(path, line, endLine, label);
    }

    private JsonObject objectAt(JsonObject object, String key, List<String> diagnostics) {
        JsonElement element = object.get(key);
        if (element == null || !element.isJsonObject()) {
            diagnostics.add("Missing object field: " + key);
            return new JsonObject();
        }
        return element.getAsJsonObject();
    }

    private JsonArray arrayAt(JsonObject object, String key, List<String> diagnostics) {
        JsonElement element = object.get(key);
        if (element == null || !element.isJsonArray()) {
            diagnostics.add("Missing array field: " + key);
            return null;
        }
        return element.getAsJsonArray();
    }

    private String stringAt(
        JsonObject object,
        String key,
        String fallback,
        List<String> diagnostics
    ) {
        JsonElement element = object.get(key);
        if (element == null || !element.isJsonPrimitive()) {
            diagnostics.add("Missing string field: " + key);
            return fallback;
        }
        String value = element.getAsString();
        if (value.isBlank()) {
            diagnostics.add("Empty string field: " + key);
        }
        return value;
    }

    private int intAt(JsonObject object, String key, int fallback, List<String> diagnostics) {
        JsonElement element = object.get(key);
        if (element == null || !element.isJsonPrimitive()) {
            diagnostics.add("Missing integer field: " + key);
            return fallback;
        }
        try {
            return element.getAsInt();
        } catch (NumberFormatException exception) {
            diagnostics.add("Invalid integer field: " + key);
            return fallback;
        }
    }

    private Integer optionalIntAt(JsonObject object, String key, List<String> diagnostics) {
        JsonElement element = object.get(key);
        if (element == null) {
            return null;
        }
        if (!element.isJsonPrimitive()) {
            diagnostics.add("Invalid integer field: " + key);
            return null;
        }
        try {
            return element.getAsInt();
        } catch (NumberFormatException exception) {
            diagnostics.add("Invalid integer field: " + key);
            return null;
        }
    }

    private String optionalStringAt(JsonObject object, String key) {
        JsonElement element = object.get(key);
        if (element == null || !element.isJsonPrimitive()) {
            return null;
        }
        return element.getAsString();
    }
}
