package Main;

import database.DatabaseHelper;
import models.MenuItem;
import orders.DineInOrder;
import orders.OrderDAO;
import orders.TakeOutOrder;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Scanner;

public class Main {

    private static final Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        boolean isRunning = true;

        System.out.println("=========================================");
        System.out.println("   WELCOME TO GARAHE NI MATEICLA (POS)   ");
        System.out.println("=========================================");
        System.out.println("\n[1] View Full Live Menu");
        System.out.println("[2] View Menu by Category");
        System.out.println("[3] Process Dine-In Order");
        System.out.println("[4] Process Take-Out Order");
        System.out.println("[5] Admin: Add New Menu Item");
        System.out.println("[6] Exit System");


        // --- OOP INTENT: THE SYSTEM LOOP ---
        while (isRunning) {

            System.out.print("\nSelect an option: ");

            int choice = scanner.nextInt();

            switch (choice) {
                case 1:
                    viewLiveMenu();
                    break;
                case 2:
                    System.out.println("\n--- SELECT CATEGORY ---");
                    System.out.println("[1] Appetizer");
                    System.out.println("[2] Soup");
                    System.out.println("[3] Rice Bowl");
                    System.out.println("[4] Dessert");
                    System.out.println("[5] Beverages");
                    System.out.println("[6] Add-Ons");
                    System.out.print("Choose a category to filter: ");

                    int catChoice = scanner.nextInt();
                    String selectedCategory = "";

                    // Map the number to the exact spelling in your database
                    switch (catChoice) {
                        case 1: selectedCategory = "Appetizer"; break;
                        case 2: selectedCategory = "Soup"; break;
                        case 3: selectedCategory = "Rice Bowl"; break; // or "Rice Bowls" depending on what you saved to Supabase
                        case 4: selectedCategory = "Dessert"; break;
                        case 5: selectedCategory = "Beverage"; break;
                        case 6: selectedCategory = "Add-Ons"; break;
                        default:
                            System.out.println("Invalid category choice. Returning to main menu.");
                            break;
                    }

                    // If a valid category was selected, pass it to the filter method
                    if (!selectedCategory.isEmpty()) {
                        viewMenuByCategory(selectedCategory);
                    }
                    break;
                case 3:
                    processOrder(new DineInOrder()); // Passing the specific behavior
                    break;
                case 4:
                    processOrder(new TakeOutOrder()); // Passing the specific behavior
                    break;
                case 5:
                    System.out.println("\n--- ADD NEW MENU ITEM ---");
                    // Clear the scanner buffer
                    scanner.nextLine();

                    System.out.print("Enter Item Name: ");
                    String newName = scanner.nextLine();

                    System.out.print("Enter Price: ");
                    double newPrice = scanner.nextDouble();

                    System.out.print("Enter Stock Quantity: ");
                    int newStock = scanner.nextInt();

                    scanner.nextLine(); // Clear buffer again
                    System.out.print("Enter Category (e.g., Beverages, Sushi, Ramen): ");
                    String newCategory = scanner.nextLine();

                    // Create the object using our overloaded constructor (the one without an ID)
                    MenuItem newItem = new MenuItem(newName, newPrice, newStock, newCategory);

                    // Pass the object to our MenuManager to handle the cloud upload
                    MenuManager.addMenuItem(newItem);
                    break;
                case 6:
                    isRunning = false;
                    System.out.println("Shutting down the system. Goodbye!");
                    break;
                default:
                    System.out.println("Invalid selection. Please try again.");
            }
        }
        scanner.close();
    }

    // --- OOP INTENT: DYNAMIC QUERIES & EXCEPTION HANDLING (Module 5) ---
    private static void viewLiveMenu() {
        System.out.println("\n--- LIVE CLOUD MENU ---");
        String sql = "SELECT * FROM menu_items ORDER BY id ASC";

        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            conn = DatabaseHelper.getConnection();
            pstmt = conn.prepareStatement(sql);
            rs = pstmt.executeQuery(); // Executes the SELECT command

            System.out.printf("%-5s %-25s %-10s %-10s %-15s\n", "ID", "Item Name", "Price", "Stock", "Category");
            System.out.println("----------------------------------------------------------------------");

            // Loop through the live Supabase data
            while (rs.next()) {
                System.out.printf("%-5d %-25s ₱%-9.2f %-10d %-15s\n",
                        rs.getInt("id"),
                        rs.getString("item_name"),
                        rs.getDouble("price"),
                        rs.getInt("stock_quantity"),
                        rs.getString("category"));
            }

        } catch (SQLException e) {
            System.out.println("Database Error: Could not fetch the menu.");
            System.out.println(e.getMessage());

            // --- OOP INTENT: RESOURCE CLEANUP (Module 5 Grading Requirement) ---
            // This finally block guarantees that even if the internet drops mid-query,
            // the memory objects are safely closed to prevent memory leaks.
        } finally {
            try {
                if (rs != null) rs.close();
                if (pstmt != null) pstmt.close();
                if (conn != null) conn.close();
            } catch (SQLException ex) {
                System.out.println("Error closing database resources.");
            }
        }
    }

    // --- OOP INTENT: DYNAMIC FILTERING ---
    // This method overloads the concept of viewing the menu by accepting a specific parameter.
    private static void viewMenuByCategory(String categoryFilter) {
        System.out.println("\n--- LIVE CLOUD MENU: " + categoryFilter.toUpperCase() + " ---");

        // The SQL command uses the WHERE clause to pull only matching rows
        String sql = "SELECT * FROM menu_items WHERE category = ? ORDER BY id ASC";

        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            conn = DatabaseHelper.getConnection();
            pstmt = conn.prepareStatement(sql);

            // We safely inject the requested category (e.g., "Beverages") into the '?' placeholder
            pstmt.setString(1, categoryFilter);

            rs = pstmt.executeQuery();

            System.out.printf("%-5s %-25s %-10s %-10s %-15s\n", "ID", "Item Name", "Price", "Stock", "Category");
            System.out.println("----------------------------------------------------------------------");

            boolean foundItems = false;

            // Loop through the filtered Supabase data
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
                System.out.println("No items found in the '" + categoryFilter + "' category.");
            }

        } catch (SQLException e) {
            System.out.println("Database Error: Could not fetch the menu.");
            System.out.println(e.getMessage());
        } finally {
            try {
                if (rs != null) rs.close();
                if (pstmt != null) pstmt.close();
                if (conn != null) conn.close();
            } catch (SQLException ex) {
                System.out.println("Error closing database resources.");
            }
        }
    }

    // --- OOP INTENT: POLYMORPHISM IN ACTION (Module 4) ---
    // Notice the parameter here is the INTERFACE (orders.OrderDAO).
    // This method doesn't care if it's Take-Out or Dine-In; it just knows how to call checkout().
    private static void processOrder(OrderDAO orderType) {
        System.out.print("\nEnter the ID of the item to order: ");
        int itemId = scanner.nextInt();
        System.out.print("Enter quantity: ");
        int qty = scanner.nextInt();

        // 1. Fetch the selected item from the database
        MenuItem selectedItem = fetchItemById(itemId);

        if (selectedItem != null) {
            if (selectedItem.getStockQuantity() >= qty) {
                // 2. Execute the polymorphic checkout method!
                orderType.checkout(selectedItem, qty);
            } else {
                System.out.println("Error: Insufficient stock! Only " + selectedItem.getStockQuantity() + " left.");
            }
        } else {
            System.out.println("Error: Item ID not found in the database.");
        }
    }

    // Helper method to retrieve a specific item and turn it into a Java Object
    private static MenuItem fetchItemById(int id) {
        String sql = "SELECT * FROM menu_items WHERE id = ?";

        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    // We use our Phase 3 Encapsulated Constructor here!
                    return new MenuItem(
                            rs.getInt("id"),
                            rs.getString("item_name"),
                            rs.getDouble("price"),
                            rs.getInt("stock_quantity"),
                            rs.getString("category")
                    );
                }
            }
        } catch (SQLException e) {
            System.out.println("Error fetching item: " + e.getMessage());
        }
        return null;
    }
}