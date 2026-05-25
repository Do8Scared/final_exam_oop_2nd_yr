public class Appetizer extends MenuItem {

    private int piecesCount;

    public Appetizer(int id, String itemName, double price, int stockQuantity, String category, int piecesCount) {
        super(id, itemName, price, stockQuantity, category);
        this.piecesCount = piecesCount;

    }
    public int getPiecesCount() {
        return piecesCount;
    }
    public void setPiecesCount(int piecesCount) {
        if (piecesCount > 0 ) {
            this.piecesCount = piecesCount;
        } else {
            System.out.println("Invalid pieces count");
        }
    }
}
