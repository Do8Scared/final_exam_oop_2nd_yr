# Changelog

All notable changes made during the recent hardening/audit work are documented here.

## 2026-06-01

### Breaking Changes
- **Renamed interface:** `orders.OrderDAO` → `orders.FulfillmentType` (clarifies fulfillment fee contract vs. database OrderDAO)
- **MenuManager API:** `addMenuItem()` now returns `boolean` (success/failure) instead of void
- **CartItem validation:** Constructor now rejects null items and zero/negative quantities via exceptions

### Fixed - CRITICAL
- **CartItem null handling:** Now validates MenuItem at construction; throws `IllegalArgumentException` on null item
- **MenuItem data integrity:** Negative price/stock values now rejected with exceptions (instead of silent `Math.max()` coercion)
- **RiceBowl protein validation:** `setMainProtein()` now validates and rejects empty/null values
- **Dessert sweetness default:** Now defaults to "Standard" if null/empty (was silently storing null)
- **MenuDAO category null check:** Added explicit null check before calling `.equalsIgnoreCase()` (prevented NullPointerException)
- **Audit integrity:** Checkout commits transaction before attempting audit; audit failure no longer rolls back successful checkout
- **Admin PIN validation:** Now enforces 4-6 digit PIN format at startup and login time (was accepting any string)
- **Admin PIN race condition:** Added synchronized locking for `failedPinAttempts` and `lockoutEndTime` (thread-safe counter)
- **Admin PIN auditing:** Now logs `ADMIN_PIN_ATTEMPT` events with success/failure status (was missing)
- **GCash/Maya validation:** Added payment confirmation prompt for non-Cash payments (was auto-accepting without confirmation)
- **Transaction ID generation:** Improved collision resistance using timestamp + UUID hash (was substring-based)

### Fixed - HIGH
- **BigDecimal precision:** All monetary calculations now use `BigDecimal` with proper rounding to avoid floating-point errors
- **Beverage volume parsing:** Hardened parser to handle uppercase/lowercase/non-numeric variations (e.g., "500ML", "500ml", "five hundred")
- **JSON escaping:** Added proper escape order for backslash and all control characters (prevented double-escaping and data corruption)
- **Category normalization:** Categories now normalized to canonical forms (Beverages, Appetizer, Dessert, Soup, Rice Bowl, Add-Ons)
- **Batch operation safety:** Added `clearBatch()` on failure to prevent stale batch data from persisting
- **Rollback error handling:** Swallowed rollback exceptions now logged as WARN instead of silently ignored
- **Price re-check at checkout:** Cart items now have prices re-validated at checkout time + customer notified of changes

### Fixed - MEDIUM
- **.env search depth:** Now searches unlimited parent directories (not just 6 levels)
- **Receipt sorting:** Items now grouped by name alphabetically in receipt output
- **System clock validation:** Added check for obviously-wrong system time (before 2020) at receipt generation
- **Currency symbol:** Centralized as constant `CURRENCY_SYMBOL` (consistent ₱ usage everywhere)
- **Error message format:** Standardized all console output with `INFO`, `SUCCESS`, `WARN`, `ERROR` prefixes
- **Startup validation:** Added explicit database connection test at application startup (fail-fast on config errors)
- **Menu item name validation:** Length limit (100 chars), character whitelist, explicit validation method
- **MenuManager overloading:** Removed unused 2-parameter `addMenuItem()` dead code variant
- **Explicit imports:** Removed wildcard imports; now using explicit `import database.MenuDAO`, etc.

### Improved
- **Checkout flow:** Now sorts cart items by ID before locking to prevent deadlock (deterministic lock order)
- **Receipt output:** Sorted items by name, validated system time, enhanced with special details display
- **Supabase pooler compatibility:** Automatic URL parameter injection for PgBouncer connection handlers
- **Audit trail depth:** Audit logs now capture full transaction details in JSON format (items, subtotal, etc.)
- **Error recovery:** System now logs partial failures and allows safe retry (OrderDAO doesn't crash on malformed input)

## 2026-05-27

### Added
- Automatic `.env` loader (dependency-free)
  - New: `Main/config/Dotenv.java`
  - Auto-loads `.env` (searched upward from working directory) and maps:
    - `POS_DB_URL` → `pos.db.url`
    - `POS_DB_USER` → `pos.db.user`
    - `POS_DB_PASSWORD` → `pos.db.password`
    - `POS_ADMIN_PIN` → `pos.admin.pin`
  - Does **not** override existing JVM `-D...` properties or existing OS environment variables.

- Audit trail (DB-backed)
  - New migration: `db/create_audit_table.sql` (creates `audit_logs` + indexes)
  - New runtime smoke test: `Main/database/AuditTrail.java` (inserts then deletes a test audit row)

### Changed
- Database config hardening
  - `Main/database/DatabaseHelper.java` no longer relies on hardcoded secrets.
  - Reads DB config from env vars / JVM properties; provides clear missing-config errors.

- Supabase pooler / PgBouncer compatibility
  - `Main/database/DatabaseHelper.java` automatically appends `preferQueryMode=simple` for pooler-looking URLs
    to avoid prepared-statement issues.

- JDBC driver robustness
  - `Main/database/DatabaseHelper.java` now attempts to load `org.postgresql.Driver` and rethrows a clearer
    error for the common runtime failure: `No suitable driver found ...` (missing jar on classpath).

- Admin PIN default
  - `Main/Main/Main.java` default admin PIN is now **9876** (still overrideable via `POS_ADMIN_PIN` / `-Dpos.admin.pin`).

- Menu item creation now audited
  - `Main/Main/MenuManager.java`
    - Insert uses `RETURNING id` to capture the created `menu_items.id`.
    - Writes `MENU_ITEM_CREATED` audit row in the same transaction.
  - `Main/Main/Main.java` calls `MenuManager.addMenuItem(newItem, "admin")`.

- Checkout flow now audited
  - `Main/database/OrderDAO.java`
    - Success path writes `ORDER_CHECKOUT` audit row inside the checkout transaction.
    - Failure path writes best-effort `ORDER_CHECKOUT_FAILED` audit row after rollback.

- Admin access attempts now audited
  - `Main/Main/Main.java` logs `ADMIN_PIN_ATTEMPT` with `{ "success": true|false }` (does not log the PIN).

### Fixed
- Checkout stock race/oversell prevention
  - `Main/database/OrderDAO.java` uses `SELECT ... FOR UPDATE` to lock stock rows and re-check stock inside the transaction.

### Verification (manual/CLI)
- Compile:
  - `javac` compiled all `*.java` into `Main/out` successfully.
- Smoke tests (with PostgreSQL JDBC jar on runtime classpath):
  - `database.DatabaseHelper` (DB connectivity)
  - `database.AuditTrail` (audit insert + cleanup)
  - `Main.Main` (View Full Live Menu → Exit)

