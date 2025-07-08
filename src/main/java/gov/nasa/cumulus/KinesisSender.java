package gov.nasa.cumulus;

import com.amazonaws.services.kinesis.AmazonKinesis;
import com.amazonaws.services.kinesis.AmazonKinesisClientBuilder;
import com.amazonaws.services.kinesis.model.PutRecordRequest;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import java.nio.ByteBuffer;

/**
 * Kinesis message sender
 */
public class KinesisSender extends Sender {

    private AmazonKinesis kinesisClient;

    public KinesisSender(String region) {
        super(region);
        this.kinesisClient = AmazonKinesisClientBuilder.standard().withRegion(region).build();
    }

    /**
     * Sends response message to specified Kinesis stream
     *
     * @param response The message to send to the kinesis stream
     * @param endpoint The stream name, not ARN, of the kinesis stream
     */
    public void sendMessage(String response, String endpoint) {
        byte[] bytes = response.getBytes();

        if (bytes == null) {
            return;
        }
        PutRecordRequest putRecord = new PutRecordRequest();

        if (endpoint.startsWith("arn:aws:kinesis:")) {
            putRecord.setStreamARN(endpoint);
        } else {
            putRecord.setStreamName(endpoint);
        }

        putRecord.setPartitionKey("1");
        putRecord.setData(ByteBuffer.wrap(bytes));
        this.kinesisClient.putRecord(putRecord);
    }

    /**
     * Sends response message to specified Kinesis streams
     *
     * @param response  The message to send to the kinesis stream
     * @param endpoints JsonArray of stream name, not ARN, of the kinesis stream
     */
    public void sendMessage(String response, JsonArray endpoints) {
        for (JsonElement stream : endpoints) {
            this.sendMessage(response, stream.getAsString());
        }
    }
}
