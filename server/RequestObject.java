package server;

import com.google.gson.JsonElement;

public class RequestObject {
    String type;
    JsonElement key;
    JsonElement value;

    public String getType() {
        return type;
    }

    public JsonElement getKey() {
        return key;
    }

    public JsonElement getValue() {
        return value;
    }
}
