---
title: Objects as Condition Variables - wait, notify, & notifyAll
date: 2026-03-27
draft: false 
---

### 1. The "Why"
Every Java object has an **Intrinsic Lock** (the monitor). Along with that lock, every object maintains a **Wait Set**—a list of threads that are suspended and waiting for a signal related to that object. This allows threads to communicate without using complex external libraries, using only the objects they are already synchronizing on.

### 2. Comparison: `wait/notify` vs. `Condition` Objects

| Feature | `wait()` / `notify()` | `Condition` (`await/signal`) |
| :--- | :--- | :--- |
| **Origin** | Part of `java.lang.Object` (Available since JDK 1.0). | Part of `java.util.concurrent` (Added in JDK 1.5). |
| **Locking** | Works with `synchronized` blocks. | Works with `ReentrantLock`. |
| **Wait Sets** | Only **one** per object. | **Multiple** per lock (e.g., `notFull`, `notEmpty`). |
| **Efficiency** | `notifyAll()` wakes *everyone*, even if they can't proceed. | `signal()` can target specific groups of threads. |

---

### 3. The "Golden" Snippet: Classic Producer-Consumer
In this version, we don't need a `ReentrantLock`. We use the `synchronized` keyword and the shared object itself to coordinate.



```java
public class ClassicBuffer {
    private final java.util.Queue<Integer> queue = new java.util.LinkedList<>();
    private final int CAPACITY = 5;

    public synchronized void produce(int item) throws InterruptedException {
        // 1. Always check the condition in a WHILE loop
        while (queue.size() == CAPACITY) {
            // 2. Releases the 'this' lock and sleeps
            wait(); 
        }
        
        queue.add(item);
        System.out.println("Produced: " + item);

        // 3. Wake up waiting consumers
        notifyAll(); 
    }

    public synchronized int consume() throws InterruptedException {
        while (queue.isEmpty()) {
            // Releases the 'this' lock and sleeps
            wait(); 
        }

        int item = queue.poll();
        System.out.println("Consumed: " + item);

        // Wake up waiting producers
        notifyAll(); 
        return item;
    }
}
```

#### Code Explanation:
1.  **`synchronized`**: Both methods are synchronized on `this`. A thread must own the object's monitor to call `wait()` or `notify()`.
2.  **`wait()`**: When called, the thread is added to the object's Wait Set and **releases the lock**. This allows other threads to enter and change the state (e.g., a Consumer to remove an item).
3.  **`notifyAll()`**: This wakes up **all** threads currently in the Wait Set. They all move to the "Blocked" state and fight to re-acquire the lock. 
4.  **The Loop**: Once a thread re-acquires the lock, it starts right after the `wait()` call. The `while` loop ensures it re-checks the condition (e.g., "is the queue still empty?") before proceeding.

#### Example Output:
```text
[Producer]: Queue full. Calling wait()...
[Consumer]: Removed item. Calling notifyAll()...
[Producer]: Waking up, re-checking while loop, adding item.
```

---

### 4. The Gotchas
* **Spurious Wakeups**: Occasionally, a thread wakes up without any `notify()` being called (a quirk of OS/JVM interaction). This is why a `while` loop is **mandatory**, never use an `if` statement with `wait()`.
* **The `notify()` Risk**: `notify()` only wakes up *one* random thread. If you have multiple Producers and Consumers waiting, `notify()` might wake up another Producer when the queue is already full, leading to a "Deadlock-like" hang where everyone is sleeping. **When in doubt, use `notifyAll()`.**
* **IllegalMonitorStateException**: If you call `wait()` outside of a `synchronized` block, your code will crash immediately.
