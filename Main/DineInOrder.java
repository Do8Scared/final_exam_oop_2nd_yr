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

        // 1. Calculate the standard price
        double totalAmount = item.getPrice() * quantityOrdered;
        System.out.println("--- DINE-IN RECEIPT ---");
        System.out.println("Item: " + item.getItemName());
        System.out.println("Total Paid: ₱" + totalAmount);

        // 2. Update the live database stock
        // We use an UPDATE command to subtract the ordered amount from the cloud
        String sql = "UPDATE menu_items SET stock_quantity = stock_quantity - ? WHERE id = ?";

        // --- OOP INTENT: EXCEPTION HANDLING & RESOURCE CLEANUP (Module 5) ---
        // Using 'try-with-resources' automatically closes the connection, preventing memory leaks!
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, quantityOrdered);
            pstmt.setInt(2, item.getId()); // This uses the final ID we locked down in Phase 3!

            pstmt.executeUpdate();
            System.out.println("[Database synced: Stock reduced for " + item.getItemName() + "]");

        } catch (SQLException e) {
            System.out.println("Transaction Failed: Could not update database.");
            System.out.println("Error details: " + e.getMessage());
        }
    }
}