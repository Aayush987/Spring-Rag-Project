---
title: Locking Strategies & Deadlocks
date: 2026-03-27
draft: false 
---

### 1. The "Why"
As applications grow, a single lock (like `synchronized(this)`) becomes a bottleneck. To improve performance, we use **Fine-Grained Locking** (multiple locks for different resources). However, the moment you have more than one lock, the **Order of Acquisition** matters. If two threads try to acquire the same two locks in a different order, they can end up in a Deadlock.

### 2. Comparison: Coarse-Grained vs. Fine-Grained Locking

| Feature | Coarse-Grained | Fine-Grained |
| :--- | :--- | :--- |
| **Simplicity** | High (One lock for everything). | Low (Many locks to manage). |
| **Performance** | Low (Threads queue up unnecessarily). | High (Threads only block if using the same resource). |
| **Risk of Deadlock** | Zero (You can't deadlock with one lock). | High (Requires strict discipline). |
| **Example** | `synchronized(database)` | `lockTableA`, `lockTableB` |

---

### 3. The "Golden" Snippet: The Deadlock Trap
In this example, two threads are trying to transfer money between two accounts. Each account has its own lock.



```java
public class DeadlockDemo {
    private final Object lockA = new Object();
    private final Object lockB = new Object();

    public void processTransaction1() {
        synchronized (lockA) { // Thread 1 takes Lock A
            System.out.println("Thread 1: Holding lock A...");
            try { Thread.sleep(50); } catch (InterruptedException e) {}

            System.out.println("Thread 1: Waiting for lock B...");
            synchronized (lockB) { // Thread 1 waits for Lock B
                System.out.println("Thread 1: Acquired A & B!");
            }
        }
    }

    public void processTransaction2() {
        synchronized (lockB) { // Thread 2 takes Lock B
            System.out.println("Thread 2: Holding lock B...");
            try { Thread.sleep(50); } catch (InterruptedException e) {}

            System.out.println("Thread 2: Waiting for lock A...");
            synchronized (lockA) { // Thread 2 waits for Lock A
                System.out.println("Thread 2: Acquired B & A!");
            }
        }
    }
}
```

#### Code Explanation:
1.  **The Standoff:** Thread 1 grabs `lockA`. Thread 2 grabs `lockB`.
2.  **The Circular Wait:** Thread 1 now wants `lockB` (which Thread 2 is holding). Thread 2 now wants `lockA` (which Thread 1 is holding). 
3.  **The Result:** Neither thread can proceed. They will sit there forever. The JVM will not throw an error; the CPU usage will drop to 0%, and the app will simply stop responding.

#### Example Output:
```text
Thread 1: Holding lock A...
Thread 2: Holding lock B...
Thread 1: Waiting for lock B...
Thread 2: Waiting for lock A...
... (Infinite Silence) ...
```

---

### 4. How to Prevent Deadlocks
There are three main strategies to avoid or break a deadlock:

1.  **Strict Lock Ordering (Recommended):** Always acquire locks in the same order. If both threads always lock `A` then `B`, a deadlock is physically impossible.
2.  **Lock Timeout (`tryLock`):** Instead of `synchronized`, use `ReentrantLock.tryLock(timeout)`. If a thread can't get the lock in 5 seconds, it gives up, releases its own locks, and tries again later.
3.  **Deadlock Detection:** Using tools like `jconsole` or `jstack` to see the "Thread Dump" and identify which threads are stuck.

---

### 5. The Gotchas
* **The Hidden Lock:** Sometimes you don't realize you're locking. Calling a `synchronized` method inside another `synchronized` method on a different object is a hidden multi-lock scenario.
* **The "Dining Philosophers" Problem:** A classic interview question based on this exact concept. The solution is always to break the "Circular Wait" condition.
* **Locking on Strings/Integers:** Never use `synchronized("myLock")`. Java "interns" strings, meaning two unrelated parts of your code might accidentally share the same lock object, leading to mysterious deadlocks.
