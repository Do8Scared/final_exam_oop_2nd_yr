package services;

import models.CartItem;
import orders.OrderType;
import orders.DineInOrder;
import orders.TakeOutOrder;
import database.TransactionDAO;

import java.util.List;

/**
 * Orchestrates the checkout process for a transaction.
 * Validates payment, calculates totals with consistent rounding,
 * and delegates database operations to TransactionDAO.
 */
public class CheckoutService {

    private final InputHelper input;

    /**
     * Constructs a CheckoutService with the given input helper.
     *
     * @param input the InputHelper for reading user input
     */
    public CheckoutService(InputHelper input) {
        this.input = input;
    }

    /**
     * Runs the full checkout flow: order type selection, payment method selection,
     * amount collection, and transaction processing.
     *
     * @param cartItems the list of cart items to check out
     * @return true if the transaction completed successfully, false otherwise
     */
    public boolean processCheckout(List<CartItem> cartItems) {
        // --- Order Type Selection ---
        System.out.println("\n--- CHECKOUT FULFILLMENT ---");
        System.out.println("[1] Dine-In");
        System.out.println("[2] Take-Out");
        int typeChoice = input.getValidIntegerInput("Select Order Type: ");

        OrderType fulfillment;
        String orderType;
        if (typeChoice == 2) {
            fulfillment = new TakeOutOrder(getConfiguredTakeOutFee());
            orderType = "Take-Out";
        } else {
            fulfillment = new DineInOrder();
            orderType = "Dine-In";
        }

        // --- Payment Method Selection with Validation (fixes #20) ---
        System.out.println("\n--- PAYMENT METHOD ---");
        System.out.println("[1] Cash");
        System.out.println("[2] GCash");
        System.out.println("[3] Maya");
        int payChoice;
        while (true) {
            payChoice = input.getValidIntegerInput("Select Payment: ");
            if (payChoice >= 1 && payChoice <= 3) break;
            System.out.println(">> [ERROR] Invalid payment method. Please select 1, 2, or 3.");
        }

        String paymentMethod;
        switch (payChoice) {
            case 2: paymentMethod = "GCash"; break;
            case 3: paymentMethod = "Maya"; break;
            default: paymentMethod = "Cash"; break;
        }

        double amountTendered = 0;
        double packagingFee = fulfillment.getPackagingFee();

        // Calculate totals with consistent rounding
        double subtotal = 0;
        for (CartItem item : cartItems) {
            subtotal += item.getSubtotal();
        }
        subtotal = Math.round(subtotal * 100.0) / 100.0;
        double grandTotal = Math.round((subtotal + packagingFee) * 100.0) / 100.0;

        if (paymentMethod.equals("Cash")) {
            System.out.println("\nGrand Total to Pay: ₱" + String.format("%.2f", grandTotal));
            while (true) {
                amountTendered = input.getValidDoubleInput("Enter Amount Tendered: ₱");
                if (amountTendered >= grandTotal) {
                    break;
                } else {
                    System.out.println(">> [ERROR] Short payment! Amount tendered must meet the grand total.");
                }
            }
        } else {
            // E-wallet payment confirmation (fixes #13)
            System.out.println("\nGrand Total to Pay: ₱" + String.format("%.2f", grandTotal));
            System.out.println(">> Awaiting " + paymentMethod + " payment confirmation...");
            String confirmation = input.getNonEmptyInput("Has the " + paymentMethod + " payment of ₱" + String.format("%.2f", grandTotal) + " been received? (yes/no): ");
            if (!confirmation.equalsIgnoreCase("yes") && !confirmation.equalsIgnoreCase("y")) {
                System.out.println(">> Checkout cancelled. Payment not confirmed.");
                return false;
            }
            amountTendered = grandTotal;
        }

        System.out.println("\n>> Connecting to Supabase Cloud securely...");
        return TransactionDAO.processCheckout(cartItems, orderType, paymentMethod, amountTendered, packagingFee);
    }

    /**
     * Retrieves the configured take-out fee from environment variables or system properties.
     * Falls back to 20.00 if not configured or invalid.
     *
     * @return the take-out fee amount
     */
    private double getConfiguredTakeOutFee() {
        String feeValue = System.getenv("POS_TAKEOUT_FEE");
        if (feeValue == null || feeValue.isBlank()) {
            feeValue = System.getProperty("pos.takeout.fee");
        }

        if (feeValue == null || feeValue.isBlank()) {
            System.out.println(">> [WARN] POS_TAKEOUT_FEE is not configured. Using default take-out fee of ₱20.00.");
            return 20.00;
        }

        try {
            return Math.round(Double.parseDouble(feeValue.trim()) * 100.0) / 100.0;
        } catch (NumberFormatException e) {
            System.out.println(">> [WARN] Invalid POS_TAKEOUT_FEE value. Using default take-out fee of ₱20.00.");
            return 20.00;
        }
    }
}
