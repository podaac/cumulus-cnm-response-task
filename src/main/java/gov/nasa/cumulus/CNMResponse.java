package gov.nasa.cumulus;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.google.gson.*;
import cumulus_message_adapter.message_parser.AdapterLogger;
import cumulus_message_adapter.message_parser.ITask;
import cumulus_message_adapter.message_parser.MessageAdapterException;
import cumulus_message_adapter.message_parser.MessageParser;
import gov.nasa.cumulus.bo.MessageAttribute;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.utils.URIBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;


public class CNMResponse implements ITask, IConstants, RequestHandler<String, String> {
    String className = this.getClass().getName();

    public enum ErrorCode {VALIDATION_ERROR, TRANSFER_ERROR, PROCESSING_ERROR};

    public String handleRequest(String input, Context context) {
        MessageParser parser = new MessageParser();
        try {
            AdapterLogger.LogDebug(this.className + " handleRequest input:" + input);
            return parser.RunCumulusTask(input, context, new CNMResponse());
        } catch (MessageAdapterException e) {
            AdapterLogger.LogError(this.className + " handleRequest error:" + e.getMessage());
            return e.getMessage();
        }
    }

    public void handleRequestStreams(InputStream inputStream, OutputStream outputStream, Context context) throws IOException, MessageAdapterException {
        MessageParser parser = new MessageParser();
        String input = IOUtils.toString(inputStream, "UTF-8");
        AdapterLogger.LogDebug(this.className + " handleRequestStreams input:" + input);
        String output = parser.RunCumulusTask(input, context, new CNMResponse());
        AdapterLogger.LogDebug(this.className + " handleRequestStreams output:" + output);
        outputStream.write(output.getBytes(Charset.forName("UTF-8")));
    }

    public static JsonObject getResponseObject(String exception) {
        JsonObject response = new JsonObject();

        if (exception == null || new String("").equals(exception) || new String("None").equals(exception) || new String("\"None\"").equals(exception)) {
            //success
            AdapterLogger.LogInfo(CNMResponse.class.getName() + " status: SUCCESS");
            response.addProperty("status", "SUCCESS");
        } else {
            //fail
            AdapterLogger.LogWarning(CNMResponse.class.getName() + " status: FAILURE");
            response.addProperty("status", "FAILURE");

            //logic for failure types here
            JsonObject workflowException = JsonParser.parseString(exception).getAsJsonObject();

            String error = workflowException.get("Error").getAsString();
            AdapterLogger.LogWarning(CNMResponse.class.getName() + " error:" + error);
            switch (error) {
                case "FileNotFound":
                case "RemoteResourceError":
                case "ConnectionTimeout":
                    response.addProperty("errorCode", ErrorCode.TRANSFER_ERROR.toString());
                    break;
                case "InvalidChecksum":
                case "UnexpectedFileSize":
                    response.addProperty("errorCode", ErrorCode.VALIDATION_ERROR.toString());
                    break;
                default:
                    response.addProperty("errorCode", ErrorCode.PROCESSING_ERROR.toString());
            }

            String causeString = workflowException.get("Cause").getAsString();
            AdapterLogger.LogWarning(CNMResponse.class.getName() + " causeString:" + causeString);
            try {
                JsonObject cause = new JsonParser().parse(causeString).getAsJsonObject();
                AdapterLogger.LogWarning(CNMResponse.class.getName() + " cause:" + cause);
                String errorMessage = cause.get("errorMessage").getAsString();
                AdapterLogger.LogWarning(CNMResponse.class.getName() + " errorMessage:" + errorMessage);
                response.addProperty("errorMessage", errorMessage);
            } catch (Exception e) {
                AdapterLogger.LogError(CNMResponse.class.getName() + " Exception:" + e);
                response.addProperty("errorMessage", causeString);
            }
        }
        return response;
    }

    public static String generateOutput(String inputCnm, String exception, JsonObject granule, JsonObject inputConfig)
            throws Exception {
        //convert CNM to GranuleObject
        JsonObject response = getResponseObject(exception);
        JsonElement jelement = new JsonParser().parse(inputCnm);
        JsonObject inputKey = jelement.getAsJsonObject();
        // Only add product.name, product.files under inputKey when SUCCESS
        if (granule != null && StringUtils.equals(response.get("status").getAsString(), "SUCCESS")) {
            String distribute_url = inputConfig.getAsJsonPrimitive("distribution_endpoint").getAsString();

            JsonArray granuleFiles = granule.getAsJsonArray("files");
            // build product.files off input granule's files
            JsonArray productFiles = new JsonArray();
            for (int i = 0; i < granuleFiles.size(); i++) {
                JsonElement e = granuleFiles.get(i);
                JsonObject f = new JsonObject();
                //type
                f.addProperty("type", e.getAsJsonObject().getAsJsonPrimitive("type").getAsString());
                // subtype : skip
                // name
                f.addProperty("name", e.getAsJsonObject().getAsJsonPrimitive("fileName").getAsString());
                // uri
                String bucket = e.getAsJsonObject().getAsJsonPrimitive("bucket").getAsString();
                String key = e.getAsJsonObject().getAsJsonPrimitive("key").getAsString();
                String filepath = bucket + '/' + key;
                try {
                    URIBuilder uriBuilder = new URIBuilder(distribute_url);
                    f.addProperty("uri", uriBuilder.setPath(uriBuilder.getPath() + filepath).build().normalize().toString());
                } catch (URISyntaxException uriSyntaxException) {
                    throw uriSyntaxException;
                }
                // checksumType
                if (e.getAsJsonObject().getAsJsonPrimitive("checksumType") != null)
                    f.addProperty("checksumType", e.getAsJsonObject().getAsJsonPrimitive("checksumType").getAsString());
                if (e.getAsJsonObject().getAsJsonPrimitive("checksum") != null)
                    f.addProperty("checksum", e.getAsJsonObject().getAsJsonPrimitive("checksum").getAsString());
                f.addProperty("size", e.getAsJsonObject().getAsJsonPrimitive("size").getAsLong());
                productFiles.add(f);
            }

            // Adding newly created files to product
            inputKey.get("product").getAsJsonObject().remove("files");
            inputKey.get("product").getAsJsonObject().add("files", productFiles);
            // Adding granuleID into product object
            String granuleId = granule.get("granuleId").getAsString();
            inputKey.get("product").getAsJsonObject().remove("name");
            inputKey.get("product").getAsJsonObject().addProperty("name", granuleId);

            // Only add CMR metadata if available
            if (granule.get("cmrConceptId") != null && granule.get("cmrLink") != null) {
                JsonObject ingestionMetadata = new JsonObject();
                ingestionMetadata.addProperty("catalogId", granule.get("cmrConceptId").getAsString());
                ingestionMetadata.addProperty("catalogUrl", granule.get("cmrLink").getAsString());
                response.add("ingestionMetadata", ingestionMetadata);
            }
        } else {
            inputKey.remove("product");
        }
        // no matter SUCCESS or FAILURE, added response under inputKey
        // but the FAILURE case, response does not include cmr data
        inputKey.add("response", response);

        inputKey.addProperty("processCompleteTime", CNMResponse.getCompletionTimestamp());
        return new Gson().toJson(inputKey);
    }

    /**
     * Builds the completion timestamp using the current Time
     * @return      the timestamp, properly formatted, as string
     */
    public static String getCompletionTimestamp() {
        TimeZone tz = TimeZone.getTimeZone("UTC");
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        df.setTimeZone(tz);
        return df.format(new Date());
    }

    public String getError(JsonObject input, String key) {

        String exception = null;

        if (input.get(key) != null) {
            exception = input.get(key).toString();
            AdapterLogger.LogError(this.className + " WorkflowException:" + input.get(key));
        } else {
            AdapterLogger.LogError(this.className + " WorkflowException: not finding exception by key");
        }
        return exception;
    }

    /**
     * Generates the minimum required JSON object according to the
     * CNM Schema 1.0; see file: cumulus_sns_v1.0_response_failure_2.json
     * <br><br>
     * This will be called/run when we encounter (and catch)
     * any exception during 'performFunction'
     *<br><br>
     * @param input     the input, as JSON from 'PerformFunction'
     * @param cause     the message string from the exception that was thrown
     * @return          the response failure JSON, as string
     */
    public String buildGeneralError(JsonObject input, String cause) {
        // the minimum fields necessary to conform to the CNM schema
        String[] schemaFields = {"version", "provider", "collection", "submissionTime", "receivedTime", "identifier"};
        JsonObject failureJson = new JsonObject();
        JsonObject cnm;
        // determine if the values we need are under 'input' or
        // 'config > originalcnm' key
        if (input.getAsJsonObject("input").has("collection")) {
            cnm = input.getAsJsonObject("input");
        } else {
            cnm = input.getAsJsonObject("config").getAsJsonObject("OriginalCNM");
        }
        for (String field : schemaFields) {
            if (cnm.has(field)) {
                // if the input json has the field we need, use it
                failureJson.add(field, cnm.get(field));
            } else {
                // otherwise, for the time being, just fill in as unknown/missing.
                // theoretically this should never happen, but we need to ensure the
                // message always gets sent.
                failureJson.addProperty(field, "Unknown/Missing " + field);
            }
        }
        // add the actual failure response now
        JsonObject response = new JsonObject();
        response.addProperty("status", "FAILURE");
        response.addProperty("errorCode", ErrorCode.PROCESSING_ERROR.toString());
        response.addProperty("errorMessage", cause);
        failureJson.add("response", response);
        // add the completion timestamp
        failureJson.addProperty("processCompleteTime", CNMResponse.getCompletionTimestamp());
        return new Gson().toJson(failureJson);
    }

    /**
     * Gets the value for the 'DataVersion' field, to be used
     * by the 'buildMessageAttributesHash' method.
     * <br><br>
     * Tries to use the 'OriginalCNM > Product > DataVersion' from the
     * 'input > config' section, of the provided Json, but in case of an error,
     * return a generic error string.
     *<br><br>
     * @param input     the input > config section, as a JsonObject
     * @return          the string to use as the 'dataVersion' value
     */
    public String getDataVersion(JsonObject input) {
        try {
            return input.getAsJsonObject("OriginalCNM")
                    .getAsJsonObject("product")
                    .get("dataVersion").getAsString();
        } catch (Exception e) {
            return "Unknown/Missing";
        }
    }

    /**
     * Gets the value for the 'collection' field, to be used
     * by the 'buildMessageAttributesHash' method.
     * <br><br>
     * First try to use the OriginalCNM > collection field
     * Next checks input > collection
     * If neither is available, then returns a generic error string
     *<br><br>
     * @param input     the raw input to PerformFunction, as JsonObject
     * @return          the string to use as the 'collection' value
     */
    public String getCollection(JsonObject input) {
        try {
            return input.getAsJsonObject("config")
                    .getAsJsonObject("OriginalCNM")
                    .get("collection").getAsString();
        } catch (Exception e) {
            if (input.getAsJsonObject("input").has("collection")) {
                return input.getAsJsonObject("input").get("collection").getAsString();
            } else {
                return "Unknown/Missing";
            }
        }
    }

    /**
     * Intermediate method called during 'PerformFunction' to
     * build the 'output' for the final message, via 'generateOutput'
     * <br><br>
     * @param inputKey      the complete 'input' as JsonObject
     * @param inputConfig   the input > config section, as JsonObject
     * @return              the output json, as a string,
     * @throws Exception
     */
    public String buildMessage(JsonObject inputKey, JsonObject inputConfig) throws Exception {
        String cnm = new Gson().toJson(inputConfig.get("OriginalCNM"));
        JsonObject granule = inputKey.get("input").getAsJsonObject()
                .get("granules").getAsJsonArray()
                .get(0).getAsJsonObject();
        String exception = getError(inputConfig, "WorkflowException");
        return CNMResponse.generateOutput(cnm, exception, granule, inputConfig);
    }

    /**
     * Actually sends the message, using the specified output string,
     * method (Kinesis or sns), region, and endpoint.
     * <br><br>
     * Builds the attributeMap for the message via 'buildMessageAttributesHash'
     * using the 'response > status' field from the 'output'
     * along with the provided values for 'collection' and 'dataversion'
     * <br><br>
     * @param output        the final output json message, as String
     * @param method        the method to use, 'Kinesis' or 'Sns' when sending
     * @param region        the region to send the message to
     * @param endpoint      the actual endpoint for the message
     * @param collection    the collection value for the attribute hash
     * @param dataVersion   the dataVersion value for the attribute hash
     */
    public void sendMessage(String output, String method, String region,
                                   JsonElement endpoint, String collection,
                                   String dataVersion) {
        // convert the final output to a JsonObject, so we can get 'response status'
        JsonObject outputJsonObj = new JsonParser().parse(output).getAsJsonObject();
        String final_status = outputJsonObj.getAsJsonObject("response").get("status").getAsString();
        if (method != null) {
            Map<String, MessageAttribute> attributeBOMap = buildMessageAttributesHash(collection, dataVersion, final_status);
            Sender sender = SenderFactory.getSender(region, method);
            sender.addMessageAttributes(attributeBOMap);
            if (endpoint.isJsonArray()) {
                sender.sendMessage(output, endpoint.getAsJsonArray());
            } else {
                sender.sendMessage(output, endpoint.getAsString());
            }
        }
    }

    //inputs
    // OriginalCNM
    // CNMResponseStream
    // WorkflowException
    // region
    public String PerformFunction(String input, Context context) throws Exception {
        AdapterLogger.LogDebug(this.className + " Entered PerformFunction with input String: " + input);
        JsonElement jelement = new JsonParser().parse(input);
        JsonObject inputKey = jelement.getAsJsonObject();
        JsonObject inputConfig = inputKey.getAsJsonObject("config");
        String method = inputConfig.get("type").getAsString();
        String region = inputConfig.get("region").getAsString();
        AdapterLogger.LogInfo(this.className + " region:" + region + " method:" + method);
        JsonElement responseEndpoint = inputConfig.get("response-endpoint");
        // get collection and dataVersion for use in message attribute hash
        String collection = getCollection(inputKey);
        String dataVersion = getDataVersion(inputConfig);
        String output;
        try {
            output = buildMessage(inputKey, inputConfig);
            sendMessage(output, method, region, responseEndpoint, collection, dataVersion);
            /* create new object:
             *
             * {cnm: output, input:input}
             *
             */
            JsonObject bigOutput = new JsonObject();
            bigOutput.add("cnm", new JsonParser().parse(output).getAsJsonObject());
            bigOutput.add("input", new JsonParser().parse(input).getAsJsonObject());
            return new Gson().toJson(bigOutput);
        } catch (Exception ex) {
            AdapterLogger.LogError(this.className + " encountered exception with input String: " + input);
            output = buildGeneralError(inputKey, ex.getMessage());
            sendMessage(output, method, region, responseEndpoint, collection, dataVersion);
            // re-throw the exception now.
            throw ex;
        }
    }

    Map<String, MessageAttribute> buildMessageAttributesHash(String collection_name, String dataVersion, String status) {
        Map<String, MessageAttribute> attributeBOMap = new HashMap<>();
        MessageAttribute collectionNameBO = new MessageAttribute();
        collectionNameBO.setType(MessageFilterTypeEnum.String);
        collectionNameBO.setValue(collection_name);
        attributeBOMap.put(this.COLLECTION_SHORT_NAME_ATTRIBUTE_KEY, collectionNameBO);
        MessageAttribute statusBO = new MessageAttribute();
        statusBO.setType(MessageFilterTypeEnum.String);
        statusBO.setValue(status);
        attributeBOMap.put(this.CNM_RESPONSE_STATUS_ATTRIBUTE_KEY, statusBO);
        MessageAttribute dataVersionBO = new MessageAttribute();
        dataVersionBO.setType(MessageFilterTypeEnum.String);
        dataVersionBO.setValue(dataVersion);
        attributeBOMap.put(this.DATA_VERSION_ATTRIBUTE_KEY, dataVersionBO);
        return attributeBOMap;
    }
}
