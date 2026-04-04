---
title: UseCase Scenario For Reentrant & ReentrantReadWrite Lock
date: 2026-03-27
draft: false 
---


The choice between a standard **ReentrantLock** and a **ReentrantReadWriteLock** usually comes down to one metric: the **Read-to-Write ratio**. 

If your threads are mostly looking at data without changing it, a standard lock creates a massive, unnecessary bottleneck. If your threads are constantly updating data, the overhead of a ReadWriteLock actually makes it slower than a simple lock.

---

## 1. Scenario A: High-Contention Updates (Use `ReentrantLock`)
**Scenario:** A **Counter** or a **Bank Account** where every single thread that enters is there to modify the value (e.g., `increment()` or `deposit()`).



```java
import java.util.concurrent.locks.ReentrantLock;

public class AtomicCounter {
    private int count = 0;
    private final ReentrantLock lock = new ReentrantLock();

    public void increment() {
        lock.lock(); // Only ONE thread can enter, period.
        try {
            count++;
        } finally {
            lock.unlock();
        }
    }

    public int getCount() {
        lock.lock(); // Even readers are blocked if someone is incrementing
        try {
            return count;
        } finally {
            lock.unlock();
        }
    }
}
```
**Why use `ReentrantLock` here?**
Since almost every operation is a "Write" (`count++`), there is no benefit to allowing multiple readers. A standard `ReentrantLock` is lightweight and has less internal management overhead than a ReadWriteLock.

---

## 2. Scenario B: High-Read Metadata (Use `ReentrantReadWriteLock`)
**Scenario:** A **Product Catalog** or a **Configuration Cache**. Thousands of users are checking the "Price" or "Settings" every second, but an admin only updates the price once or twice an hour.



```java
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.HashMap;
import java.util.Map;

public class ProductCatalog {
    private final Map<String, Double> prices = new HashMap<>();
    private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();

    public Double getPrice(String productId) {
        rwLock.readLock().lock(); // MULTIPLE threads can hold this at once
        try {
            return prices.get(productId);
        } finally {
            rwLock.readLock().unlock();
        }
    }

    public void updatePrice(String productId, Double newPrice) {
        rwLock.writeLock().lock(); // EXCLUSIVE: Blocks all readers and other writers
        try {
            prices.put(productId, newPrice);
        } finally {
            rwLock.writeLock().unlock();
        }
    }
}
```
**Why use `ReentrantReadWriteLock` here?**
If you used a standard `ReentrantLock`, User A looking at the price of "Laptop" would block User B looking at the price of "Phone." With a `ReadLock`, 1,000 users can read the catalog simultaneously. The only time anyone waits is when the admin triggers a `WriteLock`.

---

## 3. The Key Differences at a Glance

| Feature | `ReentrantLock` | `ReentrantReadWriteLock` |
| :--- | :--- | :--- |
| **Concurrency Style** | **Pessimistic / Mutual Exclusion** | **Shared-Read / Exclusive-Write** |
| **Readers vs. Readers** | **Blocking** (One by one) | **Non-Blocking** (Simultaneous) |
| **Readers vs. Writers** | Blocking | Blocking |
| **Performance Cost** | Low overhead | Higher overhead (managing two lock states) |
| **Best For...** | Frequent updates, small critical sections. | Large data structures with 90%+ Read operations. |

---

### Critical "Rule of Thumb"
* **Use `ReentrantLock`** if the time spent inside the lock is very short (e.g., updating a primitive) or if writes happen frequently.
* **Use `ReentrantReadWriteLock`** if the "Read" operation is expensive (e.g., iterating through a large Map) and writes are rare.
