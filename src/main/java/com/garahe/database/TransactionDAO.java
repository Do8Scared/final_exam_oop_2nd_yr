package com.garahe.database;

import com.garahe.models.CartItem;
import com.garahe.models.User;
import com.garahe.util.JsonUtil;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Data access object for order and transaction processing.
 * Manages the ACID-compliant checkout process and transaction history.
 *
 * Renamed from OrderDAO to TransactionDAO to avoid naming collision with
 * the orders.OrderType interface and to better represent its purpose.
 */
public class TransactionDAO {
    private static final UserDAO userDAO = new UserDAOImpl();

    /**
     * Processes a customer checkout transaction with ACID guarantees.
     * Inserts transaction records, allocates items to the order, updates stock
     * levels,
     * and logs the transaction to the audit trail. Rolls back all changes if any
     * step fails.
     *
     * @param cart           the list of CartItems being purchased
     * @param orderType      the order type (Dine-In or Take-Out)
     * @param paymentMethod  the payment method (Cash, GCash, Maya)
     * @param amountTendered the cash amount provided by the customer (for Cash
     *                       payments)
     * @param additionalFee  the additional fee (e.g., delivery fee)
     * @param email          the email of the logged-in user
     * @param customerName   the name of the customer
     * @param contactNumber  the contact number
     * @param deliverTo      the delivery address
     * @param notes          any additional notes
     * @return the transaction ID if successful, null otherwise
     */
    public static String processCheckout(List<CartItem> cart, String orderType, String paymentMethod,
            double additionalFee, String email, String customerName, String contactNumber, String deliverTo, String notes) {
        if (cart == null || cart.isEmpty()) {
            System.out.println("Transaction Failed: Cart is empty.");
            return null;
        }

        Integer userId = null;
        if (email != null && !email.isEmpty()) {
            User u = userDAO.getUserByEmail(email);
            if (u != null) {
                userId = u.getId();
            }
        }

        String txnId = "TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase() + "-"
                + orderType.toUpperCase();
        double subtotal = 0;
        for (CartItem item : cart) {
            subtotal += item.getSubtotal();
        }

        subtotal = Math.round(subtotal * 100.0) / 100.0;
        double grandTotal = Math.round((subtotal + additionalFee) * 100.0) / 100.0;
        double amountTendered = grandTotal;
        double changeDue = 0.00;

        String insertTxnSql = "INSERT INTO transactions (transaction_id, order_type, payment_method, total_amount, amount_tendered, change_due, user_id, customer_name, contact_number, deliver_to, notes) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        String insertItemsSql = "INSERT INTO transaction_items (transaction_id, menu_item_id, quantity, subtotal) VALUES (?, ?, ?, ?)";
        String updateStockSql = "UPDATE menu_items SET stock_quantity = stock_quantity - ? WHERE id = ?";
        String lockStockSql = "SELECT stock_quantity FROM menu_items WHERE id = ? AND is_active = TRUE FOR UPDATE";

        Connection conn = null;
        try {
            conn = DatabaseHelper.getConnection();
            conn.setAutoCommit(false);

            try (PreparedStatement pstmtTxn = conn.prepareStatement(insertTxnSql)) {
                pstmtTxn.setString(1, txnId);
                pstmtTxn.setString(2, orderType);
                pstmtTxn.setString(3, paymentMethod);
                pstmtTxn.setDouble(4, grandTotal);
                pstmtTxn.setDouble(5, amountTendered);
                pstmtTxn.setDouble(6, changeDue);
                if (userId != null) pstmtTxn.setInt(7, userId); else pstmtTxn.setNull(7, Types.INTEGER);
                pstmtTxn.setString(8, customerName);
                pstmtTxn.setString(9, contactNumber);
                pstmtTxn.setString(10, deliverTo);
                pstmtTxn.setString(11, notes);
                pstmtTxn.executeUpdate();
            }

            try (PreparedStatement pstmtItems = conn.prepareStatement(insertItemsSql);
                    PreparedStatement pstmtStock = conn.prepareStatement(updateStockSql);
                    PreparedStatement pstmtLock = conn.prepareStatement(lockStockSql)) {

                for (CartItem c : cart) {
                    if (c == null || c.getItem() == null) {
                        throw new SQLException("Cart contains an invalid line item.");
                    }
                    if (c.getQuantity() <= 0) {
                        throw new SQLException(
                                "Cart contains an invalid quantity for item ID " + c.getItem().getId() + ".");
                    }

                    pstmtLock.setInt(1, c.getItem().getId());
                    try (ResultSet stockRs = pstmtLock.executeQuery()) {
                        if (!stockRs.next()) {
                            throw new SQLException("Item ID " + c.getItem().getId() + " is no longer available.");
                        }

                        int currentStock = stockRs.getInt("stock_quantity");
                        if (currentStock < c.getQuantity()) {
                            throw new SQLException("Insufficient stock for " + c.getItem().getItemName()
                                    + ". Requested " + c.getQuantity() + ", available " + currentStock + ".");
                        }
                    }

                    pstmtItems.setString(1, txnId);
                    pstmtItems.setInt(2, c.getItem().getId());
                    pstmtItems.setInt(3, c.getQuantity());
                    pstmtItems.setDouble(4, c.getSubtotal());
                    pstmtItems.addBatch();

                    pstmtStock.setInt(1, c.getQuantity());
                    pstmtStock.setInt(2, c.getItem().getId());
                    pstmtStock.addBatch();
                }

                pstmtItems.executeBatch();
                pstmtStock.executeBatch();
            }

            try {
                StringBuilder itemsJson = new StringBuilder();
                itemsJson.append("[");
                boolean first = true;
                for (CartItem c : cart) {
                    if (!first)
                        itemsJson.append(',');
                    Map<String, Object> itemMap = new LinkedHashMap<>();
                    itemMap.put("id", c.getItem().getId());
                    itemMap.put("name", c.getItem().getItemName());
                    itemMap.put("qty", c.getQuantity());
                    itemsJson.append(JsonUtil.buildJsonObject(itemMap));
                    first = false;
                }
                itemsJson.append("]");

                Map<String, Object> detailsMap = new LinkedHashMap<>();
                detailsMap.put("txnId", txnId);
                detailsMap.put("orderType", orderType);
                detailsMap.put("total", grandTotal);

                String detailsBase = JsonUtil.buildJsonObject(detailsMap);
                // Insert items array into the details JSON
                String details = detailsBase.substring(0, detailsBase.length() - 1) + ",\"items\":" + itemsJson + "}";

                DatabaseHelper.insertAudit(conn, "terminal", "ORDER_CHECKOUT", txnId, details);
            } catch (SQLException ex) {
                System.out.println("Failed to write audit log: " + ex.getMessage());
                conn.rollback();
                return null;
            }

            conn.commit();

            printUnifiedReceipt(txnId, cart, subtotal, additionalFee, grandTotal, paymentMethod, amountTendered,
                    changeDue, customerName, contactNumber, deliverTo, notes);
            return txnId;

        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                }
            }
            if (e.getMessage().contains("check_stock_positive")) {
                System.out.println(
                        ">> [TRANSACTION FAILED] Someone just bought the last item! Insufficient stock in the cloud.");
            } else {
                System.out.println(">> [TRANSACTION FAILED] Database Error: " + e.getMessage());
            }
            return null;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    System.out.println("Error closing transaction connection: " + e.getMessage());
                }
            }
        }
    }

    /**
     * Displays a formatted receipt for a completed transaction.
     *
     * @param txnId      the transaction ID
     * @param cart       the list of items purchased
     * @param subtotal   the subtotal before fees
     * @param packingFee the packaging/processing fee
     * @param grandTotal the total amount due
     * @param payMethod  the payment method used
     * @param tendered   the cash amount tendered (for Cash payments)
     * @param change     the change amount due (for Cash payments)
     * @param customerName  the name of the customer
     * @param contactNumber the contact number
     * @param deliverTo     the delivery address
     * @param notes         any additional notes
     */
    private static void printUnifiedReceipt(String txnId, List<CartItem> cart, double subtotal, double additionalFee,
            double grandTotal, String payMethod, double tendered, double change,
            String customerName, String contactNumber, String deliverTo, String notes) {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        System.out.println("\n========================================");
        System.out.println("         GARAHE NI MATEICLA APP         ");
        System.out.println("           OFFICIAL RECEIPT             ");
        System.out.println("========================================");
        System.out.println("ID  : " + txnId);
        System.out.println("DATE: " + dtf.format(LocalDateTime.now()));
        if (customerName != null && !customerName.isEmpty()) System.out.println("Customer: " + customerName);
        if (contactNumber != null && !contactNumber.isEmpty()) System.out.println("Contact : " + contactNumber);
        if (deliverTo != null && !deliverTo.isEmpty()) System.out.println("Deliver To: " + deliverTo);
        if (notes != null && !notes.isEmpty()) System.out.println("Notes   : " + notes);
        System.out.println("----------------------------------------");

        for (CartItem c : cart) {
            System.out.println(c.getItem().getItemName() + " (x" + c.getQuantity() + ")");
            if (!c.getItem().getSpecialDetails().isEmpty()) {
                System.out.println("  " + c.getItem().getSpecialDetails());
            }
            System.out.println("  Subtotal: PHP " + String.format("%.2f", c.getSubtotal()));
        }

        System.out.println("----------------------------------------");
        System.out.println("SUBTOTAL     : PHP " + String.format("%.2f", subtotal));
        if (additionalFee > 0) {
            System.out.println("DELIVERY FEE : PHP " + String.format("%.2f", additionalFee));
        }
        System.out.println("GRAND TOTAL  : PHP " + String.format("%.2f", grandTotal));
        System.out.println("PAYMENT VIA  : " + payMethod.toUpperCase());
        System.out.println("========================================");
        System.out.println("       THANK YOU, PLEASE COME AGAIN!    ");
        System.out.println("========================================\n");
    }

    /**
     * Retrieves all transactions and returns them as a DefaultTableModel.
     */
    public static javax.swing.table.DefaultTableModel getTransactionHistoryTableModel() {
        javax.swing.table.DefaultTableModel model = new javax.swing.table.DefaultTableModel(
            new Object[]{"Transaction ID", "Date", "Order Type", "Payment Method", "Grand Total"}, 0
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        String sql = "SELECT transaction_id, created_at, order_type, payment_method, total_amount FROM transactions ORDER BY created_at DESC";
        try (Connection conn = DatabaseHelper.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                model.addRow(new Object[]{
                    rs.getString("transaction_id"),
                    rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toString() : "N/A",
                    rs.getString("order_type"),
                    rs.getString("payment_method"),
                    String.format("PHP %.2f", rs.getDouble("total_amount"))
                });
            }
        } catch (SQLException e) {
            System.out.println("Error fetching transaction history: " + e.getMessage());
        }
        return model;
    }

    /**
     * Retrieves all transactions for a specific user based on their email.
     */
    public static String getUserTransactionsJson(String email) {
        Integer userId = null;
        if (email != null && !email.isEmpty()) {
            User u = userDAO.getUserByEmail(email);
            if (u != null) {
                userId = u.getId();
            }
        }

        if (userId == null) {
            return "[]";
        }

        String sql = "SELECT t.transaction_id, t.created_at, t.order_type, t.payment_method, t.total_amount, " +
                     "t.customer_name, t.deliver_to " +
                     "FROM transactions t " +
                     "WHERE t.user_id = ? " +
                     "ORDER BY t.created_at DESC";

        StringBuilder json = new StringBuilder("[");
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
             
            pstmt.setInt(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                boolean first = true;
                while (rs.next()) {
                    if (!first) json.append(",");
                    
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("transactionId", rs.getString("transaction_id"));
                    map.put("date", rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toString() : "");
                    map.put("orderType", rs.getString("order_type"));
                    map.put("paymentMethod", rs.getString("payment_method"));
                    map.put("totalAmount", rs.getDouble("total_amount"));
                    map.put("customerName", rs.getString("customer_name"));
                    map.put("deliverTo", rs.getString("deliver_to"));

                    json.append(JsonUtil.buildJsonObject(map));
                    first = false;
                }
            }
        } catch (SQLException e) {
            System.out.println("Error fetching user transactions: " + e.getMessage());
        }
        json.append("]");
        return json.toString();
    }
}
