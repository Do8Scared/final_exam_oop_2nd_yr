package services;

import models.CartItem;
import models.MenuItem;
import database.MenuDAO;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Manages the shopping cart for a single transaction.
 * Handles adding, editing, and removing items with stock validation.
 *
 * On every add/edit operation, the current stock is re-fetched from the database
 * to prevent stale-data race conditions (TOCTOU fix).
 */
public class CartService {

    private final List<CartItem> cart = new ArrayList<>();

    /**
     * Attempts to add an item to the cart by its menu item ID.
     * Re-fetches the item from the database to get the latest stock level.
     *
     * @param itemId the menu item ID to add
     * @param quantity the number of units to add
     * @return a result describing the outcome of the operation
     */
    public AddResult addItem(int itemId, int quantity) {
        if (quantity <= 0) {
            return new AddResult(false, "Quantity must be greater than zero.", null);
        }

        // Re-fetch from database to get latest stock (fixes stale stock TOCTOU race)
        MenuItem freshItem = MenuDAO.fetchItemById(itemId);
        if (freshItem == null) {
            return new AddResult(false, "ID not found. Please try again.", null);
        }

        CartItem existingItem = findByItemId(freshItem.getId());
        int currentQtyInCart = (existingItem != null) ? existingItem.getQuantity() : 0;
        int totalRequestedQty = currentQtyInCart + quantity;

        if (freshItem.getStockQuantity() < totalRequestedQty) {
            return new AddResult(false,
                    "Insufficient stock. You have " + currentQtyInCart + " in cart, and cloud stock is " + freshItem.getStockQuantity() + ".",
                    freshItem);
        }

        if (existingItem != null) {
            existingItem.setQuantity(totalRequestedQty);
            return new AddResult(true,
                    "Updated " + freshItem.getItemName() + " to " + totalRequestedQty + " items.",
                    freshItem);
        } else {
            cart.add(new CartItem(freshItem, quantity));
            return new AddResult(true, "Added to cart.", freshItem);
        }
    }

    /**
     * Edits the quantity of an item at the given cart index (0-based).
     * Re-fetches the item's stock from the database for validation.
     * If the new quantity is <= 0, the item is removed from the cart.
     *
     * @param index the 0-based index of the cart item
     * @param newQuantity the new quantity to set
     * @return a message describing the outcome
     */
    public String editItemQuantity(int index, int newQuantity) {
        if (index < 0 || index >= cart.size()) {
            return "Invalid line number.";
        }

        CartItem itemToEdit = cart.get(index);

        if (newQuantity <= 0) {
            cart.remove(index);
            return "Item removed from cart.";
        }

        // Re-fetch from database to get latest stock
        MenuItem freshItem = MenuDAO.fetchItemById(itemToEdit.getItem().getId());
        if (freshItem == null) {
            cart.remove(index);
            return "Item is no longer available. Removed from cart.";
        }

        if (newQuantity > freshItem.getStockQuantity()) {
            return "Cannot update. Only " + freshItem.getStockQuantity() + " available in stock.";
        }

        itemToEdit.setQuantity(newQuantity);
        return "Quantity updated.";
    }

    /**
     * Removes an item from the cart at the given index (0-based).
     *
     * @param index the 0-based index of the cart item to remove
     * @return a message describing the outcome
     */
    public String removeItem(int index) {
        if (index < 0 || index >= cart.size()) {
            return "Invalid line number.";
        }
        cart.remove(index);
        return "Item successfully removed.";
    }

    /**
     * Returns an unmodifiable view of the current cart items.
     *
     * @return the list of cart items (read-only)
     */
    public List<CartItem> getItems() {
        return Collections.unmodifiableList(cart);
    }

    /**
     * Checks whether the cart is empty.
     *
     * @return true if no items are in the cart
     */
    public boolean isEmpty() {
        return cart.isEmpty();
    }

    /**
     * Returns the number of items in the cart.
     *
     * @return the cart size
     */
    public int size() {
        return cart.size();
    }

    /**
     * Calculates the running subtotal of all items in the cart.
     *
     * @return the subtotal with consistent rounding
     */
    public double getSubtotal() {
        double subtotal = 0;
        for (CartItem item : cart) {
            subtotal += item.getSubtotal();
        }
        return Math.round(subtotal * 100.0) / 100.0;
    }

    /**
     * Clears all items from the cart.
     */
    public void clear() {
        cart.clear();
    }

    /**
     * Finds a cart item by its underlying menu item ID.
     *
     * @param menuItemId the menu item ID to search for
     * @return the matching CartItem, or null if not found
     */
    private CartItem findByItemId(int menuItemId) {
        for (CartItem c : cart) {
            if (c.getItem().getId() == menuItemId) {
                return c;
            }
        }
        return null;
    }

    /**
     * Represents the result of an add-to-cart operation.
     */
    public static class AddResult {
        private final boolean success;
        private final String message;
        private final MenuItem item;

        public AddResult(boolean success, String message, MenuItem item) {
            this.success = success;
            this.message = message;
            this.item = item;
        }

        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public MenuItem getItem() { return item; }
    }
}
