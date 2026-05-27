package models;

/**
 * Represents a menu item in the POS system.
 * Contains pricing, inventory, and category information.
 * Subclasses can override getSpecialDetails() for category-specific attributes.
 */
public class MenuItem {

    private final int id;
    private String itemName;
    private double price;
    private int stockQuantity;
    private final String category;

    /**
     * Constructs a MenuItem with a database-assigned ID.
     * Used when loading items from the database.
     *
     * @param id the menu item ID assigned by the database
     * @param itemName the name of the menu item
     * @param price the price of the item
     * @param stockQuantity the number of units in stock
     * @param category the category this item belongs to
     */
    public MenuItem(int id, String itemName, double price, int stockQuantity, String category) {
        this.id = id;
        this.itemName = sanitizeText(itemName, "Unnamed Item");
        this.price = Math.max(0, price);
        this.stockQuantity = Math.max(0, stockQuantity);
        this.category = sanitizeText(category, "Uncategorized");
    }

    /**
     * Constructs a new MenuItem without a database ID.
     * Used when creating brand-new items before insertion into the database.
     * The database will assign the real ID upon insertion.
     *
     * @param itemName the name of the menu item
     * @param price the price of the item
     * @param stockQuantity the number of units in stock
     * @param category the category this item belongs to
     */
    public MenuItem(String itemName, double price, int stockQuantity, String category) {
        this.id = 0;
        this.itemName = sanitizeText(itemName, "Unnamed Item");
        this.price = Math.max(0, price);
        this.stockQuantity = Math.max(0, stockQuantity);
        this.category = sanitizeText(category, "Uncategorized");
    }

    /**
     * Gets the menu item ID.
     *
     * @return the item ID
     */
    public int getId() {
        return id;
    }

    /**
     * Gets the item name.
     *
     * @return the item name
     */
    public String getItemName() {
        return itemName;
    }

    /**
     * Gets the item price.
     *
     * @return the price in pesos
     */
    public double getPrice() {
        return price;
    }

    /**
     * Gets the current stock quantity.
     *
     * @return the number of units available
     */
    public int getStockQuantity() {
        return stockQuantity;
    }

    /**
     * Gets the category name.
     *
     * @return the category name
     */
    public String getCategory() {
        return category;
    }

    /**
     * Sets the item name with validation.
     *
     * @param itemName the new item name
     */
    public void setItemName(String itemName) {
        this.itemName = sanitizeText(itemName, this.itemName);
    }

    /**
     * Sets the item price. Negative values are rejected.
     *
     * @param newPrice the new price
     */
    public void setPrice(double newPrice) {
        if (newPrice >= 0) {
            this.price = newPrice;
        } else {
            System.out.println("Error: Price cannot be negative.");
        }
    }

    /**
     * Sets the stock quantity. Negative values are rejected.
     *
     * @param newQuantity the new stock quantity
     */
    public void setStockQuantity(int newQuantity) {
        if (newQuantity >= 0) {
            this.stockQuantity = newQuantity;
        } else {
            System.out.println("Error: Stock cannot be negative.");
        }
    }

    /**
     * Removes leading/trailing whitespace and null values in a string field.
     *
     * @param value the input string
     * @param fallback the default value if input is null or empty
     * @return the sanitized string
     */
    private String sanitizeText(String value, String fallback) {
        if (value == null) {
            return fallback;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? fallback : trimmed;
    }

    /**
     * Returns category-specific details for this menu item.
     * Base implementation returns empty string. Subclasses override this method
     * to provide special attributes (volume, spice level, protein, etc.).
     *
     * @return special details as a string, or empty string if none
     */
    public String getSpecialDetails() {
        return "";
    }
}