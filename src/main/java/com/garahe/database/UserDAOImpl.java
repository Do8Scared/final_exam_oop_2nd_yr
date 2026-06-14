package com.garahe.database;

import com.garahe.models.User;
import java.sql.*;

public class UserDAOImpl implements UserDAO {
    
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
}
