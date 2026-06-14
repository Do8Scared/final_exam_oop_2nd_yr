package ui;

import Main.MenuManager;
import models.MenuItem;
import database.MenuDAO;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class MerchantDashboardFrame extends JFrame {
    private JTextField nameField;
    private JTextField priceField;
    private JTextField stockField;
    private JComboBox<String> categoryCombo;
    private JTextField newCategoryField;
    private JTextField specialAttrField;

    public MerchantDashboardFrame() {
        setTitle("Merchant Dashboard - Add New Item");
        setSize(500, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        JLabel headerLabel = new JLabel("Add New Menu Item", SwingConstants.CENTER);
        headerLabel.setFont(new Font("Arial", Font.BOLD, 22));
        headerLabel.setBorder(BorderFactory.createEmptyBorder(20, 10, 20, 10));
        add(headerLabel, BorderLayout.NORTH);

        JPanel formPanel = new JPanel(new GridLayout(6, 2, 10, 15));
        formPanel.setBorder(BorderFactory.createEmptyBorder(10, 40, 20, 40));

        formPanel.add(new JLabel("Item Name:"));
        nameField = new JTextField();
        formPanel.add(nameField);

        formPanel.add(new JLabel("Price (PHP):"));
        priceField = new JTextField();
        formPanel.add(priceField);

        formPanel.add(new JLabel("Stock Quantity:"));
        stockField = new JTextField();
        formPanel.add(stockField);

        formPanel.add(new JLabel("Category:"));
        JPanel categoryPanel = new JPanel(new BorderLayout(5, 0));
        List<String> activeCategories = MenuDAO.getActiveCategories();
        activeCategories.add("--- Create New ---");
        categoryCombo = new JComboBox<>(activeCategories.toArray(new String[0]));
        newCategoryField = new JTextField();
        newCategoryField.setEnabled(false);
        
        categoryCombo.addActionListener(e -> {
            boolean isNew = "--- Create New ---".equals(categoryCombo.getSelectedItem());
            newCategoryField.setEnabled(isNew);
        });

        categoryPanel.add(categoryCombo, BorderLayout.NORTH);
        categoryPanel.add(newCategoryField, BorderLayout.SOUTH);
        formPanel.add(categoryPanel);

        formPanel.add(new JLabel("Special Attribute:"));
        specialAttrField = new JTextField();
        specialAttrField.setToolTipText("E.g., Spice Level, Size, Volume. Default: Standard");
        formPanel.add(specialAttrField);

        add(formPanel, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 20, 10));

        JButton addBtn = new JButton("Add Item");
        addBtn.setFont(new Font("Arial", Font.BOLD, 14));
        addBtn.setBackground(new Color(70, 130, 180));
        addBtn.setForeground(Color.WHITE);
        addBtn.addActionListener(e -> handleAddItem());

        JButton backBtn = new JButton("Back to Portal");
        backBtn.setFont(new Font("Arial", Font.PLAIN, 14));
        backBtn.addActionListener(e -> {
            new RoleSelectionFrame().setVisible(true);
            dispose();
        });

        bottomPanel.add(addBtn);
        bottomPanel.add(backBtn);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    private void handleAddItem() {
        String name = nameField.getText().trim();
        String priceStr = priceField.getText().trim();
        String stockStr = stockField.getText().trim();
        String category = categoryCombo.getSelectedItem().toString();
        
        if ("--- Create New ---".equals(category)) {
            category = newCategoryField.getText().trim();
        }
        
        String specialAttr = specialAttrField.getText().trim();
        if (specialAttr.isEmpty()) {
            specialAttr = "Standard";
        }

        if (name.isEmpty() || priceStr.isEmpty() || stockStr.isEmpty() || category.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please fill in all required fields.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            double price = Double.parseDouble(priceStr);
            int stock = Integer.parseInt(stockStr);

            if (price < 0 || stock < 0) {
                JOptionPane.showMessageDialog(this, "Price and Stock must be non-negative.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            MenuItem newItem = new MenuItem(name, price, stock, category);
            MenuManager.addMenuItem(newItem, specialAttr, "admin_gui");

            JOptionPane.showMessageDialog(this, "Item successfully added to the live database!", "Success", JOptionPane.INFORMATION_MESSAGE);
            
            // Clear fields
            nameField.setText("");
            priceField.setText("");
            stockField.setText("");
            newCategoryField.setText("");
            specialAttrField.setText("");
            
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Price and Stock must be valid numbers.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
