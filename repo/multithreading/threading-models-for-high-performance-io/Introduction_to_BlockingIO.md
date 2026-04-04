---
title: Introduction to Blocking IO
date: 2026-03-27
draft: false 
---

### 1. The "Why"
**Blocking IO** is the "classic" way of handling data. When a thread asks the Operating System (OS) for data (like reading a 1GB file or waiting for a network packet), the OS puts that thread to sleep. The thread cannot do any other work until the data arrives.

* **The Problem:** If you have 1,000 users and each requires a blocking thread, you need 1,000 threads. Threads are expensive—they consume memory (Stack) and cause "Context Switching" overhead.

### 2. Comparison: Blocking IO vs. Non-Blocking IO (NIO)

| Feature | Blocking IO (BIO) | Non-Blocking IO (NIO) |
| :--- | :--- | :--- |
| **Thread Behavior** | Thread "stops" and waits for data. | Thread "asks" and moves on if data isn't ready. |
| **Efficiency** | High for single, long-lived connections. | High for thousands of concurrent connections. |
| **Complexity** | Simple (Sequential code). | High (Requires Event Loops/Callbacks). |
| **Scalability** | Limited by Thread Count / Memory. | Limited by CPU / Network Bandwidth. |

---

### 3. The "Golden" Snippet: The Standard Blocking Server
This is a classic "One Thread Per Connection" model. It works fine for a few users, but it scales poorly.



```java
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class BlockingServer {
    public void startServer() throws IOException {
        ServerSocket serverSocket = new ServerSocket(8080);
        System.out.println("Server listening on port 8080...");

        while (true) {
            // 1. BLOCKING CALL: The main thread stops here until a client connects
            Socket clientSocket = serverSocket.accept(); 
            
            // 2. We must spawn a new thread, or the server can't accept User B
            new Thread(() -> handleRequest(clientSocket)).start();
        }
    }

    private void handleRequest(Socket socket) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            // 3. BLOCKING CALL: The thread stops here until the client sends text
            String request = in.readLine(); 
            System.out.println("Received: " + request);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
```

#### Code Explanation:
1.  **`accept()`**: This is the first roadblock. The thread literally does nothing until a user connects.
2.  **`new Thread()`**: Because `handleRequest` also blocks, we *must* create a new thread for every single user. If 5,000 users connect, we create 5,000 threads.
3.  **`readLine()`**: If a user connects but doesn't send data for 10 seconds, that thread sits idle, holding onto ~1MB of Stack memory and doing nothing.

#### Example Output:
```text
Server listening on port 8080...
[Thread-1] Client connected. Waiting for data...
[Thread-2] Client connected. Waiting for data...
[Thread-1] Received: Hello Server!
[Thread-1] Finished. Thread terminated.
```

---

### 4. The Gotchas
* **The C10K Problem:** Can one server handle 10,000 concurrent connections? With Blocking IO, you'd need 10,000 threads. Most OS kernels will struggle with the "Context Switching" (swapping between 10,000 threads) more than the actual data processing.
* **Resource Exhaustion:** Each thread in Java typically takes **512KB to 1MB** of RAM for its Stack. 1,000 idle threads = 1GB of RAM wasted just "waiting."
* **Simplicity vs. Scale:** Blocking IO is much easier to debug because the code is linear. You read line 1, then line 2. In Non-Blocking IO, line 2 might execute before line 1 is "ready."