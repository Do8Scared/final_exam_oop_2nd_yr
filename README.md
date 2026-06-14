# Garahe ni Mateicla - Online Food Ordering System

An enterprise-grade, object-oriented Graphical User Interface (GUI) application simulating a modern food delivery and ordering platform (e.g., GrabFood, Foodpanda). Built using Java Spring Boot for the backend and a modern React + Vite for the frontend, it features live cloud synchronization via Supabase (PostgreSQL), ACID database transactions, secure authentication, and a robust cart management system.

---

## 🚀 Quick Start Tutorial (How to Run)

Follow these steps to easily run and test the application:

### 1. Running the Java Spring Boot Backend
The backend serves the API, connects to the Supabase database, and handles all transactions.
* **Where to navigate:** Open the root folder (`final_exam_oop_2nd_yr`) in your IDE (VS Code, IntelliJ, or Eclipse).
* **How to run:** 
  * Open the `src/main/java/com/garahe/GaraheApplication.java` file.
  * Click the **Run** button in your IDE, or run the `main` method. 
  * Wait for the console to display `Started GaraheApplication in X seconds`. The server is now running on `http://localhost:8081`.

### 2. Running the React Frontend (The Website)
The frontend is the visual website the customer interacts with.
* **Where to navigate:** Open a new Terminal window and navigate to the frontend folder by running:
  ```bash
  cd "Interactive food ordering website"
  ```
* **How to run:**
  * Start the development server by running:
  ```bash
  npm run dev
  ```
  * Open your web browser and go to the link provided in the terminal (usually `http://localhost:5173`).

### 3. How to Navigate the Website
* **Menu Browsing:** Scroll through the beautiful UI to see our dynamic menu. Use the category filters (e.g., *Mains*, *Soups*) at the top to sort items.
* **Authentication:** Click the "LOGIN" button at the top right. You can either log in with an existing account or register a new one to unlock features like Order History.
* **Adding to Cart:** Click the `+` button on any food item to add it to your floating cart.
* **Checkout:** Open the cart and click "Proceed to Checkout". If a user is logged in, their details will auto-fill. Choose Delivery or Pick-up.
* **Order History:** Once logged in, click "MY ORDERS" in the top navigation bar to view past transactions dynamically loaded from the database.

---

## ✨ Key Features
* **Authentication**: Secure user login and registration system.
* **Transaction History**: View past orders dynamically pulled from the cloud database.
* **Detailed Receipts**: Terminal and UI receipts display comprehensive customer and delivery details.
* **Live Menu System**: A fully synchronized menu displaying dynamic categories and options.

---

## 👨‍💻 Development Team
* **Mirexelle** (Lead Developer / Architect)
* **Matthew** (Core Logic)
* **Fliance** (Database Integration)
* **Ivan** (UI/UX & Cart Systems)
* **Jhaneya** (QA & Security)

---

## 🏛️ System Architecture: MVC & DAO Patterns
To ensure the code is clean, scalable, and easy to debug, the system is separated into distinct layers using the **Model-View-Controller (MVC)** and **Data Access Object (DAO)** patterns.

* **Models (`models/` package):** The blueprints for the data. Classes like `MenuItem`, `CartItem`, `Person`, and `User` represent the core entities of the business. They hold the data and enforce basic business rules (e.g., preventing negative prices).
* **Controllers (`controllers/` package):** The middle-men (`AuthController` and `OrderController`). They receive HTTP requests from the React frontend, pass the data to the DAOs, and send JSON responses back.
* **DAOs (`database/` package):** The Data Access Objects (`TransactionDAO`, `UserDAO`). They securely communicate with the PostgreSQL database, execute SQL queries (like inserting transactions or verifying passwords), and return the result.

---

## 🏗️ The 4 Pillars of OOP
This system was built strictly adhering to modern Object-Oriented Programming (OOP) design patterns. Every feature uses these pillars to ensure the code is robust and secure. The following outlines how the 4 Pillars are implemented within the architecture:

### 1. Encapsulation (Data Hiding & Security)
* **Concept:** Bundling data (variables) and methods into a single unit, and restricting direct access to sensitive data using `private` fields and public getters/setters.
* **Where it is used:** 
  * The `CartItem` and `MenuItem` classes. A cart item's internal price or identity cannot be directly altered from the outside; the controlled `.setQuantity(int quantity)` method must be used. This method contains validation logic (`if (quantity <= 0)`) to prevent negative numbers.
  * The `User` class (Authentication). Sensitive fields like `password`, `email`, and `id` are kept strictly `private`. They can only be accessed or modified through explicit getters and setters, protecting customer data integrity from external interference.

### 2. Inheritance (Reusability & Hierarchy)
* **Concept:** Creating new child classes based on existing parent classes to promote code reusability using the `extends` keyword.
* **Where it is used:** 
  * **Authentication System:** A base parent class called `Person.java` was created (containing universal traits like `id`, `name`, and `email`). The `User.java` class inherits from `Person` via the `extends` keyword and introduces the additional `password` trait specifically for system users. This prevents duplicating name and email code.
  * **Menu System:** `MenuItem.java` is a Parent class. Specific Child classes like `Beverage.java`, `Soup.java`, and `RiceBowl.java` were created to `extend MenuItem`. This allows a `Beverage` to inherit all the pricing logic, but safely add its own unique property (e.g., `volume` in mL).

### 3. Polymorphism (Dynamic Behavior)
* **Concept:** The ability of different objects or methods to respond to the exact same call in their own unique way (Method Overriding and Interface implementation).
* **Where it is used:** 
  * **Database Implementation:** The `UserDAO` interface acts as a polymorphic contract. `UserDAOImpl` implements this interface dynamically. The `AuthController` only talks to the `UserDAO` interface, allowing Java to dynamically execute the implementation. If the system later switches to Firebase Auth, a `FirebaseUserDAOImpl` can be created that behaves entirely differently, without breaking the application.
  * **The Order Interface:** The `OrderType` interface. Both `DeliveryOrder` and `PickUpOrder` implement this interface. When the checkout engine asks for `.getAdditionalFee()`, Delivery dynamically calculates and returns `50.00`, while Pick-up returns `0.00`.
  * **Dynamic Menu Data:** The `getSpecialDetails()` method. The parent `MenuItem` has an empty method, but every child class `@Override`s it. Soups return `[Preparation: Non-Spicy]`, drinks return `[Volume: 500ml]`.

### 4. Abstraction (Hiding Complexity)
* **Concept:** Hiding complex background implementation details and showing only the essential features to the user (or other parts of the code).
* **Where it is used:** 
  * **Auth DAO:** The `AuthController` securely registers and logs in users by calling `userDAO.authenticateUser()`. The Controller does not write or see any SQL queries; it relies entirely on the Abstraction provided by the `UserDAO` interface. The SQL implementation is strictly hidden away.
  * **Transaction DAO:** When a customer completes checkout, the system calls a clean, readable method: `TransactionDAO.processCheckout()`. The DAO completely hides the complex mechanics of `PreparedStatement` mapping, looping over arrays, committing ACID transactions, and rollback errors away from the rest of the application.

---

## 🚀 Tech Stack

### Backend
* **Language:** Java 17+
* **Framework:** Spring Boot (REST API)
* **Database:** Supabase (PostgreSQL)
* **Driver:** JDBC PostgreSQL 42.6.0

### Frontend
* **Framework:** React + Vite
* **Language:** TypeScript
* **Styling:** Tailwind CSS + Radix UI + Custom CSS
* **Animations:** Motion (Framer Motion)
