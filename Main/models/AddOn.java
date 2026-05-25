package models;

public class AddOn extends MenuItem {

    // --- OOP INTENT: SPECIALIZATION ---
    private boolean isCondiment;

    // CONSTRUCTOR
    public AddOn(int id, String itemName, double price, int stockQuantity, String category, boolean isCondiment) {
        super(id, itemName, price, stockQuantity, category);
        this.isCondiment = isCondiment;
    }

    // ENCAPSULATION
    public boolean isCondiment() {
        return isCondiment;
    }

    public void setCondiment(boolean condiment) {
        this.isCondiment = condiment;
    }
}