---
title: Virtual Threads - Deep Dive
date: 2026-03-27
draft: false 
---


To understand what Virtual Threads do under the hood, we have to look at the "Magic" that happens between the Java code you write and the Operating System (OS) that actually executes it.

Before Java 21, a **Java Thread** was just a thin wrapper around an **OS Thread**. If you created 1,000 Java threads, the OS had to manage 1,000 stacks, which is why your memory would spike.

### 1. The Architecture: M:N Scheduling
Virtual Threads introduce a "layered" approach. Instead of a 1:1 relationship with the OS, we now have an **M:N relationship**.

* **Virtual Threads (The "M"):** These are cheap, "user-mode" threads managed by the **Java Virtual Machine (JVM)**. They are stored as objects on the Java Heap, not as expensive OS resources.
* **Carrier Threads (The "N"):** These are standard OS threads (usually in a `ForkJoinPool`). These are the "engines" that actually run the Virtual Threads.



---

### 2. The Mechanics: Mounting and Unmounting
The "magic" happens when a Virtual Thread performs a **Blocking IO** operation (like reading from a database or a socket).

1.  **Mounting:** The JVM scheduler takes a Virtual Thread from a queue and "mounts" it onto a **Carrier Thread**. The Carrier Thread starts executing the code.
2.  **The Roadblock:** Your code hits a blocking call: `inputStream.read()`.
3.  **Unmounting (The Secret Sauce):** * Instead of the OS thread freezing, the JVM **captures the state** (the stack frame) of the Virtual Thread and moves it to the **Java Heap**.
    * The Virtual Thread is "unmounted." 
    * The **Carrier Thread is now free!** It immediately picks up a *different* Virtual Thread to work on.
4.  **Resuming:** Once the OS signals that the data is ready (via non-blocking IO under the hood), the JVM scheduler puts the original Virtual Thread back in the queue. It will eventually be **remounted** onto any available Carrier Thread to finish its job.



---

### 3. Comparison: How they save resources

| Action | Platform Thread (Old) | Virtual Thread (New) |
| :--- | :--- | :--- |
| **Creation** | Involves a System Call (Slow). | Just an Object allocation (Fast). |
| **Memory** | ~1MB reserved for Stack. | ~Hundreds of bytes on the Heap. |
| **Blocking** | Thread is "stuck" (idling CPU). | Thread is "parked" (releasing CPU). |
| **Context Switch** | OS Kernel (Heavy). | JVM Scheduler (Lightweight). |

---

### 4. Why they aren't "Faster" for Math
It is a common misconception that Virtual Threads make your code run faster. 
* If you have a loop calculating Pi to a billion digits, a Virtual Thread will take exactly as much time as a Platform Thread. 
* Virtual Threads provide **Scalability (Throughput)**, not **Speed (Latency)**. They allow a single server to handle 1,000,000 simultaneous "waiters" without needing 1,000,000 expensive OS threads.

---

### 5. The "Pinning" Trap
The only time the "under the hood" magic fails is during **Pinning**. If a Virtual Thread is inside a `synchronized` block or calling a Native (C/C++) method, the JVM cannot unmount it. The Virtual Thread becomes "stuck" to the Carrier Thread, effectively turning it back into an expensive Platform Thread until the block is finished. 

> **Pro-Tip:** This is why the Java community is moving away from `synchronized` and toward `ReentrantLock`, which does not cause pinning.
