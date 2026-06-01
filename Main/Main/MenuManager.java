package Main;

import models.MenuItem;
import database.DatabaseHelper;
import util.JsonUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

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
     * Properly rolls back on any failure path.
     *
     * @param item the MenuItem to add
     * @param specialAttribute the special attribute value (e.g., volume, spice level, protein)
     * @param actor the user or system identifier performing the action (for audit purposes)
     */
    public static void addMenuItem(MenuItem item, String specialAttribute, String actor) {
        String sql = "INSERT INTO menu_items (item_name, price, stock_quantity, category, special_attribute) VALUES (?, ?, ?, ?, ?) RETURNING id";

        Connection conn = null;
        try {
            conn = DatabaseHelper.getConnection();
            conn.setAutoCommit(false);

            Integer createdId = null;
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, item.getItemName());
                pstmt.setDouble(2, item.getPrice());
                pstmt.setInt(3, item.getStockQuantity());
                pstmt.setString(4, item.getCategory());
                pstmt.setString(5, specialAttribute);

                try (java.sql.ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        createdId = rs.getInt("id");
                    }
                }
            }

            if (createdId != null) {
                Map<String, Object> details = new LinkedHashMap<>();
                details.put("id", createdId);
                details.put("name", item.getItemName());
                details.put("price", item.getPrice());
                details.put("stock", item.getStockQuantity());
                details.put("category", item.getCategory());
                details.put("special_attribute", specialAttribute);

                DatabaseHelper.insertAudit(conn, actor, "MENU_ITEM_CREATED",
                        String.valueOf(createdId), JsonUtil.buildJsonObject(details));

                conn.commit();
                System.out.println("\n[SYSTEM] Success! " + item.getItemName() + " was added to the live database.");
            } else {
                conn.rollback();
                System.out.println("\n[SYSTEM ERROR] Menu item insert did not return a new ID.");
            }

        } catch (SQLException e) {
            // Fix: always rollback on exception (was missing before)
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ex) { /* rollback best-effort */ }
            }
            System.out.println("\n[SYSTEM ERROR] Could not add the menu item to the database.");
            System.out.println("Error details: " + e.getMessage());
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    System.out.println("Error closing connection: " + e.getMessage());
                }
            }
        }
    }
}
