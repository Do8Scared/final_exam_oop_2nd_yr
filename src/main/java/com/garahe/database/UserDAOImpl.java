package com.garahe.database;

import com.garahe.models.User;
import java.sql.*;

/**
 * Implementation of the UserDAO interface.
 * 
 * Part of the Data Access Object (DAO) pattern. This class is responsible for
 * directly interacting with the Supabase PostgreSQL database. It abstracts away
 * the complexity of JDBC connections, PreparedStatement mapping, and SQL execution.
 */
public class UserDAOImpl implements UserDAO {
    
    /**
     * Inserts a new user into the database.
     * 
     * @param user The User object containing registration details.
     * @return true if the database row was successfully inserted, false otherwise.
     */
    @Override
    public boolean registerUser(User user) {
        String sql = "INSERT INTO users (name, email, password) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, user.getName());
            pstmt.setString(2, user.getEmail());
            pstmt.setString(3, user.getPassword()); // For academic purposes, plaintext
            
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("Database Error during registration: " + e.getMessage());
            return false;
        }
    }

    /**
     * Verifies the user credentials against the database.
     * 
     * @param email The user's email address.
     * @param password The user's plaintext password.
     * @return A User object if the credentials match, or null if login fails.
     */
    @Override
    public User authenticateUser(String email, String password) {
        String sql = "SELECT id, name, email, password FROM users WHERE email = ?";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, email);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    String storedPassword = rs.getString("password");
                    if (storedPassword.equals(password)) {
                        return new User(
                            rs.getInt("id"),
                            rs.getString("name"),
                            rs.getString("email"),
                            storedPassword
                        );
                    }
                }
            }
        } catch (SQLException e) {
            System.out.println("Database Error during login: " + e.getMessage());
        }
        return null;
    }

    /**
     * Retrieves a user from the database strictly by their email.
     * Used for mapping relationships (like fetching user_id for a transaction).
     * 
     * @param email The user's email address.
     * @return The User object if found, null otherwise.
     */
    @Override
    public User getUserByEmail(String email) {
        String sql = "SELECT id, name, email, password FROM users WHERE email = ?";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, email);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new User(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("email"),
                        rs.getString("password")
                    );
                }
            }
        } catch (SQLException e) {
            System.out.println("Database Error fetching user by email: " + e.getMessage());
        }
        return null;
    }
}
