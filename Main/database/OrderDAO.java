package database;

import models.CartItem;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/**
 * Data access object for order and transaction processing.
 * Manages the ACID-compliant checkout process and transaction history.
 */
public class OrderDAO {

    /**
     * Processes a customer checkout transaction with ACID guarantees.
     * Inserts transaction records, allocates items to the order, updates stock levels,
     * and logs the transaction to the audit trail. Rolls back all changes if any step fails.
     *
     * @param cart the list of CartItems being purchased
     * @param orderType the order type (Dine-In or Take-Out)
     * @param paymentMethod the payment method (Cash, GCash, Maya)
     * @param amountTendered the cash amount provided by the customer (for Cash payments)
     * @param packagingFee the packaging fee to add to the total (0 for Dine-In)
     * @return true if the transaction committed successfully, false otherwise
     */
    public static boolean processCheckout(List<CartItem> cart, String orderType, String paymentMethod, double amountTendered, double packagingFee) {
        if (cart == null || cart.isEmpty()) {
            System.out.println("Transaction Failed: Cart is empty.");
            return false;
        }

        String txnId = "TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase() + "-" + orderType.toUpperCase();
        double subtotal = 0;
        for (CartItem item : cart) {
            subtotal += item.getSubtotal();
        }

        subtotal = Math.round(subtotal * 100.0) / 100.0;
        double grandTotal = Math.round((subtotal + packagingFee) * 100.0) / 100.0;
        double changeDue = (paymentMethod.equalsIgnoreCase("Cash")) ? Math.round((amountTendered - grandTotal) * 100.0) / 100.0 : 0.00;

        String insertTxnSql = "INSERT INTO transactions (transaction_id, order_type, payment_method, total_amount, amount_tendered, change_due) VALUES (?, ?, ?, ?, ?, ?)";
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
                if (paymentMethod.equalsIgnoreCase("Cash")) {
                    pstmtTxn.setDouble(5, amountTendered);
                    pstmtTxn.setDouble(6, changeDue);
                } else {
                    pstmtTxn.setNull(5, Types.NUMERIC);
                    pstmtTxn.setNull(6, Types.NUMERIC);
                }
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
                        throw new SQLException("Cart contains an invalid quantity for item ID " + c.getItem().getId() + ".");
                    }

                    pstmtLock.setInt(1, c.getItem().getId());
                    try (ResultSet stockRs = pstmtLock.executeQuery()) {
                        if (!stockRs.next()) {
                            throw new SQLException("Item ID " + c.getItem().getId() + " is no longer available.");
                        }

                        int currentStock = stockRs.getInt("stock_quantity");
                        if (currentStock < c.getQuantity()) {
                            throw new SQLException("Insufficient stock for " + c.getItem().getItemName() + ". Requested " + c.getQuantity() + ", available " + currentStock + ".");
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
                    if (!first) itemsJson.append(',');
                    itemsJson.append('{')
                             .append("\"id\":").append(c.getItem().getId()).append(',')
                             .append("\"name\":\"").append(escapeJson(c.getItem().getItemName())).append("\",")
                             .append("\"qty\":").append(c.getQuantity())
                             .append('}');
                    first = false;
                }
                itemsJson.append("]");

                String details = String.format("{\"txnId\":\"%s\",\"orderType\":\"%s\",\"total\":%.2f,\"items\":%s}",
                        txnId, orderType, grandTotal, itemsJson);

                DatabaseHelper.insertAudit(conn, "terminal", "ORDER_CHECKOUT", txnId, details);
            } catch (SQLException ex) {
                System.out.println("Failed to write audit log: " + ex.getMessage());
                conn.rollback();
                return false;
            }

            conn.commit();

            printUnifiedReceipt(txnId, cart, subtotal, packagingFee, grandTotal, paymentMethod, amountTendered, changeDue);
            return true;

        } catch (SQLException e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ex) { }
            }
            if (e.getMessage().contains("check_stock_positive")) {
                System.out.println(">> [TRANSACTION FAILED] Someone just bought the last item! Insufficient stock in the cloud.");
            } else {
                System.out.println(">> [TRANSACTION FAILED] Database Error: " + e.getMessage());
            }
            return false;
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
     * @param txnId the transaction ID
     * @param cart the list of items purchased
     * @param subtotal the subtotal before fees
     * @param packingFee the packaging/processing fee
     * @param grandTotal the total amount due
     * @param payMethod the payment method used
     * @param tendered the cash amount tendered (for Cash payments)
     * @param change the change amount due (for Cash payments)
     */
    private static void printUnifiedReceipt(String txnId, List<CartItem> cart, double subtotal, double packingFee, double grandTotal, String payMethod, double tendered, double change) {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        System.out.println("\n========================================");
        System.out.println("         GARAHE NI MATEICLA POS         ");
        System.out.println("           OFFICIAL RECEIPT             ");
        System.out.println("========================================");
        System.out.println("ID  : " + txnId);
        System.out.println("DATE: " + dtf.format(LocalDateTime.now()));
        System.out.println("----------------------------------------");

        for (CartItem c : cart) {
            System.out.println(c.getItem().getItemName() + " (x" + c.getQuantity() + ")");
            if (!c.getItem().getSpecialDetails().isEmpty()) {
                System.out.println("  " + c.getItem().getSpecialDetails());
            }
            System.out.println("  Subtotal: ₱" + String.format("%.2f", c.getSubtotal()));
        }

        System.out.println("----------------------------------------");
        System.out.println("SUBTOTAL     : ₱" + String.format("%.2f", subtotal));
        if (packingFee > 0) {
            System.out.println("PACKAGING FEE: ₱" + String.format("%.2f", packingFee));
        }
        System.out.println("GRAND TOTAL  : ₱" + String.format("%.2f", grandTotal));
        System.out.println("PAYMENT VIA  : " + payMethod.toUpperCase());

        if (payMethod.equalsIgnoreCase("Cash")) {
            System.out.println("TENDERED     : ₱" + String.format("%.2f", tendered));
            System.out.println("CHANGE DUE   : ₱" + String.format("%.2f", change));
        }
        System.out.println("========================================");
        System.out.println("       THANK YOU, PLEASE COME AGAIN!    ");
        System.out.println("========================================\n");
    }

    /**
     * Escapes JSON special characters in a string for safe JSON construction.
     *
     * @param s the string to escape
     * @return the escaped string safe for JSON insertion
     */
    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }
}