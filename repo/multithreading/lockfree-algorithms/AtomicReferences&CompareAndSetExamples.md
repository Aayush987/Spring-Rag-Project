---
title: 1. AtomicReference -  Object Swapping
date: 2026-03-27
draft: false 
---

**The Concept:** While `AtomicInteger` works for primitives, `AtomicReference<V>` allows you to update a reference to an entire object as a single atomic unit. This is the foundation of "Immutable State" updates.

```java
import java.util.concurrent.atomic.AtomicReference;

public class ConfigManager {
    // A shared configuration object that many threads read
    private final AtomicReference<Config> currentConfig = new AtomicReference<>(new Config("v1"));

    public void updateConfig(String newVersion) {
        Config oldConfig;
        Config newConfig = new Config(newVersion);

        do {
            oldConfig = currentConfig.get(); // Snapshot of the pointer
            // We don't modify oldConfig; we create a whole new one!
        } while (!currentConfig.compareAndSet(oldConfig, newConfig));
        
        System.out.println("Config updated to: " + newVersion);
    }
}

class Config {
    final String version;
    Config(String v) { this.version = v; }
}
```

**Explanation:**
* **The Pointer Swap:** We aren't locking the `Config` object. We are simply saying: "If the global pointer still points to my `oldConfig`, swing the pointer to `newConfig`."
* **Immutability:** This pattern works best with immutable objects. Since the object itself never changes, readers don't need locks.

**Example Output:**
```text
Thread-1: Attempting update to v2... Success!
Thread-2: Attempting update to v3... (Failed once, retried)... Success!
```

---

# 2. Compare-And-Swap (CAS): The Heart of Lock-Free
**The Concept:** CAS is the "optimistic" alternative to locking. Instead of assuming someone will interfere (pessimism), we assume they won't, but we check right at the last millisecond before saving.



```java
// Logic inside every Atomic class:
public final boolean compareAndSet(V expectedValue, V newValue) {
    // This calls a native "Unsafe" method that talks directly to the CPU
    return U.compareAndSetReference(this, VALUE, expectedValue, newValue);
}
```

**Explanation:**
* **Hardware Level:** Modern CPUs have a specific instruction (like `CMPXCHG` on x86) that performs the "Compare" and the "Set" as a single hardware cycle. 
* **Non-Blocking:** No thread is ever "put to sleep" by the OS. They just "spin" (loop) in user-space, which is significantly faster than a context switch.

---

# 3. The ABA Problem
**The Concept:** This is the most famous bug in lock-free programming. If Thread 1 reads value **A**, then Thread 2 changes it to **B** and then back to **A**, Thread 1's CAS will succeed because it sees **A**. However, the *state* of the system has changed in between.



```java
import java.util.concurrent.atomic.AtomicStampedReference;

public class ABASolution {
    // We add a "Stamp" (version number) to the reference
    static AtomicStampedReference<String> asr = new AtomicStampedReference<>("A", 0);

    public static void main(String[] args) {
        int initialStamp = asr.getStamp(); // Version 0
        String initialRef = asr.getReference(); // "A"

        // Thread 2 comes in and does A -> B -> A
        asr.compareAndSet("A", "B", 0, 1); // Stamp is now 1
        asr.compareAndSet("B", "A", 1, 2); // Stamp is now 2

        // Thread 1 tries to update, thinking it's still version 0
        boolean result = asr.compareAndSet("A", "C", initialStamp, initialStamp + 1);

        System.out.println("Update successful? " + result); // Returns FALSE
    }
}
```

**Explanation:**
* **The Version Number:** By using `AtomicStampedReference`, we track both the **Value** and the **Stamp**.
* **The Fix:** Even though the value is back to "A", the stamp is now `2`. Thread 1's attempt to update using stamp `0` fails, effectively preventing the ABA bug.

**Example Output:**
```text
Update successful? false
(Reason: Value matched 'A', but version was 2 instead of 0)
```

---

### Summary Table: Lock-Free Techniques

| Technique | Goal | Tool |
| :--- | :--- | :--- |
| **Atomic Reference** | Update complex objects without locks. | `AtomicReference<T>` |
| **CAS Loop** | Ensure a change is valid before applying. | `do { ... } while (!cas)` |
| **ABA Protection** | Detect if a value was changed and reverted. | `AtomicStampedReference` |
| **Lock-Free Stack** | High-concurrency LIFO structure. | Linked Nodes + AtomicReference |