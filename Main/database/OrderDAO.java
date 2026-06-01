package database;

import models.CartItem;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Data access object for order and transaction processing.
 * Manages the ACID-compliant checkout process and transaction history.
 */
public class OrderDAO {

    private static final String CURRENCY_SYMBOL = "₱";
    private static final String INFO = ">> [INFO]";
    private static final String WARN = ">> [WARN]";
    private static final String ERROR = ">> [ERROR]";

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

        String safeOrderType = (orderType == null || orderType.isBlank()) ? "Unknown" : orderType.trim();
        String safePaymentMethod = (paymentMethod == null || paymentMethod.isBlank()) ? "Unknown" : paymentMethod.trim();
        String txnId = "TXN-" + System.currentTimeMillis() + "-" +
                UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase() + "-" + safeOrderType.toUpperCase();

        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal packagingFeeAmount = BigDecimal.valueOf(packagingFee).setScale(2, RoundingMode.HALF_UP);
        BigDecimal grandTotal = BigDecimal.ZERO;
        BigDecimal changeDue = BigDecimal.ZERO;

        String insertTxnSql = "INSERT INTO transactions (transaction_id, order_type, payment_method, total_amount, amount_tendered, change_due) VALUES (?, ?, ?, ?, ?, ?)";
        String insertItemsSql = "INSERT INTO transaction_items (transaction_id, menu_item_id, quantity, subtotal) VALUES (?, ?, ?, ?)";
        String updateStockSql = "UPDATE menu_items SET stock_quantity = stock_quantity - ? WHERE id = ?";
        String lockStockSql = "SELECT stock_quantity, price FROM menu_items WHERE id = ? AND is_active = TRUE FOR UPDATE";

        Connection conn = null;
        try {
            conn = DatabaseHelper.getConnection();
            conn.setAutoCommit(false);

            List<CartItem> sortedCart = new ArrayList<>(cart);
            sortedCart.sort(Comparator.comparingInt(a -> a.getItem().getId()));

            try (PreparedStatement pstmtItems = conn.prepareStatement(insertItemsSql);
                 PreparedStatement pstmtStock = conn.prepareStatement(updateStockSql);
                 PreparedStatement pstmtLock = conn.prepareStatement(lockStockSql);
                 PreparedStatement pstmtTxn = conn.prepareStatement(insertTxnSql)) {

                for (CartItem c : sortedCart) {
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

                        double currentPrice = stockRs.getDouble("price");
                        if (Math.abs(currentPrice - c.getUnitPrice()) > 0.01) {
                            System.out.println(WARN + " Price updated for " + c.getItem().getItemName() + ": " +
                                    CURRENCY_SYMBOL + String.format("%.2f", c.getUnitPrice()) + " -> " +
                                    CURRENCY_SYMBOL + String.format("%.2f", currentPrice));
                            c.setUnitPrice(currentPrice);
                        }
                    }

                    BigDecimal lineSubtotal = c.getSubtotalDecimal().setScale(2, RoundingMode.HALF_UP);
                    subtotal = subtotal.add(lineSubtotal);

                    pstmtItems.setString(1, txnId);
                    pstmtItems.setInt(2, c.getItem().getId());
                    pstmtItems.setInt(3, c.getQuantity());
                    pstmtItems.setBigDecimal(4, lineSubtotal);
                    pstmtItems.addBatch();

                    pstmtStock.setInt(1, c.getQuantity());
                    pstmtStock.setInt(2, c.getItem().getId());
                    pstmtStock.addBatch();
                }

                grandTotal = subtotal.add(packagingFeeAmount).setScale(2, RoundingMode.HALF_UP);

                if (safePaymentMethod.equalsIgnoreCase("Cash")) {
                    changeDue = BigDecimal.valueOf(amountTendered).subtract(grandTotal).setScale(2, RoundingMode.HALF_UP);
                }

                pstmtTxn.setString(1, txnId);
                pstmtTxn.setString(2, safeOrderType);
                pstmtTxn.setString(3, safePaymentMethod);
                pstmtTxn.setBigDecimal(4, grandTotal);
                if (safePaymentMethod.equalsIgnoreCase("Cash")) {
                    pstmtTxn.setBigDecimal(5, BigDecimal.valueOf(amountTendered).setScale(2, RoundingMode.HALF_UP));
                    pstmtTxn.setBigDecimal(6, changeDue);
                } else {
                    pstmtTxn.setNull(5, Types.NUMERIC);
                    pstmtTxn.setNull(6, Types.NUMERIC);
                }
                pstmtTxn.executeUpdate();

                try {
                    pstmtItems.executeBatch();
                    pstmtStock.executeBatch();
                } catch (SQLException e) {
                    pstmtItems.clearBatch();
                    pstmtStock.clearBatch();
                    throw e;
                }
            }

            conn.commit();

            System.out.println(INFO + " Checkout successful: " + txnId + " (" + safePaymentMethod + ")");

            printUnifiedReceipt(txnId, cart, subtotal, packagingFeeAmount, grandTotal, safePaymentMethod,
                    BigDecimal.valueOf(amountTendered).setScale(2, RoundingMode.HALF_UP), changeDue);

            try {
                conn.setAutoCommit(true);
                StringBuilder itemsJson = new StringBuilder();
                itemsJson.append("[");
                boolean first = true;
                for (CartItem c : sortedCart) {
                    if (!first) itemsJson.append(',');
                    itemsJson.append('{')
                             .append("\"id\":").append(c.getItem().getId()).append(',')
                             .append("\"name\":\"").append(escapeJson(c.getItem().getItemName())).append("\",")
                             .append("\"qty\":").append(c.getQuantity())
                             .append('}');
                    first = false;
                }
                itemsJson.append("]");

                String details = String.format("{\"txnId\":\"%s\",\"orderType\":\"%s\",\"total\":%s,\"items\":%s}",
                        txnId, safeOrderType, grandTotal.toPlainString(), itemsJson);

                DatabaseHelper.insertAudit(conn, "terminal", "ORDER_CHECKOUT", txnId, details);
            } catch (SQLException ex) {
                System.out.println(WARN + " Audit log failed but transaction committed: " + ex.getMessage());
            }
            return true;

        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    System.out.println(WARN + " Rollback failed (connection may be broken): " + ex.getMessage());
                }
            }
            if (e.getMessage().contains("check_stock_positive")) {
                System.out.println(">> [TRANSACTION FAILED] Someone just bought the last item! Insufficient stock in the cloud.");
            } else {
                System.out.println(ERROR + " Database Error: " + e.getMessage());
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
    private static void printUnifiedReceipt(String txnId, List<CartItem> cart, BigDecimal subtotal, BigDecimal packingFee,
                                           BigDecimal grandTotal, String payMethod, BigDecimal tendered, BigDecimal change) {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(LocalDateTime.of(2020, 1, 1, 0, 0))) {
            System.out.println(WARN + " System clock appears incorrect. Receipt date may be wrong.");
        }

        System.out.println("\n========================================");
        System.out.println("         GARAHE NI MATEICLA POS         ");
        System.out.println("           OFFICIAL RECEIPT             ");
        System.out.println("========================================");
        System.out.println("ID  : " + txnId);
        System.out.println("DATE: " + dtf.format(now));
        System.out.println("----------------------------------------");

        List<CartItem> sorted = new ArrayList<>(cart);
        sorted.sort(Comparator.comparing(a -> a.getItem().getItemName()));
        for (CartItem c : sorted) {
            System.out.println(c.getItem().getItemName() + " (x" + c.getQuantity() + ")");
            if (!c.getItem().getSpecialDetails().isEmpty()) {
                System.out.println("  " + c.getItem().getSpecialDetails());
            }
            System.out.println("  Subtotal: " + CURRENCY_SYMBOL + formatMoney(c.getSubtotalDecimal()));
        }

        System.out.println("----------------------------------------");
        System.out.println("SUBTOTAL     : " + CURRENCY_SYMBOL + formatMoney(subtotal));
        if (packingFee.compareTo(BigDecimal.ZERO) > 0) {
            System.out.println("PACKAGING FEE: " + CURRENCY_SYMBOL + formatMoney(packingFee));
        }
        System.out.println("GRAND TOTAL  : " + CURRENCY_SYMBOL + formatMoney(grandTotal));
        System.out.println("PAYMENT VIA  : " + payMethod.toUpperCase());

        if (payMethod.equalsIgnoreCase("Cash")) {
            System.out.println("TENDERED     : " + CURRENCY_SYMBOL + formatMoney(tendered));
            System.out.println("CHANGE DUE   : " + CURRENCY_SYMBOL + formatMoney(change));
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
                .replace("\b", "\\b")
                .replace("\f", "\\f")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private static String formatMoney(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }
}