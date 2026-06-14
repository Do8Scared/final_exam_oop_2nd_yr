package com.garahe.models;

/**
 * Factory for creating polymorphic MenuItem subclasses based on category.
 * Centralizes the category-to-subclass mapping so that MenuDAO and the UI layer
 * don't each maintain their own if/else chains.
 *
 * Adding a new category requires changes in only this class.
 */
public final class MenuItemFactory {

    /**
     * Private constructor to prevent instantiation of this factory utility.
     */
    private MenuItemFactory() {
    }

    /**
     * Creates the correct MenuItem subclass based on the category and special attribute.
     *
     * @param id             the menu item ID from the database
     * @param name           the item name
     * @param price          the item price
     * @param stock          the stock quantity
     * @param category       the category name (determines which subclass to create)
     * @param specialAttr    the raw special attribute string from the database
     * @return a polymorphic MenuItem instance (Beverage, Soup, Appetizer, etc.)
     */
    public static MenuItem create(int id, String name, double price, int stock,
                                  String category, String specialAttr) {
        if (specialAttr == null) specialAttr = "Standard";

        if (category.equalsIgnoreCase("Beverages") || category.equalsIgnoreCase("Beverage")) {
            int volume = parseIntOrDefault(specialAttr.replace("ml", "").trim(), 500, "beverage volume");
            return new Beverage(id, name, price, stock, category, volume);

        } else if (category.equalsIgnoreCase("Appetizer")) {
            int pieces = parseIntOrDefault(specialAttr, 1, "appetizer pieces");
            return new Appetizer(id, name, price, stock, category, pieces);

        } else if (category.equalsIgnoreCase("Dessert")) {
            return new Dessert(id, name, price, stock, category, specialAttr);

        } else if (category.equalsIgnoreCase("Soup")) {
            boolean isSpicy = specialAttr.equalsIgnoreCase("Spicy");
            return new Soup(id, name, price, stock, category, isSpicy);

        } else if (category.equalsIgnoreCase("Rice Bowl")) {
            return new RiceBowl(id, name, price, stock, category, specialAttr);

        } else if (category.equalsIgnoreCase("Add-Ons") || category.equalsIgnoreCase("Add-On")) {
            boolean isCondiment = specialAttr.equalsIgnoreCase("Condiment");
            return new AddOn(id, name, price, stock, category, isCondiment);

        } else {
            System.out.println(">> [WARN] Unrecognized menu category '" + category
                    + "' for item ID " + id + ". Loading as a generic menu item.");
            return new MenuItem(id, name, price, stock, category);
        }
    }

    /**
     * Returns the context-aware prompt text for the special attribute input,
     * based on the selected category. This allows the UI layer to display the
     * correct prompt without knowing the category-specific details.
     *
     * @param category the category name
     * @return the prompt string for the special attribute input
     */
    public static String getSpecialAttributePrompt(String category) {
        if (category.equalsIgnoreCase("Beverages") || category.equalsIgnoreCase("Beverage")) {
            return "Enter Volume (e.g., 500ml): ";
        } else if (category.equalsIgnoreCase("Soup")) {
            return "Is it spicy? (Type 'Spicy' or 'Non-Spicy'): ";
        } else if (category.equalsIgnoreCase("Appetizer")) {
            return "Enter number of pieces (e.g., 6): ";
        } else if (category.equalsIgnoreCase("Dessert")) {
            return "Enter sweetness profile (e.g., 50% Sugar): ";
        } else if (category.equalsIgnoreCase("Rice Bowl")) {
            return "Enter main protein (e.g., Beef, Pork): ";
        } else if (category.equalsIgnoreCase("Add-Ons") || category.equalsIgnoreCase("Add-On")) {
            return "Type 'Condiment' or 'Solid Food': ";
        } else {
            return "Enter Special Attribute (or press Enter for Standard): ";
        }
    }

    /**
     * Safely parses a string to an integer with a fallback value if parsing fails.
     *
     * @param value    the string value to parse
     * @param fallback the default value if parsing fails
     * @param context  a description of what value is being parsed (for error logging)
     * @return the parsed integer, or the fallback value if parsing fails
     */
    private static int parseIntOrDefault(String value, int fallback, String context) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            System.out.println(">> [WARN] Could not parse " + context + " from '" + value + "'. Using " + fallback + ".");
            return fallback;
        }
    }
}
