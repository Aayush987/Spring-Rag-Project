---
title: Thread Inheritance
date: 2026-03-26
draft: false 
---

# Thread Creation - Part 2: Thread Inheritance

## 1. The Concept: Extending the Thread Class
Instead of passing a `Runnable` object to a new Thread instance, we create a specialized class that **is** a Thread. This is done using the `extends` keyword.

* **Mechanism:** Create a subclass of `Thread` and override the `run()` method.
* **Usage:** You instantiate your subclass and call `.start()`.



---

## 2. The "Golden" Snippet (Inheritance Pattern)
This approach encapsulates the thread logic and the data it needs into a single object.

```java
// Define the specialized thread
public class NewThread extends Thread {
    @Override
    public void run() {
        // This code runs in the new thread
        System.out.println("Hello from " + this.getName());
    }
}

// In your Main class
public class Main {
    public static void main(String[] args) {
        Thread thread = new NewThread();
        thread.start();
    }
}
```

---

## 3. Inheritance vs. Interface (The Trade-offs)
This is a classic interview topic. Why choose one over the other?

| Feature | Inheritance (`extends Thread`) | Interface (`implements Runnable`) |
| :--- | :--- | :--- |
| **Flexibility** | **Low.** Java doesn't support multiple inheritance. You can't extend any other class. | **High.** You can implement multiple interfaces and still extend another class. |
| **Cleanliness** | Mixes the "Thread" mechanism with your "Business Logic." | Keeps the task (Runnable) separate from the runner (Thread). |
| **Use Case** | Good for small, simple tasks or when creating custom Thread types. | **Standard practice** for most production applications and Thread Pools. |

---

## 4. Key Takeaways for Debugging
When using inheritance, the `this` keyword inside the `run()` method refers to the **Thread object itself**. 
* In the **Runnable** approach: You must use `Thread.currentThread()`.
* In the **Inheritance** approach: You can simply use `this.getName()` or `this.getPriority()`.

> **Warning:** Even with inheritance, never call `.run()` directly. Always call `.start()`. Calling `.run()` executes the code on the *current* thread (likely `main`), defeating the purpose of multithreading.

---

## 5. Summary Checklist
* [ ] Do I know why `implements Runnable` is generally preferred over `extends Thread`?
* [ ] Can I explain the limitation of Java's single inheritance in this context?
* [ ] Do I understand that `this` in an inherited thread refers to the thread instance?

---