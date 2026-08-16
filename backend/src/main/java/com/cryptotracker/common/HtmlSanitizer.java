package com.cryptotracker.common;

public final class HtmlSanitizer {

    private HtmlSanitizer() {
    }

    public static String escape(String input) {
        if (input == null) {
            return null;
        }
        return input
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
