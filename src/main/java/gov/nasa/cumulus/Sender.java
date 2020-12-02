package gov.nasa.cumulus;

import com.amazonaws.services.sns.model.MessageAttributeValue;
import com.google.gson.JsonArray;
import cumulus_message_adapter.message_parser.AdapterLogger;
import gov.nasa.cumulus.bo.MessageAttribute;

import java.util.HashMap;
import java.util.Map;

/**
 * Abstract message sender
 */
public abstract class Sender {
    private String region;
    String className = this.getClass().getName();
    Map<String, MessageAttributeValue> messageAttributes = new HashMap<>();

    public Sender(String region) {
        this.region = region;
    }

    /**
     * This function does not support type of String.array yet but it serve the purpose
     * of String and Numeric
     */
    public void addMessageAttributes(Map<String, MessageAttribute> attributes) {
        for (Map.Entry<String, MessageAttribute> entry : attributes.entrySet()) {
            AdapterLogger.LogInfo(this.className + "Message Attributes key:value -> " + entry.getKey() + ":" + entry.getValue().getValue());
            messageAttributes.put(entry.getKey(), new MessageAttributeValue()
                    .withDataType(entry.getValue().getType().name())
                    .withStringValue(entry.getValue().getValue()));
        }
    }

    public abstract void sendMessage(String response, String endpoint);
    public abstract void sendMessage(String response, JsonArray endpoints);
}
