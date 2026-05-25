package models;

public class Soup extends MenuItem {

    // --- OOP INTENT: SPECIALIZATION ---
    private boolean isSpicy;

    // CONSTRUCTOR
    public Soup(int id, String itemName, double price, int stockQuantity, String category, boolean isSpicy) {
        super(id, itemName, price, stockQuantity, category);
        this.isSpicy = isSpicy;
    }

    // ENCAPSULATION
    public boolean isSpicy() {
        return isSpicy;
    }

    public void setSpicy(boolean spicy) {
        this.isSpicy = spicy;
    }
}