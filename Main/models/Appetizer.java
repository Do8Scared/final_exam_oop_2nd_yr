package models;

/**
 * Represents an appetizer menu item with a piece count.
 * Extends MenuItem with an additional piecesCount attribute.
 */
public class Appetizer extends MenuItem {

    private int piecesCount;

    /**
     * Constructs an Appetizer item with a specified piece count.
     *
     * @param id the menu item ID assigned by the database
     * @param itemName the name of the appetizer
     * @param price the price of the appetizer
     * @param stockQuantity the number of units in stock
     * @param category the category this item belongs to
     * @param piecesCount the number of pieces in this appetizer serving
     */
    public Appetizer(int id, String itemName, double price, int stockQuantity, String category, int piecesCount) {
        super(id, itemName, price, stockQuantity, category);
        this.piecesCount = piecesCount;
    }

    /**
     * Gets the number of pieces in this appetizer serving.
     *
     * @return the piece count
     */
    public int getPiecesCount() {
        return piecesCount;
    }

    /**
     * Sets the number of pieces. Must be greater than 0.
     *
     * @param piecesCount the new piece count
     */
    public void setPiecesCount(int piecesCount) {
        if (piecesCount > 0) {
            this.piecesCount = piecesCount;
        } else {
            System.out.println("Error: Piece count must be greater than 0.");
        }
    }

    /**
     * Returns the special details for this appetizer (piece count information).
     *
     * @return a string representation of the serving size
     */
    @Override
    public String getSpecialDetails() {
        return "[Serving: " + getPiecesCount() + " pieces]";
    }
}
