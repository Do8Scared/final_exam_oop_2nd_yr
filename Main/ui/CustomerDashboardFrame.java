package ui;

import database.DatabaseHelper;
import database.MenuDAO;
import models.CartItem;
import models.MenuItem;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class CustomerDashboardFrame extends JFrame {
    private List<CartItem> cart;
    private JPanel menuGridPanel;
    private JButton viewCartBtn;
    private String currentCategoryFilter = "All";

    public CustomerDashboardFrame() {
        this(new ArrayList<>());
    }

    public CustomerDashboardFrame(List<CartItem> cart) {
        this.cart = cart;
        setTitle("Garahe Ni Mateicla - Food Delivery");
        setSize(1000, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // WEST (Sidebar)
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(new Color(245, 245, 245));
        sidebar.setPreferredSize(new Dimension(150, 0));
        sidebar.setBorder(BorderFactory.createEmptyBorder(20, 10, 20, 10));

        JLabel logoLabel = new JLabel("Garahe", SwingConstants.CENTER);
        logoLabel.setFont(new Font("Arial", Font.BOLD, 20));
        logoLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        logoLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 30, 0));
        sidebar.add(logoLabel);

        sidebar.add(createSidebarButton("🏠 Home"));
        sidebar.add(Box.createRigidArea(new Dimension(0, 10)));
        sidebar.add(createSidebarButton("📝 Orders"));
        sidebar.add(Box.createRigidArea(new Dimension(0, 10)));
        sidebar.add(createSidebarButton("👤 Profile"));
        
        sidebar.add(Box.createVerticalGlue());
        
        JButton logoutBtn = createSidebarButton("⬅️ Portal");
        logoutBtn.addActionListener(e -> {
            new RoleSelectionFrame().setVisible(true);
            dispose();
        });
        sidebar.add(logoutBtn);

        add(sidebar, BorderLayout.WEST);

        // CENTER (Main Content)
        JPanel mainContent = new JPanel(new BorderLayout());
        mainContent.setBackground(Color.WHITE);

        // Content Header
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(Color.WHITE);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 10, 20));

        // Top Bar
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(Color.WHITE);
        
        JTextField searchField = new JTextField("Search food...");
        searchField.setFont(new Font("Arial", Font.PLAIN, 14));
        searchField.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(Color.LIGHT_GRAY, 1, true),
            BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));
        
        viewCartBtn = new JButton("🛒 View Cart (" + cart.size() + ")");
        viewCartBtn.setFont(new Font("Arial", Font.BOLD, 14));
        viewCartBtn.setBackground(new Color(240, 240, 240));
        viewCartBtn.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
        viewCartBtn.setFocusPainted(false);
        viewCartBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        viewCartBtn.addActionListener(e -> {
            new CartFrame(cart, this).setVisible(true);
            setVisible(false);
        });
        
        JPanel topBarRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        topBarRight.setBackground(Color.WHITE);
        topBarRight.add(viewCartBtn);

        topBar.add(searchField, BorderLayout.CENTER);
        topBar.add(Box.createRigidArea(new Dimension(20, 0)), BorderLayout.EAST);
        topBar.add(topBarRight, BorderLayout.EAST);

        // Category Bar
        JPanel categoryBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        categoryBar.setBackground(Color.WHITE);
        
        List<String> categories = MenuDAO.getActiveCategories();
        categories.add(0, "All");
        
        for (String cat : categories) {
            JButton catBtn = new JButton(cat);
            catBtn.setFont(new Font("Arial", Font.BOLD, 12));
            catBtn.setBackground(new Color(245, 245, 245));
            catBtn.setBorder(BorderFactory.createEmptyBorder(8, 15, 8, 15));
            catBtn.setFocusPainted(false);
            catBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
            catBtn.addActionListener(e -> {
                currentCategoryFilter = cat;
                loadMenuData();
            });
            categoryBar.add(catBtn);
        }

        JScrollPane categoryScroll = new JScrollPane(categoryBar);
        categoryScroll.setBorder(null);
        categoryScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
        categoryScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        headerPanel.add(topBar, BorderLayout.NORTH);
        headerPanel.add(Box.createRigidArea(new Dimension(0, 15)), BorderLayout.CENTER);
        headerPanel.add(categoryScroll, BorderLayout.SOUTH);

        mainContent.add(headerPanel, BorderLayout.NORTH);

        // Content Grid
        menuGridPanel = new JPanel(new GridLayout(0, 3, 20, 20));
        menuGridPanel.setBackground(Color.WHITE);
        menuGridPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JScrollPane gridScroll = new JScrollPane(menuGridPanel);
        gridScroll.setBorder(null);
        gridScroll.getVerticalScrollBar().setUnitIncrement(16);
        
        mainContent.add(gridScroll, BorderLayout.CENTER);

        add(mainContent, BorderLayout.CENTER);
        
        loadMenuData();
    }

    private JButton createSidebarButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Arial", Font.BOLD, 14));
        btn.setAlignmentX(Component.CENTER_ALIGNMENT);
        btn.setMaximumSize(new Dimension(130, 40));
        btn.setBackground(new Color(245, 245, 245));
        btn.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        btn.setFocusPainted(false);
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    public void loadMenuData() {
        menuGridPanel.removeAll();
        viewCartBtn.setText("🛒 View Cart (" + cart.size() + ")");

        String sql = "SELECT id, item_name, price, stock_quantity, category, special_attribute FROM menu_items WHERE is_active = TRUE";
        if (!"All".equals(currentCategoryFilter)) {
            sql += " AND category = '" + currentCategoryFilter.replace("'", "''") + "'";
        }
        sql += " ORDER BY id ASC";

        try (Connection conn = DatabaseHelper.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                int id = rs.getInt("id");
                String name = rs.getString("item_name");
                double price = rs.getDouble("price");
                int stock = rs.getInt("stock_quantity");
                String category = rs.getString("category");
                String specialAttr = rs.getString("special_attribute");
                
                MenuItem item = models.MenuItemFactory.create(id, name, price, stock, category, specialAttr);
                menuGridPanel.add(createItemCard(item));
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error loading menu: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
        
        menuGridPanel.revalidate();
        menuGridPanel.repaint();
    }

    private ImageIcon loadImage(String itemName) {
        String baseDir = "Main/assets/";
        if (!new java.io.File(baseDir).exists()) {
            baseDir = "assets/";
        }

        String[] possibleNames = {
            itemName + ".jpg",
            itemName.replace(" ", "") + ".jpg",
            itemName.replace(" ", "").toLowerCase() + ".jpg",
            itemName.toLowerCase().replace(" ", "") + ".jpg"
        };
        
        // Special manual mappings for typos in uploaded files
        if (itemName.equals("Yuzu Sparkler")) possibleNames = new String[]{"uzu Sparkler.jpg"};
        if (itemName.equals("Pork Chashu")) possibleNames = new String[]{"porkcashu.jpg"};
        
        for (String fileName : possibleNames) {
            java.io.File file = new java.io.File(baseDir + fileName);
            if (file.exists()) {
                try {
                    ImageIcon originalIcon = new ImageIcon(file.getAbsolutePath());
                    Image img = originalIcon.getImage();
                    int w = img.getWidth(null);
                    int h = img.getHeight(null);
                    if (w > 0 && h > 0) {
                        double ratio = (double) w / h;
                        int targetW = 150;
                        int targetH = 120;
                        if (150 / ratio <= 120) {
                            targetH = (int)(150 / ratio);
                        } else {
                            targetW = (int)(120 * ratio);
                        }
                        Image scaledImg = img.getScaledInstance(targetW, targetH, Image.SCALE_SMOOTH);
                        return new ImageIcon(scaledImg);
                    }
                } catch (Exception e) {
                    return null;
                }
            }
        }
        return null;
    }

    private JPanel createItemCard(MenuItem item) {
        int id = item.getId();
        String name = item.getItemName();
        double price = item.getPrice();
        int stock = item.getStockQuantity();
        String specialDetails = item.getSpecialDetails();

        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(new LineBorder(new Color(230, 230, 230), 1, true));

        // Top: Image Placeholder
        JLabel imageLabel = new JLabel("", SwingConstants.CENTER);
        imageLabel.setOpaque(true);
        imageLabel.setBackground(new Color(250, 250, 250));
        imageLabel.setPreferredSize(new Dimension(150, 120));
        
        ImageIcon icon = loadImage(name);
        if (icon != null) {
            imageLabel.setIcon(icon);
        } else {
            imageLabel.setText("No Image");
        }
        
        card.add(imageLabel, BorderLayout.NORTH);

        // Middle: Info
        JPanel infoPanel = new JPanel(new GridLayout(3, 1, 0, 2));
        infoPanel.setBackground(Color.WHITE);
        infoPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JLabel nameLabel = new JLabel(name);
        nameLabel.setFont(new Font("Arial", Font.BOLD, 14));
        nameLabel.setForeground(Color.DARK_GRAY);
        
        JLabel specialLabel = new JLabel(specialDetails);
        specialLabel.setFont(new Font("Arial", Font.ITALIC, 11));
        specialLabel.setForeground(new Color(150, 150, 150));

        JLabel priceLabel = new JLabel("PHP " + String.format("%.2f", price));
        priceLabel.setFont(new Font("Arial", Font.BOLD, 14));
        priceLabel.setForeground(new Color(255, 140, 0)); // Orange

        infoPanel.add(nameLabel);
        infoPanel.add(specialLabel);
        infoPanel.add(priceLabel);
        card.add(infoPanel, BorderLayout.CENTER);

        // Bottom: Add button
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setBackground(Color.WHITE);
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));

        JLabel stockLabel = new JLabel("Stock: " + stock);
        stockLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        stockLabel.setForeground(Color.GRAY);
        bottomPanel.add(stockLabel, BorderLayout.WEST);

        JButton addBtn = new JButton("+ Add");
        addBtn.setFont(new Font("Arial", Font.BOLD, 12));
        addBtn.setBackground(new Color(34, 139, 34)); // Green
        addBtn.setForeground(Color.WHITE);
        addBtn.setFocusPainted(false);
        addBtn.setBorder(BorderFactory.createEmptyBorder(5, 15, 5, 15));
        addBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        if (stock <= 0) {
            addBtn.setEnabled(false);
            addBtn.setText("Sold Out");
            addBtn.setBackground(Color.LIGHT_GRAY);
        } else {
            addBtn.addActionListener(e -> handleAddToCart(id, name));
        }

        bottomPanel.add(addBtn, BorderLayout.EAST);
        card.add(bottomPanel, BorderLayout.SOUTH);

        return card;
    }

    private void handleAddToCart(int itemId, String itemName) {
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
            JOptionPane.showMessageDialog(this, "Added " + quantity + " " + itemName + "(s) to cart!", "Success", JOptionPane.INFORMATION_MESSAGE);
            
            // Refresh view cart button text to show updated size
            viewCartBtn.setText("🛒 View Cart (" + cart.size() + ")");

        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Invalid quantity entered.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
