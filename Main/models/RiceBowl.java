package models;

public class RiceBowl extends MenuItem {

    // --- OOP INTENT: SPECIALIZATION ---
    private String mainProtein;

    // CONSTRUCTOR
    public RiceBowl(int id, String itemName, double price, int stockQuantity, String category, String mainProtein) {
        super(id, itemName, price, stockQuantity, category);
        this.mainProtein = mainProtein;
    }

    // ENCAPSULATION
    public String getMainProtein() {
        return mainProtein;
    }

    public void setMainProtein(String mainProtein) {
        this.mainProtein = mainProtein;
    }
}