package models;

/**
 * Represents a beverage menu item with volume specification.
 * Extends MenuItem with an additional volumeInMl attribute.
 */
public class Beverage extends MenuItem {

    private int volumeInMl;

    /**
     * Constructs a Beverage item with a specified volume.
     *
     * @param id the menu item ID assigned by the database
     * @param itemName the name of the beverage
     * @param price the price of the beverage
     * @param stockQuantity the number of units in stock
     * @param category the category this item belongs to
     * @param volumeInMl the volume of the beverage in milliliters
     */
    public Beverage(int id, String itemName, double price, int stockQuantity, String category, int volumeInMl) {
        super(id, itemName, price, stockQuantity, category);
        this.volumeInMl = volumeInMl;
    }

    /**
     * Gets the volume of the beverage in milliliters.
     *
     * @return the volume in ml
     */
    public int getVolumeInMl() {
        return volumeInMl;
    }

    /**
     * Sets the volume of the beverage. Must be greater than 0 ml.
     *
     * @param volumeInMl the new volume in milliliters
     */
    public void setVolumeInMl(int volumeInMl) {
        if (volumeInMl > 0) {
            this.volumeInMl = volumeInMl;
        } else {
            System.out.println("Error: Beverage volume must be greater than 0 ml.");
        }
    }

    /**
     * Returns the special details for this beverage (volume information).
     *
     * @return a string representation of the volume
     */
    @Override
    public String getSpecialDetails() {
        return "[Volume: " + getVolumeInMl() + "ml]";
    }
}