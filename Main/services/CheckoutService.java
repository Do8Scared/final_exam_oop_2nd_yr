package services;

import models.CartItem;
import orders.OrderType;
import orders.PickUpOrder;
import orders.DeliveryOrder;
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
     * confirmation, and transaction processing.
     *
     * @param cartItems the list of cart items to check out
     * @return true if the transaction completed successfully, false otherwise
     */
    public boolean processCheckout(List<CartItem> cartItems) {
        // --- Order Type Selection ---
        System.out.println("\n--- CHECKOUT FULFILLMENT ---");
        System.out.println("[1] Pick-Up");
        System.out.println("[2] Delivery");
        int typeChoice = input.getValidIntegerInput("Select Order Type: ");

        OrderType fulfillment;
        String orderType;
        if (typeChoice == 2) {
            fulfillment = new DeliveryOrder();
            orderType = "Delivery";
        } else {
            fulfillment = new PickUpOrder();
            orderType = "Pick-Up";
        }

        // --- Payment Method Selection ---
        System.out.println("\n--- PAYMENT METHOD ---");
        System.out.println("[1] GCash");
        System.out.println("[2] Cash on Delivery");
        int payChoice;
        while (true) {
            payChoice = input.getValidIntegerInput("Select Payment: ");
            if (payChoice >= 1 && payChoice <= 2) break;
            System.out.println(">> [ERROR] Invalid payment method. Please select 1 or 2.");
        }

        String paymentMethod;
        switch (payChoice) {
            case 1: paymentMethod = "GCash"; break;
            default: paymentMethod = "Cash on Delivery"; break;
        }

        double additionalFee = fulfillment.getAdditionalFee();

        // Calculate totals with consistent rounding
        double subtotal = 0;
        for (CartItem item : cartItems) {
            subtotal += item.getSubtotal();
        }
        subtotal = Math.round(subtotal * 100.0) / 100.0;
        double grandTotal = Math.round((subtotal + additionalFee) * 100.0) / 100.0;

        System.out.println("\nGrand Total to Pay: PHP " + String.format("%.2f", grandTotal));
        
        if (paymentMethod.equals("GCash")) {
            System.out.println(">> Awaiting " + paymentMethod + " payment processing...");
            String confirmation = input.getNonEmptyInput("Has the " + paymentMethod + " payment of PHP " + String.format("%.2f", grandTotal) + " been authorized? (yes/no): ");
            if (!confirmation.equalsIgnoreCase("yes") && !confirmation.equalsIgnoreCase("y")) {
                System.out.println(">> Checkout cancelled. Payment not authorized.");
                return false;
            }
        } else {
            System.out.println(">> Cash on Delivery selected. Please prepare exact amount of PHP " + String.format("%.2f", grandTotal) + " upon delivery/pick-up.");
        }

        System.out.println("\n>> Connecting to Supabase Cloud securely...");
        return TransactionDAO.processCheckout(cartItems, orderType, paymentMethod, additionalFee);
    }
}
