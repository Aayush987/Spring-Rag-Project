---
title: The 4 Conditions for Deadlock (Coffman’s Conditions)
date: 2026-03-27
draft: false 
---

### 1. Mutual Exclusion
Only one thread can have exclusive access to a resource at any given time. If a second thread tries to access that resource, it must wait until the first thread releases it.
* **The Problem:** If resources were sharable (like a Read-Only file), there would be no waiting and no deadlock.

### 2. Hold and Wait
A thread is already holding at least one resource and is waiting to acquire additional resources that are currently being held by other threads.
* **The Problem:** The thread doesn't "give up" what it already has while it waits for the next piece of the puzzle.

### 3. No Preemption
Resources cannot be forcibly taken away from a thread. A resource can only be released voluntarily by the thread holding it after that thread has completed its task.
* **The Problem:** The OS or JVM cannot "step in" and snatch a lock away to give it to a starving thread.

### 4. Circular Wait
A closed chain of threads exists such that each thread holds at least one resource needed by the next thread in the chain. 
* **Example:** Thread A waits for Thread B, Thread B waits for Thread C, and Thread C waits for Thread A.



---

### Comparison: How to Break Each Condition

| Condition | Strategy to Break It |
| :--- | :--- |
| **Mutual Exclusion** | Use non-blocking data structures (e.g., `AtomicInteger`) instead of locks. |
| **Hold and Wait** | Force a thread to request all required resources at the very beginning. |
| **No Preemption** | Use `ReentrantLock.tryLock()`. If the lock isn't available, the thread "backs at" and releases its current locks. |
| **Circular Wait** | **Global Lock Ordering:** Ensure every thread acquires locks in the exact same order (e.g., always Lock A then Lock B). |

---

### 2. The "Golden" Snippet: Breaking Circular Wait
This code shows the **Circular Wait** in action and the simple fix: **Lock Ordering**.

```java
public class DeadlockFix {
    private Object lock1 = new Object();
    private Object lock2 = new Object();

    // BAD: Potential Deadlock (Circular Wait)
    public void threadOneMethod() {
        synchronized (lock1) {
            synchronized (lock2) { /* Do work */ }
        }
    }

    // BAD: Acquires locks in opposite order
    public void threadTwoMethod() {
        synchronized (lock2) {
            synchronized (lock1) { /* Do work */ }
        }
    }

    // GOOD: Fixed via Lock Ordering
    public void fixedThreadTwoMethod() {
        synchronized (lock1) { // Order matches threadOneMethod
            synchronized (lock2) { /* Do work */ }
        }
    }
}
```

#### Code Explanation:
1.  **The Flaw:** In the "Bad" version, `threadOne` grabs `lock1` and `threadTwo` grabs `lock2`. They are now stuck forever waiting for each other (**Circular Wait**).
2.  **The Fix:** In the "Good" version, we force `threadTwo` to grab `lock1` first. If `threadOne` already has `lock1`, `threadTwo` will wait at the *first* lock instead of grabbing one and "holding and waiting" for the second.
3.  **Hierarchy:** By establishing a rule that "Lock 1 always comes before Lock 2," you eliminate the possibility of a circle.

#### Example Output:
**Before Fix (Deadlock):**
```text
Thread 1: Acquired Lock 1
Thread 2: Acquired Lock 2
... (System hangs indefinitely)
```
**After Fix (Success):**
```text
Thread 1: Acquired Lock 1
Thread 1: Acquired Lock 2
Thread 1: Released both
Thread 2: Acquired Lock 1
Thread 2: Acquired Lock 2
Task Completed!
```

---

### 3. The Gotchas
* **The Resource Hierarchy:** In complex systems, keeping track of lock order is hard. A common practice is to use the `System.identityHashCode()` of objects to determine which one to lock first.
* **The "All or Nothing" Trap:** While breaking "Hold and Wait" by requesting all locks at once sounds good, it often leads to **Starvation**, where a thread waits forever because it can never get *all* the keys it needs at the exact same time.
