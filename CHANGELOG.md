# Changelog

All notable changes to the **Garahe Ni Mateicla** project are documented in this file, ordered chronologically from the newest updates to the very beginning of the project.

---

### June 17, 2026
- **Feature:** Added Pick-Up vs Delivery toggle in the checkout process, dynamically calculating the delivery fee and updating form fields based on the selected option.
- **Feature:** Added a "View Receipt" button in the Transaction History modal to view details of past orders.
- **Fix:** Fixed z-index layering issue where ReceiptModal appeared behind TransactionHistoryModal.
- **Documentation:** Finalized comprehensive project documentation, README, and the OOP defense paper.
- **UI:** display special details and stock quantity on menu items directly on the frontend.

### June 16, 2026
- **Deployment:** Update frontend API calls to point to the live Render backend URL.
- **Docker:** Add Dockerfile for the Render deployment of the Spring Boot Java backend.
- **Vercel Fix:** Fix Vercel build crashing by correctly ignoring/removing `node_modules` from the repository.
- **Vercel Fix:** Rename frontend folder to resolve Vercel pathing bugs.
- **Bug Fix:** Fixed miscellaneous deployment bugs.

### June 14, 2026
- **Docs:** Added exception handling documentation in README.
- **Bug Fix:** Fixed categories filtering not working in the frontend.
- **Docs:** Fixed general documentation and refined the README language.
- **Backend/Auth:** Implement user authentication and models using the Data Access Object (DAO) pattern.
- **Backend/Transactions:** Implement transaction history tracking and update backend data access layer to save checkout histories.
- **Docs:** Update tech stack documentation and resolve frontend TypeScript configuration errors.
- **Spring Boot:** Successfully integrated SPRINGBOOT REST API to bridge the Java logic with the React frontend.
- **VS Code:** Add VS Code `launch.json` configurations for easier Java project execution.
- **Frontend UI:** Implement extensive UI layers for role selection, admin dashboard, cart, and the customer dashboard.
- **Docs:** Expand architecture documentation and include design pattern explanations in README for the professor.
- **Refactor:** Rename `OrderDAO` to `OrderType` and `TransactionDAO` to improve naming accuracy and prevent collisions.
- **Frontend UI:** Initialize interactive food ordering website project using `shadcn/ui` components and the image asset library.
- **Frontend UI:** Implement the merchant and customer dashboard frames with supplementary menu item image assets.
- **Backend Core:** Implement core ordering system logic with the database DAO, order types (Strategy Pattern), and service layers.

### June 01, 2026
- **Core:** Implement core POS terminal architecture with input validation, order fulfillment, and checkout service layers.
- **Revert:** Revert "feat: implement FulfillmentType interface and enhance order handling with validation improvements".
- **Refactor:** Implement `FulfillmentType` interface and enhance order handling with validation improvements.

### May 28, 2026
- **Code Review:** Add comprehensive code review document highlighting critical issues and recommendations for the POS system.

### May 27, 2026
- **Config & Security:** Add `.env` loader for secure passwords, implement the database Audit Trail, and conduct a massive POS refactor.

### May 26, 2026
- **Models:** Add `AddOn`, `RiceBowl`, and `Soup` classes with complete encapsulation and specific traits.

### May 25, 2026
- **Features:** Implement new order flow with category browsing and cart management.
- **Features:** Enhance menu management with category filtering and add new item functionality.
- **Packages:** Added project packages and rename files for strict package structure consistency.
- **Models:** Add `Appetizer` class with inheritance and encapsulation.
- **Database & OOP Implementation:** 
  - Added `OrderDAO.java` interface to establish a strict checkout contract (Module 4 Abstraction). 
  - Added `DineInOrder.java` and `TakeOutOrder.java` to implement polymorphic behavior (TakeOut automatically adds a ₱20 packaging fee). 
  - Created `Main.java` as the primary execution entry point. 
  - Integrated live Supabase `SELECT` queries to dynamically view the cloud menu. 
  - Enforced strict Exception Handling using `finally` blocks to safely close `ResultSet` and `Connection` objects and prevent memory leaks.
- **Bug Fix:** Fixed "dessert" spelling.
- **Models:** Add `Dessert` and `Beverage` child classes and enhance `MenuItem` constructor to fully support Polymorphism.
- **Models Update:** 
  - Updated `MenuItem.java` to perfectly match Supabase database columns (`id`, `itemName`, `price`, `stockQuantity`, `category`). 
  - Implemented Module 2 Encapsulation by setting all attributes to `private`. 
  - Locked the database ID using the `final` keyword and removed its setter to prevent accidental overwrites (Data Hiding). 
  - Added validation logic inside setters to prevent negative numbers for price and stock. 
  - Added detailed OOP presentation comments for the final exam demo.
- **Database:** Successfully setup live cloud database connection via Supabase.

### May 24, 2026
- **Initial Setup:** Add `MenuItems` class with properties and methods for item management.
- **Docs:** Updated Readme and pushed `MainItems` Class.
- **Init:** Initial commit.
