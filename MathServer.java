/*
 * ============================================================
 * CS 4390 - Computer Networks  |  Spring 2026
 * Centralized Math Server
 *
 * PROTOCOL SPECIFICATION
 * ----------------------
 * All messages are newline-terminated, fields separated by '|'.
 *
 *   Client → Server:
 *     JOIN|<clientName>               — Register with the server
 *     MATH|<clientName>|<expression>  — Submit a math expression
 *     BYE|<clientName>                — Request disconnection
 *
 *   Server → Client:
 *     ACK|<clientName>|<message>      — Confirm successful JOIN
 *     RESULT|<expression>|<result>    — Return computed result
 *     GOODBYE|<clientName>|<message>  — Confirm disconnection
 *     ERROR|<message>                 — Protocol or input error
 *
 * ARCHITECTURE
 * ------------
 *   - Main thread:         accepts incoming TCP connections
 *   - ClientHandler:       one thread per client (reads messages, queues requests)
 *   - RequestProcessor:    single thread that drains the FIFO queue,
 *                          ensuring requests are answered in arrival order
 * ============================================================
 */

import java.io.*;
import java.net.*;
import java.text.*;
import java.util.*;
import java.util.concurrent.*;

public class MathServer {

    static final int PORT = 6789;

    /** All currently connected clients, keyed by client name. */
    static final Map<String, ClientInfo> connectedClients = new ConcurrentHashMap<>();

    /**
     * Central FIFO queue. Every ClientHandler enqueues requests here;
     * the single RequestProcessor thread drains it, guaranteeing
     * that responses are sent in the order requests were received.
     */
    static final BlockingQueue<MathRequest> requestQueue = new LinkedBlockingQueue<>();

    // ------------------------------------------------------------------
    // Entry point
    // ------------------------------------------------------------------

    public static void main(String[] args) throws Exception {
        ServerSocket serverSocket = new ServerSocket(PORT);
        log("Math Server started on port " + PORT);
        log("Waiting for client connections...\n");

        Thread processor = new Thread(new RequestProcessor(), "RequestProcessor");
        processor.setDaemon(true);
        processor.start();

        while (true) {
            Socket clientSocket = serverSocket.accept();
            new Thread(new ClientHandler(clientSocket)).start();
        }
    }

    /** Prints a timestamped server log line to stdout. */
    static void log(String msg) {
        String ts = new SimpleDateFormat("HH:mm:ss").format(new Date());
        System.out.println("[" + ts + "] [SERVER] " + msg);
    }

    // ------------------------------------------------------------------
    // Data classes
    // ------------------------------------------------------------------

    /** Stores metadata for one connected client. */
    static class ClientInfo {
        final String name;
        final String address;
        final long   connectTime;

        ClientInfo(String name, String address) {
            this.name        = name;
            this.address     = address;
            this.connectTime = System.currentTimeMillis();
        }

        String connectTimestamp() {
            return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(connectTime));
        }

        String sessionDuration() {
            long seconds = (System.currentTimeMillis() - connectTime) / 1000;
            return seconds + "s";
        }
    }

    /** Wraps one pending math request along with the output stream to reply on. */
    static class MathRequest {
        final String      clientName;
        final String      expression;
        final PrintWriter out;

        MathRequest(String clientName, String expression, PrintWriter out) {
            this.clientName = clientName;
            this.expression = expression;
            this.out        = out;
        }
    }

    // ------------------------------------------------------------------
    // Request processor — single thread, FIFO order
    // ------------------------------------------------------------------

    static class RequestProcessor implements Runnable {

        @Override
        public void run() {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    MathRequest req = requestQueue.take();
                    String result = evaluate(req.expression);
                    log("RESULT  | from=" + req.clientName
                            + "  expr=\"" + req.expression + "\""
                            + "  result=" + result);
                    req.out.println("RESULT|" + req.expression + "|" + result);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        /** Top-level evaluator: returns a formatted number string or an error string. */
        private String evaluate(String expr) {
            try {
                double result = eval(expr.trim());
                if (Double.isNaN(result))      return "ERROR: Not a number";
                if (Double.isInfinite(result)) return "ERROR: Division by zero";
                // Return whole-number results without a decimal point
                if (result == Math.floor(result)) {
                    return String.valueOf((long) result);
                }
                return String.format("%.4f", result)
                             .replaceAll("0+$", "")
                             .replaceAll("\\.$", "");
            } catch (ArithmeticException e) {
                return "ERROR: " + e.getMessage();
            } catch (Exception e) {
                return "ERROR: Invalid expression";
            }
        }

        /**
         * Recursive descent evaluator with standard operator precedence.
         *
         * Strategy: scan the expression right-to-left at parenthesis depth 0.
         * The rightmost lowest-precedence operator splits the expression into
         * two sub-expressions that are recursed into, naturally producing
         * left-to-right evaluation and correct precedence (+/- < * /).
         *
         * Supported: integer and decimal literals, +  -  *  /  and parentheses.
         * Unary minus is supported on bare numbers (e.g., "-5") but not on
         * sub-expressions (write "0 - x" instead of "- x").
         */
        private double eval(String expr) {
            expr = expr.trim();
            if (expr.isEmpty()) throw new IllegalArgumentException("Empty expression");

            // Strip enclosing parentheses, e.g. "(3 + 4)" → "3 + 4"
            if (expr.charAt(0) == '(' &&
                    matchingParen(expr, 0) == expr.length() - 1) {
                return eval(expr.substring(1, expr.length() - 1));
            }

            // Scan right-to-left for + or - at depth 0 (lowest precedence)
            int depth = 0;
            for (int i = expr.length() - 1; i > 0; i--) {
                char c = expr.charAt(i);
                if      (c == ')') depth++;
                else if (c == '(') depth--;
                else if (depth == 0 && (c == '+' || c == '-')) {
                    double left  = eval(expr.substring(0, i));
                    double right = eval(expr.substring(i + 1));
                    return c == '+' ? left + right : left - right;
                }
            }

            // Scan right-to-left for * or / at depth 0
            depth = 0;
            for (int i = expr.length() - 1; i > 0; i--) {
                char c = expr.charAt(i);
                if      (c == ')') depth++;
                else if (c == '(') depth--;
                else if (depth == 0 && (c == '*' || c == '/')) {
                    double left  = eval(expr.substring(0, i));
                    double right = eval(expr.substring(i + 1));
                    if (c == '/') {
                        if (right == 0) throw new ArithmeticException("Division by zero");
                        return left / right;
                    }
                    return left * right;
                }
            }

            // Base case: parse as a number (handles negatives like "-5")
            return Double.parseDouble(expr);
        }

        /** Returns the index of the ')' that matches the '(' at startIndex. */
        private int matchingParen(String expr, int startIndex) {
            int depth = 0;
            for (int i = startIndex; i < expr.length(); i++) {
                if      (expr.charAt(i) == '(') depth++;
                else if (expr.charAt(i) == ')') {
                    if (--depth == 0) return i;
                }
            }
            return -1;
        }
    }

    // ------------------------------------------------------------------
    // Client handler — one thread per connected client
    // ------------------------------------------------------------------

    static class ClientHandler implements Runnable {

        private final Socket socket;
        private PrintWriter    out;
        private BufferedReader in;
        private String         clientName = null;

        ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                out = new PrintWriter(socket.getOutputStream(), true);
                in  = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                // ---- Handshake: first message must be JOIN ----
                String msg = in.readLine();
                if (msg == null || !msg.startsWith("JOIN|")) {
                    out.println("ERROR|First message must be JOIN|<name>");
                    return;
                }

                clientName = msg.substring(5).trim();
                if (clientName.isEmpty()) {
                    out.println("ERROR|Client name cannot be empty");
                    return;
                }

                String addr = socket.getInetAddress().getHostAddress();
                ClientInfo info = new ClientInfo(clientName, addr);
                connectedClients.put(clientName, info);

                log("JOIN    | name=" + clientName
                        + "  ip=" + addr
                        + "  at=" + info.connectTimestamp());
                log("Active clients: " + connectedClients.keySet());

                out.println("ACK|" + clientName + "|Welcome to the Math Server, " + clientName + "!");

                // ---- Main message loop ----
                while ((msg = in.readLine()) != null) {

                    if (msg.startsWith("MATH|")) {
                        String[] parts = msg.split("\\|", 3);
                        if (parts.length < 3 || parts[2].trim().isEmpty()) {
                            out.println("ERROR|Invalid MATH format — use: MATH|<name>|<expression>");
                            continue;
                        }
                        String expression = parts[2].trim();
                        log("MATH    | from=" + clientName + "  expr=\"" + expression + "\"");
                        requestQueue.put(new MathRequest(clientName, expression, out));

                    } else if (msg.startsWith("BYE|")) {
                        handleDisconnect();
                        break;

                    } else {
                        out.println("ERROR|Unknown message type — valid types: MATH, BYE");
                    }
                }

            } catch (IOException e) {
                log("Connection lost: " + (clientName != null ? clientName : socket.getInetAddress()));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                // Clean up if client disconnected without sending BYE
                if (clientName != null && connectedClients.containsKey(clientName)) {
                    handleDisconnect();
                }
                try { socket.close(); } catch (IOException ignored) {}
            }
        }

        /** Removes the client from the registry and sends a GOODBYE message. */
        private void handleDisconnect() {
            ClientInfo info     = connectedClients.remove(clientName);
            String     duration = info != null ? info.sessionDuration() : "unknown";
            log("BYE     | name=" + clientName + "  duration=" + duration);
            log("Active clients: " + connectedClients.keySet());
            out.println("GOODBYE|" + clientName
                    + "|Goodbye, " + clientName + "! Session duration: " + duration);
        }
    }
}
