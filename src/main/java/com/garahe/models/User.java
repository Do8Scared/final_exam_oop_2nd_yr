package com.garahe.models;

/**
 * Represents an Authenticated User in the system.
 * 
 * OOP Pillars demonstrated:
 * 1. Inheritance (Child): By using the `extends` keyword, this class inherits 
 *    the `id`, `name`, and `email` properties from the parent `Person` class,
 *    allowing us to reuse code without duplicating it.
 * 2. Encapsulation: The sensitive `password` field is kept strictly private
 *    and accessed safely through getters/setters.
 */
public class User extends Person {
    private String password;

    public User() {
        super();
    }

    public User(int id, String name, String email, String password) {
        super(id, name, email);
        this.password = password;
    }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
