package orders;

import database.DatabaseHelper;
import models.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class TakeOutOrder implements OrderDAO {

    // A constant variable for our custom fee
    private static final double PACKAGING_FEE = 20.00;

    // --- OOP INTENT: POLYMORPHISM ---
    // This is the exact same method signature as orders.DineInOrder, but it behaves differently!
    // This is Polymorphism in action: one interface, multiple behaviors.
    @Override
    public void checkout(MenuItem item, int quantityOrdered) {

        double subtotal = item.getPrice() * quantityOrdered;
        double totalAmount = subtotal + PACKAGING_FEE;

        System.out.println("\n--- TAKE-OUT RECEIPT ---");
        System.out.println("Item: " + item.getItemName() + " (x" + quantityOrdered + ")");

        // --- OOP INTENT: INSTANCEOF & DOWNCASTING ---
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

        System.out.println("Subtotal: ₱" + String.format("%.2f", subtotal));
        System.out.println("Packaging Fee: ₱" + String.format("%.2f", PACKAGING_FEE));
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