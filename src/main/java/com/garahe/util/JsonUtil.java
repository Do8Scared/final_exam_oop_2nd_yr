package com.garahe.util;

import java.util.Map;

/**
 * Shared JSON utility methods for building safe JSON strings.
 * Centralizes JSON escaping to avoid duplication across DAO and service classes.
 */
public final class JsonUtil {

    /**
     * Private constructor to prevent instantiation of this utility class.
     */
    private JsonUtil() {
    }

    /**
     * Escapes JSON special characters in a string for safe JSON construction.
     * Handles backslashes, double quotes, newlines, carriage returns, and tabs.
     *
     * @param s the string to escape
     * @return the escaped string safe for JSON insertion, or empty string if null
     */
    public static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    /**
     * Builds a simple JSON object string from a map of key-value pairs.
     * Values are formatted based on their type:
     * - String values are escaped and quoted
     * - Number values are written as-is
     * - Boolean values are written as-is
     * - Null values are written as JSON null
     * - Other types are converted via toString(), escaped, and quoted
     *
     * @param entries the key-value pairs to serialize
     * @return a JSON object string
     */
    public static String buildJsonObject(Map<String, Object> entries) {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Object> entry : entries.entrySet()) {
            if (!first) sb.append(',');
            sb.append('"').append(escapeJson(entry.getKey())).append("\":");

            Object value = entry.getValue();
            if (value == null) {
                sb.append("null");
            } else if (value instanceof Number) {
                sb.append(value);
            } else if (value instanceof Boolean) {
                sb.append(value);
            } else {
                sb.append('"').append(escapeJson(value.toString())).append('"');
            }
            first = false;
        }
        sb.append('}');
        return sb.toString();
    }
}
