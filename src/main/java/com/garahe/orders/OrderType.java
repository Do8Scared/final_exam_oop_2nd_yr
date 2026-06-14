package com.garahe.orders;

/**
 * Interface for order fulfillment types (e.g., Delivery, Pick-Up).
 * Defines the contract for order-type-specific fees and behaviors.
 *
 * Renamed from OrderDAO to OrderType to avoid naming collision with
 * database.TransactionDAO and to accurately reflect what this interface represents:
 * an order TYPE, not a data access object.
 */
public interface OrderType {

    /**
     * Returns the additional fee associated with this order type.
     *
     * @return the additional fee amount
     */
    double getAdditionalFee();

}
