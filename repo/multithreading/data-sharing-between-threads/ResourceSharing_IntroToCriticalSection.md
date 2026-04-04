---
title: Resource Sharing & Critical Sections (Deep Dive)
date: 2026-03-27
draft: false 
---

### 1. The "Why"
In multithreading, a **Critical Section** is any segment of code that accesses a shared resource (like a variable, a file, or a database connection) where at least one thread is performing a **write** operation. 

The problem is that most high-level operations (like `count++`) are not **atomic**. To the CPU, `count++` is actually three distinct steps:
1. **Load:** Move the value from the Main Memory (Heap) into a CPU Register.
2. **Increment:** Add 1 to the value inside the Register.
3. **Store:** Move the new value from the Register back to Main Memory.

If Thread A is "paused" (context-switched) after Step 2, and Thread B performs all three steps, Thread A will eventually wake up and overwrite Thread B's work with an outdated value. This is a **Race Condition**.

### 2. Visual Logic: The "Lost Update" Problem
Imagine a shared variable `counter = 5`.
* **Thread 1** reads `5`, increments it to `6` in its private register, but then the OS pauses it.
* **Thread 2** reads `5`, increments it to `6`, and saves `6` to memory.
* **Thread 1** wakes up, still holding the value `6` in its register, and saves `6` to memory.
* **The Result:** Even though two increments happened, the value is `6` instead of `7`. One update was "lost."



### 3. The "Golden" Snippets

#### Example A: The Broken Counter (Race Condition)
This code looks correct but will produce a different total every time you run it because the `inventory` variable is shared on the Heap without protection.

```java
public class SharedResourceDemo {
    public static void main(String[] args) throws InterruptedException {
        Inventory inventory = new Inventory();
        
        // Thread to add items
        Thread incrementThread = new Thread(() -> {
            for (int i = 0; i < 10000; i++) inventory.increment();
        });

        // Thread to remove items
        Thread decrementThread = new Thread(() -> {
            for (int i = 0; i < 10000; i++) inventory.decrement();
        });

        incrementThread.start();
        decrementThread.start();
        
        incrementThread.join();
        decrementThread.join();

        // Expected: 0, Actual: Random value (e.g., -14, 22, 5)
        System.out.println("Final items: " + inventory.getItems());
    }

    static class Inventory {
        private int items = 0; // Shared on the Heap

        public void increment() { items++; } // CRITICAL SECTION
        public void decrement() { items--; } // CRITICAL SECTION
        public int getItems() { return items; }
    }
}
```

#### Example B: The "Check-Then-Act" Race Condition
Race conditions aren't just about math; they are about **timing**. Even if you use thread-safe components, the *logic* between two calls can be a critical section.

```java
public void withdraw(int amount) {
    // Critical Section starts here
    if (balance >= amount) {           // 1. Check
        // Context switch could happen here!
        balance = balance - amount;    // 2. Act
    }
}
```
If two threads check the balance at the same time, they might both see `$100`, both pass the check, and both withdraw `$100`, leaving the account at `-$100`.

### 4. The Gotchas
* **Non-Atomic Longs/Doubles:** In Java, 64-bit types like `long` and `double` are not even guaranteed to have atomic "writes" on 32-bit JVMs. A thread could theoretically write the first 32 bits, get interrupted, and leave the variable in a "half-written" corrupted state.
* **The Visibility Problem:** Sometimes a thread updates a variable on the Heap, but another thread keeps reading a "cached" version from its CPU Core's local cache. This is why we need `volatile` or `synchronized`.
* **Identifying Critical Sections:** A common interview task is to point out the critical section. Rule of thumb: If you see a shared variable and an assignment operator (`=`, `++`, `+=`), you are looking at a critical section.

---