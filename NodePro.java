import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class NodePro {
    private static int myPort;
    private static int currentLeader = -1;
    private static Map<Integer, Boolean> alivePeers = new ConcurrentHashMap<>();
    private static int lastSeenJobId = 0;

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: java Node [port]");
            return;
        }
        myPort = Integer.parseInt(args[0]);
        int[] allPorts = {8081, 8082, 8083};

        for (int p : allPorts) {
            if (p != myPort) alivePeers.put(p, false);
        }
        new Thread(() -> listen()).start();
        new Thread(() -> broadcastHeartbeats()).start();

        System.out.println("Node " + myPort + " started. Election logic active...");
        runElectionLoop();
    }

    public static void listen() {
        try (ServerSocket serverSocket = new ServerSocket(myPort)) {
            while (true) {
                try (Socket socket = serverSocket.accept();
                     BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
                    String line = in.readLine();
                    if (line != null) {
                        if (line.startsWith("ALIVE:")) {
                            int senderPort = Integer.parseInt(line.split(":")[1]);
                            alivePeers.put(senderPort, true);
                        }
                        // to handle Jobs from the Leader
                        else if (line.startsWith("JOB:")) {
                            String[] parts = line.split(":");
                            int leaderPort = Integer.parseInt(parts[1]);
                            int jobId = Integer.parseInt(parts[2]);
                            alivePeers.put(leaderPort, true);
                            lastSeenJobId = jobId;
                            System.out.println("  [WORK] Received Job #" + jobId + " from Leader " + leaderPort);
                        }
                    }
                } catch (Exception e) { }
            }
        } catch (IOException e) {
            System.err.println("Could not listen on port " + myPort);
        }
    }

    public static void broadcastHeartbeats() {
        int jobCounter = lastSeenJobId;

        while (true) {
             if (currentLeader == myPort && jobCounter <= lastSeenJobId) {
                jobCounter = lastSeenJobId + 1;
            }

            for (Integer peerPort : alivePeers.keySet()) {
                try (Socket socket = new Socket()) {
                    socket.connect(new InetSocketAddress("localhost", peerPort), 500);
                    PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

                    if (currentLeader == myPort) {
                        out.println("JOB:" + myPort + ":" + jobCounter);
                    } else {
                        out.println("ALIVE:" + myPort);
                    }

                    alivePeers.put(peerPort, true);
                } catch (IOException e) {
                    alivePeers.put(peerPort, false);
                }
            }

            if (currentLeader == myPort) {
                System.out.println(">> [LEADER] Dispatched Job #" + jobCounter);
                jobCounter++;
                lastSeenJobId = jobCounter;
            }

            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public static void runElectionLoop() {
        while (true) {
            try {
                Thread.sleep(2000);
                int maxPort = myPort;

                for (Map.Entry<Integer, Boolean> entry : alivePeers.entrySet()) {
                    if (entry.getValue() && entry.getKey() > maxPort) {
                        maxPort = entry.getKey();
                    }
                }

                if (maxPort != currentLeader) {
                    currentLeader = maxPort;
                    System.out.println("\n*** NEW ELECTION ***");
                    System.out.println("Leader is now: " + currentLeader);
                }

                String role = (currentLeader == myPort) ? "[LEADER]" : "[FOLLOWER]";
                System.out.println("Node " + myPort + " " + role + " | Cluster: " + alivePeers);

            } catch (InterruptedException e) {}
        }
    }
}
