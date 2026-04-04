---
title: Thread Per Task / Thread Per Request Model
date: 2026-03-27
draft: false 
---

### 1. The "Why"
In a standard web application, a "task" is usually a single HTTP request.
* **The Goal:** Isolate users from each other. If User A’s request takes 10 seconds to process a heavy database query, User B should still get their response in 100ms on a different thread.
* **The Implementation:** The server maintains a "Thread Pool." When a request arrives, the "Boss" thread grabs a "Worker" thread from the pool, hands it the socket, and says, "Call me when you're done."

### 2. Comparison: One Thread vs. Thread Pool (Thread-Per-Task)

| Feature | Single Threaded | Thread-Per-Task (Pool) |
| :--- | :--- | :--- |
| **Concurrency** | None (One at a time). | High (N tasks at a time). |
| **Isolation** | One crash kills the server. | One crash only kills that thread. |
| **Throughput** | Very Low. | High (Parallel processing). |
| **Blocking** | One slow DB call stops everyone. | Only that specific worker thread is blocked. |

---

### 3. The "Golden" Snippet: Executor-Based Web Server
Instead of creating a `new Thread()` every time (which is slow), we use a `FixedThreadPool` to reuse existing threads.



```java
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ThreadPerRequestServer {
    // 1. Create a pool of 100 workers. 
    // This limits our memory usage to ~100MB of Stack.
    private final ExecutorService threadPool = Executors.newFixedThreadPool(100);

    public void start() throws IOException {
        ServerSocket serverSocket = new ServerSocket(8080);
        
        while (true) {
            Socket clientSocket = serverSocket.accept(); // Main thread blocks here
            
            // 2. Hand the task to the pool. The main thread immediately 
            // goes back to .accept() to wait for the next user.
            threadPool.submit(() -> handleRequest(clientSocket));
        }
    }

    private void handleRequest(Socket socket) {
        try (socket) {
            // Simulate heavy DB work
            Thread.sleep(2000); 
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            out.println("HTTP/1.1 200 OK\r\n\r\nHello from Thread: " + Thread.currentThread().getName());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```

#### Code Explanation:
1.  **`newFixedThreadPool(100)`**: This is our "Safety Valve." It prevents the server from trying to create 10,000 threads and crashing the OS with an `OutOfMemoryError`. If more than 100 users connect, the 101st user waits in a queue.
2.  **`threadPool.submit()`**: This is non-blocking for the *Main* thread. It puts the task in a queue and returns immediately.
3.  **Isolation**: If `handleRequest` throws a `RuntimeException`, that thread dies and is replaced by the pool, but the `ServerSocket` loop keeps running.

#### Example Output:
```text
[Main] Waiting for connection...
[Main] Accepted User 1. Handed to pool-thread-1.
[Main] Accepted User 2. Handed to pool-thread-2.
[pool-thread-1] Processing DB query...
[pool-thread-2] Processing DB query...
```

---

### 4. The Gotchas
* **Thread Starvation:** If your pool size is 100, and 100 users are performing a "Long Poll" (waiting for data that isn't there), the 101st user is blocked even if their request would only take 1ms to complete.
* **Context Switching:** If you set your pool size too high (e.g., 5,000 threads on an 8-core CPU), the CPU spends more time switching between threads than actually running your code.
* **The Memory Wall:** Every thread has its own stack memory. In modern cloud environments (Docker/Kubernetes), memory is expensive. Thread-per-request is "heavy" compared to the newer "Virtual Threads" or "NIO" models.
