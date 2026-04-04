---
title: Recursive Task
date: 2026-03-27
draft: false 
---

While **Parallel Streams** are the "automatic transmission" of the ForkJoinPool, **`RecursiveTask`** is the "manual transmission." It gives you full control over how a large problem is sliced into smaller pieces and how those pieces are joined back together.

It is the core class you extend to implement the **Divide and Conquer** strategy for tasks that return a result.

---

### 1. The "Why"
Sometimes your data doesn't fit into a simple `Stream`. For example, if you are:
* Processing a complex file directory tree.
* Running a recursive mathematical algorithm (like Fibonacci or Merge Sort).
* Analyzing a custom data structure that isn't a standard Collection.

In these cases, you need a way to say: *"If this task is too big, split it. If it's small enough, just do it."*

---

### 2. The Mechanics: The "Compute" Method
When you extend `RecursiveTask<V>`, you must implement the `compute()` method. This method follows a standard template:



```java
if (task is small enough) {
    return solve it sequentially;
} else {
    Split task into Task A and Task B;
    Task A.fork();             // Push Task A to the pool (asynchronous)
    return Task B.compute()    // Do Task B in the current thread
           + Task A.join();    // Wait for Task A and combine results
}
```

---

### 3. The "Golden" Snippet: Summing a Massive Array
Let's look at how we manually implement a parallel sum using a `RecursiveTask`.

```java
import java.util.concurrent.RecursiveTask;
import java.util.concurrent.ForkJoinPool;

public class ArraySumTask extends RecursiveTask<Long> {
    private final long[] array;
    private final int start, end;
    private static final int THRESHOLD = 10_000; // Small enough to do sequentially

    public ArraySumTask(long[] array, int start, int end) {
        this.array = array;
        this.start = start;
        this.end = end;
    }

    @Override
    protected Long compute() {
        // 1. BASE CASE: Is the work small enough?
        if (end - start <= THRESHOLD) {
            long sum = 0;
            for (int i = start; i < end; i++) {
                sum += array[i];
            }
            return sum;
        } else {
            // 2. RECURSIVE STEP: Split the work in half
            int mid = start + (end - start) / 2;
            ArraySumTask leftTask = new ArraySumTask(array, start, mid);
            ArraySumTask rightTask = new ArraySumTask(array, mid, end);

            // 3. FORK: Send the left task to another thread
            leftTask.fork();

            // 4. COMPUTE & JOIN: Do the right half and wait for the left half
            long rightResult = rightTask.compute(); 
            long leftResult = leftTask.join(); 

            return leftResult + rightResult;
        }
    }

    public static void main(String[] args) {
        long[] data = new long[100_000];
        // ... fill data ...
        ForkJoinPool pool = new ForkJoinPool();
        long total = pool.invoke(new ArraySumTask(data, 0, data.length));
        System.out.println("Total Sum: " + total);
    }
}
```

#### Code Explanation:
* **`THRESHOLD`**: This is the most important variable. If it's too high, you don't use your CPU cores. If it's too low (e.g., 1), the overhead of creating Task objects will make your program much slower than a simple loop.
* **`fork()`**: This puts the task into the **Work-Stealing** queue. It doesn't necessarily start a new thread immediately; it just makes the work "available" for any idle thread in the pool to "steal."
* **`join()`**: This is a "blocking" call, but it's a "smart block." While waiting for the result, the current thread can go off and help with other tasks in the pool.

---

### 4. The Gotchas
* **The "Lopsided" Split:** Always try to split your data as evenly as possible (usually 50/50). If you split it 1/99, one thread will do all the work while others stay idle.
* **`RecursiveAction` vs `RecursiveTask`**: If your task doesn't return a result (like a parallel "Sort" that modifies an array in place), use `RecursiveAction` instead.
* **The `join()` Order:** Always call `fork()` then `compute()` then `join()`. If you call `fork()` and then immediately `join()`, you've just made your program sequential because the thread stops and waits instead of doing the second half of the work!
