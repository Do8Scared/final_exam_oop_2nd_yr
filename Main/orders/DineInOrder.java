package orders;

/**
 * Represents a dine-in order with no additional packaging fees.
 * Implements the OrderDAO interface for order type-specific behavior.
 */
public class DineInOrder implements OrderDAO {

    /**
     * Initializes a dine-in order.
     * Dine-in orders have no packaging fee since service is on premises.
     */
    public DineInOrder() {
    }

    /**
     * Returns the packaging fee for a dine-in order.
     *
     * @return 0.00 (no fee for dine-in service)
     */
    @Override
    public double getPackagingFee() {
        return 0.00;
    }
}