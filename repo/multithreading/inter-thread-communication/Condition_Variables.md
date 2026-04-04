---
title: Condition Variables - Inter-Thread Communication
date: 2026-03-27
draft: false 
---

### 1. The "Why"
Sometimes a thread has the lock but cannot proceed because the data isn't ready (e.g., a Consumer finds an empty queue). 
* **The Problem:** If the thread just loops (`while(queue.isEmpty());`), it wastes 100% of the CPU. 
* **The Solution:** The thread "waits" on a condition. This **releases the lock** and puts the thread to sleep. When another thread makes the condition true, it "signals" (notifies) the sleeping thread to wake up and try again.

### 2. Comparison: `wait/notify` vs. `Condition` Object

| Feature | `Object.wait/notify` | `java.util.concurrent.Condition` |
| :--- | :--- | :--- |
| **Association** | Every Java Object has one. | Associated with a `ReentrantLock`. |
| **Capability** | Only one "wait set" per object. | **Multiple** conditions per lock (e.g., `notFull` and `notEmpty`). |
| **Control** | Standard `synchronized` blocks. | Precision control with `signal()` and `await()`. |
| **Analogy** | A single waiting room for a whole office. | Multiple specific waiting rooms (e.g., "Radiology" vs "ER"). |

---

### 3. The "Golden" Snippet: Multi-Condition Producer-Consumer
Using `Condition` objects with `ReentrantLock` allows us to be very specific about *which* threads we wake up.



```java
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class AdvancedBuffer {
    private final Queue<Integer> queue = new LinkedList<>();
    private final int CAPACITY = 5;
    
    private final Lock lock = new ReentrantLock();
    // Two separate "waiting rooms"
    private final Condition stackEmptyCondition = lock.newCondition();
    private final Condition stackFullCondition = lock.newCondition();

    public void add(int item) throws InterruptedException {
        lock.lock();
        try {
            while (queue.size() == CAPACITY) {
                // Buffer is full; Producer goes to sleep
                stackFullCondition.await(); 
            }
            queue.add(item);
            System.out.println("Added: " + item);
            
            // Wake up only the Consumers
            stackEmptyCondition.signal(); 
        } finally {
            lock.unlock();
        }
    }

    public int remove() throws InterruptedException {
        lock.lock();
        try {
            while (queue.isEmpty()) {
                // Buffer is empty; Consumer goes to sleep
                stackEmptyCondition.await(); 
            }
            int item = queue.poll();
            System.out.println("Removed: " + item);
            
            // Wake up only the Producers
            stackFullCondition.signal(); 
            return item;
        } finally {
            lock.unlock();
        }
    }
}
```

#### Code Explanation:
1.  **`await()`**: This atomicaly **releases the lock** and puts the thread in a waiting state. This is crucial—if it didn't release the lock, the other thread could never enter to change the condition!
2.  **`while` vs `if`**: Always use a `while` loop to check the condition. This handles **Spurious Wakeups** (where a thread wakes up for no reason) and ensures the condition is still true when the thread regains the lock.
3.  **`signal()` vs `signalAll()`**: `signal()` wakes up one thread. `signalAll()` wakes up everyone. `signal()` is more efficient but requires you to be certain that any thread woken up can actually proceed.

#### Example Output:
```text
[Consumer]: Queue empty. Waiting...
[Producer]: Added 10. Signaling stackEmptyCondition...
[Consumer]: Woken up! Removed 10. Signaling stackFullCondition...
```

---

### 4. The Gotchas
* **IllegalMonitorStateException**: You **must** hold the lock before calling `await()` or `signal()`. If you call these outside of the `lock.lock()` block, the code will crash.
* **The Lost Signal**: If you `signal()` before a thread is actually `awaiting()`, the signal is lost forever. Unlike Semaphores (which remember permits), Conditions have no memory.
* **Deadlock**: If Thread A is waiting for a signal from Thread B, and Thread B is waiting for a signal from Thread A, and neither can signal without the other moving, you have a deadlock.