/*
 * ============================================================
 * Math Server Client
 *
 * Usage:
 *   java MathClient <YourName>
 *   java MathClient          (auto-generates a name like "Client427")
 *
 * Behavior:
 *   1. Connects to the server and sends JOIN|<name>
 *   2. Waits for ACK from server
 *   3. Prompts the user to enter math expressions one at a time
 *   4. Sends each expression, waits for the result, then prompts again
 *   5. Sends BYE|<name> and closes the connection when user types 'done'
 *
 * Protocol (same as MathServer.java — reproduced here for reference):
 *   Client → Server:  JOIN|<name>  /  MATH|<name>|<expression>  /  BYE|<name>
 *   Server → Client:  ACK|<name>|<msg>  /  RESULT|<expr>|<val>  /
 *                     GOODBYE|<name>|<msg>  /  ERROR|<msg>
 * ============================================================
 */

import java.io.*;
import java.net.*;
import java.util.*;

public class MathClient {

    static final String HOST = "127.0.0.1";
    static final int    PORT = 6789;

    public static void main(String[] args) throws Exception {
        String clientName = (args.length > 0)
                ? args[0]
                : "Client" + (new Random().nextInt(900) + 100);

        System.out.println("[" + clientName + "] Connecting to " + HOST + ":" + PORT + "...");

        try (Socket socket = new Socket(HOST, PORT)) {

            PrintWriter    out    = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in     = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            BufferedReader userIn = new BufferedReader(new InputStreamReader(System.in));

            // ---- Step 1: Send JOIN ----
            out.println("JOIN|" + clientName);

            // ---- Step 2: Wait for ACK ----
            String ack = in.readLine();
            if (ack == null || !ack.startsWith("ACK|")) {
                System.out.println("[" + clientName + "] Connection refused: " + ack);
                return;
            }
            System.out.println("[" + clientName + "] " + ack.split("\\|", 3)[2]);
            System.out.println("[" + clientName + "] Type a math expression and press Enter. Type 'done' to disconnect.");

            // ---- Step 3: Interactive expression loop ----
            while (true) {
                System.out.print("[" + clientName + "] > ");
                String input = userIn.readLine();

                // null means stdin was closed (e.g. piped input exhausted)
                if (input == null || input.trim().equalsIgnoreCase("done")) break;
                if (input.trim().isEmpty()) continue;

                out.println("MATH|" + clientName + "|" + input.trim());

                // Wait for the result before prompting again
                String response = in.readLine();
                if (response == null) break;
                String[] parts = response.split("\\|", 3);
                switch (parts[0]) {
                    case "RESULT":
                        System.out.println("[" + clientName + "] = " + parts[2]);
                        break;
                    case "ERROR":
                        System.out.println("[" + clientName + "] Error: " + parts[1]);
                        break;
                    default:
                        System.out.println("[" + clientName + "] Server: " + response);
                }
            }

            // ---- Step 4: Send BYE ----
            System.out.println("[" + clientName + "] Disconnecting...");
            out.println("BYE|" + clientName);

            String goodbye = in.readLine();
            if (goodbye != null && goodbye.startsWith("GOODBYE|")) {
                System.out.println("[" + clientName + "] " + goodbye.split("\\|", 3)[2]);
            }
        }

        System.out.println("[" + clientName + "] Connection closed.");
    }
}
