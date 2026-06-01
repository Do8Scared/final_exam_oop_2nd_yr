package Main;

import config.Dotenv;
import database.*;
import models.*;

import java.util.*;

/**
 * Main point-of-sale system for Garahe Ni Mateicla.
 * Handles customer transactions, menu management, and admin operations.
 */
public class Main {

    private static final Scanner scanner = new Scanner(System.in);
    private static int failedPinAttempts = 0;
    private static long lockoutEndTime = 0;

    public static void main(String[] args) {
        Dotenv.loadIfPresent();

        boolean isRunning = true;

        System.out.println("=========================================");
        System.out.println("   WELCOME TO GARAHE NI MATEICLA (POS)   ");
        System.out.println("=========================================");

        while (isRunning) {
            System.out.println("\n--- MAIN MENU ---");
            System.out.println("[1] Start New Transaction (Jollibee POS Flow)");
            System.out.println("[2] View Full Live Menu");
            System.out.println("[3] View Menu by Category");
            System.out.println("[4] Admin: Add New Menu Item");
            System.out.println("[5] Exit System");

            int choice = getValidIntegerInput("Select an option: ");

            switch (choice) {
                case 1:
                    startNewOrder();
                    break;
                case 2:
                    viewLiveMenu();
                    break;
                case 3:
                    handleCategoryFilter();
                    break;
                case 4:
                    if (authenticateAdmin()) {
                        handleAdminAddNewItem();
                    }
                    break;
                case 5:
                    isRunning = false;
                    System.out.println("Shutting down the POS terminal. Goodbye!");
                    break;
                default:
                    System.out.println("Invalid selection. Please try again.");
            }
        }
        scanner.close();
    }

    /**
     * Manages the order flow: cart building, item editing, and checkout.
     */
    private static void startNewOrder() {
        List<CartItem> activeCart = new ArrayList<>();
        boolean isOrdering = true;

        while (isOrdering) {
            printLiveDashboard(activeCart);

            System.out.println("\n[1] Add Item to Cart");
            System.out.println("[2] Edit Item Quantity");
            System.out.println("[3] Remove Item from Cart");
            System.out.println("[4] PROCEED TO CHECKOUT");
            System.out.println("[5] Cancel Transaction (Discard Cart)");

            int choice = getValidIntegerInput("\nCashier Action: ");

            switch (choice) {
                case 1:
                    boolean addingItem = true;
                    while(addingItem) {
                        int itemId = getValidIntegerInput("Enter Item ID to add (or 0 to go back): ");
                        if (itemId == 0) break;

                        MenuItem selectedItem = database.MenuDAO.fetchItemById(itemId);
                        if (selectedItem != null) {
                            int qty = getValidIntegerInput("Enter quantity for " + selectedItem.getItemName() + ": ");

                            CartItem existingItem = null;
                            for (CartItem c : activeCart) {
                                if (c.getItem().getId() == selectedItem.getId()) {
                                    existingItem = c;
                                    break;
                                }
                            }

                            int currentQtyInCart = (existingItem != null) ? existingItem.getQuantity() : 0;
                            int totalRequestedQty = currentQtyInCart + qty;

                            if (qty > 0 && selectedItem.getStockQuantity() >= totalRequestedQty) {
                                if (existingItem != null) {
                                    existingItem.setQuantity(totalRequestedQty);
                                    System.out.println(">> SUCCESS: Updated " + selectedItem.getItemName() + " to " + totalRequestedQty + " items.");
                                } else {
                                    activeCart.add(new CartItem(selectedItem, qty));
                                    System.out.println(">> SUCCESS: Added to cart.");
                                }
                                addingItem = false;
                            } else if (qty <= 0) {
                                System.out.println(">> ERROR: Quantity must be greater than zero.");
                            } else {
                                System.out.println(">> ERROR: Insufficient stock. You have " + currentQtyInCart + " in cart, and cloud stock is " + selectedItem.getStockQuantity() + ".");
                            }
                        } else {
                            System.out.println(">> ERROR: ID not found. Please try again.");
                        }
                    }
                    break;

                case 2:
                    editCartItem(activeCart);
                    break;

                case 3:
                    removeCartItem(activeCart);
                    break;

                case 4:
                    if (activeCart.isEmpty()) {
                        System.out.println(">> Cannot checkout an empty cart!");
                    } else {
                        System.out.println("\n--- CHECKOUT FULFILLMENT ---");
                        System.out.println("[1] Dine-In");
                        System.out.println("[2] Take-Out");
                        int typeChoice = getValidIntegerInput("Select Order Type: ");
                        orders.OrderDAO fulfillment = (typeChoice == 2) ? new orders.TakeOutOrder(getConfiguredTakeOutFee()) : new orders.DineInOrder();
                        String orderType = (typeChoice == 2) ? "Take-Out" : "Dine-In";

                        System.out.println("\n--- PAYMENT METHOD ---");
                        System.out.println("[1] Cash");
                        System.out.println("[2] GCash");
                        System.out.println("[3] Maya");
                        int payChoice = getValidIntegerInput("Select Payment: ");

                        String paymentMethod = "Cash";
                        if (payChoice == 2) paymentMethod = "GCash";
                        if (payChoice == 3) paymentMethod = "Maya";

                        double amountTendered = 0;
                        double packagingFee = fulfillment.getPackagingFee();
                        if (paymentMethod.equals("Cash")) {
                            double subtotal = 0;
                            for(CartItem item : activeCart) subtotal += item.getSubtotal();

                            double grandTotal = Math.round((subtotal + packagingFee) * 100.0) / 100.0;

                            System.out.println("\nGrand Total to Pay: ₱" + String.format("%.2f", grandTotal));
                            while (true) {
                                amountTendered = getValidDoubleInput("Enter Amount Tendered: ₱");
                                if (amountTendered >= grandTotal) {
                                    break;
                                } else {
                                    System.out.println(">> [ERROR] Short payment! Amount tendered must meet the grand total.");
                                }
                            }
                        }

                        System.out.println("\n>> Connecting to Supabase Cloud securely...");
                        boolean success = database.OrderDAO.processCheckout(activeCart, orderType, paymentMethod, amountTendered, packagingFee);

                        if (success) {
                            activeCart.clear();
                            isOrdering = false;
                        } else {
                            System.out.println(">> System recovered safely. You can try checking out again or edit items.");
                        }
                    }
                    break;

                case 5:
                    System.out.println(">> Transaction Cancelled. Cart discarded.");
                    isOrdering = false;
                    break;

                default:
                    System.out.println(">> Invalid action.");
            }
        }
    }

    /**
     * Displays a numbered list of menu categories and allows the user to view items by category.
     */
    private static void handleCategoryFilter() {
        List<String> categories = database.MenuDAO.getActiveCategories();

        if (categories.isEmpty()) {
            System.out.println(">> No categories found in the database.");
            return;
        }

        System.out.println("\n--- DYNAMIC MENU CATEGORIES ---");
        for (int i = 0; i < categories.size(); i++) {
            System.out.println("[" + (i + 1) + "] " + categories.get(i));
        }
        System.out.println("[" + (categories.size() + 1) + "] Go Back");

        int choice = getValidIntegerInput("Select a category: ");

        if (choice > 0 && choice <= categories.size()) {
            String selectedCategory = categories.get(choice - 1);
            database.MenuDAO.printItemsByCategory(selectedCategory);
        } else if (choice != categories.size() + 1) {
            System.out.println(">> Invalid selection.");
        }
    }

    /**
     * Guides the admin through creating a new menu item with dynamic category selection
     * and context-aware prompts for special attributes (volume, spice level, protein, etc.).
     */
    private static void handleAdminAddNewItem() {
        System.out.println("\n--- ADD NEW MENU ITEM ---");
        String newName = getNonEmptyInput("Enter Item Name: ");
        double newPrice = getPositiveDoubleInput("Enter Price: ");
        int newStock = getNonNegativeIntegerInput("Enter Stock Quantity: ");

        List<String> categories = database.MenuDAO.getActiveCategories();
        System.out.println("\n--- SELECT CATEGORY ---");
        for (int i = 0; i < categories.size(); i++) {
            System.out.println("[" + (i + 1) + "] " + categories.get(i));
        }
        int newCatOption = categories.size() + 1;
        System.out.println("[" + newCatOption + "] + Create New Category");

        int catChoice;
        while (true) {
            catChoice = getValidIntegerInput("Select a category option: ");
            if (catChoice >= 1 && catChoice <= newCatOption) break;
            System.out.println(">> [ERROR] Invalid selection.");
        }

        String newCategory;
        if (catChoice == newCatOption) {
            newCategory = getNonEmptyInput("Enter New Category Name (e.g., Sushi): ");
        } else {
            newCategory = categories.get(catChoice - 1);
        }

        String specialAttrPrompt = "Enter Special Attribute (or press Enter for Standard): ";
        if (newCategory.equalsIgnoreCase("Beverages") || newCategory.equalsIgnoreCase("Beverage")) {
            specialAttrPrompt = "Enter Volume (e.g., 500ml): ";
        } else if (newCategory.equalsIgnoreCase("Soup")) {
            specialAttrPrompt = "Is it spicy? (Type 'Spicy' or 'Non-Spicy'): ";
        } else if (newCategory.equalsIgnoreCase("Appetizer")) {
            specialAttrPrompt = "Enter number of pieces (e.g., 6): ";
        } else if (newCategory.equalsIgnoreCase("Dessert")) {
            specialAttrPrompt = "Enter sweetness profile (e.g., 50% Sugar): ";
        } else if (newCategory.equalsIgnoreCase("Rice Bowl")) {
            specialAttrPrompt = "Enter main protein (e.g., Beef, Pork): ";
        } else if (newCategory.equalsIgnoreCase("Add-Ons") || newCategory.equalsIgnoreCase("Add-On")) {
            specialAttrPrompt = "Type 'Condiment' or 'Solid Food': ";
        }

        System.out.print(specialAttrPrompt);
        String specialAttr = scanner.nextLine().trim();
        if (specialAttr.isEmpty()) {
            specialAttr = "Standard";
        }

        MenuItem newItem = new MenuItem(newName, newPrice, newStock, newCategory);
        MenuManager.addMenuItem(newItem, specialAttr, "admin");
    }

    /**
     * Displays the complete menu from the database with all items, prices, and stock levels.
     */
    private static void viewLiveMenu() {
        System.out.println("\n--- LIVE CLOUD MENU ---");
        String sql = "SELECT * FROM menu_items ORDER BY id ASC";
        database.MenuDAO.executeSelectQuery(sql, null);
    }

    /**
     * Displays the live cart contents with item quantities and subtotal.
     *
     * @param cart the list of cart items to display
     */
    private static void printLiveDashboard(List<CartItem> cart) {
        System.out.println("\n========================================");
        System.out.println("          POS LIVE DASHBOARD            ");
        System.out.println("========================================");
        if (cart.isEmpty()) {
            System.out.println("  [ Cart is currently empty ]");
        } else {
            double subtotal = 0;
            for (int i = 0; i < cart.size(); i++) {
                CartItem c = cart.get(i);
                double itemTotal = c.getSubtotal();
                subtotal += itemTotal;

                System.out.println(" [" + (i + 1) + "] " + c.getItem().getItemName() +
                        " (x" + c.getQuantity() + ") -> ₱" + String.format("%.2f", itemTotal));
            }
            System.out.println("----------------------------------------");
            System.out.println(" RUNNING SUBTOTAL: ₱" + String.format("%.2f", subtotal));
        }
        System.out.println("========================================");
    }

    /**
     * Allows the cashier to modify a cart item quantity or remove it entirely.
     *
     * @param cart the list of cart items to edit
     */
    private static void editCartItem(List<CartItem> cart) {
        if (cart.isEmpty()) return;
        int index = getValidIntegerInput("Enter the cart line number to edit: ") - 1;

        if (index >= 0 && index < cart.size()) {
            CartItem itemToEdit = cart.get(index);
            int newQty = getValidIntegerInput("Enter new quantity: ");

            if (newQty <= 0) {
                cart.remove(index);
                System.out.println(">> Item removed from cart.");
            } else if (newQty <= itemToEdit.getItem().getStockQuantity()) {
                itemToEdit.setQuantity(newQty);
                System.out.println(">> Quantity updated.");
            } else {
                System.out.println(">> ERROR: Cannot update. Only " + itemToEdit.getItem().getStockQuantity() + " available in stock.");
            }
        } else {
            System.out.println(">> Invalid line number.");
        }
    }

    /**
     * Removes an item from the cart by line number.
     *
     * @param cart the list of cart items
     */
    private static void removeCartItem(List<CartItem> cart) {
        if (cart.isEmpty()) return;
        int index = getValidIntegerInput("Enter the cart line number to remove: ") - 1;
        if (index >= 0 && index < cart.size()) {
            cart.remove(index);
            System.out.println(">> Item successfully removed.");
        } else {
            System.out.println(">> Invalid line number.");
        }
    }

    /**
     * Prompts the user for an integer input and repeats until a valid integer is provided.
     *
     * @param prompt the message to display to the user
     * @return the parsed integer value
     */
    private static int getValidIntegerInput(String prompt) {
        while (true) {
            try {
                System.out.print(prompt);
                return Integer.parseInt(scanner.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.println(">> [SYSTEM ERROR] Invalid input. Please type a number.");
            }
        }
    }

    /**
     * Prompts the user for a floating-point monetary input and repeats until valid.
     *
     * @param prompt the message to display to the user
     * @return the parsed double value
     */
    private static double getValidDoubleInput(String prompt) {
        while (true) {
            try {
                System.out.print(prompt);
                return Double.parseDouble(scanner.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.println(">> [SYSTEM ERROR] Invalid monetary value. Please type a number.");
            }
        }
    }

    /**
     * Prompts the user for non-empty text input and repeats until provided.
     *
     * @param prompt the message to display to the user
     * @return the trimmed string input
     */
    private static String getNonEmptyInput(String prompt) {
        while (true) {
            System.out.print(prompt);
            String value = scanner.nextLine().trim();
            if (!value.isEmpty()) {
                return value;
            }
            System.out.println(">> [SYSTEM ERROR] This field cannot be blank.");
        }
    }

    /**
     * Prompts the user for a positive double value (must be > 0).
     *
     * @param prompt the message to display to the user
     * @return the positive double value
     */
    private static double getPositiveDoubleInput(String prompt) {
        while (true) {
            double value = getValidDoubleInput(prompt);
            if (value > 0) {
                return value;
            }
            System.out.println(">> [SYSTEM ERROR] Value must be greater than zero.");
        }
    }

    /**
     * Prompts the user for a non-negative integer value (must be >= 0).
     *
     * @param prompt the message to display to the user
     * @return the non-negative integer value
     */
    private static int getNonNegativeIntegerInput(String prompt) {
        while (true) {
            int value = getValidIntegerInput(prompt);
            if (value >= 0) {
                return value;
            }
            System.out.println(">> [SYSTEM ERROR] Value cannot be negative.");
        }
    }

    /**
     * Retrieves the configured take-out fee from environment variables or system properties.
     * Falls back to 20.00 if not configured or invalid.
     *
     * @return the take-out fee amount
     */
    private static double getConfiguredTakeOutFee() {
        String feeValue = System.getenv("POS_TAKEOUT_FEE");
        if (feeValue == null || feeValue.isBlank()) {
            feeValue = System.getProperty("pos.takeout.fee");
        }

        if (feeValue == null || feeValue.isBlank()) {
            System.out.println(">> [WARN] POS_TAKEOUT_FEE is not configured. Using default take-out fee of ₱20.00.");
            return 20.00;
        }

        try {
            return Math.round(Double.parseDouble(feeValue.trim()) * 100.0) / 100.0;
        } catch (NumberFormatException e) {
            System.out.println(">> [WARN] Invalid POS_TAKEOUT_FEE value. Using default take-out fee of ₱20.00.");
            return 20.00;
        }
    }

    /**
     * Authenticates admin access with brute-force protection.
     * Locks the system for 30 seconds after 3 failed PIN attempts.
     * Returns false if PIN is not configured (fail-secure design).
     *
     * @return true if admin credentials are valid, false otherwise
     */
    private static boolean authenticateAdmin() {
        if (System.currentTimeMillis() < lockoutEndTime) {
            long remainingSeconds = (lockoutEndTime - System.currentTimeMillis()) / 1000;
            System.out.println("\n>> [SECURITY ALERT] System locked due to multiple failed attempts. Try again in " + remainingSeconds + " seconds.");
            return false;
        }

        String configuredPin = System.getenv("POS_ADMIN_PIN");
        if (configuredPin == null || configuredPin.isBlank()) {
            configuredPin = System.getProperty("pos.admin.pin");
        }
        if (configuredPin == null || configuredPin.isBlank()) {
            System.out.println("\n>> [CRITICAL] Admin PIN is not configured in the environment. Admin access is disabled for security.");
            return false;
        }

        System.out.print("\n[SECURITY] Enter 4-digit Admin PIN: ");
        String pin = scanner.nextLine().trim();

        if (pin.equals(configuredPin)) {
            failedPinAttempts = 0;
            System.out.println(">> [SYSTEM] Access Granted.");
            return true;
        } else {
            failedPinAttempts++;
            System.out.println(">> [SECURITY ALERT] Invalid PIN.");

            if (failedPinAttempts >= 3) {
                System.out.println(">> [SECURITY ALERT] Maximum attempts reached. Admin console locked for 30 seconds.");
                lockoutEndTime = System.currentTimeMillis() + 30000;
            }
            return false;
        }
    }
}

