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
     * Fetches a single menu item by ID, converting the database record into a polymorphic MenuItem subclass.
     * Filters to active items only. Returns null if item is not found.
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
                    if (specialAttr == null) specialAttr = "Standard";

                    if (category.equalsIgnoreCase("Beverages")) {
                        int volume = parseIntOrDefault(specialAttr.replace("ml", "").trim(), 500, "beverage volume");
                        return new Beverage(fetchedId, name, price, stock, category, volume);

                    } else if (category.equalsIgnoreCase("Appetizer")) {
                        int pieces = parseIntOrDefault(specialAttr, 1, "appetizer pieces");
                        return new Appetizer(fetchedId, name, price, stock, category, pieces);

                    } else if (category.equalsIgnoreCase("Dessert")) {
                        return new Dessert(fetchedId, name, price, stock, category, specialAttr);

                    } else if (category.equalsIgnoreCase("Soup")) {
                        boolean isSpicy = specialAttr.equalsIgnoreCase("Spicy");
                        return new Soup(fetchedId, name, price, stock, category, isSpicy);

                    } else if (category.equalsIgnoreCase("Rice Bowl")) {
                        return new RiceBowl(fetchedId, name, price, stock, category, specialAttr);

                    } else if (category.equalsIgnoreCase("Add-Ons") || category.equalsIgnoreCase("Add-On")) {
                        boolean isCondiment = specialAttr.equalsIgnoreCase("Condiment");
                        return new AddOn(fetchedId, name, price, stock, category, isCondiment);

                    } else {
                        System.out.println(">> [WARN] Unrecognized menu category '" + category + "' for item ID " + fetchedId + ". Loading as a generic menu item.");
                        return new MenuItem(fetchedId, name, price, stock, category);
                    }
                }
            }
        } catch (SQLException e) {
            System.out.println("Error fetching item: " + e.getMessage());
        }
        return null;
    }

    /**
     * Executes a SELECT query on the menu_items table and displays the results in tabular format.
     *
     * @param sql the SQL query to execute (should be a SELECT statement)
     * @param parameter optional parameter to bind to a prepared statement (for WHERE clauses)
     */
    public static void executeSelectQuery(String sql, String parameter) {
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            if (parameter != null) {
                pstmt.setString(1, parameter);
            }

            try (ResultSet rs = pstmt.executeQuery()) {
                System.out.printf("%-5s %-25s %-10s %-10s %-15s\n", "ID", "Item Name", "Price", "Stock", "Category");
                System.out.println("----------------------------------------------------------------------");

                boolean foundItems = false;
                while (rs.next()) {
                    foundItems = true;
                    System.out.printf("%-5d %-25s ₱%-9.2f %-10d %-15s\n",
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
                            " - ₱" + String.format("%.2f", rs.getDouble("price")) +
                            " (Stock: " + rs.getInt("stock_quantity") + ")");
                }
                if (!hasItems) System.out.println("No items available in this category.");
            }
        } catch (SQLException e) {
            System.out.println("Error fetching items: " + e.getMessage());
        }
    }

    /**
     * Safely parses a string to an integer with a fallback value if parsing fails.
     *
     * @param value the string value to parse
     * @param fallback the default value if parsing fails
     * @param context a description of what value is being parsed (for error logging)
     * @return the parsed integer, or the fallback value if parsing fails
     */
    private static int parseIntOrDefault(String value, int fallback, String context) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            System.out.println(">> [WARN] Could not parse " + context + " from '" + value + "'. Using " + fallback + ".");
            return fallback;
        }
    }
}