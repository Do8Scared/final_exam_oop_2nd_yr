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

    /**
     * Adds a menu item to the database with default actor "unknown".
     *
     * @param item the MenuItem to add
     * @param specialAttribute the special attribute value (e.g., volume, spice level, protein)
     */
    public static void addMenuItem(MenuItem item, String specialAttribute) {
        addMenuItem(item, specialAttribute, "unknown");
    }

    /**
     * Adds a menu item to the database with the specified special attribute and actor information.
     * Performs an atomic transaction with audit trail logging.
     *
     * @param item the MenuItem to add
     * @param specialAttribute the special attribute value (e.g., volume, spice level, protein)
     * @param actor the user or system identifier performing the action (for audit purposes)
     */
    public static void addMenuItem(MenuItem item, String specialAttribute, String actor) {
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
                System.out.println("\n[SYSTEM] Success! " + item.getItemName() + " was added to the live database.");
            } else {
                conn.rollback();
                System.out.println("\n[SYSTEM ERROR] Menu item insert did not return a new ID.");
            }

        } catch (SQLException e) {
            System.out.println("\n[SYSTEM ERROR] Could not add the menu item to the database.");
            System.out.println("Error details: " + e.getMessage());
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
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }
}
