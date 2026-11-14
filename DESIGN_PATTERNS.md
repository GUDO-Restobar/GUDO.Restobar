# Design Patterns in GUDO Restobar

This file is the quick reference for the project defense. Every pattern below
is marked with a short comment in the source file named here.

| # | Pattern | Main file(s) | Used by |
|---|---|---|---|
| 1 | Singleton | `src/util/feat/OrderSession.java` | kiosk screens, `KioskMain` |
| 2 | Prototype | `src/util/feat/Prototype.java`, `OrderLine.copy()`, `Cart.copy()` | `QuantityPage`, `EditOrder`, `Cart` |
| 3 | Builder | `src/util/feat/OrderLine.java` (`OrderLine.Builder`) | `AddOrder`, `OrderLine.copy()` |
| 4 | Factory Method | `src/util/ui/UiFactory.java` | `QuantityPage`, manager pages |
| 5 | Abstract Factory | `src/util/ui/UiFactory.java` (`Kiosk`, `Manager`) | `QuantityPage`, manager pages |
| 6 | Adapter | `src/util/ui/VideoPanel.java` | `OrderConfirmation`, `StartOver` |
| 7 | Bridge | `src/util/ui/FadeInPage.java` (`Entrance`, `FadeInSequence`) | kiosk pages |
| 8 | Composite | `src/util/feat/OrderComponent.java`, `OrderLine.java`, `Cart.java` | order totals |
| 9 | Decorator | `src/util/ui/FadingPanel.java` (`FadingPanel`, `RisingPanel`) | fade-in pages, `OrderOption` |
| 10 | Facade | `src/util/ui/UiKit.java` | nearly every page |
| 11 | Flyweight | `src/util/ui/UiKit.java` | pictures and fonts |
| 12 | Proxy | `src/data/java/dbconnect.java` (`shield`) | every database query |

## How each pattern works here

### 1. Singleton - `OrderSession`
The kiosk must remember one order type, one payment method and one saved order
ID for the whole session. `OrderSession` has a **private constructor** and one
`static final INSTANCE`; every screen gets it through `getInstance()`. This
guarantees there is never a second, conflicting session.

### 2. Prototype - `Prototype`, `OrderLine`, `Cart`
`Prototype<T>` declares `copy()`. `OrderLine.copy()` returns an independent
line with the same ID, and `Cart.copy()` copies every line. The Add/Edit Meal
pages edit a **copy** of the line, so the real cart only changes when the
customer presses Save.

### 3. Builder - `OrderLine.Builder`
A line has a meal, a quantity, extras, favorites, water and cutlery. Instead of
one long constructor, `OrderLine.builder(product).quantity(1).build()` sets only
what is needed. `copy()` also uses the builder, which keeps that method short.

### 4. Factory Method - `UiFactory.heading`
`heading(String)` is the factory method. Each concrete factory decides how to
create the label, so callers never write `new JLabel(...)` plus font and colour
settings themselves.

### 5. Abstract Factory - `UiFactory.Kiosk` / `UiFactory.Manager`
`UiFactory` is the abstract factory; `Kiosk` and `Manager` are two concrete
families. The kiosk gets 29 pt headings and the manager gets 33 pt headings, but
both pages ask the same method: `UiFactory.kiosk().heading(...)` or
`UiFactory.manager().heading(...)`.

### 6. Adapter - `VideoPanel`
The kiosk is Swing, but the videos are played by JavaFX. `VideoPanel` is a
`JPanel` that contains a `JFXPanel`, a `MediaPlayer` and a `MediaView`. It adapts
the JavaFX media API to the Swing component API, so `OrderConfirmation` and
`StartOver` can add it like any normal panel.

### 7. Bridge - `Entrance`, `FadeInSequence`, `FadeInPage`
`Entrance` is the abstraction used by `FadeInPage`; `FadeInSequence` is the
implementation that owns the timer and easing. Both are nested in
`FadeInPage.java`, so the whole entrance toolkit lives in one file. The page
only calls `entrance.start()`, `entrance.reset()` and `entrance.add(...)`.
The animation can be replaced without touching the page classes.

### 8. Composite - `OrderComponent`, `OrderLine`, `Cart`
`OrderComponent` declares `getSubtotal()`. `OrderLine` is a **leaf** (one meal
plus its add-ons). `Cart` is the **composite** (a group of lines) and its
`getSubtotal()` is the sum of its leaves. Both can be asked for a price in the
same way.

### 9. Decorator - `FadingPanel`, `RisingPanel`
`FadingPanel` wraps any component and paints it with an `AlphaComposite`
without changing its layout or behaviour. `RisingPanel` is a concrete decorator
that adds the hover-rise movement. Both are in `FadingPanel.java`, together with
the fade-aware `FadeLabel`; `FadeInPage.fade(...)` wraps page elements in these
panels to animate them.

### 10. Facade - `UiKit`
Pages do not load images, scale them, register fonts or attach hover listeners
directly. `UiKit` offers a few simple methods (`icon`, `font`, `peso`, `hover`,
`translucent`) that hide those details behind one class.

### 11. Flyweight - `UiKit`
Every scaled picture is cached by path and size, and every font is cached by
file path. Repeated cards and pages therefore share the same `ImageIcon` and
`Font` objects instead of reading and scaling the files again.

### 12. Proxy - `dbconnect.shield`
`dbconnect.getConnection()` returns a **dynamic proxy** that implements
`Connection`. Callers use it like a real connection, but `close()` is
intercepted: the proxy rolls back any open transaction and parks the connection
for reuse instead of closing it. All other methods are forwarded to the real
connection.

## Supporting pattern (not in the required twelve)

**Template Method** - `FadeInPage` and `QuantityPage` own a fixed sequence of
steps (build header, build body, build buttons, play entrance) and let
subclasses fill in only the different parts (`titleLines()`,
`saveButtonPrefix()`, `commitOrder()`, `buildButtons()`).
