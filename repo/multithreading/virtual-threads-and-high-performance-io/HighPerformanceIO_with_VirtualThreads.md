---
title: High-Performance IO with Virtual Threads
date: 2026-03-27
draft: false 
---

This section is the culmination of everything we've learned about IO. It explains how **Virtual Threads** allow us to write code that *looks* like the old "Thread-per-Request" model but *performs* like the "Asynchronous/NIO" model. 



### 1. The "Why"
In the previous NIO (Non-Blocking) models, we had to break our logic into "callbacks" or "promises." This made error handling and stack traces a nightmare. 
Virtual Threads change the game:
* **The Magic:** When a Virtual Thread performs a blocking IO operation (like `socket.read()` or `jdbc.executeQuery()`), the underlying JVM **unmounts** the virtual thread from the physical OS thread (Carrier Thread). 
* **The Result:** The physical OS thread is now free to run *another* Virtual Thread while the first one waits for the network. No OS-level context switching occurs.

### 2. Comparison: NIO (Selectors) vs. Virtual Threads

| Feature | NIO / Netty / Event Loop | Virtual Threads (Loom) |
| :--- | :--- | :--- |
| **Code Style** | Asynchronous / Reactive. | **Synchronous / Procedural.** |
| **Stack Traces** | Often fragmented and useless. | **Complete and readable.** |
| **Debugging** | Difficult (Step-through is hard). | **Easy** (Standard debugger works). |
| **Scalability** | High (Millions of connections). | **High** (Millions of connections). |
| **Resource Usage** | Low. | **Low.** |

---

### 3. The "Golden" Snippet: The "Simple" High-Scale Server
This code looks exactly like a basic blocking server from 20 years ago, but it can handle 1,000,000 concurrent connections on a standard server.



```java
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Executors;

public class HighPerformanceServer {

    public void start(int port) throws IOException {
        try (var serverSocket = new ServerSocket(port)) {
            // 1. No fixed pool! We create a NEW virtual thread for every user.
            var executor = Executors.newVirtualThreadPerTaskExecutor();

            while (true) {
                Socket clientSocket = serverSocket.accept();
                
                // 2. Submit task. This is incredibly cheap.
                executor.submit(() -> handleClient(clientSocket));
            }
        }
    }

    private void handleClient(Socket socket) {
        try (socket; 
             var in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             var out = new PrintWriter(socket.getOutputStream(), true)) {
            
            String line;
            while ((line = in.readLine()) != null) {
                // 3. This LOOKS blocking, but it's actually non-blocking
                // at the OS level thanks to the JVM magic.
                out.println("Echo: " + line);
            }
        } catch (IOException e) {
            // Standard try-catch works perfectly here
        }
    }
}
```

#### Code Explanation:
1.  **`newVirtualThreadPerTaskExecutor()`**: We don't limit ourselves to 100 or 1,000 threads. If 50,000 users connect, we have 50,000 virtual threads.
2.  **`in.readLine()`**: In the old days, this would freeze an OS thread. Now, the JVM sees this call, "parks" the virtual thread, and lets the physical CPU core work on someone else's request until the bytes arrive.
3.  **No More Callbacks**: Notice there are no `CompletableFuture`, `.thenAccept()`, or `flux.subscribe()`. It's just a `while` loop.

#### Example Output:
```text
Server started.
[OS-Thread-1] Handling User 1 (VirtualThread-101)
[OS-Thread-1] User 1 is waiting for IO... (Unmounting VirtualThread-101)
[OS-Thread-1] Now handling User 2 (VirtualThread-102) 
... (Repeat for millions of users)
```

---

### 4. The Gotchas (The "Pinning" Problem)
While Virtual Threads are powerful, there is one major way to break them: **Pinning**.
* **`synchronized` blocks:** If a Virtual Thread enters a `synchronized` block and then tries to do IO, it becomes "pinned" to the OS thread. The OS thread cannot be released, and scalability drops back down to old-school levels.
* **The Fix:** Replace `synchronized` with `ReentrantLock`. `ReentrantLock` is "Loom-friendly" and allows the virtual thread to unmount properly.
* **Native Code (JNI):** Calling C or C++ code that blocks will also pin the thread.

---

### 5. Final Performance Tip: Use `ThreadLocal` Judiciously
In the old model, `ThreadLocal` was used to cache heavy objects (like Database Connections). In the Virtual Thread world, if you have 1,000,000 threads, you might accidentally create 1,000,000 copies of those heavy objects, leading to an `OutOfMemoryError`. 
* **Solution:** Use **Scoped Values** (another new Java feature) instead of `ThreadLocal` when dealing with millions of threads.
