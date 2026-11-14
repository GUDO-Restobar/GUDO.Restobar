# GUDO Restobar

Kiosk + manager desktop app — Java Swing, MySQL, and JavaFX (intro video).

## Requirements

- **JDK 17 or newer** — `java` and `javac` on your PATH (check with `java -version`)
- **MySQL** — schema files are in `src/data/`
- **Windows x64** — the bundled JavaFX is the Windows build

Everything else is bundled in the project folder:

- **JavaFX 21 LTS** in `lib/javafx-sdk` — no separate install needed
- MySQL JDBC driver in `lib/`

## Run

| What | How |
|---|---|
| Kiosk | double-click **`run.bat`** |
| Manager | double-click **`run-manager.bat`** |
| VS Code | open the folder, click Run/Debug on `KioskMain` or `ManagerMain` |

The `.bat` files compile first (into `bin/`), so they always work on a fresh copy.

## Copying to another laptop

1. Copy the **whole project folder** (including `lib/javafx-sdk`)
2. Install a JDK 17+ and make sure `java -version` works
3. Double-click `run.bat`

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
