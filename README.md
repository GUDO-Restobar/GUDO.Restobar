# GUDO Restobar

### Final Project for Object-Oriented Programming and Information Management, BS Computer Science - 2nd Year

Object-Oriented Programming Grades:
Final Defense: 100/100

Information Management Grades:
80/100

Self-service ordering kiosk and manager dashboard for Gudo Restobar — a Java Swing desktop app backed by MySQL, with JavaFX for the intro and order animations.

## The Purpose

**The kiosk** lets a customer order at the counter without a cashier taking the order:

1. The customer picks **Dine-in or Takeout**
2. Browses the **menu** and adds meals — each line can carry its own extras and favorites
3. Reviews the order in **Checkout** (edit a meal, order more, or start over)
4. Pays by **Cash or Cashless** (QR / tap card)
5. Gets a **receipt with an order number**, then the kiosk resets itself for the next customer

Stock is enforced the whole way: items that run out are greyed out, quantities are capped to what remains, and a last check at save time returns the customer to Checkout if something ran out mid-order. Every completed order is written to MySQL and its stock is deducted there.

**The manager app** is the other half of the same database: it keeps the **menu and stock** up to date, shows **order history** (update or delete orders), and produces a **sales summary**. Changes made here are picked up by the kiosk when the customer moves to another page.

In short: two windows, one database — the kiosk sells, the manager manages.

## How to Run

### Requirements

- **JDK 17 or newer** — `java` and `javac` on your PATH (check with `java -version`)
- **MySQL 8+** — reachable either as a local server or a cloud host
- **Windows x64** — the bundled JavaFX is the Windows build

Everything else is bundled in the project folder:

- **JavaFX 21 LTS** in `lib/javafx-sdk` — no separate install needed
- **MySQL JDBC driver** and **dotenv-java** in `lib/`

### First-time setup

1. **Create the database** — import `src/data/sql/gudo_database.sql` into MySQL
   (it creates the `defaultdb` schema: `MENU_ITEM`, `CUSTOMER_ORDER`, `ORDER_DETAIL`, `PAYMENT`).
   Use `src/data/sql/reset_stock.sql` later to put the stock back for a demo.
2. **Create a `.env` file** in the project root — `dbconnect.java` reads the
   connection details from it (do **not** hardcode them into the source):

   ```env
   DB_URL = jdbc:mysql://YOUR_HOST:PORT/defaultdb?sslMode=REQUIRED
   DB_USER = your_user
   DB_PASSWORD = your_password
   ```

   For a local MySQL server: `DB_URL = jdbc:mysql://localhost:3306/defaultdb`
   with your own root credentials.
3. That's it — `run.bat` compiles everything into `bin/` on first run, so a fresh
   copy of the project always builds from source.

### Starting the app

| What | How |
|---|---|
| Kiosk | double-click **`run.bat`** |
| Manager | double-click **`run-manager.bat`** |
| VS Code | open the folder, click Run/Debug on `KioskMain` or `ManagerMain` |

> **Kiosk tip:** the app looks for its videos relative to the folder it is started
> from — always launch `run.bat` from inside the project folder (the `.bat` already
> does this with `cd /d "%~dp0"`), otherwise the intro animation is skipped.

## Project Structure

```
src/
  KioskMain.java            kiosk window / navigation
  ManagerMain.java          manager window / tabs
  data/
    testconnection.java     connection test
    java/                   database code: dbconnect, DbWatcher, MenuItemDAO, OrderDAO
    sql/                    schema and reset scripts
  pages/
    kiosk/                  kiosk screens
    manager/                manager screens
  util/
    feat/                   order model: Cart, OrderLine, Product, Inventory, OrderSession
    ui/                     shared Swing widgets, animations, UiKit, UiFactory
```

## Design Patterns

The project applies twelve design patterns. Each one is marked in its source
file with a short comment. See **[DESIGN_PATTERNS.md](DESIGN_PATTERNS.md)** for
the pattern-to-file map and a one-line explanation of each.

## Copying to Another Laptop

1. Copy the **whole project folder** — including `lib/javafx-sdk` **and the `.env` file**
   (it's in `.gitignore`, so a git clone won't bring it; copy it manually)
2. Install a JDK 17+ and make sure `java -version` works
3. Make sure the `.env` database is reachable from the new machine
4. Double-click `run.bat`

No JavaFX install, no VS Code, no extra setup.

> Other OS (macOS / Linux): download the JavaFX SDK for that OS from
> <https://gluonhq.com/products/javafx/> and replace `lib/javafx-sdk`.

## Troubleshooting

| Error | Fix |
|---|---|
| `'java' is not recognized` | Install a JDK 17+ and add it to PATH |
| `ERROR: JavaFX not found at lib\javafx-sdk\lib` | The `lib\javafx-sdk` folder wasn't copied with the project |
| `UnsupportedClassVersionError` | Classes were built by a newer JDK — just run `run.bat` (it rebuilds everything) |
| `COMPILE FAILED` | Read the javac errors above it — usually a missing file or a syntax mistake |
| `Communications link failure` / database error dialog at startup | MySQL isn't running or is unreachable — check the `.env` details and the host's firewall |
| `NullPointerException` at startup about the database | The `.env` file is missing, or `DB_URL` / `DB_USER` / `DB_PASSWORD` are misspelled |
| Intro / order animations don't play | Launch from the project folder so `public/assets/animations` is found; the video must be H.264 MP4 for JavaFX |
