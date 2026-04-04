---
title: Virtual Threads - Best Practices
date: 2026-03-27
draft: false 
---

### 1. Don't Pool Virtual Threads
**The Old Rule:** Threads are expensive; create a `FixedThreadPool` and reuse them.
**The New Rule:** Virtual threads are disposable objects. Create a new one for every single task.

* **Why?** A pool's purpose is to limit resource usage and avoid the cost of thread creation. Since Virtual Threads cost almost nothing to create and take up very little memory, pooling them just adds unnecessary complexity and contention.



```java
// DO THIS (New for every task)
try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
    executor.submit(() -> doWork());
}

// DON'T DO THIS (Pooling virtual threads is an anti-pattern)
// ExecutorService pool = Executors.newFixedThreadPool(100); 
```

---

### 2. Avoid "Pinning" (Replace `synchronized`)
As we discussed, **Pinning** happens when a Virtual Thread cannot be "unmounted" from its Carrier Thread during a blocking operation. This usually happens inside `synchronized` blocks.

* **The Best Practice:** Replace `synchronized` with `ReentrantLock` in code that performs IO or long-running tasks. `ReentrantLock` allows the Virtual Thread to unmount properly, keeping your scalability high.



```java
// POTENTIAL ISSUE: Synchronized can pin the thread during IO
public synchronized void fetchData() {
    // line.read() here might pin the carrier thread
}

// BEST PRACTICE: Use ReentrantLock
private final ReentrantLock lock = new ReentrantLock();
public void fetchData() {
    lock.lock();
    try {
        // Virtual thread can unmount here freely
    } finally {
        lock.unlock();
    }
}
```

---

### 3. Be Careful with `ThreadLocal`
**The Problem:** Many frameworks use `ThreadLocal` to store things like Database Connections or Security Contexts. If you have **1,000,000** Virtual Threads, you could end up with **1,000,000** copies of these objects in memory.

* **The Best Practice:** 1.  Use `ThreadLocal` only for very small objects.
    2.  If you need to share large data, consider using **Scoped Values** (a Java 21+ feature designed specifically for Virtual Threads).

---

### 4. Don't Use Virtual Threads for CPU-Bound Work
Virtual threads are designed for **waiting** (IO). If your code is doing heavy math, video encoding, or complex searching, a Virtual Thread offers no advantage over a Platform Thread.

* **Rule of Thumb:** * **IO-Bound?** (DB, Web, File) $\rightarrow$ **Virtual Threads**.
    * **CPU-Bound?** (Math, Logic, Loops) $\rightarrow$ **Platform Thread Pool** (sized to your number of CPU cores).

---

### 5. Summary Checklist

| Topic | Old Best Practice (Platform) | New Best Practice (Virtual) |
| :--- | :--- | :--- |
| **Creation** | Use `ThreadPoolExecutor`. | Use `newVirtualThreadPerTaskExecutor`. |
| **Locking** | `synchronized` is fine. | Prefer `ReentrantLock` to avoid pinning. |
| **Scaling** | Limit threads to 200-500. | Scale to millions. |
| **IO Style** | Use Reactive/NIO for scale. | Use Simple Blocking code for scale. |
