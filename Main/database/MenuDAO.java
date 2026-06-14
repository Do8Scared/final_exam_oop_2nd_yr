package database;

import models.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data access object for menu item operations.
 * Handles fetching, displaying, and category management of menu items.
 */
public class MenuDAO {

    /**
     * Fetches a single menu item by ID, converting the database record into a polymorphic MenuItem subclass
     * via the MenuItemFactory. Filters to active items only. Returns null if item is not found.
     *
     * @param id the menu item ID
     * @return the MenuItem object (or a subclass based on category), or null if not found
     */
    public static MenuItem fetchItemById(int id) {
        String sql = "SELECT * FROM menu_items WHERE id = ? AND is_active = TRUE";

        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    int fetchedId = rs.getInt("id");
                    String name = rs.getString("item_name");
                    double price = rs.getDouble("price");
                    int stock = rs.getInt("stock_quantity");
                    String category = rs.getString("category");
                    String specialAttr = rs.getString("special_attribute");

                    return MenuItemFactory.create(fetchedId, name, price, stock, category, specialAttr);
                }
            }
        } catch (SQLException e) {
            System.out.println("Error fetching item: " + e.getMessage());
        }
        return null;
    }

    /**
     * Displays all active menu items from the database in tabular format.
     * Encapsulates the SQL query so the UI layer never constructs raw SQL.
     */
    public static void printAllActiveItems() {
        String sql = "SELECT * FROM menu_items WHERE is_active = TRUE ORDER BY id ASC";
        executeSelectQuery(sql, null);
    }

    /**
     * Displays all menu items in a specified category with ID, name, price, and stock.
     *
     * @param category the category name to filter by
     */
    public static void printItemsByCategory(String category) {
        System.out.println("\n--- " + category.toUpperCase() + " ---");
        String sql = "SELECT id, item_name, price, stock_quantity FROM menu_items WHERE category = ? AND is_active = TRUE";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, category);
            try (ResultSet rs = pstmt.executeQuery()) {
                boolean hasItems = false;
                while (rs.next()) {
                    hasItems = true;
                    System.out.println("ID: " + rs.getInt("id") +
                            " | " + rs.getString("item_name") +
                            " - PHP " + String.format("%.2f", rs.getDouble("price")) +
                            " (Stock: " + rs.getInt("stock_quantity") + ")");
                }
                if (!hasItems) System.out.println("No items available in this category.");
            }
        } catch (SQLException e) {
            System.out.println("Error fetching items: " + e.getMessage());
        }
    }

    /**
     * Retrieves a list of all active menu item categories from the database.
     * Returns an empty list if no categories are found.
     *
     * @return a list of category names
     */
    public static List<String> getActiveCategories() {
        List<String> categories = new ArrayList<>();
        String sql = "SELECT DISTINCT category FROM menu_items WHERE is_active = TRUE ORDER BY category";
        try (Connection conn = DatabaseHelper.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                categories.add(rs.getString("category"));
            }
        } catch (SQLException e) {
            System.out.println("Error loading categories: " + e.getMessage());
        }
        return categories;
    }

    /**
     * Internal method that executes a SELECT query on menu_items and prints results in tabular format.
     * Not exposed to the UI layer — callers should use printAllActiveItems() or printItemsByCategory().
     *
     * @param sql       the SQL query to execute (should be a SELECT statement)
     * @param parameter optional parameter to bind to a prepared statement (for WHERE clauses)
     */
    private static void executeSelectQuery(String sql, String parameter) {
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            if (parameter != null) {
                pstmt.setString(1, parameter);
            }

            try (ResultSet rs = pstmt.executeQuery()) {
                System.out.printf("%-5s %-25s %-10s %-10s %-15s%n", "ID", "Item Name", "Price", "Stock", "Category");
                System.out.println("----------------------------------------------------------------------");

                boolean foundItems = false;
                while (rs.next()) {
                    foundItems = true;
                    System.out.printf("%-5d %-25s PHP %-7.2f %-10d %-15s%n",
                            rs.getInt("id"),
                            rs.getString("item_name"),
                            rs.getDouble("price"),
                            rs.getInt("stock_quantity"),
                            rs.getString("category"));
                }

                if (!foundItems) {
                    System.out.println("No items found.");
                }
            }

        } catch (SQLException e) {
            System.out.println("Database Error: Could not fetch the menu.");
            System.out.println(e.getMessage());
        }
    }
}