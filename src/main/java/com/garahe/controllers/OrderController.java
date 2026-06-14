package com.garahe.controllers;

import com.garahe.database.MenuDAO;
import com.garahe.database.TransactionDAO;
import com.garahe.models.CartItem;
import com.garahe.models.MenuItem;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/checkout")
@CrossOrigin(origins = "*")
public class OrderController {

    @PostMapping
    public ResponseEntity<String> checkout(@RequestBody CheckoutPayload payload) {
        if (payload.getItems() == null || payload.getItems().isEmpty()) {
            return ResponseEntity.badRequest().body("Cart is empty");
        }

        List<CartItem> cart = new ArrayList<>();
        for (CartItemPayload itemPayload : payload.getItems()) {
            MenuItem menuItem = MenuDAO.fetchItemById(itemPayload.getMenuItemId());
            if (menuItem == null) {
                return ResponseEntity.badRequest().body("Item with ID " + itemPayload.getMenuItemId() + " not found or inactive.");
            }
            cart.add(new CartItem(menuItem, itemPayload.getQuantity()));
        }

        boolean success = TransactionDAO.processCheckout(
                cart,
                payload.getOrderType(),
                payload.getPaymentMethod(),
                payload.getAdditionalFee()
        );

        if (success) {
            return ResponseEntity.ok("Checkout successful");
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Checkout failed. Please check stock or server logs.");
        }
    }

    public static class CheckoutPayload {
        private String orderType;
        private String paymentMethod;
        private double additionalFee;
        private List<CartItemPayload> items;

        public String getOrderType() { return orderType; }
        public void setOrderType(String orderType) { this.orderType = orderType; }

        public String getPaymentMethod() { return paymentMethod; }
        public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

        public double getAdditionalFee() { return additionalFee; }
        public void setAdditionalFee(double additionalFee) { this.additionalFee = additionalFee; }

        public List<CartItemPayload> getItems() { return items; }
        public void setItems(List<CartItemPayload> items) { this.items = items; }
    }

    public static class CartItemPayload {
        private int menuItemId;
        private int quantity;

        public int getMenuItemId() { return menuItemId; }
        public void setMenuItemId(int menuItemId) { this.menuItemId = menuItemId; }

        public int getQuantity() { return quantity; }
        public void setQuantity(int quantity) { this.quantity = quantity; }
    }
}
