package orders;

/**
 * Represents a pick-up order with no additional fees.
 * Implements the OrderType interface for order type-specific behavior.
 */
public class PickUpOrder implements OrderType {

    /**
     * Initializes a pick-up order.
     * Pick-up orders have no additional fee since service is on premises.
     */
    public PickUpOrder() {
    }

    /**
     * Returns the additional fee for a pick-up order.
     *
     * @return 0.00 (no fee for pick-up service)
     */
    @Override
    public double getAdditionalFee() {
        return 0.00;
    }
}
