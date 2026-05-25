package Main;

import database.DatabaseHelper;
import models.*;
import orders.DineInOrder;
import orders.OrderDAO;
import orders.TakeOutOrder;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList; // NEW: Required for our Cart System
import java.util.Scanner;

public class Main {

    private static final Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        boolean isRunning = true;

        System.out.println("=========================================");
        System.out.println("   WELCOME TO GARAHE NI MATEICLA (POS)   ");
        System.out.println("=========================================");

        // --- OOP INTENT: THE SYSTEM LOOP ---
        while (isRunning) {
            System.out.println("\n--- MAIN MENU ---");
            System.out.println("[1] Start New Transaction");
            System.out.println("[2] View Full Live Menu");
            System.out.println("[3] View Menu by Category");
            System.out.println("[4] Admin: Add New Menu Item");
            System.out.println("[5] Exit System");
            System.out.print("Select an option: ");

            int choice = scanner.nextInt();

            switch (choice) {
                case 1:
                    startNewOrder();
                    break;
                case 2:
                    viewLiveMenu();
                    break;
                case 3:
                    handleCategoryFilter();
                    break;
                case 4:
                    handleAdminAddNewItem();
                    break;
                case 5:
                    isRunning = false;
                    System.out.println("Shutting down the POS terminal. Goodbye!");
                    break;
                default:
                    System.out.println("Invalid selection. Please try again.");
            }
        }
        scanner.close();
    }


// --- POS ORDER FLOW (Quick Service Restaurant Style) ---
private static void startNewOrder() {
    System.out.println("\n=========================================");
    System.out.println("          STARTING NEW ORDER             ");
    System.out.println("=========================================");
    System.out.println("[1] Dine-In");
    System.out.println("[2] Take-Out");
    System.out.println("[3] Cancel (Back to Main Menu)");
    System.out.print("Select Order Type: ");

    int typeChoice = scanner.nextInt();
    OrderDAO currentOrderType = null;

    // Establish Polymorphic behavior based on upfront selection
    if (typeChoice == 1) {
        currentOrderType = new DineInOrder();
        System.out.println(">> Order Type Set: DINE-IN");
    } else if (typeChoice == 2) {
        currentOrderType = new TakeOutOrder();
        System.out.println(">> Order Type Set: TAKE-OUT");
    } else if (typeChoice == 3) {
        return; // Exits safely back to the Main Menu
    } else {
        System.out.println("Invalid choice. Returning to main menu.");
        return;
    }

    // We use ArrayLists to temporarily hold the items in memory
    ArrayList<MenuItem> cartItems = new ArrayList<>();
    ArrayList<Integer> cartQuantities = new ArrayList<>();

    boolean isOrdering = true;

    // --- THE CASHIER LOOP ---
    while (isOrdering) {
        System.out.println("\n--- CURRENT CART: " + cartItems.size() + " Items ---");
        System.out.println("[1] Add Item to Cart");
        System.out.println("[2] View Items in Cart");
        System.out.println("[3] Proceed to Checkout & Pay");
        System.out.println("[4] Cancel Order (Discard Cart)");
        System.out.print("Cashier Action: ");

        int action = scanner.nextInt();

        switch (action) {
            // --- NEW OPTION 1: SEAMLESS BROWSE & ADD FLOW ---
            case 1:
                System.out.println("\n--- SELECT CATEGORY TO BROWSE ---");
                System.out.println("[1] Appetizer");
                System.out.println("[2] Soup");
                System.out.println("[3] Rice Bowl");
                System.out.println("[4] Dessert");
                System.out.println("[5] Beverages");
                System.out.println("[6] Add-Ons");
                System.out.println("[7] Show All Items");
                System.out.print("Choose category: ");

                int catChoice = scanner.nextInt();
                String selectedCategory = "";

                switch (catChoice) {
                    case 1: selectedCategory = "Appetizer"; break;
                    case 2: selectedCategory = "Soup"; break;
                    case 3: selectedCategory = "Rice Bowl"; break;
                    case 4: selectedCategory = "Dessert"; break;
                    case 5: selectedCategory = "Beverages"; break;
                    case 6: selectedCategory = "Add-Ons"; break;
                    case 7: selectedCategory = "ALL"; break;
                    default:
                        System.out.println("Invalid choice. Returning to cart menu.");
                        continue; // Safely skips the rest of Case 1 and restarts the loop
                }

                // Display the requested menu
                if (selectedCategory.equals("ALL")) {
                    viewLiveMenu();
                } else {
                    viewMenuByCategory(selectedCategory);
                }

                // --- UX UPDATE: THE ESCAPE HATCH ---
                System.out.print("\nEnter Item ID to add (or type 0 to go back): ");
                int itemId = scanner.nextInt();

                // If they type 0, we immediately abort the add process
                if (itemId == 0) {
                    System.out.println(">> Canceling item selection. Returning to cart...");
                    continue;
                }

                System.out.print("Enter Quantity: ");
                int qty = scanner.nextInt();

                MenuItem selectedItem = fetchItemById(itemId);

                if (selectedItem != null) {
                    if (selectedItem.getStockQuantity() >= qty) {
                        cartItems.add(selectedItem);
                        cartQuantities.add(qty);
                        System.out.println(">> SUCCESS: Added " + qty + "x " + selectedItem.getItemName() + " to cart!");
                    } else {
                        System.out.println(">> ERROR: Insufficient stock! Only " + selectedItem.getStockQuantity() + " left.");
                    }
                } else {
                    System.out.println(">> ERROR: Item ID not found in the database.");
                }
                break;

            // --- VIEW CART ---
            case 2:
                if (cartItems.isEmpty()) {
                    System.out.println("\n>> The cart is currently empty.");
                } else {
                    System.out.println("\n--- CART CONTENTS ---");
                    for (int i = 0; i < cartItems.size(); i++) {
                        double lineTotal = cartItems.get(i).getPrice() * cartQuantities.get(i);
                        System.out.printf("%dx %-20s | ₱%.2f\n",
                                cartQuantities.get(i),
                                cartItems.get(i).getItemName(),
                                lineTotal);
                    }
                }
                break;

            // --- CHECKOUT CONFIRMATION ---
            case 3:
                if (cartItems.isEmpty()) {
                    System.out.println(">> ERROR: The cart is empty. Please add items first.");
                } else {
                    System.out.println("\n=========================================");
                    System.out.println("          ORDER SUMMARY PREVIEW          ");
                    System.out.println("=========================================");

                    double grandTotal = 0;

                    for (int i = 0; i < cartItems.size(); i++) {
                        double itemTotal = cartItems.get(i).getPrice() * cartQuantities.get(i);
                        System.out.printf("%dx %-20s : ₱%.2f\n", cartQuantities.get(i), cartItems.get(i).getItemName(), itemTotal);
                        grandTotal += itemTotal;
                    }

                    if (typeChoice == 2) {
                        System.out.println("Packaging Fee          : ₱20.00");
                        grandTotal += 20.00;
                    }

                    System.out.println("-----------------------------------------");
                    System.out.printf("GRAND TOTAL            : ₱%.2f\n", grandTotal);
                    System.out.println("=========================================");

                    System.out.println("\n[1] Confirm & Process Payment");
                    System.out.println("[2] Go Back to Cart");
                    System.out.print("Action: ");

                    int confirmAction = scanner.nextInt();

                    if (confirmAction == 1) {
                        System.out.println("\n>> PRINTING OFFICIAL RECEIPTS & UPDATING CLOUD...");

                        for (int i = 0; i < cartItems.size(); i++) {
                            currentOrderType.checkout(cartItems.get(i), cartQuantities.get(i));
                        }

                        System.out.println("\n>> TRANSACTION COMPLETE! Cloud Database Updated.");
                        isOrdering = false;
                    } else {
                        System.out.println("\n>> Returning to cart...");
                    }
                }
                break;

            // --- CANCEL ORDER ---
            case 4:
                System.out.println(">> Order Cancelled. Cart deleted. Returning to Main Menu...");
                isOrdering = false;
                break;

            default:
                System.out.println(">> Invalid action. Please try again.");
        }
    }
}
    // --- UI HELPER METHODS ---
    private static void handleCategoryFilter() {
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

        switch (catChoice) {
            case 1: selectedCategory = "Appetizer"; break;
            case 2: selectedCategory = "Soup"; break;
            case 3: selectedCategory = "Rice Bowl"; break;
            case 4: selectedCategory = "Dessert"; break;
            case 5: selectedCategory = "Beverages"; break;
            case 6: selectedCategory = "Add-Ons"; break;
            default:
                System.out.println("Invalid category choice. Returning to main menu.");
                return;
        }
        viewMenuByCategory(selectedCategory);
    }

    private static void handleAdminAddNewItem() {
        System.out.println("\n--- ADD NEW MENU ITEM ---");
        scanner.nextLine(); // Clear buffer

        System.out.print("Enter Item Name: ");
        String newName = scanner.nextLine();

        System.out.print("Enter Price: ");
        double newPrice = scanner.nextDouble();

        System.out.print("Enter Stock Quantity: ");
        int newStock = scanner.nextInt();

        scanner.nextLine(); // Clear buffer
        System.out.print("Enter Category (e.g., Beverages, Dessert, Rice Bowl): ");
        String newCategory = scanner.nextLine();

        MenuItem newItem = new MenuItem(newName, newPrice, newStock, newCategory);
        MenuManager.addMenuItem(newItem);
    }

    // --- DATABASE QUERY METHODS ---
    private static void viewLiveMenu() {
        System.out.println("\n--- LIVE CLOUD MENU ---");
        String sql = "SELECT * FROM menu_items ORDER BY id ASC";
        executeSelectQuery(sql, null);
    }

    private static void viewMenuByCategory(String categoryFilter) {
        System.out.println("\n--- LIVE CLOUD MENU: " + categoryFilter.toUpperCase() + " ---");
        String sql = "SELECT * FROM menu_items WHERE category = ? ORDER BY id ASC";
        executeSelectQuery(sql, categoryFilter);
    }

    // --- OOP INTENT: DRY PRINCIPLE (Don't Repeat Yourself) ---
    // This helper method runs all SELECT statements to clean up the code.
    private static void executeSelectQuery(String sql, String parameter) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            conn = DatabaseHelper.getConnection();
            pstmt = conn.prepareStatement(sql);

            if (parameter != null) {
                pstmt.setString(1, parameter);
            }

            rs = pstmt.executeQuery();

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

    // Helper method to retrieve a specific item and turn it into a Java Object
    private static MenuItem fetchItemById(int id) {
        String sql = "SELECT * FROM menu_items WHERE id = ?";
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

                    // --- OOP INTENT: POLYMORPHISM & UPCASTING ---
                    // Every menu item category is successfully mapped to its specialized child class!
                    if (category.equalsIgnoreCase("Beverages")) {
                        return new Beverage(fetchedId, name, price, stock, category, 500);

                    } else if (category.equalsIgnoreCase("Appetizer")) {
                        return new Appetizer(fetchedId, name, price, stock, category, 4);

                    } else if (category.equalsIgnoreCase("Dessert")) {
                        return new Dessert(fetchedId, name, price, stock);

                    } else if (category.equalsIgnoreCase("Soup")) {
                        // Passing 'false' as default for isSpicy
                        return new Soup(fetchedId, name, price, stock, category, false);

                    } else if (category.equalsIgnoreCase("Rice Bowl")) {
                        // Passing "Assorted" as a default protein
                        return new RiceBowl(fetchedId, name, price, stock, category, "Assorted");

                    } else if (category.equalsIgnoreCase("Add-Ons") || category.equalsIgnoreCase("Add-On")) {
                        // Passing 'false' as default for isCondiment
                        return new AddOn(fetchedId, name, price, stock, category, false);

                    } else {
                        // Strict fallback for any untracked categories
                        return new MenuItem(fetchedId, name, price, stock, category);
                    }
                }
            }
        } catch (SQLException e) {
            System.out.println("Error fetching item: " + e.getMessage());
        }
        return null;
    }
}