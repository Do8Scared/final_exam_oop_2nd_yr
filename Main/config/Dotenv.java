package config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Minimal environment configuration loader for .env files.
 * Provides dependency-free loading of KEY=VALUE pairs from .env files into JVM system properties.
 *
 * Behavior:
 * - Searches for a .env file in the current directory and parent directories
 * - Loads KEY=VALUE pairs (ignoring blank lines and comments starting with '#')
 * - Never overwrites existing JVM properties or OS environment variables
 * - Maps POS_* environment keys to pos.* JVM properties for backward compatibility
 *
 * This is useful for local development when running from the IDE, where .env files
 * sit in the repository root but the working directory may be a subdirectory.
 */
public final class Dotenv {

    private static volatile boolean loaded = false;

    /**
     * Private constructor to prevent instantiation of this utility class.
     */
    private Dotenv() {
    }

    /**
     * Loads environment variables from a .env file (if found) into JVM system properties.
     * Thread-safe and idempotent—subsequent calls do nothing if already loaded.
     *
     * Failures (missing or unreadable .env file) are silently ignored;
     * the application will use OS environment variables or JVM properties instead.
     */
    public static void loadIfPresent() {
        if (loaded) return;
        synchronized (Dotenv.class) {
            if (loaded) return;
            try {
                Path envFile = findEnvFile(Paths.get("").toAbsolutePath());
                if (envFile != null) {
                    Map<String, String> entries = parse(envFile);
                    apply(entries);
                }
            } catch (Exception ignored) {
                // Best-effort only. If .env is missing or unreadable, app should still run
                // using OS env vars / JVM properties.
            } finally {
                loaded = true;
            }
        }
    }

    /**
     * Searches for a .env file starting from the given directory and walking up the parent hierarchy.
     * Searches up to 6 levels of parent directories.
     *
     * @param start the starting directory path
     * @return the path to the .env file if found, null otherwise
     */
    private static Path findEnvFile(Path start) {
        Path dir = start;
        for (int i = 0; i < 6 && dir != null; i++) {
            Path candidate = dir.resolve(".env");
            if (Files.isRegularFile(candidate)) {
                return candidate;
            }
            dir = dir.getParent();
        }
        return null;
    }

    /**
     * Parses a .env file and extracts KEY=VALUE pairs into a map.
     * Handles comments (lines starting with '#'), blank lines, and quoted values.
     *
     * @param envFile the path to the .env file
     * @return a map of environment keys to values
     * @throws IOException if the file cannot be read
     */
    private static Map<String, String> parse(Path envFile) throws IOException {
        List<String> lines = Files.readAllLines(envFile, StandardCharsets.UTF_8);
        Map<String, String> map = new HashMap<>();
        for (String raw : lines) {
            if (raw == null) continue;
            String line = raw.trim();
            if (line.isEmpty() || line.startsWith("#")) continue;

            int eq = line.indexOf('=');
            if (eq <= 0) continue;

            String key = line.substring(0, eq).trim();
            String value = line.substring(eq + 1).trim();
            value = stripOptionalQuotes(value);

            if (!key.isEmpty() && !value.isEmpty()) {
                map.put(key, value);
            }
        }
        return map;
    }

    /**
     * Removes surrounding quotes from a string value.
     * Handles both single and double quotes.
     *
     * @param value the value potentially wrapped in quotes
     * @return the unquoted value, or the original value if not quoted
     */
    private static String stripOptionalQuotes(String value) {
        if (value == null) return null;
        if ((value.startsWith("\"") && value.endsWith("\"")) || (value.startsWith("'") && value.endsWith("'"))) {
            if (value.length() >= 2) {
                return value.substring(1, value.length() - 1);
            }
        }
        return value;
    }

    /**
     * Applies loaded environment variables to JVM system properties.
     * Maps POS_* environment keys to pos.* JVM property keys.
     * Never overwrites existing JVM properties or OS environment variables.
     *
     * @param entries the parsed KEY=VALUE pairs from the .env file
     */
    private static void apply(Map<String, String> entries) {
        // Map env keys to the JVM properties used throughout the codebase.
        Map<String, String> mappings = new HashMap<>();
        mappings.put("POS_DB_URL", "pos.db.url");
        mappings.put("POS_DB_USER", "pos.db.user");
        mappings.put("POS_DB_PASSWORD", "pos.db.password");
        mappings.put("POS_ADMIN_PIN", "pos.admin.pin");

        for (Map.Entry<String, String> e : mappings.entrySet()) {
            String envKey = e.getKey();
            String propKey = e.getValue();

            // Never override an explicit JVM property.
            if (System.getProperty(propKey) != null && !System.getProperty(propKey).isBlank()) {
                continue;
            }

            // Never override an OS environment variable.
            String osEnv = System.getenv(envKey);
            if (osEnv != null && !osEnv.isBlank()) {
                continue;
            }

            String value = entries.get(envKey);
            if (value != null && !value.isBlank()) {
                System.setProperty(propKey, value.trim());
            }
        }
    }
}
