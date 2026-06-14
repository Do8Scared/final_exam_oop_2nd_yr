package ui;

import database.TransactionDAO;
import models.CartItem;
import orders.DeliveryOrder;
import orders.OrderType;
import orders.PickUpOrder;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/**
 * Graphical User Interface representing the customer's shopping cart.
 * Displays the current selected items, calculates subtotals and grand totals,
 * and handles the checkout process via communication with TransactionDAO.
 */
public class CartFrame extends JFrame {
    private JTable cartTable;
    private DefaultTableModel tableModel;
    private List<CartItem> cart;
    private CustomerDashboardFrame parentFrame;
    private JLabel totalLabel;

    /**
     * Constructs the CartFrame, rendering a table of currently selected items
     * and initializing the checkout action panel.
     *
     * @param cart the in-memory list of CartItems
     * @param parentFrame the calling dashboard frame (used for returning back to the menu)
     */
    public CartFrame(List<CartItem> cart, CustomerDashboardFrame parentFrame) {
        this.cart = cart;
        this.parentFrame = parentFrame;

        setTitle("Your Cart");
        setSize(700, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        JLabel headerLabel = new JLabel("Your Cart", SwingConstants.CENTER);
        headerLabel.setBorder(BorderFactory.createEmptyBorder(15, 10, 15, 10));
        add(headerLabel, BorderLayout.NORTH);

        tableModel = new DefaultTableModel(new String[]{"Item Name", "Quantity", "Unit Price", "Subtotal"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        cartTable = new JTable(tableModel);
        cartTable.setRowHeight(25);
        
        JScrollPane scrollPane = new JScrollPane(cartTable);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        add(scrollPane, BorderLayout.CENTER);

        JPanel rightPanel = new JPanel(new GridLayout(6, 1, 10, 10));
        rightPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 20));

        JComboBox<String> orderTypeCombo = new JComboBox<>(new String[]{"Pick-Up", "Delivery"});
        JComboBox<String> paymentCombo = new JComboBox<>(new String[]{"GCash", "Maya", "COD"});

        rightPanel.add(new JLabel("Fulfillment:"));
        rightPanel.add(orderTypeCombo);
        rightPanel.add(new JLabel("Payment Method:"));
        rightPanel.add(paymentCombo);

        JButton removeBtn = new JButton("Remove Selected");
        removeBtn.addActionListener(e -> {
            int selectedRow = cartTable.getSelectedRow();
            if (selectedRow == -1) {
                JOptionPane.showMessageDialog(this, "Select an item to remove.", "Warning", JOptionPane.WARNING_MESSAGE);
                return;
            }
            cart.remove(selectedRow);
            loadCartData();
        });
        rightPanel.add(removeBtn);

        totalLabel = new JLabel("Total: PHP 0.00", SwingConstants.CENTER);
        rightPanel.add(totalLabel);
        
        add(rightPanel, BorderLayout.EAST);

        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 20, 10));

        JButton backBtn = new JButton("Back to Menu");
        backBtn.addActionListener(e -> {
            parentFrame.setVisible(true);
            dispose();
        });

        JButton placeOrderBtn = new JButton("Place Order");
        placeOrderBtn.setBackground(new Color(34, 139, 34));
        placeOrderBtn.setForeground(Color.WHITE);
        placeOrderBtn.addActionListener(e -> handleCheckout(orderTypeCombo.getSelectedItem().toString(), paymentCombo.getSelectedItem().toString()));

        bottomPanel.add(backBtn);
        bottomPanel.add(placeOrderBtn);
        add(bottomPanel, BorderLayout.SOUTH);
        
        loadCartData();
    }

    /**
     * Re-populates the JTable with the latest data from the in-memory cart list.
     * Also triggers a recalculation of the cart subtotal.
     */
    private void loadCartData() {
        tableModel.setRowCount(0);
        for (CartItem item : cart) {
            tableModel.addRow(new Object[]{
                item.getItem().getItemName(),
                item.getQuantity(),
                String.format("%.2f", item.getUnitPrice()),
                String.format("%.2f", item.getSubtotal())
            });
        }
        updateTotal();
    }

    /**
     * Iterates through the cart items, calculating the combined subtotal,
     * and updates the totalLabel display on the UI.
     */
    private void updateTotal() {
        double subtotal = 0;
        for (CartItem item : cart) {
            subtotal += item.getSubtotal();
        }
        totalLabel.setText("Subtotal: PHP " + String.format("%.2f", subtotal));
    }

    /**
     * Handles the checkout sequence. Validates the cart, delegates transaction processing
     * to the DAO layer, and displays the generated official receipt via a dialog.
     *
     * @param orderTypeStr the selected fulfillment method (Pick-Up, Delivery)
     * @param paymentMethod the selected payment method (GCash, Maya, COD)
     */
    private void handleCheckout(String orderTypeStr, String paymentMethod) {
        if (cart.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Cart is empty!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        OrderType fulfillment = orderTypeStr.equals("Delivery") ? new DeliveryOrder() : new PickUpOrder();
        double additionalFee = fulfillment.getAdditionalFee();

        double subtotal = 0;
        for (CartItem item : cart) {
            subtotal += item.getSubtotal();
        }
        
        double grandTotal = Math.round((subtotal + additionalFee) * 100.0) / 100.0;
        
        int confirm = JOptionPane.showConfirmDialog(this, 
            "Subtotal: PHP " + String.format("%.2f", subtotal) + "\n" +
            "Additional Fee: PHP " + String.format("%.2f", additionalFee) + "\n" +
            "Grand Total: PHP " + String.format("%.2f", grandTotal) + "\n\n" +
            "Proceed with " + paymentMethod + "?", 
            "Confirm Order", JOptionPane.YES_NO_OPTION);

        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        java.io.PrintStream ps = new java.io.PrintStream(baos);
        java.io.PrintStream old = System.out;
        System.setOut(ps);

        boolean success = TransactionDAO.processCheckout(cart, orderTypeStr, paymentMethod, additionalFee);
        
        System.out.flush();
        System.setOut(old);

        if (success) {
            String receiptText = baos.toString();
            
            JTextArea textArea = new JTextArea(receiptText);
            textArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
            textArea.setEditable(false);
            
            JScrollPane scrollPane = new JScrollPane(textArea);
            scrollPane.setPreferredSize(new Dimension(350, 450));
            
            JOptionPane.showMessageDialog(this, scrollPane, "Official Receipt", JOptionPane.INFORMATION_MESSAGE);

            cart.clear();
            parentFrame.loadMenuData();
            parentFrame.setVisible(true);
            dispose();
        } else {
            JOptionPane.showMessageDialog(this, "Failed to place order. Someone might have bought the last item, or database error.", "Checkout Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
