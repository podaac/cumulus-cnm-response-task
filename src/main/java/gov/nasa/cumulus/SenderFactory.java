package gov.nasa.cumulus;

/**
 * Factory class to return Sender based on type
 */
public class SenderFactory {
    public static final String METHOD_KINESIS = "kinesis";
    public static final String METHOD_SNS = "sns";

    public static Sender getSender(String region, String type) {
        if (METHOD_KINESIS.equalsIgnoreCase(type)) {
            return new KinesisSender(region);
        } else if (METHOD_SNS.equalsIgnoreCase(type)) {
            return new SNSSender(region);
        } else {
            return null;
        }
    }
}
