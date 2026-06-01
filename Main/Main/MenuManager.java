package Main;

import models.MenuItem;
import database.DatabaseHelper;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Manages menu item creation and insertion into the Supabase database.
 * Handles ACID transactions and audit trail logging for menu modifications.
 */
public class MenuManager {

    private static final String INFO = ">> [INFO]";
    private static final String WARN = ">> [WARN]";
    private static final String ERROR = ">> [ERROR]";
    private static final String SUCCESS = ">> [SUCCESS]";

    /**
     * Adds a menu item to the database with the specified special attribute and actor information.
     * Performs an atomic transaction with audit trail logging.
     *
     * @param item the MenuItem to add
     * @param specialAttribute the special attribute value (e.g., volume, spice level, protein)
     * @param actor the user or system identifier performing the action (for audit purposes)
     * @return true if the insert and audit succeeded, false otherwise
     */
    public static boolean addMenuItem(MenuItem item, String specialAttribute, String actor) {
        String sql = "INSERT INTO menu_items (item_name, price, stock_quantity, category, special_attribute) VALUES (?, ?, ?, ?, ?) RETURNING id";

        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            conn.setAutoCommit(false);

            pstmt.setString(1, item.getItemName());
            pstmt.setDouble(2, item.getPrice());
            pstmt.setInt(3, item.getStockQuantity());
            pstmt.setString(4, item.getCategory());
            pstmt.setString(5, specialAttribute);

            Integer createdId = null;
            try (java.sql.ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    createdId = rs.getInt("id");
                }
            }

            if (createdId != null) {
                String details = String.format(
                        "{\"id\":%d,\"name\":\"%s\",\"price\":%.2f,\"stock\":%d,\"category\":\"%s\",\"special_attribute\":\"%s\"}",
                        createdId,
                        escapeJson(item.getItemName()),
                        item.getPrice(),
                        item.getStockQuantity(),
                        escapeJson(item.getCategory()),
                        escapeJson(specialAttribute)
                );
                DatabaseHelper.insertAudit(conn, actor, "MENU_ITEM_CREATED", String.valueOf(createdId), details);

                conn.commit();
                System.out.println("\n" + SUCCESS + " " + item.getItemName() + " was added to the live database.");
                return true;
            } else {
                conn.rollback();
                System.out.println("\n" + ERROR + " Menu item insert did not return a new ID.");
                return false;
            }

        } catch (SQLException e) {
            System.out.println("\n" + ERROR + " Could not add the menu item to the database.");
            System.out.println(WARN + " Error details: " + e.getMessage());
            try {
                String details = String.format("{\"error\":\"%s\"}", escapeJson(e.getMessage()));
                DatabaseHelper.insertAudit(actor, "MENU_ITEM_CREATE_FAILED", item.getItemName(), details);
            } catch (SQLException logEx) {
                System.out.println(WARN + " Failed to write audit log for menu item failure: " + logEx.getMessage());
            }
            return false;
        }
    }

    /**
     * Escapes JSON special characters in a string for safe JSON construction.
     *
     * @param s the string to escape
     * @return the escaped string safe for JSON insertion
     */
    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\b", "\\b")
                .replace("\f", "\\f")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
