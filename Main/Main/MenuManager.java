package Main;
import models.MenuItem;
import database.DatabaseHelper;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class MenuManager {

    // --- OOP INTENT: SEPARATION OF CONCERNS ---
    // This class is dedicated solely to adding new items to the cloud database.
    // By keeping this logic out of Main.java and MenuItem.java, our code remains modular and clean.

    public static void addMenuItem(MenuItem item) {

        // The SQL command. The '?' symbols are secure placeholders.
        // Notice we do NOT insert the 'id' column because Supabase generates it automatically!
        String sql = "INSERT INTO menu_items (item_name, price, stock_quantity, category) VALUES (?, ?, ?, ?)";

        // --- OOP INTENT: EXCEPTION HANDLING (Module 5) ---
        // Using 'try-with-resources' automatically closes the database connection and statement
        // when the operation is done, saving us from writing a massive 'finally' block!
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            // We use the Encapsulated Getters from Phase 3 to extract the object's data
            pstmt.setString(1, item.getItemName());
            pstmt.setDouble(2, item.getPrice());
            pstmt.setInt(3, item.getStockQuantity());
            pstmt.setString(4, item.getCategory());

            // Execute the command to push data to Supabase
            int rowsInserted = pstmt.executeUpdate();

            if (rowsInserted > 0) {
                System.out.println("\n[SYSTEM] Success! " + item.getItemName() + " was added to the live database.");
            }

        } catch (SQLException e) {
            System.out.println("\n[SYSTEM ERROR] Could not add the menu item to the database.");
            System.out.println("Error details: " + e.getMessage());
        }
    }
}
