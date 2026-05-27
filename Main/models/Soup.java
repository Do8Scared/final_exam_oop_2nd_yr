package models;

/**
 * Represents a soup menu item with spice level specification.
 * Extends MenuItem with an additional isSpicy attribute.
 */
public class Soup extends MenuItem {

    private boolean isSpicy;

    /**
     * Constructs a Soup item with a specified spice level.
     *
     * @param id the menu item ID assigned by the database
     * @param itemName the name of the soup
     * @param price the price of the soup
     * @param stockQuantity the number of units in stock
     * @param category the category this item belongs to
     * @param isSpicy true if the soup is prepared spicy, false otherwise
     */
    public Soup(int id, String itemName, double price, int stockQuantity, String category, boolean isSpicy) {
        super(id, itemName, price, stockQuantity, category);
        this.isSpicy = isSpicy;
    }

    /**
     * Checks if this soup is prepared spicy.
     *
     * @return true if spicy, false otherwise
     */
    public boolean isSpicy() {
        return isSpicy;
    }

    /**
     * Sets the spice level for this soup.
     *
     * @param spicy true to prepare spicy, false for non-spicy
     */
    public void setSpicy(boolean spicy) {
        this.isSpicy = spicy;
    }

    /**
     * Returns the special details for this soup (preparation/spice level information).
     *
     * @return a string representation of the preparation style
     */
    @Override
    public String getSpecialDetails() {
        return "[Preparation: " + (isSpicy() ? "Spicy" : "Standard Non-Spicy") + "]";
    }
}