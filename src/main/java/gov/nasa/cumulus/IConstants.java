package gov.nasa.cumulus;

/**
 * variables in interface are automatic public , static and final
 */
public interface IConstants {
    String COLLECTION_SHORT_NAME_ATTRIBUTE_KEY = "COLLECTION_SHORT_NAME";
    String CNM_RESPONSE_STATUS_ATTRIBUTE_KEY = "CNM_RESPONSE_STATUS";
    String DATA_VERSION = "DATA_VERSION";
    enum MessageFilterTypeEnum {
        String,
        Number
    }
}
