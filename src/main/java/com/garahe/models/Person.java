package com.garahe.models;

/**
 * Represents a generic Person in the system.
 * 
 * OOP Pillars demonstrated:
 * 1. Abstraction: This class is `abstract`, meaning you cannot instantiate a raw Person.
 *    It serves only as a foundational blueprint for more specific roles (like User).
 * 2. Encapsulation: All fields are marked `private` to prevent unauthorized access,
 *    and are only modifiable through public getters and setters.
 * 3. Inheritance (Parent): This is the base class that other entities inherit from.
 */
public abstract class Person {
    private int id;
    private String name;
    private String email;

    public Person() {}

    public Person(int id, String name, String email) {
        this.id = id;
        this.name = name;
        this.email = email;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}
