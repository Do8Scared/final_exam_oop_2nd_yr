package models;

/**
 * Represents a rice bowl menu item with a protein specification.
 * Extends MenuItem with an additional mainProtein attribute.
 */
public class RiceBowl extends MenuItem {

    private String mainProtein;

    /**
     * Constructs a RiceBowl item with a specified protein.
     *
     * @param id the menu item ID assigned by the database
     * @param itemName the name of the rice bowl
     * @param price the price of the rice bowl
     * @param stockQuantity the number of units in stock
     * @param category the category this item belongs to
     * @param mainProtein the main protein ingredient (e.g., Beef, Pork, Chicken)
     */
    public RiceBowl(int id, String itemName, double price, int stockQuantity, String category, String mainProtein) {
        super(id, itemName, price, stockQuantity, category);
        this.mainProtein = mainProtein;
    }

    /**
     * Gets the main protein ingredient of this rice bowl.
     *
     * @return the protein name
     */
    public String getMainProtein() {
        return mainProtein;
    }

    /**
     * Sets the main protein ingredient of this rice bowl.
     *
     * @param mainProtein the new protein ingredient
     */
    public void setMainProtein(String mainProtein) {
        this.mainProtein = mainProtein;
    }

    /**
     * Returns the special details for this rice bowl (protein information).
     *
     * @return a string representation of the protein ingredient
     */
    @Override
    public String getSpecialDetails() {
        return "[Protein: " + getMainProtein() + "]";
    }
}