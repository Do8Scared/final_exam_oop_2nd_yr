package orders;

/**
 * Interface for order fulfillment pricing.
 * Defines the contract that all order types must follow.
 */
public interface FulfillmentType {

    /**
     * Returns the packaging or fulfillment fee associated with this order type.
     *
     * @return the packaging fee amount
     */
    double getPackagingFee();

}
