package models;

// --- OOP INTENT: INHERITANCE (Module 3) ---
// By using the 'extends' keyword, models.Beverage instantly inherits the locked-down
// ID, name, price, stock, and category attributes from the models.MenuItem parent class.
public class Beverage extends MenuItem {

    // --- OOP INTENT: SPECIALIZATION ---
    // This is the unique trait. General food items don't track milliliters, 
    // but beverages (like Ramune or Matcha Iced Tea) do.
    private int volumeInMl;

    // CONSTRUCTOR
    public Beverage(int id, String itemName, double price, int stockQuantity, String category, int volumeInMl) {

        // --- OOP INTENT: CONSTRUCTOR CHAINING ('super' keyword) ---
        // This MUST be the first line. We pass the main data UP to the models.MenuItem
        // constructor so it can securely assign the 'final id' and other core details.
        super(id, itemName, price, stockQuantity, category);

        // After the parent is successfully built, we handle our unique child data.
        this.volumeInMl = volumeInMl;
    }

    // --- OOP INTENT: CHILD ENCAPSULATION ---
    // We protect the unique attribute with a getter and a validated setter.

    public int getVolumeInMl() {
        return volumeInMl;
    }

    public void setVolumeInMl(int volumeInMl) {
        if (volumeInMl > 0) {
            this.volumeInMl = volumeInMl;
        } else {
            System.out.println("Error: models.Beverage volume must be greater than 0 ml.");
        }
    }
}