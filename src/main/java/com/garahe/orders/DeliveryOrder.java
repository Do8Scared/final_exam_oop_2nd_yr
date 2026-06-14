package com.garahe.orders;

/**
 * Represents a delivery order with a fixed delivery fee.
 * Implements the OrderType interface for order type-specific behavior.
 */
public class DeliveryOrder implements OrderType {

    /**
     * Initializes a delivery order.
     */
    public DeliveryOrder() {
    }

    /**
     * Returns the delivery fee for this order.
     *
     * @return 50.00
     */
    @Override
    public double getAdditionalFee() {
        return 50.00;
    }
}
