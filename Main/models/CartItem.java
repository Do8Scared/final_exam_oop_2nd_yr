package models;

/**
 * Represents a single line item in a customer's shopping cart.
 * Stores a MenuItem reference with a quantity and captures the unit price at the time of addition.
 */
public class CartItem {

    private MenuItem item;
    private int quantity;
    private double unitPrice;

    /**
     * Constructs a CartItem with the specified menu item and quantity.
     * Records the unit price from the menu item at the time of creation.
     *
     * @param item the MenuItem being added to the cart
     * @param quantity the number of units of this item
     */
    public CartItem(MenuItem item, int quantity) {
        this.item = item;
        this.quantity = quantity;
        this.unitPrice = (item != null) ? item.getPrice() : 0;
    }

    /**
     * Gets the menu item associated with this cart line.
     *
     * @return the MenuItem object
     */
    public MenuItem getItem() {
        return item;
    }

    /**
     * Sets a new menu item for this cart line and updates the unit price.
     *
     * @param item the new MenuItem
     */
    public void setItem(MenuItem item) {
        this.item = item;
        this.unitPrice = (item != null) ? item.getPrice() : 0;
    }

    /**
     * Gets the quantity of items in this cart line.
     *
     * @return the quantity
     */
    public int getQuantity() {
        return quantity;
    }

    /**
     * Sets the quantity of items in this cart line.
     *
     * @param quantity the new quantity
     */
    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    /**
     * Gets the unit price of this item (captured at time of cart addition).
     *
     * @return the unit price in pesos
     */
    public double getUnitPrice() {
        return unitPrice;
    }

    /**
     * Calculates and returns the subtotal for this cart line (unitPrice * quantity).
     *
     * @return the line subtotal in pesos
     */
    public double getSubtotal() {
        return unitPrice * quantity;
    }
}