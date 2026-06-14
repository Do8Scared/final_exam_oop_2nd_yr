package com.garahe.models;

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
