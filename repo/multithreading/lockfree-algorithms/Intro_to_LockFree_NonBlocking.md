---
title: Introduction to Non-Blocking & Lock-Free Operations
date: 2026-03-27
draft: false 
---

### 1. The "Why"
Traditional locks (`synchronized`, `ReentrantLock`) have major downsides:
* **Deadlocks:** Threads waiting for each other forever.
* **Priority Inversion:** A low-priority thread holding a lock prevents a high-priority thread from running.
* **Performance Overhead:** Suspending and resuming a thread (context switching) is expensive for the OS.

**Lock-Free** operations use hardware-level atomic instructions to ensure that at least one thread always makes progress, even if others are interrupted or fail.

### 2. Comparison: Blocking vs. Lock-Free (Non-Blocking)

| Feature | Blocking (Locks) | Lock-Free (Non-Blocking) |
| :--- | :--- | :--- |
| **Philosophy** | "Pessimistic" - Assume trouble and lock the door. | "Optimistic" - Assume success and retry if failed. |
| **Hardware Tool** | Mutex / Monitors. | **CAS (Compare-And-Swap)** instructions. |
| **Deadlock Risk** | High. | **Zero.** |
| **Throughput** | Limited by lock contention. | High (Scales better with more CPU cores). |
| **Complexity** | Simple to write and reason about. | Extremely difficult to design correctly. |

---

### 3. The "Golden" Snippet: Atomic Compare-And-Swap (CAS)
The heart of every lock-free algorithm is the **CAS** operation. It is a single CPU instruction that says: *"If the value in memory is X, change it to Y. If it's not X anymore, don't do anything and tell me I failed."*



```java
import java.util.concurrent.atomic.AtomicInteger;

public class LockFreeCounter {
    private AtomicInteger value = new AtomicInteger(0);

    public void increment() {
        int oldValue;
        int newValue;

        do {
            // 1. Read the current value
            oldValue = value.get();
            // 2. Calculate the new value
            newValue = oldValue + 1;

            // 3. TRY to update. 
            // If another thread changed the value in the meantime, 
            // compareAndSet returns 'false' and we loop again.
        } while (!value.compareAndSet(oldValue, newValue));
    }

    public int getValue() {
        return value.get();
    }
}
```

#### Code Explanation:
1.  **The "Snapshot"**: We read `oldValue`. Between reading it and trying to update it, another thread could have incremented it.
2.  **The Challenge**: `compareAndSet(oldValue, newValue)` is an **Atomic** instruction. The CPU guarantees that no other thread can modify the memory address during this specific check.
3.  **The "Spin"**: If the update fails (returns `false`), it means we were "raced" by another thread. Instead of sleeping (blocking), we immediately try again with the latest value.

#### Example Output:
```text
Thread-A: Reads 5.
Thread-B: Reads 5.
Thread-B: Successfully CAS(5, 6). Value is now 6.
Thread-A: Tries CAS(5, 6). Fails! (Value is actually 6).
Thread-A: Loops, Reads 6, Successfully CAS(6, 7). Value is now 7.
```

---

### 4. The Gotchas
* **Livelock / Starvation**: While deadlocks are gone, a thread could theoretically fail its CAS check forever if other threads are constantly updating the value faster than it can.
* **The ABA Problem**: If a value changes from A to B and back to A, a CAS operation might think nothing changed and proceed, which can cause bugs in complex data structures like Stacks. (Solution: `AtomicStampedReference`).
* **CPU Cycles**: Lock-free is only faster if contention is moderate. If 1,000 threads are fighting for one `AtomicInteger`, they will waste massive amounts of CPU cycles just "spinning" in `while` loops.
