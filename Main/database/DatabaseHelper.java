package database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseHelper {

    // --- ENCAPSULATION--
    // We store our database credentials as private constants.
    // This ensures no outside classes can accidentally modify our cloud connection details.
    // We are using the Supabase Session Pooler (port 6543) to ensure connection compatibility across different Wi-Fi networks.
    private static final String URL = "jdbc:postgresql://aws-1-ap-southeast-1.pooler.supabase.com:6543/postgres";
    private static final String USER = "postgres.cbifpatnpbfudrpchkjg";
    private static final String PASSWORD = "arboladorasadangtoledopiolgagap";

    // --- OOP CONCEPT: EXCEPTION HANDLING (The 'throws' keyword) ---
    // Connecting to a database is a risky operation (the internet might be down).
    // By adding 'throws SQLException' to the method signature, we are propagating the error.
    // This forces any other part of our system that calls this method to handle potential failures safely.
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    // --- OOP CONCEPT: EXCEPTION HANDLING (try-catch-finally block) ---
    // This main method is a standalone test to verify our cloud connection before launching the main system.
    public static void main(String[] args) {
        Connection conn = null;

        // TRY BLOCK: We place the risky code here. We attempt to establish the connection to the cloud.
        try {
            System.out.println("Attempting to connect to the cloud...");
            conn = getConnection();
            System.out.println("Success! Connected to the OOP PROJECT database.");

            // CATCH BLOCK: If the connection fails (e.g., wrong password, no internet), the program jumps here.
            // Instead of crashing abruptly, we catch the checked SQLException and display a user-friendly error message.
        } catch (SQLException e) {
            System.out.println("Connection Failed! Check your URL, username, or password.");
            System.out.println("Error details: " + e.getMessage());

            // FINALLY BLOCK: This block executes regardless of whether the try block succeeded or failed.
            // Best Practice: We use this specifically for resource cleanup to close the connection and prevent memory leaks.
        } finally {
            try {
                if (conn != null) {
                    conn.close();
                    System.out.println("Connection safely closed.");
                }
            } catch (SQLException ex) {
                System.out.println("Error closing connection.");
            }
        }
    }
}