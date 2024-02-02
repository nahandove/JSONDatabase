package server;

import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class Main {
    public static final String ADDRESS = "127.0.0.1";
    public static final int PORT = 23456;
    public static final String PATH = "C://Users/nahan/Desktop/JetBrainsTasks/JSON Database with Java/" +
            "JSON Database with Java/task/src/server/data/db.json";

    public static void main(String[] args) {
        ServerSocket server = null;
        ExecutorService executor = Executors.newFixedThreadPool(4);
        System.out.println("Server started!");
        try {
            server = new ServerSocket(PORT, 50, InetAddress.getByName(ADDRESS));
            Socket socket;
            while (true) {
                socket = server.accept();
                Session session = new Session(socket, PATH);
                Boolean isContinue = executor.submit(session).get();
                if (!isContinue) {
                    break;
                }
            }
        } catch (Exception e) {

        }
        executor.shutdown();
        try {
            if (server != null) {
                server.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
