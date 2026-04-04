---
title: Thread Introduction
date: 2026-03-26
draft: false 
---

# Thread Creation Part 1 Capabilites & Debugging

## 1. Core Concept: Thread vs. Process
Before diving into code, it is vital to understand the "Container" relationship:
* **Process:** An instance of a running program. It has its own memory space (Heap, Stack, etc.).
* **Thread:** A unit of execution within a process. Multiple threads share the same **Heap** but have their own **Stack**.



---

## 2. Thread Capabilities
Threads allow us to perform multiple tasks concurrently. Key capabilities include:
* **Parallelism:** Running tasks at the exact same time on multi-core CPUs.
* **Responsiveness:** Keeping a UI or main service active while a background task (like a file download) runs.
* **Resource Sharing:** Easily passing data between threads since they share the same memory space.

---

## 3. The "Golden" Snippet (Java Example)
Most Udemy courses use Java for these fundamentals. Here is the cleanest way to create a thread and set its properties for debugging.

```java
public class Main {
    public static void main(String[] args) {
        Thread thread = new Thread(() -> {
            // Code to be executed in the new thread
            System.out.println("We are now in thread: " + Thread.currentThread().getName());
            System.out.println("Current thread priority is: " + Thread.currentThread().getPriority());
        });

        // 1. Give the thread a name (Crucial for Debugging)
        thread.setName("Worker-Thread-01");

        // 2. Set Priority (1 to 10)
        thread.setPriority(Thread.MAX_PRIORITY);

        System.out.println("Starting thread from: " + Thread.currentThread().getName());
        thread.start();
    }
}
```

---

## 4. Debugging & Monitoring
One of the most important takeaways from this section is how to identify threads when things go wrong.

### Why set a Thread Name?
If your application crashes or hangs, a "Thread Dump" will show names like `Thread-0`, `Thread-1`, etc. This is useless in a large app. 
> **Rule of Thumb:** Always use `thread.setName("DescriptiveName")` so you can find it in your logs or debugger.

### Exception Handling in Threads
Standard `try-catch` blocks inside `main` won't catch exceptions thrown in a separate thread. You must use an **UncaughtExceptionHandler**:

```java
thread.setUncaughtExceptionHandler((t, e) -> {
    System.out.println("A critical error occurred in thread " + t.getName() 
        + " : " + e.getMessage());
});
```

---

## 5. Summary Checklist for Revision
* [ ] Do I understand the difference between `thread.start()` and `thread.run()`? (Hint: `run()` doesn't start a new thread!)
* [ ] Can I name a thread for better logging?
* [ ] Do I know how to set an Exception Handler for background errors?

---