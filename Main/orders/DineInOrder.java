package orders;

import database.DatabaseHelper;
import models.*;


import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

// --- OOP INTENT: IMPLEMENTING ABSTRACTION ---
public class DineInOrder implements OrderDAO {

    // --- OOP INTENT: POLYMORPHISM (Module 4) ---
    // The @Override annotation proves we are taking the abstract method 
    // from the interface and giving it a specific "Dine-In" form.
    @Override
    public void checkout(MenuItem item, int quantityOrdered) {

        double totalAmount = item.getPrice() * quantityOrdered;
        System.out.println("\n--- DINE-IN RECEIPT ---");
        System.out.println("Item: " + item.getItemName() + " (x" + quantityOrdered + ")");

        // --- OOP INTENT: INSTANCEOF & DOWNCASTING (Proving Inheritance) ---
        // We check what specific child class this item secretly is,
        // then we unlock and print its unique attributes!
        if (item instanceof Beverage) {
            Beverage bev = (Beverage) item;
            System.out.println("  -> [Volume: " + bev.getVolumeInMl() + "ml]");

        } else if (item instanceof Appetizer) {
            Appetizer app = (Appetizer) item;
            System.out.println("  -> [Serving: " + app.getPiecesCount() + " pieces]");

        } else if (item instanceof Soup) {
            Soup soup = (Soup) item;
            System.out.println("  -> [Preparation: " + (soup.isSpicy() ? "Spicy" : "Standard Non-Spicy") + "]");

        } else if (item instanceof RiceBowl) {
            RiceBowl bowl = (RiceBowl) item;
            System.out.println("  -> [Protein: " + bowl.getMainProtein() + "]");

        } else if (item instanceof AddOn) {
            AddOn addon = (AddOn) item;
            System.out.println("  -> [Type: " + (addon.isCondiment() ? "Condiment/Sauce" : "Solid Food") + "]");
        }

        System.out.println("Total Paid: ₱" + String.format("%.2f", totalAmount));
        System.out.println("-----------------------");

        // Update the live database stock
        String sql = "UPDATE menu_items SET stock_quantity = stock_quantity - ? WHERE id = ?";

        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, quantityOrdered);
            pstmt.setInt(2, item.getId());
            pstmt.executeUpdate();

        } catch (SQLException e) {
            System.out.println("Transaction Failed: Could not update database.");
            System.out.println("Error details: " + e.getMessage());
        }
    }
}