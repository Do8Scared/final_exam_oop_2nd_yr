package ui;

import database.AuditTrail;
import database.MenuDAO;
import database.TransactionDAO;
import Main.MenuManager;
import models.MenuItem;
import models.MenuItemFactory;

import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;

public class AdminDashboardFrame extends JFrame {
    private JPanel mainPanel;
    private CardLayout cardLayout;

    private JTable inventoryTable;
    private JTable transactionTable;
    private JTable auditTable;

    public AdminDashboardFrame() {
        setTitle("Admin Dashboard - Garahe Ni Mateicla");
        setSize(1000, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // Sidebar
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setPreferredSize(new Dimension(250, 0));

        JLabel titleLabel = new JLabel("Admin Panel");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 20));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        titleLabel.setBorder(BorderFactory.createEmptyBorder(30, 0, 40, 0));
        sidebar.add(titleLabel);

        JButton invBtn = createSidebarButton("📦 Manage Inventory");
        JButton txnBtn = createSidebarButton("💳 Transaction History");
        JButton auditBtn = createSidebarButton("🛡️ System Audits");
        JButton logoutBtn = createSidebarButton("⬅️ Logout");

        sidebar.add(invBtn);
        sidebar.add(Box.createRigidArea(new Dimension(0, 10)));
        sidebar.add(txnBtn);
        sidebar.add(Box.createRigidArea(new Dimension(0, 10)));
        sidebar.add(auditBtn);
        sidebar.add(Box.createVerticalGlue());
        sidebar.add(logoutBtn);
        sidebar.add(Box.createRigidArea(new Dimension(0, 20)));

        add(sidebar, BorderLayout.WEST);

        // Main Content (CardLayout)
        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);

        mainPanel.add(createInventoryPanel(), "Inventory");
        mainPanel.add(createTransactionPanel(), "Transactions");
        mainPanel.add(createAuditPanel(), "Audits");

        add(mainPanel, BorderLayout.CENTER);

        // Action Listeners
        invBtn.addActionListener(e -> { cardLayout.show(mainPanel, "Inventory"); loadInventoryData(); });
        txnBtn.addActionListener(e -> { cardLayout.show(mainPanel, "Transactions"); loadTransactionData(); });
        auditBtn.addActionListener(e -> { cardLayout.show(mainPanel, "Audits"); loadAuditData(); });
        logoutBtn.addActionListener(e -> {
            new RoleSelectionFrame().setVisible(true);
            dispose();
        });

        // Load initial data
        loadInventoryData();
    }

    private JButton createSidebarButton(String text) {
        JButton btn = new JButton(text);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setAlignmentX(Component.CENTER_ALIGNMENT);
        btn.setMaximumSize(new Dimension(250, 40));
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        
        return btn;
    }

    private JTextField adminSearchField;
    private JComboBox<String> adminSortCombo;

    // --- VIEW 1: INVENTORY ---
    private JPanel createInventoryPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        JPanel header = new JPanel(new BorderLayout());
        header.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Top Header: Add/Delete buttons
        JPanel actionBtns = new JPanel(new FlowLayout(FlowLayout.LEFT));

        JButton addBtn = new JButton("Add New Item");
        addBtn.setBackground(new Color(40, 167, 69));
        addBtn.setForeground(Color.WHITE);
        addBtn.setFocusPainted(false);
        addBtn.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
        addBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        JButton delBtn = new JButton("Delete Selected Item");
        delBtn.setBackground(new Color(220, 53, 69));
        delBtn.setForeground(Color.WHITE);
        delBtn.setFocusPainted(false);
        delBtn.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
        delBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        actionBtns.add(addBtn);
        actionBtns.add(delBtn);
        
        // Bottom Header: Search and Sort
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        
        adminSearchField = new JTextField("Search food...", 20);
        adminSearchField.setPreferredSize(new Dimension(200, 30));
        adminSearchField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent e) {
                if (adminSearchField.getText().equals("Search food...")) {
                    adminSearchField.setText("");
                }
            }
            public void focusLost(java.awt.event.FocusEvent e) {
                if (adminSearchField.getText().isEmpty()) {
                    adminSearchField.setText("Search food...");
                }
            }
        });
        adminSearchField.addActionListener(e -> loadInventoryData());

        adminSortCombo = new JComboBox<>(new String[]{"Sort by ID", "Sort by Name", "Sort by Price", "Sort by Quantity"});
        adminSortCombo.setPreferredSize(new Dimension(150, 30));
        adminSortCombo.addActionListener(e -> loadInventoryData());

        filterPanel.add(new JLabel("Search: "));
        filterPanel.add(adminSearchField);
        filterPanel.add(Box.createRigidArea(new Dimension(20, 0)));
        filterPanel.add(new JLabel("Sort: "));
        filterPanel.add(adminSortCombo);

        header.add(actionBtns, BorderLayout.NORTH);
        header.add(filterPanel, BorderLayout.SOUTH);

        panel.add(header, BorderLayout.NORTH);

        inventoryTable = new JTable();
        styleTable(inventoryTable);
        JScrollPane scroll = new JScrollPane(inventoryTable);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        panel.add(scroll, BorderLayout.CENTER);

        addBtn.addActionListener(e -> showAddItemDialog());
        delBtn.addActionListener(e -> deleteSelectedItem());

        return panel;
    }

    private void loadInventoryData() {
        String searchTerm = adminSearchField != null ? adminSearchField.getText() : "";
        String sortBy = adminSortCombo != null ? (String) adminSortCombo.getSelectedItem() : "Sort by ID";
        DefaultTableModel model = MenuDAO.getAllMenuItems(searchTerm, sortBy);
        if (inventoryTable != null) {
            inventoryTable.setModel(model);
        }
    }

    private void showAddItemDialog() {
        JDialog dialog = new JDialog(this, "Add New Item", true);
        dialog.setSize(400, 300);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout());

        JPanel form = new JPanel(new GridLayout(5, 2, 10, 15));
        form.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JTextField nameField = new JTextField();
        JTextField priceField = new JTextField();
        JTextField stockField = new JTextField();
        JComboBox<String> categoryCombo = new JComboBox<>(new String[]{"Appetizer", "Soup", "Rice Bowl", "Beverage", "Dessert", "Add-ons"});
        JTextField attrField = new JTextField();

        form.add(new JLabel("Name:")); form.add(nameField);
        form.add(new JLabel("Price:")); form.add(priceField);
        form.add(new JLabel("Stock:")); form.add(stockField);
        form.add(new JLabel("Category:")); form.add(categoryCombo);
        form.add(new JLabel("Special Attribute:")); form.add(attrField);

        dialog.add(form, BorderLayout.CENTER);

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton saveBtn = new JButton("Save");
        JButton cancelBtn = new JButton("Cancel");
        btns.add(saveBtn);
        btns.add(cancelBtn);
        dialog.add(btns, BorderLayout.SOUTH);

        cancelBtn.addActionListener(e -> dialog.dispose());
        saveBtn.addActionListener(e -> {
            try {
                String name = nameField.getText();
                double price = Double.parseDouble(priceField.getText());
                int stock = Integer.parseInt(stockField.getText());
                String category = categoryCombo.getSelectedItem().toString();
                String attr = attrField.getText();

                MenuItem item = MenuItemFactory.create(0, name, price, stock, category, attr);
                MenuManager.addMenuItem(item, attr, "admin");
                
                loadInventoryData();
                dialog.dispose();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dialog, "Invalid input! Price and stock must be numbers.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        dialog.setVisible(true);
    }

    private void deleteSelectedItem() {
        int row = inventoryTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Please select an item to delete.");
            return;
        }
        int id = (int) inventoryTable.getValueAt(row, 0);
        int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to delete item ID " + id + "?");
        if (confirm == JOptionPane.YES_OPTION) {
            if (MenuDAO.deleteItem(id)) {
                JOptionPane.showMessageDialog(this, "Item deleted successfully.");
                loadInventoryData();
            } else {
                JOptionPane.showMessageDialog(this, "Failed to delete item.");
            }
        }
    }

    // --- VIEW 2: TRANSACTIONS ---
    private JPanel createTransactionPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        transactionTable = new JTable();
        styleTable(transactionTable);
        JScrollPane scroll = new JScrollPane(transactionTable);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        panel.add(scroll, BorderLayout.CENTER);

        return panel;
    }

    private void loadTransactionData() {
        DefaultTableModel model = TransactionDAO.getTransactionHistoryTableModel();
        transactionTable.setModel(model);
    }

    // --- VIEW 3: AUDITS ---
    private JPanel createAuditPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        JPanel header = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        header.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JButton clearBtn = new JButton("Clear All Audits");
        clearBtn.setBackground(new Color(220, 53, 69));
        clearBtn.setForeground(Color.WHITE);
        clearBtn.setFocusPainted(false);
        clearBtn.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
        clearBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        header.add(clearBtn);
        panel.add(header, BorderLayout.NORTH);

        auditTable = new JTable();
        styleTable(auditTable);
        JScrollPane scroll = new JScrollPane(auditTable);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        panel.add(scroll, BorderLayout.CENTER);

        clearBtn.addActionListener(e -> {
            String pin = JOptionPane.showInputDialog(this, "Enter Admin PIN to confirm clearing audits:");
            if ("123456".equals(pin)) {
                if (AuditTrail.clearAllAudits()) {
                    JOptionPane.showMessageDialog(this, "Audits cleared successfully.");
                    loadAuditData();
                } else {
                    JOptionPane.showMessageDialog(this, "Failed to clear audits.");
                }
            } else if (pin != null) {
                JOptionPane.showMessageDialog(this, "Invalid PIN.", "Security Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        return panel;
    }

    private void loadAuditData() {
        DefaultTableModel model = AuditTrail.getAuditLogsTableModel();
        auditTable.setModel(model);
    }

    private void styleTable(JTable table) {
        // FlatLaf automatically applies modern rendering and row heights, so we leave this mostly blank.
        // We can add custom logic here if needed.
    }
}
