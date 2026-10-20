package dev.amazepr.plugin;

record WalkthroughContent(String title, String status, String body) {
    static WalkthroughContent loaded(String title, String status, String body) {
        return new WalkthroughContent(title, status, body);
    }

    static WalkthroughContent missing(String body) {
        return new WalkthroughContent("Missing manifest", "No manifest found.", body);
    }

    static WalkthroughContent error(String body) {
        return new WalkthroughContent("Read error", "Could not load manifest.", body);
    }
}
