package com.aiusage.util;

public class TokenFormatter {
    public static String format(long tokens) {
        if (tokens < 1_000) return tokens + " tokens";
        if (tokens < 1_000_000) return String.format("%.1fK", tokens / 1_000.0);
        return String.format("%.1fM", tokens / 1_000_000.0);
    }

    public static String formatDetailed(long tokens) {
        if (tokens < 1_000) return tokens + " tokens";
        if (tokens < 1_000_000) return String.format("%,d tokens", tokens);
        return String.format("%,d (%.1fM)", tokens, tokens / 1_000_000.0);
    }
}
