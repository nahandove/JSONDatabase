package client;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;

public class Main {
    public static final String ADDRESS = "127.0.0.1";
    public static final int PORT = 23456;
    @Parameter(names={"-t"})
    String type;
    @Parameter(names={"-k"})
    String key;
    @Parameter(names={"-v"})
    String value;
    @Parameter(names={"-in"})
    String fileName = "";

    public static void main(String[] args) {
        System.out.println("Client started!");
        try (Socket socket = new Socket(InetAddress.getByName(ADDRESS), PORT);
             DataInputStream input = new DataInputStream(socket.getInputStream());
             DataOutputStream output = new DataOutputStream(socket.getOutputStream())) {
                Main main = new Main();
                JCommander.newBuilder()
                        .addObject(main)
                        .build()
                        .parse(args);
                main.run(output);
                String serverMessage = input.readUTF();
                System.out.println("Received: " + serverMessage);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void run(DataOutputStream output) throws IOException {
        String clientMessage = "Sent: ";
        if (!"".equals(fileName)) {
            clientMessage += parseFile(fileName);
        } else {
            if ("set".equals(type)) {
                clientMessage += String.format("{\"type\":\"%s\",\"key\":\"%s\",\"value\":\"%s\"}", type, key, value);
            } else if ("get".equals(type) || "delete".equals(type)) {
                clientMessage += String.format("{\"type\":\"%s\",\"key\":\"%s\"}", type, key);
            } else if ("exit".equals(type)) {
                clientMessage += "{\"type\":\"exit\"}";
            }
        }
        output.writeUTF(clientMessage);
        System.out.println(clientMessage);
    }

    public String parseFile(String fileName) {
        StringBuilder jsonString = new StringBuilder();
        try (BufferedReader fileReader = new BufferedReader(new FileReader(
                "C://Users/nahan/Desktop/JetBrainsTasks/JSON Database with Java/JSON Database with Java/" +
                        "task/src/client/data/" + fileName))) {
            while (fileReader.ready()) {
                jsonString.append(fileReader.readLine());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return jsonString.toString();
    }
}
