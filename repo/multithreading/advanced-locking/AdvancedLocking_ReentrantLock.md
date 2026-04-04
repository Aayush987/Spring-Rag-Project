---
title: ReentrantLock - tryLock & Interruptibility
date: 2026-03-27
draft: false 
---


### 1. The "Why"
`ReentrantLock` is a manual alternative to `synchronized`. We use it when we need to break **Coffman's Conditions** for deadlocks. Specifically:
* **Breaking "No Preemption":** With `tryLock()`, if a thread can't get the lock, it can walk away instead of hanging.
* **Breaking "Uninterruptible Wait":** With `lockInterruptibly()`, we can stop a waiting thread externally using `thread.interrupt()`.

### 2. Comparison: `synchronized` vs. `ReentrantLock`

| Feature | `synchronized` | `ReentrantLock` |
| :--- | :--- | :--- |
| **Flexibility** | Low (Block-scoped only). | High (Can start lock in one method, end in another). |
| **Fairness** | No (Random thread gets the lock). | Optional (Can grant lock to the longest-waiting thread). |
| **Timeout Support** | No (Waits forever). | **Yes (`tryLock`)**. |
| **Interruptibility** | No (Cannot be interrupted while waiting). | **Yes (`lockInterruptibly`)**. |
| **Syntax** | Simple (Automatic cleanup). | Complex (Requires manual `unlock()` in a `finally` block). |

---

### 3. The "Golden" Snippet: The Safe Transfer (No Deadlock)
This snippet uses `tryLock()` to attempt to acquire two locks. If it fails to get the second one, it "preempts" itself by releasing the first one and trying again later.



```java
import java.util.concurrent.locks.ReentrantLock;
import java.util.Random;

public class SafeBank {
    private final ReentrantLock lockA = new ReentrantLock();
    private final ReentrantLock lockB = new ReentrantLock();

    public void transferMoney() {
        Random random = new Random();
        while (true) {
            boolean gotLockA = lockA.tryLock();
            boolean gotLockB = lockB.tryLock();

            if (gotLockA && gotLockB) {
                try {
                    System.out.println(Thread.currentThread().getName() + " acquired both locks!");
                    // Critical Section: Perform Transfer
                    break; 
                } finally {
                    lockA.unlock();
                    lockB.unlock();
                }
            }

            // If we only got one, we MUST release it to avoid holding and waiting
            if (gotLockA) lockA.unlock();
            if (gotLockB) lockB.unlock();

            // Sleep a bit before retrying to avoid "Livelock"
            try { Thread.sleep(random.nextInt(10)); } catch (InterruptedException e) { return; }
        }
    }
}
```

#### Code Explanation:
1.  **`tryLock()`**: Unlike a normal lock, this returns `true` immediately if the lock is available and `false` if it isn't. It never blocks your thread.
2.  **`finally { lock.unlock() }`**: This is **MANDATORY**. Because `ReentrantLock` isn't bound by `{}` brackets like `synchronized`, if you forget to unlock, the lock stays held until the app crashes.
3.  **Livelock Prevention**: We sleep for a random amount of time. If two threads both release their locks and retry at the *exact* same microsecond, they might keep bumping into each other forever (**Livelock**). Randomness breaks this cycle.

#### Example Output:
```text
Thread-1: Failed to get Lock B, releasing Lock A...
Thread-2: Failed to get Lock A, releasing Lock B...
Thread-1: Acquired both locks!
Thread-2: Acquired both locks!
```

---

### 4. The Gotchas
* **The "Forgotten Unlock":** If your code throws an exception inside the critical section and you didn't put `unlock()` in a `finally` block, the lock is never released. Other threads will be blocked forever.
* **Overhead:** `ReentrantLock` is slightly more memory-intensive than `synchronized`. Don't use it unless you actually need features like `tryLock` or `fairness`.
* **The `lock()` vs `tryLock()`:** If you call `lock.lock()`, it behaves exactly like `synchronized` (it blocks). Only `tryLock()` provides the non-blocking behavior.
