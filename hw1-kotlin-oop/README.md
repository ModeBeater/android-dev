# HW1 — FurniStore: Kotlin Fundamentals and OOP

Mobile Programming (KBTU, Fall 2026) · Homework #1: Kotlin fundamentals and OOP.

**FurniStore** is a console simulation of a small furniture store. It keeps a catalog of
furniture (sofas, tables, chairs, wardrobes), tracks warehouse stock, accepts customer
orders, applies discount rules, and then processes payment and shipping for all orders
concurrently with coroutines. It is a plain Kotlin/JVM application — no Android components.

## Requirements

- JDK 17 or newer (IntelliJ IDEA / Android Studio bundle one).
- Nothing else: the Gradle wrapper downloads Gradle and the single dependency
  (`kotlinx-coroutines-core`) on the first run.

## How to run

**IntelliJ IDEA:** `File → Open…` this folder, wait for the Gradle import to finish,
open `src/main/kotlin/furnistore/Main.kt` and press the green ▶ next to `fun main()`.

**Terminal:**

```bash
./gradlew run        # macOS / Linux
gradlew.bat run      # Windows
```

`./gradlew build` compiles the project and produces a runnable distribution in `build/`.

## Project structure

```
src/main/kotlin/furnistore/
├── Main.kt                  entry point: builds the catalog and runs the whole scenario
├── model/
│   ├── Product.kt           interfaces, abstract base class and 4 product subclasses
│   └── Order.kt             data classes, sealed class OrderStatus, class Order
├── store/
│   └── Inventory.kt         warehouse: Map / MutableMap based stock, higher-order search
├── service/
│   └── OrderService.kt      orders, discount rules (lambdas), suspend functions, coroutines
└── util/
    └── Format.kt            extension function for price formatting, output helpers
```

## Where the requirements are demonstrated

| Requirement | Where |
| --- | --- |
| Variables, data types, conditions, loops | `Main.kt` — `val`/`var`, `const val` (`STORE_NAME`), `Int`/`Double`/`Boolean`/`String`; `if`, `when` (by value, by range, by type); `for` over lists, ranges and `withIndex()`; `while` over the assembly queue; `repeat` in the heartbeat coroutine |
| List, Set, Map | `Inventory.kt` — `Map<Int, Product>` (`associateBy`), `MutableMap<Int, Int>` stock, `Set<String>` of low-stock names; `Main.kt` — `List` catalog, `Set` wishlist/trending with `intersect`, `Map` from `groupBy` |
| Collection operations: map, filter, reduce | `Main.kt` "Collection operations" section — `map`, `filter`, `reduce`, plus `partition`, `groupBy`, `minByOrNull`/`maxByOrNull`, `flatMap`, `sumOf`; `OrderService.discountFor` combines rules with `reduce`; `Order.totalQuantity` uses `reduce(Int::plus)` |
| Functions, higher-order functions, lambdas | `Inventory.find(predicate: (Product) -> Boolean)`; `typealias DiscountRule = (Order) -> Double` and the list of discount lambdas in `Main.kt`; local function `item()` in `main`; default parameter in `lowStock(threshold = 2)`; extension function `Double.tenge()` in `Format.kt` |
| Classes and objects | `Order`, `Inventory`, `OrderService` (regular classes); `object OrderNumbers` (singleton); `enum class Material` |
| Inheritance | `abstract class Product` → `Sofa`, `Table`, `Chair`, `Wardrobe` (`Product.kt`); subclasses override `describe()` and `assemblyMinutes()`, `Wardrobe` also overrides `deliveryCost()` |
| Interfaces and polymorphism | `interface Describable`, `interface Deliverable` (with a default method) in `Product.kt`; `Main.kt` iterates over `List<Product>` and calls `describe()`, `assemblyMinutes()`, `deliveryCost()` — each subclass answers differently |
| Data class | `Customer` and `OrderItem` (`Order.kt`); `Main.kt` shows `copy()`, structural equality and destructuring |
| Sealed class | `OrderStatus` (`New`, `Paid`, `Shipped`, `Cancelled`) with an exhaustive `when` in `Order.statusText()`; `PaymentResult` (`Success`, `Declined`) in `OrderService.kt` |
| Suspend function and coroutine | `OrderService.processPayment`, `ship`, `fulfil`, `fulfilAll` are `suspend` functions using `delay`; `fulfilAll` runs one `async` coroutine per order inside `coroutineScope` and `awaitAll()`s them; `main` is wrapped in `runBlocking` and also starts a `launch` "heartbeat" coroutine |

## Sample output (shortened)

```
== Placing orders ==
Placed order #1001 for Aigerim: 618 000 ₸, discount 7% -> due 574 740 ₸
Placed order #1002 for Bereket Furniture LLP: 2 120 000 ₸, discount 15% -> due 1 802 000 ₸
Placed order #1003 for Daniyar: 300 000 ₸, discount 5% -> due 285 000 ₸
Rejected for Aigerim: not enough stock for 3x Nord sofa
Orders placed: 3 of 4

== Fulfilment (coroutines) ==
  ...gateway still working
  ...gateway still working
  processed 1002: cancelled — wholesale credit limit exceeded
  ...gateway still working
  processed 1001: shipped, tracking KZ1001ALM
  processed 1003: shipped, tracking KZ1003SHY
All orders processed concurrently in ~517 ms (sequentially it would take ~1500 ms)

== Order summary ==
Order #1001 for Aigerim (Almaty): 5 item(s), 618 000 ₸ — shipped, tracking KZ1001ALM
Order #1002 for Bereket Furniture LLP (Astana): 6 item(s), 2 120 000 ₸ — cancelled — wholesale credit limit exceeded
Order #1003 for Daniyar (Shymkent): 12 item(s), 300 000 ₸ — shipped, tracking KZ1003SHY
Revenue by city:
  Almaty: 574 740 ₸
  Shymkent: 285 000 ₸
Assembly workload for shipped orders: 3 h 50 min
```
