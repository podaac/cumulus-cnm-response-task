package gov.nasa.cumulus;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.gson.*;
import org.apache.commons.io.IOUtils;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

import cumulus_message_adapter.message_parser.ITask;
import cumulus_message_adapter.message_parser.MessageAdapterException;
import cumulus_message_adapter.message_parser.MessageParser;
import cumulus_message_adapter.message_parser.AdapterLogger;


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

		if(exception == null || new String("").equals(exception) ||  new String("None").equals(exception) ||  new String("\"None\"").equals(exception)){
			//success
			AdapterLogger.LogInfo(CNMResponse.class.getName() + " status: SUCCESS");
			response.addProperty("status", "SUCCESS");
		}else{
			//fail
			AdapterLogger.LogWarning(CNMResponse.class.getName() + " status: FAILURE");
			response.addProperty("status", "FAILURE");

			//logic for failure types here
			JsonObject workflowException = new JsonParser().parse(exception).getAsJsonObject();

			String error = workflowException.get("Error").getAsString();
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

	public static String generateOutput(String inputCnm, String exception, JsonObject granule, JsonObject  inputConfig){
		//convert CNM to GranuleObject
		JsonElement jelement = new JsonParser().parse(inputCnm);
		JsonObject inputKey = jelement.getAsJsonObject();
		String distribute_url = inputConfig.getAsJsonPrimitive("distribution_endpoint").getAsString();

		// inputKey.remove("product");
		JsonArray files = inputKey.getAsJsonObject("product").getAsJsonArray("files");
		files.forEach( (JsonElement f) -> {
			Matcher m = getSourceBucketAndKey(f.getAsJsonObject().getAsJsonPrimitive("uri").getAsString());
			if (m.find()) {
				String sourceBucket = m.group(1);
				String key = m.group(2);
				f.getAsJsonObject().remove("uri");
				f.getAsJsonObject().addProperty("uri", distribute_url + key);
			}
		});
		// Adding granuleID into product object
		String granuleId = granule.get("granuleId").getAsString();
		inputKey.get("product").getAsJsonObject().remove("name");
		inputKey.get("product").getAsJsonObject().addProperty("name", granuleId);

		JsonObject response = getResponseObject(exception);
		inputKey.add("response", response);

		if (granule != null && response.get("status").getAsString().equals("SUCCESS")) {
			JsonObject ingestionMetadata = new JsonObject();
			ingestionMetadata.addProperty("catalogId", granule.get("cmrConceptId").getAsString());
			ingestionMetadata.addProperty("catalogUrl", granule.get("cmrLink").getAsString());
			response.add("ingestionMetadata", ingestionMetadata);
		}

		TimeZone tz = TimeZone.getTimeZone("UTC");
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
		df.setTimeZone(tz);
		String nowAsISO = df.format(new Date());

		inputKey.addProperty("processCompleteTime", nowAsISO);
		return new Gson().toJson(inputKey);
	}

	/**
	 * Parses S3 bucket and key based on regex.
	 *
	 * @param s3Path path to archived location of file
	 * @return Matcher object where group(1) is the bucket and group(2) is the key
	 */
	public static Matcher getSourceBucketAndKey(String s3Path) {
		Pattern p = Pattern.compile("s3://([^/]*)/(.*)");
		Matcher m = p.matcher(s3Path);
		return m;
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

		JsonObject granule = inputKey.get("input").getAsJsonObject().get("granules").getAsJsonArray().get(0).getAsJsonObject();

		String output = CNMResponse.generateOutput(cnm,exception, granule, inputConfig);
		String method = inputConfig.get("type").getAsString();
		String region = inputConfig.get("region").getAsString();
		AdapterLogger.LogInfo(this.className + " region:" + region + " method:" + method);
		JsonElement responseEndpoint = inputConfig.get("response-endpoint");
		if (method != null) {
			if (responseEndpoint.isJsonArray()) {
				SenderFactory.getSender(region, method).sendMessage(output, responseEndpoint.getAsJsonArray());
			} else {
				SenderFactory.getSender(region, method).sendMessage(output, responseEndpoint.getAsString());
			}
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
