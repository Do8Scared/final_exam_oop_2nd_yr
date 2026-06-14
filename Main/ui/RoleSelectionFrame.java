package ui;

import config.Dotenv;

import javax.swing.*;
import java.awt.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * The initial landing portal of the application.
 * Allows users to route into the system as either a Customer (open access)
 * or an Admin (PIN protected with brute-force prevention).
 */
public class RoleSelectionFrame extends JFrame {
    
    // Static lockout variables to match AuthService logic
    private static int failedAttempts = 0;
    private static long lockoutEndTime = 0;
    private static final int MAX_ATTEMPTS = 3;
    private static final long LOCKOUT_DURATION_MS = 30000;

    /**
     * Constructs the portal window.
     * Initializes the environment variables required for admin authentication.
     */
    public RoleSelectionFrame() {
        Dotenv.loadIfPresent();
        setTitle("Garahe Ni Mateicla - Portal");
        setSize(400, 300);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        JLabel titleLabel = new JLabel("Welcome to Garahe Ni Mateicla", SwingConstants.CENTER);
        titleLabel.setBorder(BorderFactory.createEmptyBorder(30, 10, 20, 10));
        add(titleLabel, BorderLayout.NORTH);

        JPanel buttonPanel = new JPanel(new GridLayout(2, 1, 10, 20));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 60, 50, 60));

        JButton customerBtn = new JButton("Enter as Customer");
        customerBtn.addActionListener(e -> {
            new CustomerDashboardFrame().setVisible(true);
            dispose();
        });

        JButton adminBtn = new JButton("Enter as Admin");
        adminBtn.addActionListener(e -> {
            if (authenticateAdmin()) {
                new AdminDashboardFrame().setVisible(true);
                dispose();
            }
        });

        buttonPanel.add(customerBtn);
        buttonPanel.add(adminBtn);
        add(buttonPanel, BorderLayout.CENTER);
    }

    /**
     * Handles the secure authentication flow for the Admin Dashboard.
     * Prevents brute-force attacks via a temporary lockout mechanism and validates
     * the entered PIN against the system's securely configured environment variables.
     *
     * @return true if the PIN is correct, false if incorrect or locked out
     */
    private boolean authenticateAdmin() {
        if (System.currentTimeMillis() < lockoutEndTime) {
            long remaining = (lockoutEndTime - System.currentTimeMillis()) / 1000;
            JOptionPane.showMessageDialog(this, 
                "System locked due to multiple failed attempts. Try again in " + remaining + " seconds.", 
                "Security Alert", JOptionPane.ERROR_MESSAGE);
            return false;
        }

        String configuredPin = System.getenv("POS_ADMIN_PIN");
        if (configuredPin == null || configuredPin.trim().isEmpty()) {
            configuredPin = System.getProperty("pos.admin.pin");
        }
        
        if (configuredPin == null || configuredPin.trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, 
                "Admin PIN is not configured. Access disabled.", 
                "Critical Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }

        JPasswordField pf = new JPasswordField();
        int okCxl = JOptionPane.showConfirmDialog(this, pf, "Enter 4-digit Admin PIN:", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (okCxl == JOptionPane.OK_OPTION) {
            String enteredPin = new String(pf.getPassword());
            if (constantTimeEquals(enteredPin, configuredPin)) {
                failedAttempts = 0;
                return true;
            } else {
                failedAttempts++;
                if (failedAttempts >= MAX_ATTEMPTS) {
                    lockoutEndTime = System.currentTimeMillis() + LOCKOUT_DURATION_MS;
                    JOptionPane.showMessageDialog(this, 
                        "Maximum attempts reached. Admin console locked for 30 seconds.", 
                        "Security Alert", JOptionPane.ERROR_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(this, "Invalid PIN.", "Security Alert", JOptionPane.ERROR_MESSAGE);
                }
            }
        }
        return false;
    }

    /**
     * Compares two strings securely in constant time to prevent timing attacks.
     *
     * @param a the first string
     * @param b the second string
     * @return true if the strings are identical
     */
    private boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) return a == b;
        byte[] aBytes = a.getBytes(StandardCharsets.UTF_8);
        byte[] bBytes = b.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(aBytes, bBytes);
    }
}
