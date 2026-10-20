package dev.amazepr.plugin;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

final class ManifestWalkthroughLoader {
    WalkthroughContent load(Path projectRoot) {
        WalkthroughContent walkthrough = loadWalkthrough(projectRoot);
        if (walkthrough != null) {
            return walkthrough;
        }

        WalkthroughContent manifest = loadManifest(projectRoot);
        if (manifest != null) {
            return manifest;
        }

        return missing();
    }

    WalkthroughContent loadWalkthroughOnly(Path projectRoot) {
        WalkthroughContent walkthrough = loadWalkthrough(projectRoot);
        if (walkthrough != null) {
            return walkthrough;
        }

        return WalkthroughContent.missing(
            "No rendered walkthrough found.\n\n"
                + "Expected:\n"
                + "- " + ManifestPaths.WALKTHROUGH_PATH + "\n\n"
                + "Run:\n"
                + "python tools/change_manifest_walkthrough.py "
                + "--manifest .change-manifest/manifest.json "
                + "--output .change-manifest/walkthrough.md\n"
        );
    }

    WalkthroughContent loadManifestOnly(Path projectRoot) {
        WalkthroughContent manifest = loadManifest(projectRoot);
        if (manifest != null) {
            return manifest;
        }

        return WalkthroughContent.missing(
            "No manifest JSON found.\n\n"
                + "Expected:\n"
                + "- " + ManifestPaths.MANIFEST_PATH + "\n"
        );
    }

    private WalkthroughContent loadWalkthrough(Path projectRoot) {
        Path walkthrough = ManifestPaths.walkthrough(projectRoot);
        if (Files.isRegularFile(walkthrough)) {
            return read(
                walkthrough,
                "Rendered walkthrough",
                "Loaded " + ManifestPaths.WALKTHROUGH_PATH + "."
            );
        }

        return null;
    }

    private WalkthroughContent loadManifest(Path projectRoot) {
        Path manifest = ManifestPaths.manifest(projectRoot);
        if (Files.isRegularFile(manifest)) {
            return read(
                manifest,
                "Manifest JSON",
                "Rendered walkthrough is missing. Showing " + ManifestPaths.MANIFEST_PATH + "."
            );
        }

        return null;
    }

    private WalkthroughContent missing() {
        return WalkthroughContent.missing(
            "No change manifest found.\n\n"
                + "Expected one of:\n"
                + "- " + ManifestPaths.WALKTHROUGH_PATH + "\n"
                + "- " + ManifestPaths.MANIFEST_PATH + "\n\n"
                + "Run:\n"
                + "python tools/change_manifest_walkthrough.py "
                + "--manifest .change-manifest/manifest.json "
                + "--output .change-manifest/walkthrough.md\n"
        );
    }

    private WalkthroughContent read(Path path, String title, String status) {
        try {
            return WalkthroughContent.loaded(
                title,
                status,
                Files.readString(path, StandardCharsets.UTF_8)
            );
        } catch (IOException exception) {
            return WalkthroughContent.error(
                "Could not read " + path + ":\n" + exception.getMessage()
            );
        }
    }
}
