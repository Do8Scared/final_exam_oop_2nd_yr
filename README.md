# Garahe ni Mateicla - Online Food Ordering System

An enterprise-grade, object-oriented Graphical User Interface (GUI) application simulating a modern food delivery and ordering platform (e.g., GrabFood, Foodpanda). Built purely with Java Swing, it features live cloud synchronization via Supabase (PostgreSQL), ACID database transactions, and a robust cart management system.

## 👨💻 Development Team
* **Mirexelle** (Lead Developer / Architect)
* **Matthew** (Core Logic)
* **Fliance** (Database Integration)
* **Ivan** (UI/UX & Cart Systems)
* **Jhaneya** (QA & Security)

---

## 🏗️ The 4 Pillars of Object-Oriented Programming (OOP)
This system was built strictly adhering to modern OOP design patterns. Here is how the 4 Pillars are implemented within the architecture:

### 1. Encapsulation (Data Hiding & Security)
We protect the internal state of our objects by keeping variables `private` and exposing them only through secure `public` getter and setter methods. 
* **Where we used it:** The `CartItem` and `MenuItem` classes. A `CartItem` securely binds a `MenuItem` to a specific `quantity`. You cannot directly alter the item's internal price or identity; you must use the controlled `.setQuantity()` method, which contains validation logic to prevent negative numbers.

### 2. Inheritance (Reusability & Hierarchy)
We eliminated redundant code by creating a parent class and allowing specialized child classes to inherit its properties.
* **Where we used it:** `MenuItem.java` is our Parent class containing universal traits (ID, Name, Price, Stock). We created specific Child classes like `Beverage.java`, `Soup.java`, and `RiceBowl.java` that `extend` `MenuItem`. This allows a `Beverage` to inherit the price logic, but safely add its own unique property (Volume in mL).

### 3. Polymorphism (Dynamic Behavior)
Our application can treat multiple different objects as if they are the same type, while each object dynamically responds in its own unique way.
* **Where we used it (Data):** The `getSpecialDetails()` method. The parent `MenuItem` has an empty method, but every child class `@Override`s it. When the `CustomerDashboardFrame` renders the frontend menu, it loops through a generic list of items and calls this method to generate dynamic sub-labels. The UI natively knows to display `[Preparation: Standard Non-Spicy]` for soups, `[Volume: 500ml]` for drinks, and `[Protein: Beef]` for rice bowls. This same polymorphism is reused by the checkout system when generating official receipts.
* **Where we used it (Fulfillment):** The `OrderType` interface. `DeliveryOrder` and `PickUpOrder` both implement the exact same interface, but when the checkout engine asks them for their `getAdditionalFee()`, Delivery dynamically returns 50.00, while Pick-up returns 0.00.

### 4. Abstraction (Hiding Complexity)
We hide complex implementation details (like raw SQL strings and database connections) behind simple, highly readable method calls so the main application loop remains clean.
* **Where we used it:** The Data Access Objects (DAO). Our user interfaces (`CustomerDashboardFrame.java` / `AdminDashboardFrame.java`) have no idea that PostgreSQL exists. When a customer adds an item to their cart and completes checkout, or when an admin fetches inventory, the UI simply calls methods like `MenuDAO.fetchItemById()` or `TransactionDAO.processCheckout()`. The DAO abstracts the complexity of `PreparedStatement`s, `ResultSet`s, transaction pooling, and network error handling away from the front-end.

---

## 🚀 Tech Stack
* **Language:** Java 17+
* **Database:** Supabase (PostgreSQL)
* **Driver:** JDBC PostgreSQL 42.6.0
* **Architecture:** MVC (Model-View-Controller) / DAO Pattern
