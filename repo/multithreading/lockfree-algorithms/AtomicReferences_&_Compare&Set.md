---
title: Atomic References & Lock-Free Data Structures
date: 2026-03-27
draft: false 
---

### 1. The "Why"
If you want to update a complex object (like a `User` profile or a `Node` in a list) across multiple threads, you usually have to lock the entire object. 
**AtomicReference** allows you to swap an entire object reference atomically. It says: "Only change the pointer from Object A to Object B if the pointer is still pointing at Object A."

### 2. Comparison: Synchronized Object vs. AtomicReference

| Feature | `synchronized (obj)` | `AtomicReference<T>` |
| :--- | :--- | :--- |
| **Safety** | Prevents multiple threads from entering. | Prevents "stale" updates via CAS. |
| **Blocking** | Yes (Threads sleep). | No (Threads "spin" and retry). |
| **Granularity** | Locks the whole block of code. | Only protects the *reference* (the pointer). |
| **Performance** | High overhead for small updates. | Extremely fast for "pointer swapping." |

---

### 3. The "Golden" Snippet: A Lock-Free Stack
A standard `Stack` uses `synchronized` on `push` and `pop`. In this high-performance version, we use `AtomicReference` to manage the "Head" of the stack without any locks.



```java
import java.util.concurrent.atomic.AtomicReference;

public class LockFreeStack<T> {
    private static class Node<T> {
        T value;
        Node<T> next;
        Node(T value) { this.value = value; }
    }

    // AtomicReference holds the current "Top" of the stack
    private AtomicReference<Node<T>> head = new AtomicReference<>();

    public void push(T value) {
        Node<T> newNode = new Node<>(value);
        Node<T> currentHead;

        do {
            currentHead = head.get();     // 1. Get current top
            newNode.next = currentHead;   // 2. Link new node to current top
            
            // 3. TRY to set new node as the head.
            // If another thread pushed/popped in the meantime, CAS fails.
        } while (!head.compareAndSet(currentHead, newNode));
    }

    public T pop() {
        Node<T> currentHead;
        Node<T> nextNode;

        do {
            currentHead = head.get();
            if (currentHead == null) return null; // Stack is empty
            
            nextNode = currentHead.next;
            
            // TRY to move the head to the next node
        } while (!head.compareAndSet(currentHead, nextNode));

        return currentHead.value;
    }
}
```

#### Code Explanation:
1.  **The Reference**: The `head` is an `AtomicReference` to a `Node`. It represents the "entrance" to our stack.
2.  **The Optimistic Push**: We *assume* the head won't change while we are preparing our new node. We point our `newNode.next` to what we *think* is the current head.
3.  **The CAS Check**: `head.compareAndSet(currentHead, newNode)` checks: "Is the head still the same one I saw in step 1?" 
    * If **Yes**: The pointer is swapped to our new node instantly.
    * If **No**: Someone else pushed a node! Our `newNode.next` is now pointing to a "stale" head. We loop back, get the *new* head, and try again.

#### Example Output:
```text
Thread-1: Starting Push("A")
Thread-2: Starting Push("B")
Thread-2: Successfully set Head to "B".
Thread-1: CAS Failed (Head is now B, not null). 
Thread-1: Retrying... Successfully set Head to "A" (pointing to B).
Final Stack: [A] -> [B] -> null
```

---

### 4. The Gotchas
* **The ABA Problem:** This is the biggest risk with `AtomicReference`. If Thread A sees value `V1`, then Thread B changes it to `V2` and back to `V1`, Thread A’s CAS will succeed even though the state changed. 
    * **The Fix:** Use `AtomicStampedReference`, which adds a "version number" or "stamp" to the reference.
* **Memory Pressure:** Lock-free structures often create many short-lived objects (like `Node` objects) because every failed CAS might require a new object. This puts more work on the Garbage Collector.
* **Side Effects:** Never perform a "side effect" (like printing to a console or writing to a file) inside the `do-while` loop of a CAS operation. Since the loop can run many times, the side effect will also happen many times!