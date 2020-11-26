package gov.nasa.cumulus;

import com.amazonaws.services.sns.model.MessageAttributeValue;
import com.google.gson.JsonArray;
import gov.nasa.cumulus.cnmresponse.bo.MessageAttributeBO;

import java.util.HashMap;
import java.util.Map;

/**
 * Abstract message sender
 */
public abstract class Sender {
    private String region;
    Map<String, MessageAttributeValue> messageAttributes = new HashMap<>();

    public Sender(String region) {
        this.region = region;
    }

    /**
     * This function does not support type of String.array yet but it serve the purpose
     * of String and Numeric
     */
    public void addMessageAttributes(Map<String, MessageAttributeBO> attributes) {
        for (Map.Entry<String, MessageAttributeBO> entry : attributes.entrySet()) {
            System.out.println(entry.getKey() + ":" + entry.getValue());
            messageAttributes.put(entry.getKey(), new MessageAttributeValue()
                    .withDataType(entry.getValue().getType())
                    .withStringValue(entry.getValue().getValue()));
        }
    }

    public abstract void sendMessage(String response, String endpoint);
    public abstract void sendMessage(String response, JsonArray endpoints);
}
