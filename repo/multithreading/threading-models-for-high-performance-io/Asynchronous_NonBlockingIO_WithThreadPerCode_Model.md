---
title: Asynchronous, Non-Blocking IO - Thread-per-Core
date: 2026-03-27
draft: false 
---

### 1. The "Why"
In the previous "Thread-per-Task" model, if you have 8 CPU cores, but 1,000 threads, the CPU spends most of its time "Context Switching" (saving the state of Thread 1 to load Thread 2). This is inefficient.

**The Thread-per-Core Goal:** Create exactly one thread for every physical CPU core. These threads **never block**. If a thread needs to read from a socket and the data isn't there, it doesn't sleep; it moves to the next socket immediately.

### 2. Comparison: Thread-per-Request vs. Thread-per-Core

| Feature | Thread-per-Request (BIO) | Thread-per-Core (NIO) |
| :--- | :--- | :--- |
| **Thread Count** | High (Hundreds or Thousands). | Low (Matches CPU Cores, e.g., 8). |
| **Idle Time** | Threads sleep while waiting for IO. | Threads never sleep; they "poll" for events. |
| **Context Switching** | Very High (OS overhead). | Extremely Low. |
| **Scalability** | Limited by RAM (Stack size). | Limited by CPU/Network. |
| **Model** | Synchronous/Blocking. | Asynchronous/Event-Driven. |

---

### 3. The "Golden" Snippet: Java NIO Selector
Instead of a `while(true)` loop that blocks on `accept()`, we use a **Selector** that acts as a "multiplexer." One thread monitors thousands of sockets and only reacts when an "Event" (like `OP_READ` or `OP_ACCEPT`) actually happens.



```java
import java.nio.channels.*;
import java.net.InetSocketAddress;
import java.util.Iterator;
import java.util.Set;

public class NonBlockingServer {
    public void start() throws Exception {
        // 1. Open a Selector (The "Event Loop" manager)
        Selector selector = Selector.open();
        ServerSocketChannel serverChannel = ServerSocketChannel.open();
        serverChannel.bind(new InetSocketAddress(8080));
        serverChannel.configureBlocking(false); // CRITICAL: Set to Non-Blocking

        // 2. Register the "Accept" event
        serverChannel.register(selector, SelectionKey.OP_ACCEPT);

        while (true) {
            // 3. This call blocks, but it waits for ANY event on ANY socket
            selector.select(); 

            Set<SelectionKey> selectedKeys = selector.selectedKeys();
            Iterator<SelectionKey> iter = selectedKeys.iterator();

            while (iter.hasNext()) {
                SelectionKey key = iter.next();

                if (key.isAcceptable()) {
                    // Handle new connection (Non-blocking)
                    registerClient(selector, serverChannel);
                } else if (key.isReadable()) {
                    // Read data only when it's actually arrived
                    readData(key);
                }
                iter.remove();
            }
        }
    }
}
```

#### Code Explanation:
1.  **`configureBlocking(false)`**: This tells the OS: "If I try to read and there's no data, return immediately with `0` bytes. Do not put my thread to sleep."
2.  **`Selector.select()`**: This is the "Traffic Cop." Instead of 1,000 threads waiting for 1,000 clients, **one thread** waits for the OS to say, "Hey, Socket #452 has data ready for you."
3.  **The Event Loop**: The thread loops through "Ready" keys, processes the work, and immediately goes back to the selector. Because it never blocks on IO, a single core can handle 10,000+ concurrent connections.

#### Example Output:
```text
[EventLoop-1] 5 sockets ready for reading...
[EventLoop-1] Processing Socket #12... Done.
[EventLoop-1] Processing Socket #88... Done.
[EventLoop-1] Going back to sleep until next event.
```

---

### 4. The Gotchas
* **The "Golden Rule":** You must **NEVER** perform a blocking operation (like `Thread.sleep()` or a standard JDBC call) inside an Event Loop thread. If you block an Event Loop thread, you block **thousands** of users simultaneously.
* **Callback Hell:** Because the model is asynchronous, you can't just write `String data = read()`. You have to provide a "handler" to be called when the data is ready, which makes the code harder to read than standard linear code.
* **CPU Intensive Tasks:** If one request requires a 2-second heavy calculation, it will freeze the Event Loop for everyone else. Heavy CPU tasks should be offloaded to a separate "Worker Pool."

---

**This is the architecture behind Node.js and Netty.**