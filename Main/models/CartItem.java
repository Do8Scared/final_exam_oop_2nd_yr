package models;

import java.math.BigDecimal;

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
        if (item == null) {
            throw new IllegalArgumentException("CartItem requires a non-null MenuItem.");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("CartItem quantity must be greater than zero.");
        }
        this.item = item;
        this.quantity = quantity;
        this.unitPrice = item.getPrice();
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
        if (item == null) {
            throw new IllegalArgumentException("CartItem requires a non-null MenuItem.");
        }
        this.item = item;
        this.unitPrice = item.getPrice();
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
        if (quantity <= 0) {
            throw new IllegalArgumentException("CartItem quantity must be greater than zero.");
        }
        this.quantity = quantity;
    }

    /**
     * Refreshes the unit price based on the latest menu item price.
     *
     * @param newUnitPrice the updated unit price
     */
    public void setUnitPrice(double newUnitPrice) {
        if (newUnitPrice < 0) {
            throw new IllegalArgumentException("Unit price cannot be negative.");
        }
        this.unitPrice = newUnitPrice;
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

    /**
     * Calculates the subtotal using BigDecimal for monetary precision.
     *
     * @return the line subtotal as BigDecimal
     */
    public BigDecimal getSubtotalDecimal() {
        return BigDecimal.valueOf(unitPrice).multiply(BigDecimal.valueOf(quantity));
    }
}