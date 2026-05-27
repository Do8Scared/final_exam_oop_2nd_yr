package database;

import config.Dotenv;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * Database connection and audit logging utilities.
 * Manages PostgreSQL connections and transaction logging for the POS system.
 */
public class DatabaseHelper {

    static {
        Dotenv.loadIfPresent();
        ensurePostgresDriverLoaded();
    }

    /**
     * Ensures the PostgreSQL JDBC driver is loaded and available.
     */
    private static void ensurePostgresDriverLoaded() {
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException ignored) {
            // If the driver jar isn't on the classpath, getConnection() will throw a
            // clearer SQLException later.
        }
    }

    private static final String URL_ENV = "POS_DB_URL";
    private static final String USER_ENV = "POS_DB_USER";
    private static final String PASSWORD_ENV = "POS_DB_PASSWORD";
    private static final String URL_PROPERTY = "pos.db.url";
    private static final String USER_PROPERTY = "pos.db.user";
    private static final String PASSWORD_PROPERTY = "pos.db.password";

    /**
     * Establishes a connection to the PostgreSQL database.
     * Resolves database credentials from JVM properties or environment variables.
     * Configuration keys: POS_DB_URL, POS_DB_USER, POS_DB_PASSWORD.
     *
     * @return an active database Connection
     * @throws SQLException if connection cannot be established or credentials are missing
     */
    public static Connection getConnection() throws SQLException {
        String url = resolveRequiredSetting(URL_ENV, URL_PROPERTY, "database URL");
        url = applyPoolerSafeSettings(url);
        String user = resolveRequiredSetting(USER_ENV, USER_PROPERTY, "database username");
        String password = resolveRequiredSetting(PASSWORD_ENV, PASSWORD_PROPERTY, "database password");
        try {
            return DriverManager.getConnection(url, user, password);
        } catch (SQLException e) {
            // Common local-dev failure: driver jar not on classpath.
            if (e.getMessage() != null && e.getMessage().toLowerCase().contains("no suitable driver")) {
                throw new SQLException(
                        "No suitable JDBC driver found for the database URL. " +
                        "Make sure the PostgreSQL driver jar is on the runtime classpath (e.g., 'libs/postgresql-42.6.0.jar' in this project). " +
                        "Original error: " + e.getMessage(),
                        e
                );
            }
            throw e;
        }
    }

    /**
     * Convenience overload for inserting audit logs from contexts without an active transaction.
     *
     * @param actor the user or system identifier performing the action
     * @param eventType the type of event (e.g., ORDER_CHECKOUT, MENU_ITEM_CREATED)
     * @param targetId the ID of the target resource affected
     * @param detailsJson a JSON string with additional event details
     * @throws SQLException if the audit insertion fails
     */
    public static void insertAudit(String actor, String eventType, String targetId, String detailsJson) throws SQLException {
        try (Connection conn = getConnection()) {
            insertAudit(conn, actor, eventType, targetId, detailsJson);
        }
    }

    /**
     * Applies PgBouncer-compatible settings to the database URL.
     * PgBouncer (connection pooler) requires preferQueryMode=simple to avoid prepared statement conflicts.
     * Detects pooler URLs by common patterns and adds the setting if not already present.
     *
     * @param url the original database URL
     * @return the URL with pooler-safe settings applied, or the original URL if not a pooler
     */
    private static String applyPoolerSafeSettings(String url) {
        if (url == null) return null;

        String lower = url.toLowerCase();
        boolean looksLikePooler = lower.contains("pooler") || lower.contains("pgbouncer") || lower.contains(":6543/");
        if (!looksLikePooler) {
            return url;
        }

        if (url.contains("preferQueryMode=")) {
            return url;
        }

        return url + (url.contains("?") ? "&" : "?") + "preferQueryMode=simple";
    }

    /**
     * Inserts an audit log entry within an existing database transaction.
     * Ensures the audit table exists before insertion. If the table is missing,
     * it will be created automatically.
     *
     * @param conn an active database Connection (preferably within a transaction)
     * @param actor the user or system identifier performing the action
     * @param eventType the type of event being logged
     * @param targetId the ID of the target resource affected (or null)
     * @param detailsJson a JSON string with additional event details (or null)
     * @throws SQLException if the audit insertion or table creation fails
     */
    public static void insertAudit(Connection conn, String actor, String eventType,
                                   String targetId, String detailsJson) throws SQLException {
        ensureAuditTableExists(conn);

        String sql = "INSERT INTO audit_logs (actor, event_type, target_id, details) VALUES (?, ?, ?, ?::jsonb)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, actor);
            ps.setString(2, eventType);
            if (targetId != null) ps.setString(3, targetId); else ps.setNull(3, java.sql.Types.VARCHAR);
            if (detailsJson != null) ps.setString(4, detailsJson); else ps.setNull(4, java.sql.Types.VARCHAR);
            ps.executeUpdate();
        }
    }

    /**
     * Creates the audit_logs table if it does not already exist.
     * Includes indexes on event_time and event_type for query performance.
     *
     * @param conn an active database Connection
     * @throws SQLException if the table creation or index creation fails
     */
    private static void ensureAuditTableExists(Connection conn) throws SQLException {
        String existsSql = "SELECT to_regclass('public.audit_logs')";
        try (PreparedStatement ps = conn.prepareStatement(existsSql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next() && rs.getString(1) != null) {
                return;
            }
        }

        try (Statement st = conn.createStatement()) {
            st.execute(
                    "CREATE TABLE IF NOT EXISTS audit_logs (" +
                            "id BIGSERIAL PRIMARY KEY," +
                            "event_time TIMESTAMPTZ NOT NULL DEFAULT now()," +
                            "actor TEXT," +
                            "event_type TEXT NOT NULL," +
                            "target_id TEXT," +
                            "details JSONB," +
                            "ip_address TEXT," +
                            "created_by TEXT" +
                    ")"
            );
            st.execute("CREATE INDEX IF NOT EXISTS idx_audit_event_time ON audit_logs (event_time)");
            st.execute("CREATE INDEX IF NOT EXISTS idx_audit_event_type ON audit_logs (event_type)");
        }
    }

    /**
     * Resolves a configuration setting from JVM properties or environment variables.
     * Checks JVM properties first, then environment variables. Throws an exception if not found.
     *
     * @param envKey the environment variable name
     * @param propertyKey the JVM system property name
     * @param label a human-readable label for error messages
     * @return the configuration value
     * @throws SQLException if the setting is not found in either source
     */
    private static String resolveRequiredSetting(String envKey, String propertyKey, String label) throws SQLException {
        String value = System.getProperty(propertyKey);
        if (value == null || value.isBlank()) {
            value = System.getenv(envKey);
        }

        if (value == null || value.isBlank()) {
            throw new SQLException(
                    "Missing " + label + ". Set JVM property '" + propertyKey + "' or environment variable '" + envKey + "'."
            );
        }

        return value.trim();
    }

    /**
     * Standalone test method to verify database connectivity before launching the system.
     *
     * @param args command-line arguments (not used)
     */
    public static void main(String[] args) {
        Connection conn = null;

        try {
            System.out.println("Attempting to connect to the cloud...");
            conn = getConnection();
            System.out.println("Success! Connected to the database.");
        } catch (SQLException e) {
            System.out.println("Connection Failed! Check your URL, username, or password.");
            System.out.println("Error details: " + e.getMessage());
        } finally {
            try {
                if (conn != null) {
                    conn.close();
                    System.out.println("Connection safely closed.");
                }
            } catch (SQLException ex) {
                System.out.println("Error closing connection.");
            }
        }
    }
}