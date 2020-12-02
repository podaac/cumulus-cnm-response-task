package gov.nasa.cumulus.bo;

import gov.nasa.cumulus.IConstants;

public class MessageAttribute implements IConstants {
    MessageFilterTypeEnum type;
    String value;

    public MessageFilterTypeEnum getType() {
        return type;
    }

    public void setType(MessageFilterTypeEnum type) {
        this.type = type;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

}
