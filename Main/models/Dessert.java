package models;

/**
 * Represents a dessert menu item with a sweetness profile.
 * Extends MenuItem with an additional sweetness attribute.
 */
public class Dessert extends MenuItem {

    private String sweetness;

    /**
     * Constructs a Dessert item with a specified sweetness profile.
     *
     * @param id the menu item ID assigned by the database
     * @param itemName the name of the dessert
     * @param price the price of the dessert
     * @param stockQuantity the number of units in stock
     * @param category the category this item belongs to
     * @param sweetness the sweetness profile (e.g., "50% Sugar", "Low Sugar")
     */
    public Dessert(int id, String itemName, double price, int stockQuantity, String category, String sweetness) {
        super(id, itemName, price, stockQuantity, category);
        this.sweetness = sweetness;
    }

    /**
     * Gets the sweetness profile of this dessert.
     *
     * @return the sweetness profile description
     */
    public String getSweetness() {
        return sweetness;
    }

    /**
     * Sets the sweetness profile of this dessert.
     *
     * @param sweetness the new sweetness profile
     */
    public void setSweetness(String sweetness) {
        this.sweetness = sweetness;
    }

    /**
     * Returns the special details for this dessert (sweetness profile information).
     *
     * @return a string representation of the sweetness profile
     */
    @Override
    public String getSpecialDetails() {
        return "[Sweetness Profile: " + sweetness + "]";
    }
}