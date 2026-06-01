# Changelog

All notable changes made during the recent hardening/audit work are documented here.

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

