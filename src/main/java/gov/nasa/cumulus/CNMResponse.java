package gov.nasa.cumulus;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.Gson;
import org.apache.commons.io.IOUtils;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

import cumulus_message_adapter.message_parser.ITask;
import cumulus_message_adapter.message_parser.MessageAdapterException;
import cumulus_message_adapter.message_parser.MessageParser;
import cumulus_message_adapter.message_parser.AdapterLogger;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.utils.URIBuilder;


public class CNMResponse implements  ITask, RequestHandler<String, String>{
	String className = this.getClass().getName();
	public enum ErrorCode {VALIDATION_ERROR, TRANSFER_ERROR, PROCESSING_ERROR};

	public String handleRequest(String input, Context context) {
		MessageParser parser = new MessageParser();
		try
		{
			AdapterLogger.LogDebug(this.className + " handleRequest input:" + input);
			return parser.RunCumulusTask(input, context, new CNMResponse());
		}
		catch(MessageAdapterException e)
		{
			AdapterLogger.LogError(this.className + " handleRequest error:" + e.getMessage());
			return e.getMessage();
		}
	}

	public void handleRequestStreams(InputStream inputStream, OutputStream outputStream, Context context) throws IOException, MessageAdapterException {
		MessageParser parser = new MessageParser();
		String input =IOUtils.toString(inputStream, "UTF-8");
		AdapterLogger.LogDebug(this.className + " handleRequestStreams input:" + input);
		String output = parser.RunCumulusTask(input, context, new CNMResponse());
		AdapterLogger.LogDebug(this.className + " handleRequestStreams output:" + output);
		outputStream.write(output.getBytes(Charset.forName("UTF-8")));
	}

	public static JsonObject getResponseObject(String exception) {
		JsonObject response = new JsonObject();

        if (exception == null || exception.isEmpty() || exception.equals("None") || exception.equals("\"None\"")) {
            //success
			AdapterLogger.LogInfo(CNMResponse.class.getName() + " status: SUCCESS");
			response.addProperty("status", "SUCCESS");
		} else {
			//fail
			AdapterLogger.LogWarning(CNMResponse.class.getName() + " status: FAILURE");
			response.addProperty("status", "FAILURE");

			//logic for failure types here
            String error;
            String causeString;
            JsonObject workflowException;

            // PODAAC-2552 - handle general exceptions being thrown
            if (exception.equalsIgnoreCase("CNMResponse Exception")) {
                error = exception;
                causeString = "Unknown cause, CNMResponse likely threw an exception";
            } else {
                workflowException = new JsonParser().parse(exception).getAsJsonObject();
                error = workflowException.get("Error").getAsString();
                causeString = workflowException.get("Cause").getAsString();
            }

			AdapterLogger.LogWarning(CNMResponse.class.getName() + " error:" + error);
			switch(error) {
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

	public static String generateOutput(String inputCnm, String exception, JsonObject granule, JsonObject  inputConfig)
	throws Exception {
		//convert CNM to GranuleObject
		JsonObject response = getResponseObject(exception);
		JsonElement jelement = new JsonParser().parse(inputCnm);
		JsonObject inputKey = jelement.getAsJsonObject();
		// Only add product.name, product.files under inputKey when SUCCESS
		if(granule !=null && StringUtils.equals(response.get("status").getAsString(), "SUCCESS")) {
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
				f.addProperty("name", e.getAsJsonObject().getAsJsonPrimitive("name").getAsString());
				// uri
				String filename = e.getAsJsonObject().getAsJsonPrimitive("filename").getAsString();
				filename = filename.replace("s3://", "/");
				try {
					URIBuilder uriBuilder = new URIBuilder(distribute_url);
					f.addProperty("uri", uriBuilder.setPath(uriBuilder.getPath() + filename).build().normalize().toString());
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

			JsonObject ingestionMetadata = new JsonObject();
			ingestionMetadata.addProperty("catalogId", granule.get("cmrConceptId").getAsString());
			ingestionMetadata.addProperty("catalogUrl", granule.get("cmrLink").getAsString());
			response.add("ingestionMetadata", ingestionMetadata);
		} else {
			inputKey.remove("product");
		}
		// no matter SUCCESS or FAILURE, added response under inputKey
		// but the FAILURE case, response does not include cmr data
		inputKey.add("response", response);

		TimeZone tz = TimeZone.getTimeZone("UTC");
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
		df.setTimeZone(tz);
		String nowAsISO = df.format(new Date());

		inputKey.addProperty("processCompleteTime", nowAsISO);
		return new Gson().toJson(inputKey);
	}

    public String getError(JsonObject input, String key){

		String exception = null;

		if(input.get(key) != null){
			exception = input.get(key).toString();
			AdapterLogger.LogError(this.className + " WorkflowException:" + input.get(key));
		} else {
			AdapterLogger.LogError(this.className + " WorkflowException: not finding exception by key");
		}
		return exception;
	}

	public static String generateGeneralError(String cnm) {
        JsonElement jelement = new JsonParser().parse(cnm);
        JsonObject inputKey = jelement.getAsJsonObject();
        JsonObject response = getResponseObject("CNMResponse Exception");
        // remove the product information
        inputKey.remove("product");
        inputKey.add("response", response);
        // add the completion timestamp
        TimeZone tz = TimeZone.getTimeZone("UTC");
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        df.setTimeZone(tz);
        String nowAsISO = df.format(new Date());
        inputKey.addProperty("processCompleteTime", nowAsISO);
        return new Gson().toJson(inputKey);
    }

    public static void sendSNS(String output, String method, String region, JsonElement endPoint) {
        if (method != null) {
            if (endPoint.isJsonArray()) {
                SenderFactory.getSender(region, method).sendMessage(output, endPoint.getAsJsonArray());
            } else {
                SenderFactory.getSender(region, method).sendMessage(output, endPoint.getAsString());
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

		JsonObject  inputConfig = inputKey.getAsJsonObject("config");
		String cnm = new Gson().toJson(inputConfig.get("OriginalCNM"));
		String exception = getError(inputConfig, "WorkflowException");

		// PODAAC-2552 - catch general exceptions, send SNS failure, then re-throw the exception
        String method = inputConfig.get("type").getAsString();
        String region = inputConfig.get("region").getAsString();
        AdapterLogger.LogInfo(this.className + " region:" + region + " method:" + method);
        JsonElement responseEndpoint = inputConfig.get("response-endpoint");
		String output;
		try {
            JsonObject granule = inputKey.get("input").getAsJsonObject().get("granules").getAsJsonArray().get(0).getAsJsonObject();
            output = CNMResponse.generateOutput(cnm, exception, granule, inputConfig);
            sendSNS(output, method, region, responseEndpoint);
        } catch (Exception ex1){
		    output = CNMResponse.generateGeneralError(cnm);
            sendSNS(output, method, region, responseEndpoint);
            throw ex1;
        }

		/* create new object:
		 *
		 * {cnm: output, input:input}
		 *
		 */
		JsonObject bigOutput = new JsonObject();
		bigOutput.add("cnm", new JsonParser().parse(output).getAsJsonObject());
		bigOutput.add("input", new JsonParser().parse(input).getAsJsonObject());

		return new Gson().toJson(bigOutput);
	}
}
