package com.example.payments;

// The two response shapes are trivial, so I hand-rolled these instead of
// pulling in Jackson. Would swap to a real serializer if the API grew.
final class Json {

    private Json() {
    }

    static String balance(String currency, String amount) {
        return "{\"currency\":\"" + esc(currency) + "\",\"amount\":" + amount + "}";
    }

    static String error(String message) {
        return "{\"error\":\"" + esc(message) + "\"}";
    }

    private static String esc(String s) {
        StringBuilder sb = new StringBuilder(s.length() + 8);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"'  -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default   -> {
                    if (c < 0x20) sb.append(String.format("\\u%04x", (int) c));
                    else sb.append(c);
                }
            }
        }
        return sb.toString();
    }
}
