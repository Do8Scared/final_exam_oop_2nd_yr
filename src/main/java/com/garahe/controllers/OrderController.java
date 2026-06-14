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

        String txnId = TransactionDAO.processCheckout(
                cart,
                payload.getOrderType(),
                payload.getPaymentMethod(),
                payload.getAdditionalFee(),
                payload.getEmail(),
                payload.getCustomerName(),
                payload.getContactNumber(),
                payload.getDeliverTo(),
                payload.getNotes()
        );

        if (txnId != null) {
            return ResponseEntity.ok("{\"transactionId\": \"" + txnId + "\"}");
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"error\": \"Checkout failed. Please check stock.\"}");
        }
    }

    @GetMapping("/history")
    public ResponseEntity<String> getHistory(@RequestParam("email") String email) {
        if (email == null || email.isEmpty()) {
            return ResponseEntity.badRequest().body("Email is required");
        }
        String json = TransactionDAO.getUserTransactionsJson(email);
        return ResponseEntity.ok(json);
    }

    public static class CheckoutPayload {
        private String orderType;
        private String paymentMethod;
        private double additionalFee;
        private String email;
        private String customerName;
        private String contactNumber;
        private String deliverTo;
        private String notes;
        private List<CartItemPayload> items;

        public String getOrderType() { return orderType; }
        public void setOrderType(String orderType) { this.orderType = orderType; }

        public String getPaymentMethod() { return paymentMethod; }
        public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

        public double getAdditionalFee() { return additionalFee; }
        public void setAdditionalFee(double additionalFee) { this.additionalFee = additionalFee; }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }

        public String getCustomerName() { return customerName; }
        public void setCustomerName(String customerName) { this.customerName = customerName; }

        public String getContactNumber() { return contactNumber; }
        public void setContactNumber(String contactNumber) { this.contactNumber = contactNumber; }

        public String getDeliverTo() { return deliverTo; }
        public void setDeliverTo(String deliverTo) { this.deliverTo = deliverTo; }

        public String getNotes() { return notes; }
        public void setNotes(String notes) { this.notes = notes; }

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
