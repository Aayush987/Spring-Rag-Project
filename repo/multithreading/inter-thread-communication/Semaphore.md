---
title: Semaphore - Scalable Producer-Consumer
date: 2026-03-27
draft: false 
---


### 1. The "Why"
A standard `Mutex` or `synchronized` block only allows **one** thread at a time. A **Semaphore**, however, maintains a set of **permits**. 
* It is used to limit the number of concurrent threads accessing a specific resource (e.g., only 5 database connections allowed).
* In a **Producer-Consumer** scenario, we use it to signal when "Space is Available" (for the producer) and when "Items are Available" (for the consumer).

### 2. Comparison: Mutex vs. Semaphore

| Feature | Mutex (or Binary Semaphore) | Counting Semaphore |
| :--- | :--- | :--- |
| **Permit Count** | Exactly 1. | $N$ (User-defined). |
| **Ownership** | Only the thread that locked it can unlock it. | **No ownership.** Any thread can release a permit. |
| **Primary Use** | Protecting a Critical Section (Safety). | Signalling and Resource Throttling (Coordination). |
| **Analogy** | A bathroom with one key. | A parking lot with $N$ spots. |

---

### 3. The "Golden" Snippet: Semaphore-Based Producer-Consumer
This implementation uses two semaphores to coordinate a shared buffer. One tracks "Full" slots and the other tracks "Empty" slots.



```java
import java.util.concurrent.Semaphore;
import java.util.LinkedList;
import java.util.Queue;

public class SharedBuffer {
    private final Queue<Integer> queue = new LinkedList<>();
    private final int capacity = 5;

    // Initially: 5 empty slots, 0 full slots
    private final Semaphore emptySlots = new Semaphore(capacity);
    private final Semaphore fullSlots = new Semaphore(0);
    private final Semaphore mutex = new Semaphore(1); // To protect the queue

    public void produce(int item) throws InterruptedException {
        emptySlots.acquire(); // Wait for an empty slot
        mutex.acquire();      // Lock the queue
        
        queue.add(item);
        System.out.println("Produced: " + item);
        
        mutex.release();      // Unlock queue
        fullSlots.release();  // Signal that an item is available
    }

    public void consume() throws InterruptedException {
        fullSlots.acquire();  // Wait for an item
        mutex.acquire();      // Lock the queue
        
        int item = queue.poll();
        System.out.println("Consumed: " + item);
        
        mutex.release();      // Unlock queue
        emptySlots.release(); // Signal that a slot is now empty
    }
}
```

#### Code Explanation:
1.  **`acquire()`**: If the permit count is 0, the thread blocks. If > 0, it decrements the count and proceeds.
2.  **`release()`**: Increments the permit count and wakes up any waiting threads.
3.  **Coordination Logic**: 
    * The **Producer** "consumes" an empty slot and "produces" a full slot.
    * The **Consumer** "consumes" a full slot and "produces" an empty slot.
4.  **The Mutex Semaphore**: Since a `LinkedList` is not thread-safe, we use a third semaphore with 1 permit as a lock to ensure only one thread modifies the actual data structure at a time.

#### Example Output:
```text
Produced: 1
Produced: 2
Consumed: 1
Produced: 3
... (Buffer fills up) ...
Produced: 5
[Producer Blocks - Waiting for emptySlots]
Consumed: 2
[Producer Wakes up and adds next item]
```

---

### 4. The Gotchas
* **Deadlock via Order**: If you call `mutex.acquire()` **before** `emptySlots.acquire()`, you will cause a deadlock. The producer will hold the lock while waiting for a slot that will never open because the consumer is blocked trying to get the lock! 
    * **Rule:** Always acquire the "Signalling" semaphore before the "Mutex" semaphore.
* **Release without Acquire**: Unlike locks, you can call `release()` on a semaphore even if you never called `acquire()`. This can accidentally increase the number of permits beyond your intended capacity if your logic is buggy.
* **Fairness**: Like `ReentrantLock`, Semaphores can be initialized with a "fair" parameter (`new Semaphore(n, true)`) to ensure the longest-waiting thread gets the permit first, though this reduces throughput.