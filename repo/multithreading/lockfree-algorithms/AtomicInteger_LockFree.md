---
title: Atomic Integers & Lock-Free E-commerce
date: 2026-03-27
draft: false 
---

### 1. The "Why"
In an e-commerce platform, the "Inventory Count" is the most contested variable. 
* **The Problem with Locks:** If 10,000 users try to buy an item, `synchronized` puts 9,999 threads to sleep while 1 thread works. The overhead of waking them all up is higher than the actual work of subtracting 1 from the inventory.
* **The Atomic Solution:** `AtomicInteger` uses the CPU's native ability to "reserve" a memory address for a nanosecond, update it, and release it without ever suspending a thread.

### 2. Comparison: Synchronized vs. AtomicInteger

| Feature | `synchronized int` | `AtomicInteger` |
| :--- | :--- | :--- |
| **Mechanism** | Blocking (Pessimistic). | Lock-Free (Optimistic CAS). |
| **Thread State** | Threads become `BLOCKED`. | Threads stay `RUNNABLE` (Spinning). |
| **Performance** | High contention = slow. | High contention = fast (until CPU saturation). |
| **Operations** | Manual `count++` (3 steps). | Atomic `decrementAndGet()` (1 step). |

---

### 3. The "Golden" Snippet: High-Traffic Inventory
This example simulates a flash sale where multiple threads attempt to decrease stock. It ensures we never sell more items than we have (Overselling) without using a single `synchronized` block.



```java
import java.util.concurrent.atomic.AtomicInteger;

public class InventorySystem {
    // Atomic variable initialized with 100 items in stock
    private final AtomicInteger stock = new AtomicInteger(100);

    public boolean purchaseItem(int quantity) {
        while (true) {
            int currentStock = stock.get();
            
            if (currentStock < quantity) {
                System.out.println(Thread.currentThread().getName() + " - Out of stock!");
                return false; 
            }

            int updatedStock = currentStock - quantity;

            // CAS: Only update if stock hasn't changed since we last checked
            if (stock.compareAndSet(currentStock, updatedStock)) {
                System.out.println(Thread.currentThread().getName() + " purchased! Remaining: " + updatedStock);
                return true;
            }
            // If compareAndSet fails, the loop "retries" immediately with the new value
        }
    }
}
```

#### Code Explanation:
1.  **`stock.get()`**: Gets the current "snapshot" of the inventory.
2.  **The "Check"**: We verify if there is enough stock. Note that this check happens *before* the update.
3.  **`compareAndSet(expect, update)`**: This is the magic. It tells the hardware: "Check if the stock is still `currentStock`. If yes, set it to `updatedStock`. If someone else bought an item in the last microsecond, fail and return `false`."
4.  **The Infinite Loop**: If `compareAndSet` fails, we don't give up. The `while(true)` loop immediately fetches the *new* stock count and tries again.

#### Example Output:
```text
Thread-1 purchased! Remaining: 99
Thread-2 purchased! Remaining: 98
Thread-3 - Attempting CAS... Failed (Thread-4 beat it!)
Thread-3 - Retrying... purchased! Remaining: 97
```

---

### 4. The Gotchas
* **The "Spin" Cost:** If 50,000 threads all try to update the same `AtomicInteger`, they will spend a lot of CPU power "looping" and failing. In extreme cases, `LongAdder` (which spreads the sum across different cells) is a better choice for pure counters.
* **Complex Logic:** Atomics are great for a *single* value. If you need to update "Inventory" AND "User Balance" together as one atomic unit, an `AtomicInteger` isn't enough—you’ll need a Lock or a more advanced lock-free structure.
* **Memory Visibility:** Like `volatile`, `AtomicInteger` ensures that when one thread updates the value, all other CPU cores see that change immediately.
