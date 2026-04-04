---
title: Parallel Streams & ForkJoinPool
date: 2026-03-27
draft: false 
---


This section transitions from **IO-bound** concurrency (waiting for databases or networks) back to **CPU-bound** efficiency. If you have a massive dataset—like 10 million rows of sales data—and you need to calculate the total tax, you don't want to use one CPU core while the other 15 sit idle.


### 1. The "Why"
Modern CPUs are "Multi-core." A standard `for` loop or a sequential `Stream` in Java only uses **one** thread. To use all your hardware's power, you need to split the data into chunks, process them simultaneously, and then merge the results.
* **Parallel Streams:** The high-level, easy way to do this.
* **ForkJoinPool:** The low-level "engine" that makes Parallel Streams work.

### 2. The Core Concept: Divide and Conquer
The `ForkJoinPool` uses a strategy called **Work-Stealing**. 
1.  **Fork:** A large task is split into smaller sub-tasks until they are small enough to handle.
2.  **Join:** The results of the sub-tasks are combined back together.
3.  **Work-Stealing:** If Worker Thread A finishes its pile of work early, it "steals" a task from the bottom of Worker Thread B's pile to keep the CPU busy.



---

### 3. The "Golden" Snippet: Parallel Stream vs. Sequential
Notice how little code changes to get a massive performance boost on large datasets.

```java
import java.util.List;
import java.util.stream.LongStream;

public class ParallelAnalysis {
    public static void main(String[] args) {
        long[] numbers = LongStream.rangeClosed(1, 10_000_000).toArray();

        // 1. SEQUENTIAL (Uses 1 Core)
        long start = System.currentTimeMillis();
        long sum1 = LongStream.of(numbers).sum();
        System.out.println("Sequential Time: " + (System.currentTimeMillis() - start) + "ms");

        // 2. PARALLEL (Uses ALL Cores)
        start = System.currentTimeMillis();
        long sum2 = LongStream.of(numbers).parallel().sum();
        System.out.println("Parallel Time: " + (System.currentTimeMillis() - start) + "ms");
    }
}
```

#### Code Explanation:
* **`.parallel()`**: This tells Java to use the common `ForkJoinPool`.
* **Splitting:** The `LongStream` is divided into chunks. Thread 1 sums 1–2.5M, Thread 2 sums 2.5M–5M, and so on.
* **Merging:** Once all threads finish, their partial sums are added together to produce the final result.

---

### 4. The "Common" ForkJoinPool
By default, all Parallel Streams share a single pool: `ForkJoinPool.commonPool()`.
* **Size:** This pool is automatically sized to $Number of Cores - 1$.
* **The Danger:** Since the pool is **global**, if you run a blocking IO operation (like a slow API call) inside a Parallel Stream, you effectively "clog" the pool for every other part of your application.

---

### 5. When NOT to use Parallel Streams
Parallelism has "overhead" (splitting the data and merging it takes time). 
* **Rule of NQ:** A rough formula for deciding if parallelism is worth it is $N \times Q > 10,000$.
    * **N**: Number of data elements.
    * **Q**: Amount of work per element.
* **Small Data:** If you only have 100 items, the cost of starting threads is higher than the time saved.
* **Linked Data:** `ArrayList` is great for parallel streams because it's easy to split. `LinkedList` is terrible because you have to traverse it from the start to find the middle.

---

### 6. Summary Comparison

| Feature | Sequential Stream | Parallel Stream |
| :--- | :--- | :--- |
| **Execution** | One thread. | Multiple threads (ForkJoinPool). |
| **Order** | Guaranteed. | Not guaranteed (unless using `.forEachOrdered`). |
| **State** | Can be stateful. | **Must be stateless** (Avoid shared variables!). |
| **Best For** | Small data / IO tasks. | Massive data / CPU-heavy math. |
