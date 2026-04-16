# CS 4390 - Computer Networks | Spring 2026
# Centralized Math Server

**Group Members:**
- Faris Shaik
- Pritam Hedge
- Aariz Habib

---

## How to Compile and Run

### Compile
```bash
make
```

### Run the server
```bash
make server
```

### Run a client (in a separate terminal)
```bash
make client NAME=Faris
```

### Run a full demo (server + 3 simultaneous clients)
```bash
make demo
```

### Clean compiled files
```bash
make clean
```

---

## Execution Parameters

| Parameter | Value |
|---|---|
| Server host | `127.0.0.1` (localhost) |
| Port | `6789` |
| Client name | Passed as command-line arg: `java MathClient Faris` — auto-generated if omitted |

---

## Protocol Design

All messages are newline-terminated with fields separated by `|`.

| Direction | Message | Description |
|---|---|---|
| Client → Server | `JOIN\|<name>` | Register with the server |
| Server → Client | `ACK\|<name>\|<message>` | Confirm successful connection |
| Client → Server | `MATH\|<name>\|<expression>` | Submit a math expression |
| Server → Client | `RESULT\|<expression>\|<value>` | Return computed result |
| Client → Server | `BYE\|<name>` | Request disconnection |
| Server → Client | `GOODBYE\|<name>\|<message>` | Confirm disconnection |
| Server → Client | `ERROR\|<message>` | Protocol or input error |

---

## Project Report

### a. Names and NetIDs
- Faris Shaik
- Pritam Hedge
- Aariz Habib

### b. Project Partners' Contributions
All three members contributed to the design of the protocol, implementation of the server and client, and testing of the application.

### c. Protocol Design
The protocol uses plain-text, pipe-delimited messages over TCP. Each message has a type prefix (`JOIN`, `ACK`, `MATH`, `RESULT`, `BYE`, `GOODBYE`, `ERROR`) followed by relevant fields, all terminated by a newline. This keeps the protocol human-readable and easy to debug. See the protocol table above for the full specification.

### d. Programming Environment
- Language: Java (standard library only, no external dependencies)
- JDK: Java 11+
- OS: macOS / Linux
- Build system: GNU Make

### e. How to Compile and Execute
See the **How to Compile and Run** section above.

### f. Parameters Needed During Execution
- **IP address:** hardcoded to `127.0.0.1` in `MathClient.java` (change `HOST` constant for remote use)
- **Port:** `6789` (change `PORT` constant in both files if needed)
- **Client name:** passed as first argument to `MathClient` — e.g., `java MathClient Faris`

### g. Comments Throughout Code
Both source files (`MathServer.java`, `MathClient.java`) include a protocol specification header, inline comments explaining the architecture, and Javadoc-style comments on all major methods and classes.

### h. Application Completeness
The application is fully complete. All server and client requirements from the project spec are implemented:
- Multi-client simultaneous connections
- FIFO request ordering across all clients
- Session tracking (name, IP, connect time, duration)
- Expression evaluator supporting `+`, `-`, `*`, `/`, and parentheses with correct operator precedence
- Graceful connect/disconnect lifecycle with logging

### i. Challenges Faced
- Ensuring FIFO ordering across multiple concurrent clients required decoupling request receipt (per-client threads) from request processing (single dedicated thread with a shared `BlockingQueue`).
- Implementing a correct recursive-descent expression evaluator with proper operator precedence without using external libraries required careful right-to-left scanning logic.

### j. What We Learned
- How to use Java TCP sockets (`Socket`, `ServerSocket`) for client-server communication.
- How to design a simple text-based application-layer protocol.
- How to use threads and shared data structures (`ConcurrentHashMap`, `LinkedBlockingQueue`) to safely handle concurrent clients.
- The importance of decoupling I/O threads from processing threads to guarantee request ordering.

### k. Output Screenshots
Run `make demo` to see live output from all three clients and the server simultaneously. Sample output is shown below:

```
[16:12:22] [SERVER] JOIN    | name=Faris   ip=127.0.0.1  at=2026-04-13 16:12:22
[16:12:22] [SERVER] JOIN    | name=Aariz   ip=127.0.0.1  at=2026-04-13 16:12:22
[16:12:22] [SERVER] JOIN    | name=Pritam  ip=127.0.0.1  at=2026-04-13 16:12:22
[16:12:22] [SERVER] Active clients: [Faris, Aariz, Pritam]
[Faris]  Server: Welcome to the Math Server, Faris!
[Aariz]  Server: Welcome to the Math Server, Aariz!
[Pritam] Server: Welcome to the Math Server, Pritam!
[Faris]  Sending: (3 + 4) * 2
[16:12:24] [SERVER] MATH    | from=Faris  expr="(3 + 4) * 2"
[16:12:24] [SERVER] RESULT  | from=Faris  expr="(3 + 4) * 2"  result=14
[Faris]  Result: (3 + 4) * 2 = 14
[Aariz]  Sending: 50 - 10 / 2
[16:12:24] [SERVER] MATH    | from=Aariz  expr="50 - 10 / 2"
[16:12:24] [SERVER] RESULT  | from=Aariz  expr="50 - 10 / 2"  result=45
[Aariz]  Result: 50 - 10 / 2 = 45
[Aariz]  Disconnecting...
[16:12:29] [SERVER] BYE     | name=Aariz   duration=6s
[Faris]  Disconnecting...
[16:12:31] [SERVER] BYE     | name=Faris   duration=8s
[Pritam] Disconnecting...
[16:12:31] [SERVER] BYE     | name=Pritam  duration=8s
```

---

## Assumptions
- Client names are unique per session. If two clients connect with the same name, the second will overwrite the first in the server's client registry.
- Math expressions use standard infix notation with integer or decimal operands and the operators `+`, `-`, `*`, `/`. Parentheses are supported.
- The server and all clients run on the same machine (localhost). To run across machines, update the `HOST` constant in `MathClient.java` to the server's IP address.
- No GUI is required or implemented.
