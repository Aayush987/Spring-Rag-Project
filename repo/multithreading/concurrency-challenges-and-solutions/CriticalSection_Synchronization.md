---
title: Critical Section & Synchronization
date: 2026-03-27
draft: false 
---


### 1. The "Why"
We need a way to make a sequence of operations **Atomic** (all-or-nothing). Since the CPU can interrupt a thread at any micro-instruction, we use **Mutual Exclusion (Mutex)**. This ensures that if Thread A is halfway through an update, Thread B is physically blocked from starting that same update until Thread A finishes.

### 2. Visual Logic: The Monitor/Lock Concept
In Java, every **Object** has a built-in "Monitor" (or Intrinsic Lock). Think of it like a bathroom with a single key:
1.  A thread wants to enter the critical section.
2.  It must "acquire" the key to the object's monitor.
3.  If another thread has the key, the current thread **suspends** (enters a BLOCKED state).
4.  Once the first thread leaves the section, it "releases" the key, and the JVM wakes up a waiting thread to take its turn.



### 3. The "Golden" Snippets

#### Pattern A: Synchronized Methods
The simplest way to protect a resource is to mark the entire method as `synchronized`. This uses the `this` object as the lock.

```java
public class SynchronizedCounter {
    private int items = 0;

    // Only one thread can be inside EITHER of these methods at once
    public synchronized void increment() {
        items++; 
    }

    public synchronized void decrement() {
        items--;
    }

    public synchronized int getItems() {
        return items;
    }
}
```

#### Pattern B: Synchronized Blocks (Finer Control)
Synchronizing an entire method can be slow if the method does other non-essential work. A **Synchronized Block** allows you to lock only the specific lines that touch shared data.

```java
public class FineGrainedLocking {
    private int countA = 0;
    private final Object lockA = new Object(); // Custom lock object

    public void incrementA() {
        // Non-critical code here (e.g., logging) can run in parallel
        System.out.println("Preparing to increment...");

        synchronized (lockA) {
            // ONLY this part is protected
            countA++;
        }
    }
}
```

#### Pattern C: Static Synchronization
If you have a `static` variable, you cannot lock on `this` (because there is no instance). You must lock on the **Class** object itself.

```java
public class StaticResource {
    private static int globalCount = 0;

    public static synchronized void increment() {
        globalCount++;
    }
    
    // Equivalent to:
    public static void manualIncrement() {
        synchronized (StaticResource.class) {
            globalCount++;
        }
    }
}
```

### 4. The Gotchas
* **The "Performance Tax":** Synchronization isn't free. Acquiring and releasing locks takes time. If you over-synchronize, your multi-threaded app might actually run slower than a single-threaded one.
* **Reentrancy:** Java locks are **Reentrant**. This means if Thread A holds a lock, it can call another synchronized method that uses the *same* lock without getting stuck. It "already has the key."
* **Locking on the Wrong Object:** This is a common bug. If Thread 1 synchronizes on `LockA` and Thread 2 synchronizes on `LockB` to access the *same* variable, they won't block each other. **Both threads must synchronize on the exact same object to be protected.**
* **Data Atomicity vs. Visibility:** `synchronized` solves both! It ensures only one thread writes at a time (Atomicity) AND it ensures that when the thread releases the lock, all changes are pushed to the main memory so other threads can see them (Visibility).

---