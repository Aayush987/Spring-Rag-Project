---
title: Joining Threads
date: 2026-03-26
draft: false 
---

# Thread Coordination - Part 2: Joining Threads

## 1. The Problem: The "Race" to the Finish
When you start a thread, it runs independently. If the `main` thread needs the result of `Thread-A` to perform a calculation, `main` might finish before `Thread-A` even starts, leading to `null` results or crashes.



---

## 2. The Solution: `thread.join()`
The `join()` method allows the calling thread (e.g., `main`) to go into a **Waiting** state until the target thread finishes its execution.

### Basic Syntax
```java
Thread worker = new Thread(new ComplexTask());
worker.start();

// The current thread will pause here until 'worker' is dead
worker.join(); 

System.out.println("Worker is done. Now I can use the results.");
```

---

## 3. Handling Coordination Failures
What if the worker thread hangs? If you call `join()` without a timeout, your main thread will wait **forever**. 

### The Timed Join
Always consider using a timeout to keep your application responsive:
```java
// Wait for at most 2 seconds
worker.join(2000); 

if (worker.isAlive()) {
    System.out.println("Worker took too long! Moving on or canceling...");
} else {
    System.out.println("Worker finished in time.");
}
```

---

## 4. Coordination Example (The Calculation Pattern)
In your course, you likely saw an example where multiple threads calculate parts of a result. Here is how they coordinate:

```java
public static void main(String[] args) throws InterruptedException {
    List<FactorialThread> threads = Arrays.asList(
        new FactorialThread(10L),
        new FactorialThread(20L)
    );

    for (Thread t : threads) t.start();

    // COORDINATION: Wait for ALL threads to finish
    for (Thread t : threads) {
        t.join(); 
    }

    // Now it is safe to gather results
    for (FactorialThread t : threads) {
        System.out.println("Result: " + t.getResult());
    }
}
```

---

## 5. Key Takeaways Table

| Feature | `Thread.sleep(ms)` | `thread.join()` |
| :--- | :--- | :--- |
| **Purpose** | Pause current thread for a fixed time. | Wait for a *specific thread* to finish. |
| **Efficiency** | Guessing (might wait too long/short). | Precise (resumes exactly when task is done). |
| **Condition** | Time-based. | Execution-based. |

---

## 6. Summary Checklist
* [ ] Do I understand that `join()` blocks the **caller** thread, not the target?
* [ ] Do I always handle `InterruptedException` when calling `join()`?
* [ ] Am I using `join(timeout)` for mission-critical, responsive apps?

---