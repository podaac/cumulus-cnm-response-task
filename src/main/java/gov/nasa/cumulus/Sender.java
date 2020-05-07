package gov.nasa.cumulus;

import com.google.gson.JsonArray;

/**
 * Abstract message sender
 */
public abstract class Sender {
    private String region;

    public Sender(String region) {
        this.region = region;
    }

    public abstract void sendMessage(String response, String endpoint);
    public abstract void sendMessage(String response, JsonArray endpoints);
}
