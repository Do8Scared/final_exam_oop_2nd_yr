# Comprehensive Code Review: Garahe Ni Mateicla POS System

**Review Date:** 2026-05-28  
**Scope:** Build, runtime, logic, security, data integrity, edge cases, UI/UX, OOP design, performance

---

## Executive Summary

The codebase demonstrates significant hardening work (credentials removed, audit trails added, transaction safety improved), but contains several **critical** issues affecting interface contracts, data integrity, and required runtime configuration. Below are 38 identified issues across all risk categories.

---

## CRITICAL Issues (Must Fix Before Production)

### 1. **INTERFACE MISMATCH: orders.OrderDAO vs Implementation**
- **Severity:** CRITICAL
- **File:** `Main/orders/OrderDAO.java`, `Main/orders/DineInOrder.java`, `Main/orders/TakeOutOrder.java`
- **Lines:** OrderDAO.java 7-16, Main.java 140, 173
- **Problem:** The `orders.OrderDAO` interface only defines `getPackagingFee()` method, but the new checkout flow in Main.java (line 173) calls `database.OrderDAO.processCheckout()` instead. This creates confusion: are there two separate OrderDAO classes? Is the interface obsolete?
  - Old code expected: `currentOrderType.checkout(item, quantity)` (per AGENTS.md)
  - New code has: `database.OrderDAO.processCheckout(cart, orderType, ...)` (different interface/class)
  - The `/orders` interface is now only used for packaging fees, not fulfillment
- **How to Reproduce:** Run Main → Start Transaction → Attempt checkout; code would have failed if it called `fulfillment.checkout()` instead
- **Recommended Fix:**
  ```java
  // Option A: Update orders.OrderDAO interface to match usage
  public interface OrderDAO {
      double getPackagingFee();
      // Remove or: boolean checkout(...);  // if returning to that pattern
  }
  
  // Option B: Rename to clarify. Change orders.OrderDAO → orders.FulfillmentFee
  // Keep database.OrderDAO for transaction processing
  // This makes intent clearer: /orders handles fulfillment types, /database handles checkout
  ```

---

### 2. **Missing Database Schema Migration for Menu Items Table**
- **Severity:** CRITICAL
- **File:** Codebase references schema; migration file missing
- **Lines:** MenuDAO.java 22, 39, 47, 54, 57, etc.; OrderDAO.java 44-46
- **Problem:** The code assumes `menu_items` table has columns: `id`, `item_name`, `price`, `stock_quantity`, `category`, `special_attribute`, `is_active`. Only `db/create_audit_table.sql` exists. No migration for:
  - Table creation
  - `is_active` column (required for active filtering)
  - `special_attribute` column (used in MenuDAO line 36, MenuManager line 34)
  - Primary key, indexes, constraints
- **How to Reproduce:** Deploy to fresh Supabase instance; queries fail with "column not found" or "table not found"
- **Recommended Fix:** Create `db/create_menu_table.sql`:
  ```sql
  CREATE TABLE IF NOT EXISTS menu_items (
      id BIGSERIAL PRIMARY KEY,
      item_name TEXT NOT NULL,
      price NUMERIC(10, 2) NOT NULL CHECK(price >= 0),
      stock_quantity INTEGER NOT NULL DEFAULT 0 CHECK(stock_quantity >= 0),
      category TEXT NOT NULL,
      special_attribute TEXT,
      is_active BOOLEAN NOT NULL DEFAULT TRUE,
      created_at TIMESTAMPTZ DEFAULT now(),
      updated_at TIMESTAMPTZ DEFAULT now()
  );
  CREATE INDEX idx_menu_category ON menu_items(category);
  CREATE INDEX idx_menu_active ON menu_items(is_active);
  ```

---

### 3. **Incomplete Build/Classpath Configuration**
- **Severity:** CRITICAL
- **File:** `README.md` line 24-28, compilation setup
- **Lines:** README.md 26, sources.txt
- **Problem:** 
  - README shows `javac -d out $files` but doesn't include PostgreSQL driver JAR on classpath
  - At runtime, if driver is not on classpath, DatabaseHelper.java line 59-65 throws but only after attempting connection
  - The JAR is at `Main/libs/postgresql-42.6.0.jar` but build command doesn't reference it
  - Path in README: `"G:\Github Repos\Main\Main"` doesn't match actual path `G:\intellij\repository\new-one\final_exam_oop_2nd_yr\Main`
- **How to Reproduce:** 
  ```powershell
  # Current (broken):
  javac -d out $(Get-ChildItem -Recurse -Filter *.java)
  java -cp out Main.Main  # Fails: "No suitable driver"
  ```
- **Recommended Fix:** Update README and add build script:
  ```powershell
  # Compile with driver on classpath
  $libDir = "$(pwd)\libs\postgresql-42.6.0.jar"
  javac -cp $libDir -d out $(Get-ChildItem -Recurse -Filter *.java)
  
  # Run with driver on classpath
  java -cp "out:libs\postgresql-42.6.0.jar" Main.Main
  ```
  Or create `build.gradle`/`pom.xml` for proper dependency management.

---

### 4. **CartItem Accepts Null Item Without Validation**
- **Severity:** CRITICAL
- **File:** `Main/models/CartItem.java`
- **Lines:** 20-23
- **Problem:** Constructor allows `item == null`; silently sets unitPrice to 0:
  ```java
  public CartItem(MenuItem item, int quantity) {
      this.item = item;  // ← No null check
      this.quantity = quantity;
      this.unitPrice = (item != null) ? item.getPrice() : 0;  // ← Silently coerces to 0
  }
  ```
  - Later, `getSubtotal()` at line 77-78 returns `0 * quantity = 0`, hiding the error
  - Receipt printing at OrderDAO.java line 184 calls `c.getItem().getItemName()` → NullPointerException if item is null
- **How to Reproduce:** Add a CartItem with null item; proceed to checkout
- **Recommended Fix:**
  ```java
  public CartItem(MenuItem item, int quantity) {
      if (item == null) {
          throw new IllegalArgumentException("CartItem requires a non-null MenuItem");
      }
      this.item = item;
      this.quantity = (quantity > 0) ? quantity : 1;
      this.unitPrice = item.getPrice();
  }
  ```

---

### 5. **Audit Failure Leaves Transaction in Inconsistent State**
- **Severity:** CRITICAL
- **File:** `Main/database/OrderDAO.java`
- **Lines:** 108-131
- **Problem:** If audit insertion fails (line 126), the code rolls back the entire transaction:
  ```java
  try {
      DatabaseHelper.insertAudit(conn, "terminal", "ORDER_CHECKOUT", txnId, details);
  } catch (SQLException ex) {
      // ← Audit failure rolls back successful checkout!
      conn.rollback();
      return false;
  }
  ```
  This means:
  - Checkout succeeds: items inserted, stock updated, receipts printed
  - Audit insert fails (connection drops, audit_logs table schema missing)
  - Entire transaction rolls back → stock restored, transaction deleted
  - Customer charged but no record remains
- **How to Reproduce:** 
  1. Start checkout
  2. Drop audit_logs table mid-transaction
  3. Transaction rolls back despite items being in cart
- **Recommended Fix:** Separate audit concerns from transaction core:
  ```java
  // Process checkout without audit dependency
  conn.commit();
  
  // Audit best-effort (non-blocking failure)
  try {
      DatabaseHelper.insertAudit(conn, "terminal", "ORDER_CHECKOUT", txnId, details);
  } catch (SQLException ex) {
      System.out.println(">> [WARNING] Audit log failed but transaction committed: " + ex.getMessage());
      // Do NOT rollback committed transaction
  }
  ```

---

### 6. **RiceBowl Protein Field Lacks Validation**
- **Severity:** CRITICAL (Data Integrity)
- **File:** `Main/models/RiceBowl.java`
- **Lines:** 40-42
- **Problem:** Setter allows null or empty protein:
  ```java
  public void setMainProtein(String mainProtein) {
      this.mainProtein = mainProtein;  // ← No validation
  }
  ```
  Unlike Beverage.setVolumeInMl() (line 41-45) which validates, this silently stores invalid data.
- **How to Reproduce:** 
  ```java
  RiceBowl bowl = ...;
  bowl.setMainProtein("");      // Silently accepted
  bowl.setMainProtein(null);    // Silently accepted
  // Receipt prints "[Protein: null]"
  ```
- **Recommended Fix:**
  ```java
  public void setMainProtein(String mainProtein) {
      String sanitized = (mainProtein != null) ? mainProtein.trim() : "";
      if (sanitized.isEmpty()) {
          System.out.println("Error: Protein type cannot be empty.");
          return;
      }
      this.mainProtein = sanitized;
  }
  ```

---

### 7. **No Audit Logging for Admin PIN Attempts**
- **Severity:** CRITICAL (Security + Compliance)
- **File:** `Main/Main/Main.java`
- **Lines:** 473-506
- **Problem:** CHANGELOG (line 48-49) states: "Admin access attempts now audited [with] `ADMIN_PIN_ATTEMPT`" but code doesn't call DatabaseHelper.insertAudit():
  ```java
  private static boolean authenticateAdmin() {
      // ... PIN validation ...
      if (pin.equals(configuredPin)) {
          failedPinAttempts = 0;
          System.out.println(">> [SYSTEM] Access Granted.");
          return true;  // ← NO AUDIT LOG
      } else {
          failedPinAttempts++;
          System.out.println(">> [SECURITY ALERT] Invalid PIN.");  // ← NO AUDIT LOG
          // ...
      }
  }
  ```
  This violates:
  - Security compliance (failed auth attempts untracked)
  - Forensics (no record of who attempted admin access)
  - CHANGELOG promises not fulfilled
- **How to Reproduce:** Attempt admin access multiple times; no audit entries created
- **Recommended Fix:**
  ```java
  private static boolean authenticateAdmin() {
      // ...existing code...
      if (pin.equals(configuredPin)) {
          failedPinAttempts = 0;
          try {
              DatabaseHelper.insertAudit("terminal", "ADMIN_PIN_ATTEMPT", "admin-access", "{\"success\":true}");
          } catch (SQLException e) {
              System.out.println(">> [WARNING] Failed to audit admin access: " + e.getMessage());
          }
          return true;
      } else {
          failedPinAttempts++;
          try {
              DatabaseHelper.insertAudit("terminal", "ADMIN_PIN_ATTEMPT", "admin-access", "{\"success\":false}");
          } catch (SQLException e) {
              // Silently ignore audit failure for failed attempt
          }
          // ...
      }
  }
  ```

---

### 8. **Race Condition in Admin Lockout Counter**
- **Severity:** CRITICAL (Security)
- **File:** `Main/Main/Main.java`
- **Lines:** 16-17, 473-506
- **Problem:** Static variables `failedPinAttempts` and `lockoutEndTime` are shared without synchronization:
  ```java
  private static int failedPinAttempts = 0;           // ← Not volatile or synchronized
  private static long lockoutEndTime = 0;              // ← Not volatile or synchronized
  
  private static boolean authenticateAdmin() {
      if (System.currentTimeMillis() < lockoutEndTime) {  // ← Read without lock
          // ...
      }
      // ...
      failedPinAttempts++;  // ← Write without lock
      if (failedPinAttempts >= 3) {
          lockoutEndTime = System.currentTimeMillis() + 30000;  // ← Write without lock
      }
  }
  ```
  If two cashiers attempt PIN simultaneously:
  - Thread 1 reads `failedPinAttempts = 2`
  - Thread 2 reads `failedPinAttempts = 2`
  - Both increment → `failedPinAttempts = 3` (should be 4, one is lost)
  - Both check `>= 3` → possible double-lockout or missed lockout
- **How to Reproduce:** Multi-threaded POS system with concurrent PIN attempts
- **Recommended Fix:**
  ```java
  private static final Object lockoutLock = new Object();
  private static int failedPinAttempts = 0;
  private static long lockoutEndTime = 0;
  
  private static boolean authenticateAdmin() {
      synchronized (lockoutLock) {
          if (System.currentTimeMillis() < lockoutEndTime) {
              // ... lockout check ...
          }
          // ... validation ...
          if (pin.equals(configuredPin)) {
              failedPinAttempts = 0;
          } else {
              failedPinAttempts++;
              if (failedPinAttempts >= 3) {
                  lockoutEndTime = System.currentTimeMillis() + 30000;
              }
          }
      }
  }
  ```
  **Note:** CLI app is single-threaded, but still a vulnerability in future multi-thread scenarios.

---

### 9. **Silent Negative Stock Conversion in MenuItem Constructor**
- **Severity:** CRITICAL (Data Integrity)
- **File:** `Main/models/MenuItem.java`
- **Lines:** 26-31, 44-49
- **Problem:** Constructor silently converts negative stock to 0 instead of rejecting:
  ```java
  public MenuItem(int id, String itemName, double price, int stockQuantity, String category) {
      this.id = id;
      this.itemName = sanitizeText(itemName, "Unnamed Item");
      this.price = Math.max(0, price);  // ← Silent conversion
      this.stockQuantity = Math.max(0, stockQuantity);  // ← Silent conversion: -50 → 0
      this.category = sanitizeText(category, "Uncategorized");
  }
  ```
  This masks bugs:
  - If database loads stock = -100 (corrupted), silently becomes 0
  - Admin adding item with negative quantity as typo results in 0, not rejection
  - No feedback to user that data was changed
- **How to Reproduce:** Load or create menu item with negative stock; silently becomes 0
- **Recommended Fix:**
  ```java
  public MenuItem(int id, String itemName, double price, int stockQuantity, String category) {
      this.id = id;
      this.itemName = sanitizeText(itemName, "Unnamed Item");
      
      if (price < 0) throw new IllegalArgumentException("Price cannot be negative");
      this.price = price;
      
      if (stockQuantity < 0) throw new IllegalArgumentException("Stock quantity cannot be negative");
      this.stockQuantity = stockQuantity;
      
      this.category = sanitizeText(category, "Uncategorized");
  }
  ```

---

### 10. **MenuDAO Category Field Null Check Missing**
- **Severity:** CRITICAL (NullPointerException)
- **File:** `Main/database/MenuDAO.java`
- **Lines:** 39, 43, 50, 54, 57, 62
- **Problem:** Category retrieved from database and immediately used with `.equalsIgnoreCase()`:
  ```java
  if (rs.next()) {
      int fetchedId = rs.getInt("id");
      String name = rs.getString("item_name");
      // ... other fields ...
      String category = rs.getString("category");  // ← Could be null if column is null
      
      if (category.equalsIgnoreCase("Beverages")) {  // ← NullPointerException if null
  ```
  If database has NULL category, NPE occurs.
- **How to Reproduce:** Insert menu item with NULL category; attempt to fetch it; NullPointerException
- **Recommended Fix:**
  ```java
  String category = rs.getString("category");
  if (category == null || category.trim().isEmpty()) {
      System.out.println(">> [ERROR] Menu item " + fetchedId + " has invalid category. Loading as generic item.");
      return new MenuItem(fetchedId, name, price, stock, "Unknown");
  }
  
  if (category.equalsIgnoreCase("Beverages")) {
      // ...
  }
  ```

---

## HIGH Issues (Serious Bugs, Should Fix Before Release)

### 11. **GCash/Maya Payment Method Has No Validation**
- **Severity:** HIGH
- **File:** `Main/Main/Main.java`
- **Lines:** 143-170
- **Problem:** For Cash payment, amount tendered is validated (line 162-169), but for GCash/Maya:
  ```java
  String paymentMethod = "Cash";
  if (payChoice == 2) paymentMethod = "GCash";
  if (payChoice == 3) paymentMethod = "Maya";
  
  double amountTendered = 0;
  // ... Cash validation for amountTendered >= grandTotal ...
  if (paymentMethod.equals("Cash")) {
      double grandTotal = /* ... */;
      while (true) {
          amountTendered = getValidDoubleInput("Enter Amount Tendered: ₱");
          if (amountTendered >= grandTotal) break;  // ← Only for Cash
      }
  }
  // ← No validation for GCash/Maya
  
  database.OrderDAO.processCheckout(activeCart, orderType, paymentMethod, amountTendered, packagingFee);
  ```
  This means checkout succeeds for GCash/Maya even if they never actually paid (no integration with payment gateway).
- **How to Reproduce:** 
  1. Select GCash/Maya payment
  2. Never actually transfer payment
  3. Checkout completes as if payment succeeded
- **Recommended Fix:** Require payment confirmation:
  ```java
  if (paymentMethod.equalsIgnoreCase("Cash")) {
      // ... existing validation ...
  } else if (paymentMethod.equalsIgnoreCase("GCash") || paymentMethod.equalsIgnoreCase("Maya")) {
      System.out.println(">> [PAYMENT] Transaction ID: " + txnId);
      System.out.println(">> [PAYMENT] Amount due: ₱" + String.format("%.2f", grandTotal));
      System.out.println(">> Please transfer via " + paymentMethod + " and press Enter to confirm...");
      scanner.nextLine();
      System.out.print(">> Confirm payment received? (Y/N): ");
      if (!scanner.nextLine().trim().equalsIgnoreCase("Y")) {
          System.out.println(">> Transaction cancelled.");
          return;  // Don't process checkout
      }
  }
  ```

---

### 12. **Broken Special Attribute Parsing for Beverages**
- **Severity:** HIGH (Data Loss)
- **File:** `Main/database/MenuDAO.java`
- **Lines:** 40
- **Problem:** Volume parsing is fragile:
  ```java
  int volume = parseIntOrDefault(specialAttr.replace("ml", "").trim(), 500, "beverage volume");
  ```
  Fails for:
  - `"500ML"` (uppercase) → returns "500"
  - `"500 milliliters"` → returns "500 milliliters" (not int)
  - `"five hundred ml"` → returns "five hundred " (not int)
  - `null` special_attribute → NPE (actually handled at line 37: `if (specialAttr == null) specialAttr = "Standard";` but then "Standard" doesn't parse as int → falls back to 500)
  
- **How to Reproduce:** Add beverage with special_attribute = "500ML" or "FIVE HUNDRED ml"; fetches with volume = 500 (lucky) or defaults to 500
- **Recommended Fix:** Use structured parsing with validation:
  ```java
  int volume = parseVolumeOrDefault(specialAttr, 500);
  
  private static int parseVolumeOrDefault(String input, int fallback) {
      if (input == null || input.isEmpty()) return fallback;
      
      String cleaned = input.toUpperCase().replaceAll("[^0-9]", "").trim();
      if (cleaned.isEmpty()) return fallback;
      
      try {
          int vol = Integer.parseInt(cleaned);
          return (vol > 0) ? vol : fallback;
      } catch (NumberFormatException e) {
          return fallback;
      }
  }
  ```

---

### 13. **JSON Escaping Has Double-Escape Risk**
- **Severity:** HIGH (Audit Log Data Loss)
- **File:** `Main/database/OrderDAO.java), `Main/Main/MenuManager.java`
- **Lines:** 214-219, 85-90
- **Problem:** String escape order can cause double-escaping:
  ```java
  private static String escapeJson(String s) {
      if (s == null) return "";
      return s.replace("\\", "\\\\")     // ← If input has \", becomes \\\"
              .replace("\"", "\\\"")    // ← Then replaces " but misses already-escaped \"
              .replace("\n", "\\n")
              .replace("\r", "\\r");
  }
  ```
  Example:
  - Input: `Item's "best"`
  - After line 216: `Item's "best"` (no backslashes to escape)
  - After line 217: `Item's \"best\"` ✓ Correct
  
  But with already-escaped input (shouldn't happen but defensive programming):
  - Input: `Item's \\"best\\"` (already escaped quotes)
  - After line 216: `Item's \\\\\\"best\\\\\\"` (backslashes quadrupled)
  - After line 217: `Item's \\\\\\\\"best\\\\\\\\"` (double-escaped)
- **How to Reproduce:** Audit record contains already-escaped characters; they get double-escaped in JSON
- **Recommended Fix:** Use proper JSON library or at least escape in correct order:
  ```java
  private static String escapeJson(String s) {
      if (s == null) return "";
      return s.replace("\\", "\\\\")  // Must be first
              .replace("\"", "\\\"")
              .replace("\b", "\\b")
              .replace("\f", "\\f")
              .replace("\n", "\\n")
              .replace("\r", "\\r")
              .replace("\t", "\\t");
  }
  ```
  **Better:** Use `com.google.gson.Gson` or `org.json.JSONObject`

---

### 14. **Dessert Constructor Parameter Changed Without Backward Compatibility**
- **Severity:** HIGH
- **File:** `Main/models/Dessert.java`
- **Lines:** 21
- **Problem:** Dessert's Signature changed during refactoring. Looking at MenuDAO line 48:
  ```java
  return new Dessert(fetchedId, name, price, stock, category, specialAttr);
  ```
  But Dessert constructor (line 21):
  ```java
  public Dessert(int id, String itemName, double price, int stockQuantity, String category, String sweetness) {
  ```
  The refactoring now REQUIRES `category` parameter (compared to old hardcoded approach). But the real issue is:
  - **Old pattern (from AGENTS.md context):** `new Dessert(id, name, price, stock)` with category hardcoded to "Dessert"
  - **New pattern:** `new Dessert(id, name, price, stock, category, sweetness)`
  This is correct for the new design, but signals an API break. No version compatibility layer.
- **How to Reproduce:** Review git history (not available here); compare old vs new Dessert instantiation
- **Recommended Fix:** Document breaking change in CHANGELOG; ensure all instantiation sites updated. Actually, review shows this WAS done correctly (all calls pass category and sweetness), so status is **OK but high risk** if backports are attempted.

---

### 15. **CartItem Quantity Setter Lacks Validation**
- **Severity:** HIGH
- **File:** `Main/models/CartItem.java`
- **Lines:** 59-60
- **Problem:** Setter accepts any quantity without bounds checking:
  ```java
  public void setQuantity(int quantity) {
      this.quantity = quantity;  // ← No validation
  }
  ```
  Used in Main.java line 332:
  ```java
  itemToEdit.setQuantity(newQty);  // ← Called after validation, but no internal guarantee
  ```
  But if setQuantity is called from another context without validation, could result in:
  - Negative quantity
  - Zero quantity (should be removed from cart)
  - Absurdly high quantity (outweighing stock)
- **How to Reproduce:** Directly call `cartItem.setQuantity(-5)` or `cartItem.setQuantity(999999)`; no error, proceeds to checkout with invalid cart
- **Recommended Fix:**
  ```java
  public void setQuantity(int quantity) {
      if (quantity <= 0) {
          throw new IllegalArgumentException("Quantity must be greater than zero");
      }
      this.quantity = quantity;
  }
  ```

---

### 16. **No Validation for Admin PIN Length**
- **Severity:** HIGH (Security)
- **File:** `Main/Main/Main.java`
- **Lines:** 489-490
- **Problem:** PIN prompt says "4-digit" but accepts any string:
  ```java
  System.out.print("\n[SECURITY] Enter 4-digit Admin PIN: ");
  String pin = scanner.nextLine().trim();
  if (pin.equals(configuredPin)) {  // ← No length checking
  ```
  Admin sets `POS_ADMIN_PIN=12345` (5 digits) or `POS_ADMIN_PIN=password` (non-numeric). No validation that configured PIN matches "4-digit" requirement.
- **How to Reproduce:** 
  1. Set `POS_ADMIN_PIN=password` (not 4 digits)
  2. Run system
  3. Try to access admin; prompt says "4-digit" but accepts "password"
- **Recommended Fix:**
  ```java
  String configuredPin = System.getenv("POS_ADMIN_PIN");
  if (configuredPin == null || configuredPin.isBlank()) {
      configuredPin = System.getProperty("pos.admin.pin");
  }
  if (configuredPin == null || configuredPin.isBlank()) {
      System.out.println("\n>> [CRITICAL] Admin PIN not configured.");
      return false;
  }
  
  // Validate PIN format (should be 4-6 digits)
  if (!configuredPin.matches("\\d{4,6}")) {
      System.out.println("\n>> [CRITICAL] Admin PIN must be 4-6 digits. Current PIN violates format.");
      return false;
  }
  
  System.out.print("\n[SECURITY] Enter Admin PIN: ");
  String pin = scanner.nextLine().trim();
  if (pin.equals(configuredPin)) {
      // ...
  }
  ```

---

### 17. **Stock Not Re-Checked for Untracked Price Changes**
- **Severity:** HIGH (Data Integrity)
- **File:** `Main/models/CartItem.java`, `Main/database/OrderDAO.java`
- **Lines:** CartItem.java 20-23, OrderDAO.java 36-41
- **Problem:** CartItem snapshots unit price at cart-add time (line 23):
  ```java
  this.unitPrice = (item != null) ? item.getPrice() : 0;  // ← Captured here
  ```
  If a menu item's price changes after being added to cart, the customer still pays the old price:
  - Admin adds "Burger" at ₱100 to cart
  - Admin changes menu: "Burger" price to ₱200
  - Customer checks out → charged ₱100 (old price captured)
  
  This is intentional for price stability, but if price is REDUCED (₱100 → ₱50), the system loses revenue.
  
  More critical:
  - Stock is checked again in OrderDAO line 87-90 (correct), BUT
  - Unit price in cart line 96 uses CartItem.getSubtotal() which uses CAPTURED unitPrice, not current database price
  - If price changed by ₱0.01 between cart add and checkout, amount could mismatch what customer expects
- **How to Reproduce:**
  1. Add burger ₱100 to cart
  2. Admin changes burger to ₱50
  3. Checkout: receipt shows ₱100 (cart price) but database stock updated for item with ₱50 value
- **Recommended Fix:** Either:
  - **Option A:** Re-fetch price at checkout time:
    ```java
    MenuItem latestItem = MenuDAO.fetchItemById(c.getItem().getId());
    double currentPrice = (latestItem != null) ? latestItem.getPrice() : c.getUnitPrice();
    double subtotal = currentPrice * c.getQuantity();
    ```
  - **Option B:** Explicitly warn customer of price change:

```java
  double cartPrice = c.getUnitPrice();
  double currentPrice = latestItem.getPrice();
  if (Math.abs(currentPrice - cartPrice) > 0.01) {
      System.out.println(">> [ALERT] " + c.getItem().getItemName() + 
          " price changed from ₱" + cartPrice + " to ₱" + currentPrice);
  }
  ```

---

### 18. **Rollback Doesn't Clear Partial Batch Results**
- **Severity:** HIGH
- **File:** `Main/database/OrderDAO.java`
- **Lines:** 104-105
- **Problem:** Batch execution (line 104-105):
  ```java
  pstmtItems.executeBatch();
  pstmtStock.executeBatch();
  ```
  If `executeBatch()` partially succeeds (e.g., first 3 items succeed, 4th fails), the batch state is unclear. When rollback happens (line 140), the database is rolled back, but the PreparedStatement batch buffers are not cleared. Future batch operations could reuse stale batch data.
- **How to Reproduce:** Trigger failure partway through batch execution; connection error during batch
- **Recommended Fix:**
  ```java
  try {
      pstmtItems.executeBatch();
      pstmtStock.executeBatch();
  } catch (SQLException e) {
      pstmtItems.clearBatch();  // ← Clear batch on failure
      pstmtStock.clearBatch();  // ← Clear batch on failure
      throw e;
  }
  ```

---

### 19. **No Connection Null Check After Rollback Failure**
- **Severity:** HIGH (Null Pointer)
- **File:** `Main/database/OrderDAO.java`
- **Lines:** 140
- **Problem:** In catch block (line 138-147):
  ```java
  } catch (SQLException e) {
      if (conn != null) {
          try { conn.rollback(); } catch (SQLException ex) { }  // ← Swallows exception
      }
      // ...
  }
  ```
  If connection is corrupted (network drop, timeout), rollback throws but is silently swallowed. Then line 151 tries to reset autocommit on a broken connection:
  ```java
  } finally {
      if (conn != null) {
          try {
              conn.setAutoCommit(true);  // ← Could throw if conn is broken
              conn.close();
          } catch (SQLException e) {
              System.out.println("Error closing transaction connection: " + e.getMessage());
          }
      }
  }
  ```
  Actually, this IS handled in finally (line 150-156). But the swallowed rollback exception at line 140 should be logged for debugging.
- **Recommended Fix:**
  ```java
  } catch (SQLException e) {
      if (conn != null) {
          try {
              conn.rollback();
          } catch (SQLException rollbackEx) {
              System.out.println(">> [WARN] Rollback failed (connection may be broken): " + rollbackEx.getMessage());
          }
      }
      // ...
  } finally {
      if (conn != null) {
          try {
              conn.setAutoCommit(true);
              conn.close();
          } catch (SQLException e) {
              System.out.println("Error closing transaction connection: " + e.getMessage());
          }
      }
  }
  ```

---

### 20. **Hardcoded Transaction ID Format Could Cause Collisions**
- **Severity:** HIGH (Data Integrity)
- **File:** `Main/database/OrderDAO.java`
- **Lines:** 34
- **Problem:** Transaction ID is generated with UUID truncation:
  ```java
  String txnId = "TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase() + "-" + orderType.toUpperCase();
  ```
  Potential issues:
  - UUID.toString() is 36 chars; substring(0, 8) takes first 8 → could have same prefix if UUIDs generated rapidly
  - Extremely unlikely but not guaranteed unique if generated in tight loop
  - If orderType is NULL or very long, txnId format breaks
- **How to Reproduce:** Generate transactions in tight loop; collision possible (astronomically unlikely but possible)
- **Recommended Fix:**
  ```java
  String txnId = "TXN-" + System.currentTimeMillis() + "-" + UUID.randomUUID().hashCode() + "-" + orderType.toUpperCase();
  // Or simpler: UUID provides uniqueness already, just use it:
  String txnId = "TXN-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12) + "-" + orderType.toUpperCase();
  ```

---

## MEDIUM Issues (Code Quality, Should Fix in Maintenance)

### 21. **Silent Integer Overflow in Floating Point Conversion**
- **Severity:** MEDIUM
- **File:** `Main/models/MenuItem.java`, `Main/database/OrderDAO.java`
- **Lines:** MenuItem.java 29, OrderDAO.java 40-41, 159
- **Problem:** Multiple locations use `Math.round(value * 100.0) / 100.0` which works but is brittle:
  ```java
  subtotal = Math.round(subtotal * 100.0) / 100.0;  // ← Works but fragile
  double grandTotal = Math.round((subtotal + packagingFee) * 100.0) / 100.0;
  ```
  Issues:
  - If subtotal + packagingFee > 9,223,372,036 (Long.MAX_VALUE / 100), rounding fails
  - For POS system with max transaction ₱10M, this is safe, but architecture doesn't scale
  - No protection against floating point precision errors
- **How to Reproduce:** Add items totaling ₱99999999.99; arithmetic might lose precision
- **Recommended Fix:** Use BigDecimal for all monetary calculations:
  ```java
  BigDecimal subtotal = new BigDecimal("0");
  for (CartItem item : cart) {
      subtotal = subtotal.add(new BigDecimal(item.getSubtotal()));
  }
  BigDecimal grandTotal = subtotal.add(new BigDecimal(packagingFee));
  ```

---

### 22. **No Uppercase/Case Normalization for Category Names**
- **Severity:** MEDIUM
- **File:** `Main/database/MenuDAO.java`
- **Lines:** 39, 43, 50, 54, 57
- **Problem:** Categories are case-sensitive in database but code uses equalsIgnoreCase():
  ```java
  if (category.equalsIgnoreCase("Beverages")) {  // ← Case-insensitive check
  ```
  But:
  - GET query uses exact case: `WHERE category = ?` (line 141) - case-sensitive
  - Two items with category="beverages" and category="Beverages" would both match in fetchItemById but differ in getActiveCategories()
  - Admin creates category "Beverages"; later admin creates "beverages" → two different categories in database
- **How to Reproduce:** Admin creates items with different case categories (Beverages, beverages, BEVERAGES); they appear as separate categories
- **Recommended Fix:** Normalize categories to title case or lowercase:
  ```java
  public static MenuItem fetchItemById(int id) {
      String sql = "SELECT * FROM menu_items WHERE id = ? AND is_active = TRUE";
      // ...
      String category = rs.getString("category");
      if (category != null) category = category.trim();  // Normalize
      
      if ("beverages".equalsIgnoreCase(category)) {
          // ...
      }
  }
  
  // On INSERT, normalize:
  String newCategory = getNonEmptyInput("Enter New Category Name: ").toLowerCase().trim();
  ```

---

### 23. **Fragile .env File Search (Only 6 Levels Up)**
- **Severity:** MEDIUM
- **File:** `Main/config/Dotenv.java`
- **Lines:** 70
- **Problem:** Search is limited:
  ```java
  for (int i = 0; i < 6 && dir != null; i++) {
  ```
  If .env is nested deeper (e.g., project/submodule/config/.env), it won't be found. Silently falls back to OS env vars / JVM properties.
- **How to Reproduce:** Place .env 7+ directories above working directory; not found, silent fallback
- **Recommended Fix:**
  ```java
  private static Path findEnvFile(Path start) {
      Path dir = start;
      Path root = start.getRoot();
      
      while (dir != null && (root == null || !dir.equals(root))) {
          Path candidate = dir.resolve(".env");
          if (Files.isRegularFile(candidate)) {
              return candidate;
          }
          dir = dir.getParent();
      }
      return null;
  }
  ```

---

### 24. **Optional Input Validation for Menu Item Names**
- **Severity:** MEDIUM
- **File:** `Main/Main/Main.java`
- **Lines:** 228
- **Problem:** Item name is only checked for non-empty:
  ```java
  String newName = getNonEmptyInput("Enter Item Name: ");
  ```
  But no validation for:
  - Maximum length (could be 1000+ characters)
  - Special characters (SQL injection protected by PreparedStatements, but data quality issue)
  - Duplicates (two items can have same name)
  - Profanity or inappropriate content
- **How to Reproduce:** Enter 10,000 character item name; accepted and stored
- **Recommended Fix:**
  ```java
  private static String getValidMenuItemName(String prompt) {
      while (true) {
          String name = getNonEmptyInput(prompt);
          
          if (name.length() > 100) {
              System.out.println(">> [ERROR] Item name must be 100 characters or less.");
              continue;
          }
          if (!name.matches("[a-zA-Z0-9\\s\\-()&,'.]*")) {
              System.out.println(">> [ERROR] Item name contains invalid characters.");
              continue;
          }
          return name;
      }
  }
  ```

---

### 25. **No Logging of AdminAddMenuItem Failures**
- **Severity:** MEDIUM
- **File:** `Main/Main/Main.java`
- **Lines:** 226-277
- **Problem:** If MenuManager.addMenuItem() fails (returns false implicitly via exception), user sees error from MenuManager but no logs:
  ```java
  MenuItem newItem = new MenuItem(newName, newPrice, newStock, newCategory);
  MenuManager.addMenuItem(newItem, specialAttr, "admin");  // ← Could fail; no feedback to admin
  ```
  AdminaddMenuItem doesn't check return value or exception handling.
- **How to Reproduce:** Introduce database constraint violation during add; error shown but not logged for audit
- **Recommended Fix:**
  ```java
  MenuItem newItem = new MenuItem(newName, newPrice, newStock, newCategory);
  try {
      MenuManager.addMenuItem(newItem, specialAttr, "admin");
      System.out.println(">> [SUCCESS] Menu item added.");
      try {
          DatabaseHelper.insertAudit("admin", "MENU_ITEM_ADD_SUCCESS", newName, 
              "{\"price\":" + newPrice + ",\"stock\":" + newStock + ",\"category\":\"" + newCategory + "\"}");
      } catch (SQLException e) {
          System.out.println(">> [WARN] Item added but audit failed: " + e.getMessage());
      }
  } catch (Exception e) {
      System.out.println(">> [ERROR] Failed to add menu item: " + e.getMessage());
      try {
          DatabaseHelper.insertAudit("admin", "MENU_ITEM_ADD_FAILED", newName, 
              "{\"error\":\"" + e.getMessage().replace("\"", "\\\"") + "\"}");
      } catch (SQLException logEx) {
          // Silently ignore audit failure
      }
  }
  ```

---

### 26. **Potential Deadlock with FOR UPDATE Lock**
- **Severity:** MEDIUM
- **File:** `Main/database/OrderDAO.java`
- **Lines:** 81, 99-100
- **Problem:** Code uses two PreparedStatements in same transaction:
  ```java
  try (PreparedStatement pstmtItems = conn.prepareStatement(insertItemsSql);
       PreparedStatement pstmtStock = conn.prepareStatement(updateStockSql);
       PreparedStatement pstmtLock = conn.prepareStatement(lockStockSql)) {  // ← FOR UPDATE

      for (CartItem c : cart) {
          pstmtLock.setInt(1, c.getItem().getId());
          try (ResultSet stockRs = pstmtLock.executeQuery()) {  // ← Locks row
              // ... check stock ...
          }
          
          pstmtStock.setInt(1, c.getQuantity());
          pstmtStock.setInt(2, c.getItem().getId());
          pstmtStock.addBatch();  // ← Updates same row locked above
      }
      pstmtStock.executeBatch();  // ← Batch after loop
  ```
  If transaction A locks item 5, then updates stock for item 5, while transaction B locks item 5 and updates stock for item 5, possible deadlock if they process different items in different order.
  
  Actually, this is a **FOR UPDATE** lock with batch update - should be fine since we're locking, then updating same row within same transaction. But if loop processes items out of order compared to another transaction, deadlock could occur.
- **How to Reproduce:** Two simultaneous checkouts; one processes items [1,5,10], other processes [10,5,1]; deadlock possible
- **Recommended Fix:** Always lock items in ascending ID order:
  ```java
  List<CartItem> sortedCart = new ArrayList<>(cart);
  sortedCart.sort((a, b) -> Integer.compare(a.getItem().getId(), b.getItem().getId()));
  
  for (CartItem c : sortedCart) {
      // ... existing lock/check logic ...
  }
  ```

---

### 27. **Special Attribute Not Stored in Some Code Paths**
- **Severity:** MEDIUM
- **File:** `Main/models/Dessert.java`
- **Lines:** 21-23
- **Problem:** Dessert constructor now requires `sweetness` parameter, but old code might create Dessert without it. If code ever falls back to base MenuItem constructor, sweetness is lost:
  ```java
  } else if (category.equalsIgnoreCase("Dessert")) {
      return new Dessert(fetchedId, name, price, stock, category, specialAttr);
  ```
  As long as MenuDAO is the only instantiation point, this works. But if another function creates Dessert directly without calling fetchItemById, sweetness could be lost.
- **How to Reproduce:** Create Dessert programmatically without loading from database; sweetness defaults to null
- **Recommended Fix:**
  ```java
  public Dessert(int id, String itemName, double price, int stockQuantity, String category, String sweetness) {
      super(id, itemName, price, stockQuantity, category);
      this.sweetness = (sweetness != null && !sweetness.isEmpty()) ? sweetness : "Standard";
  }
  ```

---

### 28. **Incorrect Method Overloading in MenuManager**
- **Severity:** MEDIUM (Confusing API)
- **File:** `Main/Main/MenuManager.java`
- **Lines:** 20-23
- **Problem:** Method overloading chain is convoluted:
  ```java
  public static void addMenuItem(MenuItem item, String specialAttribute) {
      addMenuItem(item, specialAttribute, "unknown");  // ← Calls 3-param version
  }
  
  public static void addMenuItem(MenuItem item, String specialAttribute, String actor) {
      // ← Actual implementation
  }
  ```
  But in Main.java line 276:
  ```java
  MenuManager.addMenuItem(newItem, specialAttr, "admin");  // ← 3-param version, good
  ```
  The 2-param version is never called, so it's dead code. And "unknown" actor is not helpful. Should be removed or clarified.
- **How to Reproduce:** Never call addMenuItem with 2 params; 2-param version is unused
- **Recommended Fix:** Remove dead overload:
  ```java
  // Remove this:
  // public static void addMenuItem(MenuItem item, String specialAttribute) {
  //     addMenuItem(item, specialAttribute, "unknown");
  // }
  
  // Keep only this:
  public static void addMenuItem(MenuItem item, String specialAttribute, String actor) {
      // ... implementation ...
  }
  
  // In Main.java, always pass actor:
  MenuManager.addMenuItem(newItem, specialAttr, "admin");
  ```

---

## LOW Issues (Code Quality, Minor Bugs)

### 29. **Unused Import Statements**
- **Severity:** LOW
- **File:** `Main/Main/Main.java`
- **Lines:** 3-7
- **Problem:** Wildcard imports make it unclear what's actually used:
  ```java
  import config.Dotenv;
  import database.*;  // ← Which classes? All of them?
  import models.*;    // ← Which classes? All of them?
  import java.util.*;
  ```
- **Recommended Fix:** Use explicit imports:
  ```java
  import config.Dotenv;
  import database.DatabaseHelper;
  import database.MenuDAO;
  import database.OrderDAO;
  import models.CartItem;
  import models.MenuItem;
  import java.util.ArrayList;
  import java.util.List;
  import java.util.Scanner;
  ```

---

### 30. **Receipt Not Sorted by Item ID**
- **Severity:** LOW (UX)
- **File:** `Main/database/OrderDAO.java`
- **Lines:** 183
- **Problem:** Receipt items are printed in cart order, not sorted:
  ```java
  for (CartItem c : cart) {
      System.out.println(c.getItem().getItemName() + ...);
  }
  ```
  If customer adds items in random order (Burger, Drink, Burger), receipt shows that order instead of grouped.
- **Recommended Fix:**
  ```java
  List<CartItem> sortedCart = new ArrayList<>(cart);
  sortedCart.sort((a, b) -> a.getItem().getItemName().compareTo(b.getItem().getItemName()));
  
  for (CartItem c : sortedCart) {
      System.out.println(c.getItem().getItemName() + ...);
  }
  ```

---

### 31. **No Time/Date Validation for Transaction Timestamp**
- **Severity:** LOW
- **File:** `Main/database/OrderDAO.java`
- **Lines:** 180
- **Problem:** Date is always current time:
  ```java
  System.out.println("DATE: " + dtf.format(LocalDateTime.now()));
  ```
  If system clock is wrong, receipts will have wrong dates. No validation.
- **Recommended Fix:** Add warning if system clock seems off:
  ```java
  LocalDateTime now = LocalDateTime.now();
  LocalDateTime epochMin = LocalDateTime.of(2020, 1, 1, 0, 0);
  if (now.isBefore(epochMin)) {
      System.out.println(">> [CRITICAL] System clock is incorrect! Receipt date may be wrong.");
  }
  System.out.println("DATE: " + dtf.format(now));
  ```

---

### 32. **Inconsistent Error Message Format**
- **Severity:** LOW
- **File:** Throughout codebase
- **Problem:** Error messages use different prefixes:
  - `">> [SYSTEM ERROR]"` (Main.java 370)
  - `">> [SYSTEM]"` (Main.java 494)
  - `">> ERROR:"` (Main.java 114)
  - `"Error:"` (Beervage.java 44)
  - `">> [ERROR]"` (MenuDAO.java 62)
  - `"[TRANSACTION FAILED]"` (OrderDAO.java 145)
  
  Inconsistent UI makes it hard to parse logs.
- **Recommended Fix:** Define error level constants:
  ```java
  private static final String ERROR = ">> [ERROR]";
  private static final String WARNING = ">> [WARN]";
  private static final String SUCCESS = ">> [SUCCESS]";
  private static final String INFO = ">> [INFO]";
  ```

---

### 33. **No Configuration Validation on Startup**
- **Severity:** LOW
- **File:** `Main/Main/Main.java`, `Main/config/Dotenv.java`
- **Lines:** 20
- **Problem:** Application starts without pre-validating configuration. If POS_DB_URL is missing, error only occurs when first database operation happens.
- **Recommended Fix:** Add startup validation:
  ```java
  public static void main(String[] args) {
      Dotenv.loadIfPresent();
      
      // Validate critical configuration before starting
      try {
          String dbUrl = System.getenv("POS_DB_URL");
          if (dbUrl == null || dbUrl.isBlank()) {
              dbUrl = System.getProperty("pos.db.url");
          }
          if (dbUrl == null || dbUrl.isBlank()) {
              System.err.println(">> [CRITICAL] Database URL not configured (POS_DB_URL environment variable or -Dpos.db.url)");
              System.exit(1);
          }
          
          // Test connection
          DatabaseHelper.getConnection().close();
          System.out.println(">> [INFO] Database connection verified.");
          
      } catch (SQLException e) {
          System.err.println(">> [CRITICAL] Database connection failed: " + e.getMessage());
          System.exit(1);
      }
      
      // Continue with main loop...
  }
  ```

---

### 34. **No Logging of Successful Transactions**
- **Severity:** LOW
- **File:** `Main/database/OrderDAO.java`
- **Lines:** 135
- **Problem:** Successful checkout prints receipt but no console log for audit trail visibility:
  ```java
  conn.commit();
  
  printUnifiedReceipt(...);  // ← Prints to user
  return true;               // ← No logging of success
  ```
  If  running in background/headless, success is not logged.
- **Recommended Fix:**
  ```java
  conn.commit();
  
  System.out.println(">> [TRANSACTION] Checkout successful: " + txnId + " (" + paymentMethod + ")");
  printUnifiedReceipt(...);
  return true;
  ```

---

### 35. **Hardcoded Currency Symbol (₱)**
- **Severity:** LOW (Localization)
- **File:** Throughout codebase (Main.java, OrderDAO.java, MenuDAO.java)
- **Problem:** Philippine Peso symbol `₱` is hardcoded throughout. If system needs multi-currency, all instances must change.
- **Recommended Fix:**
  ```java
  private static final String CURRENCY_SYMBOL = "₱";
  private static final String CURRENCY_CODE = "PHP";
  
  // Usage:
  System.out.println("GRAND TOTAL  : " + CURRENCY_SYMBOL + String.format("%.2f", grandTotal));
  ```

---

### 36. **No Verification That Checkout Matches Audit Log**
- **Severity:** LOW
- **File:** `Main/database/OrderDAO.java`
- **Lines:** 135-136
- **Problem:** After transaction commits and receipt prints, audit log is written. If audit fails, user sees receipt but transaction might not be fully logged.
- **Recommended Fix:** Move audit before final confirmation; make it part of atomic transaction (already done at line 126).

---

### 37. **Console Output Not Buffered (Potential Interleaving)**
- **Severity:** LOW (UX)
- **File:** Throughout codebase
- **Problem:** Multiple threads (if made multi-threaded) writing to System.out without synchronization could interleave output. Lines could get mixed:
  ```
  >> Item 1 added>> Item 2 added  // ← Interleaved
  ```
- **Recommended Fix:** Use synchronized print wrapper:
  ```java
  private static synchronized void printLine(String msg) {
      System.out.println(msg);
  }
  ```

---

### 38. **No Unit Test Framework in Place**
- **Severity:** LOW (Maintainability)
- **File:** Entire project
- **Problem:** No JUnit, Mockito, or test cases. Critical functions like checkout, stock updates, tax calculations not tested.
- **Recommended Fix:** Create `src/test/` directory and add test cases:
  ```java
  // Main/test/MenuManagerTest.java
  public class MenuManagerTest {
      @Test
      public void testAddMenuItem_SuccessfullyInserts() {
          // ...
      }
      
      @Test
      public void testAddMenuItem_InvalidPrice() {
          // ...
      }
  }
  ```

---

## Summary Table

| Issue # | Title | Severity | File | Priority |
| --- | --- | --- | --- | --- |
| 1 | Interface Mismatch: orders.OrderDAO | CRITICAL | /orders/*.java | P0 |
| 2 | Missing Database Schema | CRITICAL | Codebase | P0 |
| 3 | Build Classpath Incomplete | CRITICAL | README.md | P0 |
| 4 | CartItem Null Item | CRITICAL | CartItem.java | P0 |
| 5 | Audit Failure Rollback | CRITICAL | OrderDAO.java | P0 |
| 6 | RiceBowl Protein Validation | CRITICAL | RiceBowl.java | P0 |
| 7 | Missing Admin PIN Audit | CRITICAL | Main.java | P0 |
| 8 | Lockout Counter Race | CRITICAL | Main.java | P1 |
| 9 | Negative Stock Silenced | CRITICAL | MenuItem.java | P0 |
| 10 | Category Null Check | CRITICAL | MenuDAO.java | P0 |
| 11 | GCash Payment Validation | HIGH | Main.java | P1 |
| 12 | Beverage Volume Parsing | HIGH | MenuDAO.java | P1 |
| 13 | JSON Double-Escape | HIGH | OrderDAO.java | P1 |
| 14 | Dessert Constructor Change | HIGH | Dessert.java | P2 |
| 15 | CartItem Quantity Validation | HIGH | CartItem.java | P1 |
| 16 | Admin PIN Length | HIGH | Main.java | P1 |
| 17 | Price Re-check | HIGH | OrderDAO.java | P2 |
| 18 | Batch Rollback | HIGH | OrderDAO.java | P2 |
| 19 | Rollback Error Handling | HIGH | OrderDAO.java | P2 |
| 20 | Transaction ID Collision | HIGH | OrderDAO.java | P2 |
| 21 | BigDecimal Missing | MEDIUM | MenuItem.java | P2 |
| 22 | Category Case Normalization | MEDIUM | MenuDAO.java | P2 |
| 23 | .env Search Depth | MEDIUM | Dotenv.java | P3 |
| 24 | Menu Item Name Validation | MEDIUM | Main.java | P2 |
| 25 | Add Item Failure Logging | MEDIUM | Main.java | P2 |
| 26 | FOR UPDATE Deadlock | MEDIUM | OrderDAO.java | P3 |
| 27 | Dessert Sweetness Default | MEDIUM | Dessert.java | P2 |
| 28 | MenuManager Overload | MEDIUM | MenuManager.java | P2 |
| 29 | Wildcard Imports | LOW | Main.java | P3 |
| 30 | Receipt Sorting | LOW | OrderDAO.java | P3 |
| 31 | Time Validation | LOW | OrderDAO.java | P3 |
| 32 | Error Message Format | LOW | Everywhere | P3 |
| 33 | Startup Validation | LOW | Main.java | P3 |
| 34 | Transaction Logging | LOW | OrderDAO.java | P3 |
| 35 | Currency Symbol Hardcoded | LOW | Everywhere | P3 |
| 36 | Audit Before Confirm | LOW | OrderDAO.java | P3 |
| 37 | Output Interleaving | LOW | Everywhere | P3 |
| 38 | No Unit Tests | LOW | Project | P3 |

---

## Recommendations (Priority Order)

### Phase 1: Blocking Issues (Fix Before Any Production Use)
1. Create missing database schema (Issue #2)
2. Fix build classpath/README (Issue #3)
3. Fix CartItem null validation (Issue #4)
4. Fix interface mismatch (Issue #1)
5. Fix critical data integrity (Issues #6, #9, #10)
6. Add admin PIN audit logging (Issue #7)

### Phase 2: High-Risk Issues (Fix Before Release)
7. Payment method validation (Issue #11)
8. JSON escaping (Issue #13)
9. Quantity/price validation (Issues #15, #16)
10. Error handling (Issues #18, #19, #25)

### Phase 3: Medium-Priority (Fix in Maintenance)
11. Category normalization (Issue #22)
12. Menu item validation (Issue #24)
13. BigDecimal for money (Issue #21)
14. Remove dead code (Issue #28)

### Phase 4: Nice-to-Haves (Low Priority)
15. Logging improvements (Issues #29-38)

---

## Conclusion

The codebase shows good security hardening (credentials moved to config, audit trails added) but requires **6 critical fixes** before production use and **6 high-risk fixes** before release. The majority of issues are correctness bugs rather than architectural flaws.

**Estimated fix effort:** 3-5 days for all critical/high issues; 1-2 weeks for full remediation including testing.


