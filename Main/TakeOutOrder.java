import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class TakeOutOrder implements OrderDAO {

    // A constant variable for our custom fee
    private static final double PACKAGING_FEE = 20.00;

    // --- OOP INTENT: POLYMORPHISM ---
    // This is the exact same method signature as DineInOrder, but it behaves differently!
    // This is Polymorphism in action: one interface, multiple behaviors.
    @Override
    public void checkout(MenuItem item, int quantityOrdered) {

        // 1. Calculate price with the extra packaging fee
        double subtotal = item.getPrice() * quantityOrdered;
        double totalAmount = subtotal + PACKAGING_FEE;

        System.out.println("--- TAKE-OUT RECEIPT ---");
        System.out.println("Item: " + item.getItemName());
        System.out.println("Subtotal: ₱" + subtotal);
        System.out.println("Packaging Fee: ₱" + PACKAGING_FEE);
        System.out.println("Total Paid: ₱" + totalAmount);

        // 2. Update the live database stock (Exact same SQL process)
        String sql = "UPDATE menu_items SET stock_quantity = stock_quantity - ? WHERE id = ?";

        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, quantityOrdered);
            pstmt.setInt(2, item.getId());

            pstmt.executeUpdate();
            System.out.println("[Database synced: Stock reduced for " + item.getItemName() + "]");

        } catch (SQLException e) {
            System.out.println("Transaction Failed: Could not update database.");
            System.out.println("Error details: " + e.getMessage());
        }
    }
}