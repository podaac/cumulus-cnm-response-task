package gov.nasa.cumulus;

/**
 * Factory class to return Sender based on type
 */
public class SenderFactory {
    public static Sender getSender(String region, String type) {
        if ("kinesis".equalsIgnoreCase(type)) {
            return new KinesisSender(region);
        } else if ("sns".equalsIgnoreCase(type)) {
            return new SNSSender(region);
        } else {
            return null;
        }
    }
}
