---
title: Introduction to Virtual Threads (Project Loom)
date: 2026-03-27
draft: false 
---

### 1. The "Why"
Until now, we had two choices:
1.  **Platform Threads (BIO):** Easy to write, but expensive. 1,000 threads = 1GB RAM. You run out of memory quickly.
2.  **Asynchronous (NIO):** Scales to millions, but "Callback Hell" makes it incredibly hard to write, debug, and read.

**Virtual Threads** give you the best of both worlds: You write simple, blocking code (like in the 90s), but the JVM magically handles it like a high-performance Asynchronous system under the hood.

### 2. Comparison: Platform Threads vs. Virtual Threads

| Feature | Platform Threads (Old) | Virtual Threads (New) |
| :--- | :--- | :--- |
| **Mapping** | 1:1 with OS Threads. | M:N (Millions of Virtual : Few OS). |
| **Creation Cost** | Heavy (Slow to start). | **Cheap** (Fast as an Object). |
| **Memory** | ~1MB per thread (Stack). | **~Hundreds of bytes** per thread. |
| **Limit** | A few thousands. | **Millions.** |
| **Blocking** | Blocks the OS Thread. | **Yields** the OS Thread for others. |

---

### 3. The "Golden" Snippet: Million Thread Demo
In the old days, this code would crash your computer in seconds. With Virtual Threads, it runs on a laptop without breaking a sweat.



```java
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

public class VirtualThreadDemo {
    public static void main(String[] args) {
        // 1. Create a "Task" that simply sleeps (blocks)
        Runnable task = () -> {
            try {
                // In the old model, this would hold an OS thread hostage
                Thread.sleep(1000); 
                System.out.println("Finished on: " + Thread.currentThread());
            } catch (InterruptedException e) {}
        };

        // 2. Launch 100,000 Virtual Threads
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            IntStream.range(0, 100_000).forEach(i -> {
                executor.submit(task);
            });
        } 
        // 3. Executor auto-closes after all tasks are done
    }
}
```

#### Code Explanation:
1.  **`newVirtualThreadPerTaskExecutor()`**: This doesn't use a "Pool." It literally creates a brand new Virtual Thread for every single task. Because they are so cheap, we don't need to recycle them anymore.
2.  **The Magic of `sleep()`**: When a Virtual Thread hits a blocking call (like `sleep`, `socket.read`, or `db.query`), the JVM **detaches** it from the OS thread. The OS thread goes off to do other work, and when the data is ready, the JVM **reattaches** the Virtual Thread to continue where it left off.
3.  **Thread Name**: If you print the thread name, you'll see it’s a "Virtual Thread" running on top of a "ForkJoinPool" worker.

#### Example Output:
```text
Finished on: VirtualThread[#125]/runnable@ForkJoinPool-1-worker-3
Finished on: VirtualThread[#542]/runnable@ForkJoinPool-1-worker-1
... (100,000 times) ...
```

---

### 4. The Gotchas
* **Don't Pool Them:** We've spent 20 years learning to use `ThreadPools`. **Stop!** Virtual Threads are like Strings or Arrays—just create them when you need them and let the Garbage Collector handle them.
* **Pinned Threads:** If you use `synchronized` blocks or native (C/C++) code, the Virtual Thread might get "pinned" to the OS thread, preventing the magic "detaching" from happening. Use `ReentrantLock` instead of `synchronized` to get the most out of Loom.
* **CPU-Bound Tasks:** Virtual Threads do **not** make math faster. If your task is a heavy calculation (not waiting for IO), a standard Fixed Thread Pool is still better.
