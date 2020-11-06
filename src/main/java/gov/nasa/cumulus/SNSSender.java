package gov.nasa.cumulus;

import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.AmazonSNSClientBuilder;
import com.amazonaws.services.sns.model.PublishRequest;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

/**
 * SNS message sender
 */
public class SNSSender extends Sender {

    private AmazonSNS snsClient;

    public SNSSender(String region) {
        this(region, AmazonSNSClientBuilder.standard().withRegion(region).build());
    }

    public SNSSender(String region, AmazonSNS snsClient) {
        super(region);
        this.snsClient = snsClient;
    }

    /**
     * Sends response message to specified SNS topic
     *
     * @param response The message to send to the SNS topic
     * @param endpoint The SNS topic ARN to which the message should be sent
     */
    public void sendMessage(String response, String endpoint) {
        final PublishRequest publishRequest = new PublishRequest(endpoint, response);
        this.snsClient.publish(publishRequest);
    }

    /**
     * Sends response message to specified SNS topics
     *
     * @param response  The message to send to the SNS topic
     * @param endpoints JsonArray of SNS topic ARN to which the message should be sent
     */
    public void sendMessage(String response, JsonArray endpoints) {
        for (JsonElement topic : endpoints) {
            this.sendMessage(response, topic.getAsString());
        }
    }
}
