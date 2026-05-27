package models;

/**
 * Represents an add-on menu item (condiment or solid food).
 * Extends MenuItem with an additional isCondiment attribute.
 */
public class AddOn extends MenuItem {

    private boolean isCondiment;

    /**
     * Constructs an AddOn item with a type classification.
     *
     * @param id the menu item ID assigned by the database
     * @param itemName the name of the add-on
     * @param price the price of the add-on
     * @param stockQuantity the number of units in stock
     * @param category the category this item belongs to
     * @param isCondiment true if this is a condiment/sauce, false if it's solid food
     */
    public AddOn(int id, String itemName, double price, int stockQuantity, String category, boolean isCondiment) {
        super(id, itemName, price, stockQuantity, category);
        this.isCondiment = isCondiment;
    }

    /**
     * Checks if this add-on is a condiment or sauce.
     *
     * @return true if condiment, false if solid food
     */
    public boolean isCondiment() {
        return isCondiment;
    }

    /**
     * Sets the type classification for this add-on.
     *
     * @param condiment true for condiment/sauce, false for solid food
     */
    public void setCondiment(boolean condiment) {
        this.isCondiment = condiment;
    }

    /**
     * Returns the special details for this add-on (type information).
     *
     * @return a string representation of the add-on type
     */
    @Override
    public String getSpecialDetails() {
        return "[Type: " + (isCondiment() ? "Condiment/Sauce" : "Solid Food") + "]";
    }
}