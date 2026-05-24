public class MenuItems {
    private String itemName;
    private double price;
    private int stockQuantity;

    public MenuItems(String itemName, double price, int stockQuantity) {
        this.itemName = itemName;
        this.price = price;
        this.stockQuantity = stockQuantity;
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

    public void setPrice(double newPrice) {
        if (newPrice > 0) {
            this.price = newPrice;
        } else {
            System.out.println("Price cannot be negative");
        }
    }

    public void quantity(int newQuantity) {
        if (newQuantity > 0) {
            this.stockQuantity = newQuantity;
        } else {
            System.out.println("Quantity cannot be negative");
        }
    }

}