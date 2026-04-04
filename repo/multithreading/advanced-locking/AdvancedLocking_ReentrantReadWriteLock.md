---
title: ReentrantReadWriteLock & Database Implementation
date: 2026-03-27
draft: false 
---

### 1. The "Why"
Standard locks (`synchronized`, `ReentrantLock`) are **Mutual Exclusion** locks—they don't care if a thread is reading or writing; they block everyone else. 
**ReentrantReadWriteLock** recognizes that multiple threads reading the same data simultaneously is perfectly safe. It only enforces "Mutual Exclusion" when a thread needs to **write** (modify) the data.

### 2. Comparison: ReentrantLock vs. ReadWriteLock

| Feature | `ReentrantLock` | `ReentrantReadWriteLock` |
| :--- | :--- | :--- |
| **Read/Read** | **Blocking** (One at a time). | **Non-Blocking** (Infinite simultaneous readers). |
| **Read/Write** | Blocking. | Blocking (Reader waits for Writer). |
| **Write/Write** | Blocking. | Blocking (Only one Writer at a time). |
| **Best Use Case** | General purpose, frequent updates. | Databases, Caches, "Read-Heavy" metadata. |

---

### 3. The "Golden" Snippet: A Simple Database
In this example, we create a `SimpleDatabase` where many threads can query prices at the same time, but an update thread can safely modify them without causing data races.



```java
import java.util.TreeMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.Lock;

public class SimpleDatabase {
    private TreeMap<Integer, Integer> inventory = new TreeMap<>();
    private ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();
    
    // The "Read" side of the lock
    private Lock readLock = rwLock.readLock();
    // The "Write" side of the lock
    private Lock writeLock = rwLock.writeLock();

    public int getPrice(int productId) {
        readLock.lock();
        try {
            // Multiple threads can be here at the same time
            return inventory.getOrDefault(productId, 0);
        } finally {
            readLock.unlock();
        }
    }

    public void updatePrice(int productId, int newPrice) {
        writeLock.lock();
        try {
            // While a thread is here, NO ONE can read or write
            inventory.put(productId, newPrice);
        } finally {
            writeLock.unlock();
        }
    }
}
```

#### Code Explanation:
1.  **The Lock Split:** We initialize one `ReentrantReadWriteLock` but extract two different `Lock` objects from it: `readLock()` and `writeLock()`.
2.  **Shared Reads:** When `readLock.lock()` is called, the JVM checks: "Is anyone writing?" If no, the thread enters. Ten other threads can call `readLock.lock()` and enter simultaneously.
3.  **Exclusive Writes:** When `writeLock.lock()` is called, the thread waits until **all** current readers have finished and unlocked. Once the writer is inside, any new readers are blocked until the writer is done.

#### Example Output:
```text
Thread-Reader-1: Reading Price... (Success)
Thread-Reader-2: Reading Price... (Success - Parallel with Reader 1)
Thread-Writer: Requesting Write Lock... (Waiting for Readers to finish)
Thread-Reader-3: Requesting Read Lock... (Blocked by waiting Writer)
Thread-Writer: Write Lock Acquired. Updating...
Thread-Writer: Released.
Thread-Reader-3: Read Lock Acquired. Price is now updated.
```

---

### 4. The Gotchas
* **Write Starvation:** If you have a constant stream of readers, a writer might wait forever to get in. Java's `ReentrantReadWriteLock` handles this by prioritizing the "next in line," but in very heavy read systems, writers can still struggle.
* **No "Upgrade" directly:** You cannot "upgrade" a lock from Read to Write while holding the Read lock (this causes a Deadlock). You must `unlock()` the read lock and then `lock()` the write lock.
* **Complexity Overhead:** If your "Read" operation is extremely fast (like just reading a single `int`), the overhead of managing a `ReadWriteLock` might actually be slower than a simple `synchronized` block. Use this only when the "Read" operation takes some time or happens extremely frequently.
