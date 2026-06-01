package orders;

/**
 * Represents a take-out order with a configurable packaging fee.
 * Implements the OrderType interface for order type-specific behavior.
 */
public class TakeOutOrder implements OrderType {

    private double packagingFee;

    /**
     * Initializes a take-out order with a specified packaging fee.
     *
     * @param packagingFee the fee to charge for packaging and handling
     */
    public TakeOutOrder(double packagingFee) {
        this.packagingFee = packagingFee;
    }

    /**
     * Returns the packaging fee for this take-out order.
     *
     * @return the packaging fee amount
     */
    @Override
    public double getPackagingFee() {
        return packagingFee;
    }
}