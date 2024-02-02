package server;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.Callable;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class Session implements Callable<Boolean> {
    private final Socket socket;
    private final String fileName;
    public JsonObject database;
    ReadWriteLock lock = new ReentrantReadWriteLock();
    Lock readLock = lock.readLock();
    Lock writeLock = lock.writeLock();

    public Session(Socket socketForClient, String fileName) {
        this.socket = socketForClient;
        this.fileName = fileName;
    }

    @Override
    public Boolean call() {
        try (DataInputStream input = new DataInputStream(socket.getInputStream());
             DataOutputStream output = new DataOutputStream(socket.getOutputStream())) {
            String clientRequest = input.readUTF();
            int index = clientRequest.indexOf(":");
            String jsonString = clientRequest.substring(index + 2);
            RequestObject requestObject = new Gson().fromJson(jsonString, RequestObject.class);
            String query = requestObject.getType();
            if ("exit".equals(query)) {
                output.writeUTF("{\"response\":\"OK\"}");
                this.socket.close();
                return false;
            }
            String result;
            if ("get".equals(query)) {
                result = getCell(requestObject.getKey()).toString();
            } else if ("set".equals(query)) {
                result = setCell(requestObject.getKey(), requestObject.getValue());
            } else if ("delete".equals(query)) {
                result = deleteCell(requestObject.getKey());
            } else {
                result = "ERROR";
            }
            output.writeUTF(getResponse(result));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }

    private String getResponse(String result) {
        String response;
        if ("OK".equals(result)) {
            response = "{\"response\":\"OK\"}";
        } else if ("ERROR".equals(result)) {
            response = "{\"response\":\"ERROR\",\"reason\":\"No such key\"}";
        } else {
            response = String.format("{\"response\":\"OK\",\"value\":%s}", result);
        }
        return response;
    }

    private Object getCell(JsonElement key) {
        JsonElement value;
        JsonObject inner;
        if (!containsKey(key)) {
            return "ERROR";
        }
        if (key.isJsonArray()) {
            JsonArray array = key.getAsJsonArray();
            inner = database.getAsJsonObject(array.get(0).getAsString());
            if (array.size() == 1) {
                if (inner.isJsonObject()) {
                    return inner.getAsJsonObject();
                } else {
                    return inner.getAsString();
                }
            }
            for (int i = 1; i < array.size(); i++) {
                String newKey = array.get(i).getAsString();
                if (inner.get(newKey).isJsonObject()) {
                    inner = inner.getAsJsonObject(newKey);
                } else {
                    return inner.get(newKey);
                }
            }
            return inner;
        } else {
            value = database.get(key.getAsString());
        }
        if (value == null) {
            return "ERROR";
        }
        return value;
    }

    private String setCell(JsonElement key, JsonElement newValue) {
        database = readFile();
        JsonObject inner;
        if (key.isJsonArray()) {
            JsonArray array = key.getAsJsonArray();
            if (array.size() == 1) {
                database.add(array.get(0).getAsString(), newValue);
            } else {
                inner = database.getAsJsonObject(array.get(0).getAsString());
                if (array.size() == 2) {
                    String newKey = array.get(1).getAsString();
                    if (inner.get(newKey) != null) {
                        inner.add(inner.getAsString(), newValue);
                    } else {
                        inner.add(inner.getAsString(), array.get(1));
                        inner.add(array.get(1).getAsString(), newValue);
                    }
                } else {
                    for (int i = 1; i < array.size() - 1; i++) {
                        String newKey = array.get(i).getAsString();
                        if (inner.get(newKey) != null) {
                            inner = inner.getAsJsonObject(newKey);
                        } else {
                            inner.add(array.get(i).getAsString(), array.get(i + 1));
                        }
                    }
                    inner.add(array.get(array.size() - 1).getAsString(), newValue);
                }
            }
        } else {
            database.add(key.getAsString(), newValue);
        }
        writeFile();
        return "OK";
    }

    private String deleteCell(JsonElement key) {
        database = readFile();
        JsonObject inner;
        if (!containsKey(key)) {
            return "ERROR";
        }
        if (key.isJsonArray()) {
            JsonArray array = key.getAsJsonArray();
            inner = database.getAsJsonObject(array.get(0).getAsString());
            if (array.size() == 1) {
                database.remove(inner.getAsString());
            }
            String newKey = array.get(0).getAsString();
            for (int i = 1; i < array.size(); i++) {
                newKey = array.get(i).getAsString();
                if (inner.get(newKey).isJsonObject()) {
                    inner = inner.getAsJsonObject(newKey);
                }
            }
            inner.remove(newKey);
        } else {
            database.remove(key.getAsString());
        }
        writeFile();
        return "OK";
    }

    private boolean containsKey(JsonElement key) {
        database = readFile();
        JsonObject inner;
        if (key.isJsonArray()) {
            JsonArray array = key.getAsJsonArray();
            if (database.get(array.get(0).getAsString()) != null) {
                inner = database.getAsJsonObject(array.get(0).getAsString());
            } else {
                JsonElement value = database.get(array.get(0).getAsString());
                return value != null;
            }
            if (array.size() == 1) {
                return inner != null;
            }
            for (int i = 1; i < array.size(); i++) {
                String newKey = array.get(i).getAsString();
                if (inner.get(newKey).isJsonObject()) {
                    inner = inner.getAsJsonObject(newKey);
                } else {
                    if (i < array.size() - 1) {
                        return false;
                    }
                    JsonElement innerValue = inner.get(newKey);
                    return innerValue != null;
                }
            }
            return inner != null;
        }
        return database.has(key.getAsString());
    }

    private void writeFile() {
        try {
            writeLock.lock();
            try (FileWriter fileWriter = new FileWriter(fileName)) {
                String databaseJson = new Gson().toJson(database);
                fileWriter.write(databaseJson);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } finally {
            writeLock.unlock();
        }
    }

    private JsonObject readFile() {
        JsonObject data;
        try {
            readLock.lock();
            try (BufferedReader fileReader = new BufferedReader(new FileReader(fileName))) {
                StringBuilder json = new StringBuilder();
                while (fileReader.ready()) {
                    json.append(fileReader.readLine());
                }
                data = new Gson().fromJson(json.toString(), JsonObject.class);
                if (data != null) {
                    return data;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return new JsonObject();
        } finally {
            readLock.unlock();
        }
    }
}
