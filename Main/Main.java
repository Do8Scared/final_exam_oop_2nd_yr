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
        System.out.println("\n[1] View Live Menu");
        System.out.println("[2] Process Dine-In Order");
        System.out.println("[3] Process Take-Out Order");
        System.out.println("[4] Exit System");


        // --- OOP INTENT: THE SYSTEM LOOP ---
        while (isRunning) {

            System.out.print("\nSelect an option: ");

            int choice = scanner.nextInt();

            switch (choice) {
                case 1:
                    viewLiveMenu();
                    break;
                case 2:
                    processOrder(new DineInOrder()); // Passing the specific behavior
                    break;
                case 3:
                    processOrder(new TakeOutOrder()); // Passing the specific behavior
                    break;
                case 4:
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

    // --- OOP INTENT: POLYMORPHISM IN ACTION (Module 4) ---
    // Notice the parameter here is the INTERFACE (OrderDAO). 
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