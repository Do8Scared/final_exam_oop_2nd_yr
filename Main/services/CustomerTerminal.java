package services;

import config.Dotenv;
import database.MenuDAO;
import Main.MenuManager;
import models.*;

import java.util.List;
import java.util.Scanner;

/**
 * Console-based Customer terminal user interface.
 * Contains all menu display, user interaction loops, and cart rendering.
 *
 * This class is the only class that directly interacts with the console (System.out/in).
 * To transition to a GUI, replace this class with a GUI implementation that calls
 * the same underlying services (CartService, AuthService, CheckoutService).
 */
public class CustomerTerminal {

    private final InputHelper input;
    private final AuthService auth;

    /**
     * Constructs a CustomerTerminal with standard console I/O.
     */
    public CustomerTerminal() {
        this.input = new InputHelper(new Scanner(System.in));
        this.auth = new AuthService();
    }

    /**
     * Starts the terminal main loop.
     * Displays the main portal and handles user actions until exit.
     */
    public void run() {
        Dotenv.loadIfPresent();

        boolean isRunning = true;

        System.out.println("=========================================");
        System.out.println("   WELCOME TO GARAHE NI MATEICLA APP     ");
        System.out.println("=========================================");

        while (isRunning) {
            System.out.println("\n--- GARAHE NI MATEICLA PORTAL ---");
            System.out.println("[1] Enter as Customer");
            System.out.println("[2] Enter as Merchant");
            System.out.println("[3] Exit System");

            int portalChoice = input.getValidIntegerInput("Select an option: ");

            if (portalChoice == 1) {
                runCustomerFlow();
            } else if (portalChoice == 2) {
                if (authenticateAdmin()) {
                    runMerchantFlow();
                }
            } else if (portalChoice == 3) {
                isRunning = false;
                System.out.println("Shutting down the app. Goodbye!");
            } else {
                System.out.println("Invalid selection. Please try again.");
            }
        }
        input.close();
    }

    private void runCustomerFlow() {
        boolean inCustomer = true;
        while (inCustomer) {
            System.out.println("\n--- CUSTOMER MENU ---");
            System.out.println("[1] Order Food Now");
            System.out.println("[2] View Full Live Menu");
            System.out.println("[3] View Menu by Category");
            System.out.println("[4] Return to Main Portal");

            int choice = input.getValidIntegerInput("Select an option: ");

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
                    inCustomer = false;
                    break;
                default:
                    System.out.println("Invalid selection. Please try again.");
            }
        }
    }

    private void runMerchantFlow() {
        boolean inMerchant = true;
        while (inMerchant) {
            System.out.println("\n--- MERCHANT DASHBOARD ---");
            System.out.println("[1] Add New Menu Item");
            System.out.println("[2] Return to Main Portal");

            int choice = input.getValidIntegerInput("Select an option: ");

            switch (choice) {
                case 1:
                    handleAdminAddNewItem();
                    break;
                case 2:
                    inMerchant = false;
                    break;
                default:
                    System.out.println("Invalid selection. Please try again.");
            }
        }
    }

    /**
     * Manages the order flow: cart building, item editing, and checkout.
     * Creates a fresh CartService for each transaction.
     */
    private void startNewOrder() {
        CartService cart = new CartService();
        CheckoutService checkout = new CheckoutService(input);
        boolean isOrdering = true;

        while (isOrdering) {
            printLiveDashboard(cart);

            System.out.println("\n[1] Add Item to Cart");
            System.out.println("[2] Edit Item Quantity");
            System.out.println("[3] Remove Item from Cart");
            System.out.println("[4] PROCEED TO CHECKOUT");
            System.out.println("[5] Cancel Transaction (Discard Cart)");

            int choice = input.getValidIntegerInput("\nCustomer Action: ");

            switch (choice) {
                case 1:
                    handleAddItem(cart);
                    break;

                case 2:
                    handleEditItem(cart);
                    break;

                case 3:
                    handleRemoveItem(cart);
                    break;

                case 4:
                    if (cart.isEmpty()) {
                        System.out.println(">> Cannot checkout an empty cart!");
                    } else {
                        boolean success = checkout.processCheckout(cart.getItems());
                        if (success) {
                            cart.clear();
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
     * Handles the "Add Item" flow — prompts for item ID and quantity,
     * delegates to CartService for stock validation.
     */
    private void handleAddItem(CartService cart) {
        boolean addingItem = true;
        while (addingItem) {
            int itemId = input.getValidIntegerInput("Enter Item ID to add (or 0 to go back): ");
            if (itemId == 0) break;

            // Prompt for quantity before delegating to CartService
            MenuItem peekedItem = MenuDAO.fetchItemById(itemId);
            if (peekedItem == null) {
                System.out.println(">> ERROR: ID not found. Please try again.");
                continue;
            }

            int qty = input.getValidIntegerInput("Enter quantity for " + peekedItem.getItemName() + ": ");
            CartService.AddResult result = cart.addItem(itemId, qty);

            if (result.isSuccess()) {
                System.out.println(">> SUCCESS: " + result.getMessage());
                addingItem = false;
            } else {
                System.out.println(">> ERROR: " + result.getMessage());
            }
        }
    }

    /**
     * Handles the "Edit Item Quantity" flow.
     */
    private void handleEditItem(CartService cart) {
        if (cart.isEmpty()) {
            System.out.println(">> Cart is empty. Nothing to edit.");
            return;
        }
        int index = input.getValidIntegerInput("Enter the cart line number to edit: ") - 1;
        int newQty = input.getValidIntegerInput("Enter new quantity: ");
        String result = cart.editItemQuantity(index, newQty);
        System.out.println(">> " + result);
    }

    /**
     * Handles the "Remove Item" flow.
     */
    private void handleRemoveItem(CartService cart) {
        if (cart.isEmpty()) {
            System.out.println(">> Cart is empty. Nothing to remove.");
            return;
        }
        int index = input.getValidIntegerInput("Enter the cart line number to remove: ") - 1;
        String result = cart.removeItem(index);
        System.out.println(">> " + result);
    }

    /**
     * Displays a numbered list of menu categories and allows the user to view items by category.
     */
    private void handleCategoryFilter() {
        List<String> categories = MenuDAO.getActiveCategories();

        if (categories.isEmpty()) {
            System.out.println(">> No categories found in the database.");
            return;
        }

        System.out.println("\n--- DYNAMIC MENU CATEGORIES ---");
        for (int i = 0; i < categories.size(); i++) {
            System.out.println("[" + (i + 1) + "] " + categories.get(i));
        }
        System.out.println("[" + (categories.size() + 1) + "] Go Back");

        int choice = input.getValidIntegerInput("Select a category: ");

        if (choice > 0 && choice <= categories.size()) {
            String selectedCategory = categories.get(choice - 1);
            MenuDAO.printItemsByCategory(selectedCategory);
        } else if (choice != categories.size() + 1) {
            System.out.println(">> Invalid selection.");
        }
    }

    /**
     * Guides the admin through creating a new menu item with dynamic category selection
     * and context-aware prompts from MenuItemFactory.
     */
    private void handleAdminAddNewItem() {
        System.out.println("\n--- ADD NEW MENU ITEM ---");
        String newName = input.getNonEmptyInput("Enter Item Name: ");
        double newPrice = input.getPositiveDoubleInput("Enter Price: ");
        int newStock = input.getNonNegativeIntegerInput("Enter Stock Quantity: ");

        List<String> categories = MenuDAO.getActiveCategories();
        System.out.println("\n--- SELECT CATEGORY ---");
        for (int i = 0; i < categories.size(); i++) {
            System.out.println("[" + (i + 1) + "] " + categories.get(i));
        }
        int newCatOption = categories.size() + 1;
        System.out.println("[" + newCatOption + "] + Create New Category");

        int catChoice;
        while (true) {
            catChoice = input.getValidIntegerInput("Select a category option: ");
            if (catChoice >= 1 && catChoice <= newCatOption) break;
            System.out.println(">> [ERROR] Invalid selection.");
        }

        String newCategory;
        if (catChoice == newCatOption) {
            newCategory = input.getNonEmptyInput("Enter New Category Name (e.g., Sushi): ");
        } else {
            newCategory = categories.get(catChoice - 1);
        }

        // Use MenuItemFactory for context-aware prompt (fixes #8 — centralized category logic)
        String specialAttrPrompt = MenuItemFactory.getSpecialAttributePrompt(newCategory);
        String specialAttr = input.readLine(specialAttrPrompt);
        if (specialAttr.isEmpty()) {
            specialAttr = "Standard";
        }

        MenuItem newItem = new MenuItem(newName, newPrice, newStock, newCategory);
        MenuManager.addMenuItem(newItem, specialAttr, "admin");
    }

    /**
     * Displays the complete active menu from the database.
     * No raw SQL — delegates to MenuDAO.printAllActiveItems().
     */
    private void viewLiveMenu() {
        System.out.println("\n--- LIVE CLOUD MENU ---");
        MenuDAO.printAllActiveItems();
    }

    /**
     * Displays the live cart contents with item quantities and subtotal.
     *
     * @param cart the CartService to display
     */
    private void printLiveDashboard(CartService cart) {
        System.out.println("\n========================================");
        System.out.println("               YOUR CART                ");
        System.out.println("========================================");
        if (cart.isEmpty()) {
            System.out.println("  [ Cart is currently empty ]");
        } else {
            List<CartItem> items = cart.getItems();
            double subtotal = 0;
            for (int i = 0; i < items.size(); i++) {
                CartItem c = items.get(i);
                double itemTotal = c.getSubtotal();
                subtotal += itemTotal;

                System.out.println(" [" + (i + 1) + "] " + c.getItem().getItemName() +
                        " (x" + c.getQuantity() + ") -> PHP " + String.format("%.2f", itemTotal));
            }
            System.out.println("----------------------------------------");
            System.out.println(" RUNNING SUBTOTAL: PHP " + String.format("%.2f", subtotal));
        }
        System.out.println("========================================");
    }

    /**
     * Authenticates admin access with brute-force protection.
     * Delegates to AuthService for constant-time PIN comparison and lockout management.
     *
     * @return true if admin credentials are valid, false otherwise
     */
    private boolean authenticateAdmin() {
        if (auth.isLockedOut()) {
            long remaining = auth.getRemainingLockoutSeconds();
            System.out.println("\n>> [SECURITY ALERT] System locked due to multiple failed attempts. Try again in " + remaining + " seconds.");
            return false;
        }

        String configuredPin = auth.getConfiguredPin();
        if (configuredPin == null) {
            System.out.println("\n>> [CRITICAL] Admin PIN is not configured in the environment. Admin access is disabled for security.");
            return false;
        }

        String pin = input.readLine("\n[SECURITY] Enter 4-digit Admin PIN: ");

        if (auth.validatePin(pin, configuredPin)) {
            System.out.println(">> [SYSTEM] Access Granted.");
            return true;
        } else {
            System.out.println(">> [SECURITY ALERT] Invalid PIN.");
            if (auth.getFailedAttempts() >= auth.getMaxAttempts()) {
                System.out.println(">> [SECURITY ALERT] Maximum attempts reached. Admin console locked for 30 seconds.");
            }
            return false;
        }
    }
}
