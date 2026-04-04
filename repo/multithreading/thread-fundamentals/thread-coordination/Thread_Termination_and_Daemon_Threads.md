---
title: Thread Termination & Daemon Threads
date: 2026-03-26
draft: false 
---


# Thread Termination & Daemon Threads

## 1. Why is Termination Tricky?
Threads consume resources (Memory, CPU, File Handles). If a thread finishes its task but stays alive, it's a **Resource Leak**. 
* We cannot use the `stop()` method (it is deprecated and dangerous).
* We must use **Cooperative Termination**.

---

## 2. Method 1: The Interrupt Signal
Interrupting is a way for one thread to signal another: "Please stop what you are doing."

### Scenario A: Thread is Blocking (Sleeping/Waiting)
If a thread is executing a method like `Thread.sleep()` or `wait()`, it will throw an `InterruptedException` when interrupted.

```java
public void run() {
    try {
        Thread.sleep(500000); // Long task
    } catch (InterruptedException e) {
        System.out.println("Thread was interrupted externally. Closing resources...");
        return; // Exit the run method cleanly
    }
}
```

### Scenario B: Thread is Busy (Hard Calculation)
If the thread is in a loop doing heavy math, it won't "feel" the interrupt unless it manually checks its status.

```java
public void run() {
    for (int i = 0; i < 1000000; i++) {
        if (Thread.currentThread().isInterrupted()) {
            System.out.println("Interrupted during calculation!");
            return;
        }
        // Do heavy work
    }
}
```

---

## 3. Daemon Threads
By default, a Java application will **not** exit as long as there is at least one "User Thread" still running. Even if `main` finishes, the app stays alive.



**Daemon Threads** are background "helper" threads.
* **The Rule:** If only Daemon threads are left running, the JVM will shut down automatically.
* **Use Case:** Garbage collection, background monitoring, or auto-save features.
* **How to set:** You must set this **before** calling `.start()`.

```java
Thread bgThread = new Thread(new LongTask());
bgThread.setDaemon(true); // Now this won't block the app from closing
bgThread.start();
```

---

## 4. Key Differences Table

| Feature | User Thread (Default) | Daemon Thread |
| :--- | :--- | :--- |
| **Priority** | High / Normal | Low (usually) |
| **JVM Behavior** | JVM waits for it to finish before exiting. | JVM exits regardless of its state. |
| **Example** | Main logic, File Writing. | Cache Eviction, Heartbeat signal. |
| **Termination** | Must be handled explicitly via Interrupts. | Terminates automatically when app closes. |

---

## 5. Summary Checklist
* [ ] Do I understand that `interrupt()` is a **request**, not a command?
* [ ] Can I identify when to use `isInterrupted()` vs catching `InterruptedException`?
* [ ] Do I know that a Daemon thread's code might suddenly stop mid-execution when the app exits?

---
