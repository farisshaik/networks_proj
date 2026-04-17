/*
  ============================================================
  CS 4390 - Computer Networks  |  Spring 2026
  Math Server Client
 
  Usage:
    java MathClient <YourName>
    java MathClient          (randomly generates a name like "Client427")
 
  Behavior:
    1. Connects to the server and sends JOIN|<name>
    2. Waits for ACK from server
    3. Spawns a listener thread to print server responses as they arrive
    4. Sends 3–5 randomly chosen math expressions at random intervals
       (500 ms – 2 000 ms between each) to simulate real-world timing
    5. Sends BYE|<name> and closes the connection
 
  Protocol (same as MathServer.java — reproduced here for reference):
    Client → Server:  JOIN|<name>  /  MATH|<name>|<expression>  /  BYE|<name>
    Server → Client:  ACK|<name>|<msg>  /  RESULT|<expr>|<val>  /
                      GOODBYE|<name>|<msg>  /  ERROR|<msg>
  ============================================================
 */

import java.io.*;
import java.net.*;
import java.util.*;

public class MathClient {

    static final String HOST = "127.0.0.1";
    static final int    PORT = 6789;

    /**
      Expressions available to each client instance
      The client shuffles this list and picks 3–5 at random
     */
    static final String[] EXPRESSION_POOL = {
        "10 + 5",
        "20 - 8",
        "6 * 7",
        "100 / 4",
        "3 + 4 * 2",
        "50 - 10 / 2",
        "8 * 8 - 4",
        "(3 + 4) * 2",
        "15 + 3 * 4 - 2",
        "72 / 8 + 3",
        "100 - 25 * 3",
        "(10 + 5) * (3 - 1)"
    };

    public static void main(String[] args) throws Exception {
        // Client name comes from the command line; fall back to a random ID
        String clientName = (args.length > 0)
                ? args[0]
                : "Client" + (new Random().nextInt(900) + 100);

        System.out.println("[" + clientName + "] Connecting to " + HOST + ":" + PORT + "...");

        try (Socket socket = new Socket(HOST, PORT)) {

            PrintWriter    out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in  = new BufferedReader(
                                     new InputStreamReader(socket.getInputStream()));

            //  Step 1: Send JOIN 
            out.println("JOIN|" + clientName);

            //  Step 2: Wait for ACK 
            String ack = in.readLine();
            if (ack == null || !ack.startsWith("ACK|")) {
                System.out.println("[" + clientName + "] Connection refused: " + ack);
                return;
            }
            String[] ackParts = ack.split("\\|", 3);
            System.out.println("[" + clientName + "] Server: " + ackParts[2]);

            //  Step 3: Listener thread — prints all server responses 
            Thread listener = new Thread(() -> {
                try {
                    String response;
                    while ((response = in.readLine()) != null) {
                        String[] parts = response.split("\\|", 3);
                        switch (parts[0]) {
                            case "RESULT":
                                System.out.println("[" + clientName + "] Result: "
                                        + parts[1] + " = " + parts[2]);
                                break;
                            case "GOODBYE":
                                System.out.println("[" + clientName + "] Server: " + parts[2]);
                                break;
                            case "ERROR":
                                System.out.println("[" + clientName + "] Error from server: "
                                        + parts[1]);
                                break;
                            default:
                                System.out.println("[" + clientName + "] Server: " + response);
                        }
                    }
                } catch (IOException ignored) {
                    // Socket closed — normal shutdown path
                }
            }, "Listener-" + clientName);
            listener.setDaemon(true);
            listener.start();

            //  Step 4: Send 3–5 randomly chosen math expressions 
            Random rand = new Random();
            List<String> pool = new ArrayList<>(Arrays.asList(EXPRESSION_POOL));
            Collections.shuffle(pool, rand);

            int numRequests = 3 + rand.nextInt(3); // randomly 3, 4, or 5
            System.out.println("[" + clientName + "] Will send " + numRequests + " request(s).");

            for (int i = 0; i < numRequests; i++) {
                // Random delay simulates a real user typing at different speeds
                long delayMs = 500 + rand.nextInt(1500);
                Thread.sleep(delayMs);

                String expr = pool.get(i);
                System.out.println("[" + clientName + "] Sending: " + expr);
                out.println("MATH|" + clientName + "|" + expr);
            }

            // Allow time for the last result to arrive before disconnecting
            Thread.sleep(2000);

            //  Step 5: Send BYE and close 
            System.out.println("[" + clientName + "] Disconnecting...");
            out.println("BYE|" + clientName);
            Thread.sleep(500); // short wait so the listener can print GOODBYE
        }

        System.out.println("[" + clientName + "] Connection closed.");
    }
}
