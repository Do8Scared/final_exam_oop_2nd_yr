package ui;

import database.DatabaseHelper;
import database.MenuDAO;
import models.CartItem;
import models.MenuItem;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class CustomerDashboardFrame extends JFrame {
    private JTable menuTable;
    private DefaultTableModel tableModel;
    private List<CartItem> cart;

    public CustomerDashboardFrame() {
        this(new ArrayList<>());
    }

    public CustomerDashboardFrame(List<CartItem> cart) {
        this.cart = cart;
        setTitle("Customer Dashboard - Live Menu");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        JLabel headerLabel = new JLabel("Live Menu", SwingConstants.CENTER);
        headerLabel.setFont(new Font("Arial", Font.BOLD, 24));
        headerLabel.setBorder(BorderFactory.createEmptyBorder(15, 10, 15, 10));
        add(headerLabel, BorderLayout.NORTH);

        tableModel = new DefaultTableModel(new String[]{"ID", "Item Name", "Price (PHP)", "Stock", "Category"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        menuTable = new JTable(tableModel);
        menuTable.setRowHeight(25);
        menuTable.getTableHeader().setFont(new Font("Arial", Font.BOLD, 14));
        menuTable.setFont(new Font("Arial", Font.PLAIN, 14));
        
        loadMenuData();
        
        JScrollPane scrollPane = new JScrollPane(menuTable);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        add(scrollPane, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 20, 10));

        JButton addBtn = new JButton("Add Selected to Cart");
        addBtn.setFont(new Font("Arial", Font.PLAIN, 14));
        addBtn.addActionListener(e -> handleAddToCart());

        JButton cartBtn = new JButton("View Cart / Checkout");
        cartBtn.setFont(new Font("Arial", Font.PLAIN, 14));
        cartBtn.addActionListener(e -> {
            new CartFrame(cart, this).setVisible(true);
            setVisible(false);
        });

        JButton backBtn = new JButton("Back to Portal");
        backBtn.setFont(new Font("Arial", Font.PLAIN, 14));
        backBtn.addActionListener(e -> {
            new RoleSelectionFrame().setVisible(true);
            dispose();
        });

        bottomPanel.add(addBtn);
        bottomPanel.add(cartBtn);
        bottomPanel.add(backBtn);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    public void loadMenuData() {
        tableModel.setRowCount(0);
        String sql = "SELECT id, item_name, price, stock_quantity, category FROM menu_items WHERE is_active = TRUE ORDER BY id ASC";
        try (Connection conn = DatabaseHelper.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                tableModel.addRow(new Object[]{
                    rs.getInt("id"),
                    rs.getString("item_name"),
                    String.format("%.2f", rs.getDouble("price")),
                    rs.getInt("stock_quantity"),
                    rs.getString("category")
                });
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error loading menu: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleAddToCart() {
        int selectedRow = menuTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select an item from the table first.", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int itemId = (int) tableModel.getValueAt(selectedRow, 0);
        String itemName = (String) tableModel.getValueAt(selectedRow, 1);

        String qtyStr = JOptionPane.showInputDialog(this, "Enter quantity for " + itemName + ":", "1");
        if (qtyStr == null || qtyStr.trim().isEmpty()) return;

        try {
            int quantity = Integer.parseInt(qtyStr.trim());
            if (quantity <= 0) {
                JOptionPane.showMessageDialog(this, "Quantity must be greater than zero.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            MenuItem freshItem = MenuDAO.fetchItemById(itemId);
            if (freshItem == null) {
                JOptionPane.showMessageDialog(this, "Item is no longer available.", "Error", JOptionPane.ERROR_MESSAGE);
                loadMenuData();
                return;
            }

            CartItem existing = null;
            for (CartItem c : cart) {
                if (c.getItem().getId() == itemId) {
                    existing = c;
                    break;
                }
            }

            int currentQty = existing != null ? existing.getQuantity() : 0;
            if (freshItem.getStockQuantity() < currentQty + quantity) {
                JOptionPane.showMessageDialog(this, "Insufficient stock. Cloud stock is " + freshItem.getStockQuantity() + ".", "Stock Error", JOptionPane.WARNING_MESSAGE);
                return;
            }

            if (existing != null) {
                existing.setQuantity(currentQty + quantity);
            } else {
                cart.add(new CartItem(freshItem, quantity));
            }
            JOptionPane.showMessageDialog(this, "Added to cart successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);

        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Invalid quantity entered.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
