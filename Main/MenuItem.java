public class MenuItem {

    // --- OOP CONCEPT: ENCAPSULATION & DATA HIDING (Module 2) ---
    // We use the 'private' access modifier so these variables cannot be changed
    // directly by other classes. This protects the integrity of our menu data.
    private final int id;
    private String itemName;
    private double price;
    private int stockQuantity;
    private String category;

    // CONSTRUCTORS
    public MenuItem(int id, String itemName, double price, int stockQuantity, String category) {
        this.id = id;
        this.itemName = itemName;
        this.price = price;
        this.stockQuantity = stockQuantity;
        this.category = category;
    }

    // --- GETTERS (Read Access) ---
    // We provide controlled ways for other classes to view the data.
    public int getId() {
        return id;
    }
    public String getItemName() {
        return itemName;
    }
    public double getPrice() {
        return price;
    }
    public int getQuantity() {
        return stockQuantity;
    }
    public String getCategory() {
        return category;
    }

    // --- SETTERS & VALIDATION (Controlled Write Access) ---
    // Notice there is NO setId() method here. That is intentional to protect the final ID.
    // For price and stock, we use setters to add "security checkpoints" (if-statements)
    // ensuring no negative numbers ever reach our Supabase database.

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public void setPrice(double newPrice) {
        if (newPrice >= 0) {
            this.price = newPrice;
        } else {
            System.out.println("Error: Price cannot be negative.");
        }
    }

    public void setStockQuantity(int newQuantity) {
        if (newQuantity >= 0) {
            this.stockQuantity = newQuantity;
        } else {
            System.out.println("Error: Stock cannot be negative.");
        }
    }
}