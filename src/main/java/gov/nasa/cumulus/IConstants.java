package gov.nasa.cumulus;

/**
 * variables in interface are automatic public , static and final
 */
public interface IConstants {
    String COLLECTION_SHORT_NAME_ATTRIBUTE_KEY = "COLLECTION";
    String CNM_RESPONSE_STATUS_ATTRIBUTE_KEY = "CNM_RESPONSE_STATUS";
    String DATA_VERSION_ATTRIBUTE_KEY = "DATA_VERSION";
    enum MessageFilterTypeEnum {
        String,
        Number
    }
}
