package dev.amazepr.plugin;

import java.nio.file.Path;

final class ManifestPaths {
    static final String MANIFEST_PATH = ".change-manifest/manifest.json";
    static final String WALKTHROUGH_PATH = ".change-manifest/walkthrough.md";

    private ManifestPaths() {
    }

    static Path manifest(Path projectRoot) {
        return projectRoot.resolve(".change-manifest").resolve("manifest.json");
    }

    static Path walkthrough(Path projectRoot) {
        return projectRoot.resolve(".change-manifest").resolve("walkthrough.md");
    }
}
