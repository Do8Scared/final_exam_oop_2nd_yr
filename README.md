# Garahe ni Mateicla - Online Food Ordering System

An enterprise-grade, object-oriented Graphical User Interface (GUI) application simulating a modern food delivery and ordering platform (e.g., GrabFood, Foodpanda). Built purely with Java Swing, it features live cloud synchronization via Supabase (PostgreSQL), ACID database transactions, and a robust cart management system.

## 👨‍💻 Development Team
* **Mirexelle** (Lead Developer / Architect)
* **Matthew** (Core Logic)
* **Fliance** (Database Integration)
* **Ivan** (UI/UX & Cart Systems)
* **Jhaneya** (QA & Security)

---

## 🏛️ System Architecture: MVC & DAO Patterns
To ensure our code is clean, scalable, and easy to debug, we separated the system into distinct layers using the **Model-View-Controller (MVC)** and **Data Access Object (DAO)** patterns.

* **Models (`models/` package):** The blueprints for our data. Classes like `MenuItem`, `CartItem`, and `AddOn` represent the core entities of our business. They hold the data and enforce basic business rules (e.g., preventing negative prices).
* **Views (`ui/` package):** The graphical interface the user interacts with. Frames like `CustomerDashboardFrame` and `AdminDashboardFrame` are strictly responsible for drawing the UI (buttons, tables, layouts) using Java Swing and the modern `FlatLaf` Dark Theme. They *do not* contain database logic.
* **Controllers & DAOs (`database/` & `orders/` packages):** The middle-men. When a user clicks "Checkout" in the View, the View asks the `TransactionDAO` to process it. The DAO securely communicates with the PostgreSQL database, executes the SQL queries, and returns the result (Success/Fail) back to the View.

---

## 🏗️ The 4 Pillars of Object-Oriented Programming (OOP)
This system was built strictly adhering to modern OOP design patterns. Here is how the 4 Pillars are implemented within our architecture:

### 1. Encapsulation (Data Hiding & Security)
* **Concept:** Bundling data (variables) and methods that operate on that data into a single unit, and restricting direct access to some of the object's components.
* **Where we used it:** The `CartItem` and `MenuItem` classes. A `CartItem` securely binds a `MenuItem` to a specific `quantity`. You cannot directly alter the item's internal price or identity from the outside; you must use the controlled `.setQuantity(int quantity)` method. This method contains validation logic (`if (quantity <= 0)`) to prevent negative numbers, ensuring our cart data is always mathematically valid before checkout.

### 2. Inheritance (Reusability & Hierarchy)
* **Concept:** Creating new classes based on existing ones to promote code reusability.
* **Where we used it:** `MenuItem.java` is our **Parent** class containing universal traits that every menu item has (ID, Name, Price, Stock). We created specific **Child** classes like `Beverage.java`, `Soup.java`, and `RiceBowl.java` that `extend MenuItem`. This allows a `Beverage` to inherit all the pricing and stock validation logic for free, but safely add its own unique property (e.g., `volume` in mL) without breaking the parent.

### 3. Polymorphism (Dynamic Behavior)
* **Concept:** The ability of different objects to respond to the exact same method call in their own unique way.
* **Where we used it (Data):** The `getSpecialDetails()` method. The parent `MenuItem` has an empty method, but every child class `@Override`s it. When the `CustomerDashboardFrame` renders the visual menu, it loops through a generic list of items and calls this exact same method on all of them. The UI dynamically knows to display `[Preparation: Standard Non-Spicy]` for soups, `[Volume: 500ml]` for drinks, and `[Protein: Beef]` for rice bowls. 
* **Where we used it (Fulfillment):** The `OrderType` interface. Both `DeliveryOrder` and `PickUpOrder` implement this interface. When the checkout engine asks for `.getAdditionalFee()`, Delivery dynamically calculates and returns `50.00`, while Pick-up returns `0.00`.

### 4. Abstraction (Hiding Complexity)
* **Concept:** Hiding complex background implementation details and showing only the essential features to the user (or other parts of the code).
* **Where we used it:** The Data Access Objects (DAO). Our user interfaces (`CustomerDashboardFrame.java` / `AdminDashboardFrame.java`) have no idea that PostgreSQL exists. When a customer adds an item to their cart and completes checkout, the UI simply calls a clean, readable method like `TransactionDAO.processCheckout()`. The DAO completely hides the complex mechanics of `PreparedStatement` and `ResultSet` objects, connection handling, ACID transactions, and network errors away from the front-end.

---

## 🚀 Tech Stack
* **Language:** Java 17+
* **GUI Framework:** Java Swing + FlatMacDarkLaf
* **Database:** Supabase (PostgreSQL)
* **Driver:** JDBC PostgreSQL 42.6.0
