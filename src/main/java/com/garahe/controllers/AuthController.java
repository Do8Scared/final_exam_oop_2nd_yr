package com.garahe.controllers;

import com.garahe.database.UserDAO;
import com.garahe.database.UserDAOImpl;
import com.garahe.models.User;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    private final UserDAO userDAO;

    public AuthController() {
        // OOP Pillar: Polymorphism and Abstraction in action
        this.userDAO = new UserDAOImpl();
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody User user) {
        if (user.getName() == null || user.getEmail() == null || user.getPassword() == null) {
            return ResponseEntity.badRequest().body("{\"error\": \"Missing required fields\"}");
        }
        boolean success = userDAO.registerUser(user);
        if (success) {
            return ResponseEntity.ok("{\"message\": \"Registration successful\"}");
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"error\": \"Registration failed. Email might already exist.\"}");
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginPayload payload) {
        User user = userDAO.authenticateUser(payload.getEmail(), payload.getPassword());
        if (user != null) {
            // Remove sensitive info before returning to client
            user.setPassword(null);
            return ResponseEntity.ok(user);
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("{\"error\": \"Invalid email or password\"}");
        }
    }

    // DTO class
    public static class LoginPayload {
        private String email;
        private String password;

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }
}
