package database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

/**
 * Smoke test utility for audit trail functionality.
 * Verifies that database connectivity, audit table exists, and audit logging works correctly.
 * Cleans up test data after verification.
 */
public class AuditTrail {

    /**
     * Standalone smoke test method to verify audit trail integration.
     * Inserts a test audit record, verifies it exists, and cleans it up.
     *
     * @param args command-line arguments (not used)
     */
    public static void main(String[] args) {
        String targetId = "SMOKE-" + UUID.randomUUID();

        try (Connection conn = DatabaseHelper.getConnection()) {
            conn.setAutoCommit(false);

            DatabaseHelper.insertAudit(
                    conn,
                    "smoke-test",
                    "AUDIT_SMOKE_TEST",
                    targetId,
                    "{\"ok\":true}"
            );

            Long id = findAuditIdByTargetId(conn, targetId);
            if (id == null) {
                System.out.println("[AUDIT SMOKE TEST] Insert did not appear in audit_logs (unexpected).");
                conn.rollback();
                return;
            }

            deleteAuditById(conn, id);
            conn.commit();

            System.out.println("[AUDIT SMOKE TEST] OK (insert + cleanup successful)");
        } catch (SQLException e) {
            System.out.println("[AUDIT SMOKE TEST] FAILED: " + e.getMessage());
        }
    }

    /**
     * Finds an audit log entry by its target ID and returns its database ID.
     *
     * @param conn an active database Connection
     * @param targetId the target ID to search for
     * @return the audit log ID if found, null otherwise
     * @throws SQLException if the query fails
     */
    private static Long findAuditIdByTargetId(Connection conn, String targetId) throws SQLException {
        String sql = "SELECT id FROM audit_logs WHERE target_id = ? ORDER BY event_time DESC LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, targetId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return rs.getLong("id");
            }
        }
    }

    /**
     * Deletes an audit log entry by its ID.
     *
     * @param conn an active database Connection
     * @param id the audit log ID to delete
     * @throws SQLException if the delete operation fails
     */
    private static void deleteAuditById(Connection conn, long id) throws SQLException {
        String sql = "DELETE FROM audit_logs WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        }
    }

    /**
     * Retrieves all audit logs and returns them as a DefaultTableModel.
     */
    public static javax.swing.table.DefaultTableModel getAuditLogsTableModel() {
        javax.swing.table.DefaultTableModel model = new javax.swing.table.DefaultTableModel(
            new Object[]{"ID", "Event Time", "Actor", "Action", "Target ID", "Details"}, 0
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        String sql = "SELECT id, event_time, actor, event_type, target_id, details FROM audit_logs ORDER BY event_time DESC";
        try (Connection conn = DatabaseHelper.getConnection();
             java.sql.Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                model.addRow(new Object[]{
                    rs.getLong("id"),
                    rs.getTimestamp("event_time").toString(),
                    rs.getString("actor"),
                    rs.getString("event_type"),
                    rs.getString("target_id"),
                    rs.getString("details")
                });
            }
        } catch (SQLException e) {
            System.out.println("Error fetching audit logs: " + e.getMessage());
        }
        return model;
    }

    /**
     * Clears all audit logs from the database.
     * @return true if successful
     */
    public static boolean clearAllAudits() {
        String sql = "TRUNCATE TABLE audit_logs";
        try (Connection conn = DatabaseHelper.getConnection();
             java.sql.Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql);
            return true;
        } catch (SQLException e) {
            System.out.println("Error clearing audit logs: " + e.getMessage());
            return false;
        }
    }
}
