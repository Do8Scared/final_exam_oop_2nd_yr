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

/**
 * The primary Graphical User Interface for customers.
 * Displays the live digital menu, allows filtering/searching, 
 * and provides functionality to add items to the cart.
 */
public class CustomerDashboardFrame extends JFrame {
    private List<CartItem> cart;
    private JPanel menuGridPanel;
    private JButton viewCartBtn;
    private String currentCategoryFilter = "All";
    private JTextField searchField;
    private JComboBox<String> sortCombo;

    /**
     * Constructs a new CustomerDashboardFrame with an empty cart.
     * Used when the customer first logs in.
     */
    public CustomerDashboardFrame() {
        this(new ArrayList<>());
    }

    /**
     * Constructs a new CustomerDashboardFrame, retaining an existing cart.
     * Used when the customer navigates back from the CartFrame to continue shopping.
     *
     * @param cart the existing list of CartItems
     */
    public CustomerDashboardFrame(List<CartItem> cart) {
        this.cart = cart;
        setTitle("Garahe Ni Mateicla - Food Delivery | LAF: " + UIManager.getLookAndFeel().getName());
        setSize(1000, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // WEST (Sidebar)
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
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

        // Content Header
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 10, 20));

        // Top Bar
        JPanel topBar = new JPanel(new BorderLayout(10, 0));
        
        JPanel searchSortPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));

        searchField = new JTextField("Search food...", 15);
        searchField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent e) {
                if (searchField.getText().equals("Search food...")) searchField.setText("");
            }
            public void focusLost(java.awt.event.FocusEvent e) {
                if (searchField.getText().isEmpty()) searchField.setText("Search food...");
            }
        });
        searchField.addActionListener(e -> loadMenuData());

        sortCombo = new JComboBox<>(new String[]{"Sort by ID", "Sort by Name", "Sort by Price", "Sort by Quantity"});
        sortCombo.addActionListener(e -> loadMenuData());

        searchSortPanel.add(searchField);
        searchSortPanel.add(sortCombo);
        
        viewCartBtn = new JButton("🛒 View Cart (" + cart.size() + ")");
        viewCartBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        viewCartBtn.addActionListener(e -> {
            new CartFrame(cart, this).setVisible(true);
            setVisible(false);
        });
        
        JPanel topBarRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        topBarRight.add(viewCartBtn);

        topBar.add(searchSortPanel, BorderLayout.CENTER);
        topBar.add(topBarRight, BorderLayout.EAST);

        // Category Bar
        JPanel categoryBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        
        List<String> categories = MenuDAO.getActiveCategories();
        categories.add(0, "All");
        
        for (String cat : categories) {
            JButton catBtn = new JButton(cat);
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
        btn.setAlignmentX(Component.CENTER_ALIGNMENT);
        btn.setMaximumSize(new Dimension(130, 40));
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    /**
     * Queries the database for active menu items based on the current
     * category filter, search text, and sorting preference.
     * Rebuilds the visual grid of item cards dynamically.
     */
    public void loadMenuData() {
        menuGridPanel.removeAll();
        viewCartBtn.setText("🛒 View Cart (" + cart.size() + ")");

        String sql = "SELECT id, item_name, price, stock_quantity, category, special_attribute FROM menu_items WHERE is_active = TRUE";
        if (!"All".equals(currentCategoryFilter)) {
            sql += " AND category = '" + currentCategoryFilter.replace("'", "''") + "'";
        }

        String searchTerm = searchField != null ? searchField.getText() : "";
        if (!searchTerm.trim().isEmpty() && !searchTerm.equals("Search food...")) {
            sql += " AND item_name ILIKE ?";
        }

        String sortBy = sortCombo != null ? (String) sortCombo.getSelectedItem() : "Sort by ID";
        if (sortBy != null) {
            switch (sortBy) {
                case "Sort by Name": sql += " ORDER BY item_name ASC"; break;
                case "Sort by Price": sql += " ORDER BY price ASC"; break;
                case "Sort by Quantity": sql += " ORDER BY stock_quantity DESC"; break;
                default: sql += " ORDER BY id ASC"; break;
            }
        } else {
            sql += " ORDER BY id ASC";
        }

        try (Connection conn = DatabaseHelper.getConnection();
             java.sql.PreparedStatement pstmt = conn.prepareStatement(sql)) {
             
            if (!searchTerm.trim().isEmpty() && !searchTerm.equals("Search food...")) {
                pstmt.setString(1, "%" + searchTerm.trim() + "%");
            }

            try (ResultSet rs = pstmt.executeQuery()) {
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
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error loading menu: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
        
        menuGridPanel.revalidate();
        menuGridPanel.repaint();
    }

    /**
     * Attempts to load and scale an image for a menu item from the local filesystem.
     * Includes fallback matching for spaces, casing, and known file name typos.
     *
     * @param itemName the exact name of the menu item from the database
     * @return a scaled ImageIcon if found, or null if no matching image exists
     */
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

    /**
     * Generates a reusable UI component (JPanel) representing a single menu item.
     * Includes the item's image, name, price, polymorphic special details, 
     * and an interactive "Add to Cart" button that respects current database stock.
     *
     * @param item the dynamically fetched MenuItem (or its subclasses)
     * @return a structured JPanel card
     */
    private JPanel createItemCard(MenuItem item) {
        int id = item.getId();
        String name = item.getItemName();
        double price = item.getPrice();
        int stock = item.getStockQuantity();
        String specialDetails = item.getSpecialDetails();

        JPanel card = new JPanel(new BorderLayout());

        // Top: Image Placeholder
        JLabel imageLabel = new JLabel("", SwingConstants.CENTER);
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
        infoPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JLabel nameLabel = new JLabel(name);
        
        JLabel specialLabel = new JLabel(specialDetails);

        JLabel priceLabel = new JLabel("PHP " + String.format("%.2f", price));

        infoPanel.add(nameLabel);
        infoPanel.add(specialLabel);
        infoPanel.add(priceLabel);
        card.add(infoPanel, BorderLayout.CENTER);

        // Bottom: Add button
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));

        JLabel stockLabel = new JLabel("Stock: " + stock);
        bottomPanel.add(stockLabel, BorderLayout.WEST);

        JButton addBtn = new JButton("+ Add");
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

    /**
     * Handles the interaction of adding a specific item to the shopping cart.
     * Validates the requested quantity against the live cloud database stock
     * to prevent overselling before the item is even added to the local cart.
     *
     * @param itemId the database ID of the item
     * @param itemName the display name of the item
     */
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
