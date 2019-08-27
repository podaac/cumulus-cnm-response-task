package gov.nasa.cumulus;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.TimeZone;
import java.util.Base64.Decoder;

import org.apache.commons.io.IOUtils;

import com.amazonaws.regions.RegionUtils;
import com.amazonaws.services.kinesis.AmazonKinesis;
import com.amazonaws.services.kinesis.AmazonKinesisClient;
import com.amazonaws.services.kinesis.model.PutRecordRequest;
import com.amazonaws.services.kinesis.model.PutRecordResult;
import com.amazonaws.services.kinesis.model.Record;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.AmazonSNSClientBuilder;
import com.amazonaws.services.sns.model.PublishRequest;
import com.amazonaws.services.sns.model.PublishResult;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonParseException;

import cumulus_message_adapter.message_parser.ITask;
import cumulus_message_adapter.message_parser.MessageAdapterException;
import cumulus_message_adapter.message_parser.MessageParser;


public class CNMResponse implements  ITask, RequestHandler<String, String>{

	public enum ErrorCode {VALIDATION_ERROR, TRANSFER_ERROR, PROCESSING_ERROR};

	public static void main( String[] args ) throws Exception
    {
		CNMResponse c = new CNMResponse();

		String input = "{"
			 +" \"version\":\"v1.0\","
			 +" \"provider\": \"PODAAC\","
			  +"\"deliveryTime\":\"2018-03-12T16:50:23.458100\","
			  +"\"collection\": \"L2_HR_LAKE_AVG\","
			  +"\"identifier\": \"c5c828ac328c97b5d3d1036d08898b30-12\","
			  +"\"product\":"
			  +"  {"
			  +"    \"name\": \"L2_HR_LAKE_AVG/product_0001-of-0019.h5\","
			  +"    \"dataVersion\": \"1\","
			  +"    \"files\": ["
			  +"      {"
			  +"        \"type\": \"data\","
			  +"        \"uri\": \"s3://podaac-dev-cumulus-test-input/L2_HR_LAKE_AVG/product_0001-of-0019.h5\","
			  +"        \"name\":\"product_0001-of-0019.h5\","
			  +"        \"checksumType\": \"md5\","
			  +"        \"checksum\": \"123454321abc\","
			  +"        \"size\": 96772640"
			  +"      }"
			  +"    ]"
			  +"  }"
			  +"},"
			  + "\"config\": {}"
			  +"}";

		String output = CNMResponse.generateOutput(input, null);
		System.out.println(output);
    }


	public String handleRequest(String input, Context context) {
		MessageParser parser = new MessageParser();
		try
		{
			return parser.RunCumulusTask(input, context, new CNMResponse());
		}
		catch(MessageAdapterException e)
		{
			return e.getMessage();
		}
	}

	public void handleRequestStreams(InputStream inputStream, OutputStream outputStream, Context context) throws IOException {

		MessageParser parser = new MessageParser();

		try
		{
			String input =IOUtils.toString(inputStream, "UTF-8");
			context.getLogger().log(input);
			String output = parser.RunCumulusTask(input, context, new CNMResponse());
			System.out.println("Output: " + output);
			outputStream.write(output.getBytes(Charset.forName("UTF-8")));
		}
		catch(MessageAdapterException e)
		{
			e.printStackTrace();
			outputStream.write(e.getMessage().getBytes(Charset.forName("UTF-8")));
		}

	}


	public void handleRequestStreams2(InputStream inputStream, OutputStream outputStream, Context context) throws IOException {

		MessageParser parser = new MessageParser();


			String input =IOUtils.toString(inputStream, "UTF-8");
			context.getLogger().log(input);
			CNMResponse cnmresponse = new CNMResponse();
			try{
				String output = cnmresponse.PerformFunction(input, context);
				System.out.println("Output: " + output);
			outputStream.write(output.getBytes(Charset.forName("UTF-8")));
			}catch(Exception e){
				e.printStackTrace();
			}


	}

	public static JsonObject getResponseObject(String exception) {
		JsonObject response = new JsonObject();

		if(exception == null || new String("").equals(exception) ||  new String("None").equals(exception) ||  new String("\"None\"").equals(exception)){
			//success
			response.addProperty("status", "SUCCESS");
		}else{
			//fail
			response.addProperty("status", "FAILURE");

			//logic for failure types here
			JsonObject workflowException = new JsonParser().parse(exception).getAsJsonObject();

			String error = workflowException.get("Error").getAsString();
			switch(error) {
				case "FileNotFound":
					response.addProperty("errorCode", ErrorCode.TRANSFER_ERROR.toString());
					break;
				case "InvalidChecksum":
					response.addProperty("errorCode", ErrorCode.VALIDATION_ERROR.toString());
					break;
				default:
					response.addProperty("errorCode", ErrorCode.PROCESSING_ERROR.toString());
			}

			String causeString = workflowException.get("Cause").getAsString();
			try {
				JsonObject cause = new JsonParser().parse(causeString).getAsJsonObject();
				response.addProperty("errorMessage", cause.get("errorMessage").getAsString());
			} catch (JsonParseException e) {
				response.addProperty("errorMessage", causeString);
			}
		}
		return response;
	}

	public static String generateOutput(String inputCnm, String exception){
		//convert CNM to GranuleObject
		JsonElement jelement = new JsonParser().parse(inputCnm);
		JsonObject inputKey = jelement.getAsJsonObject();

		JsonElement sizeElement = inputKey.get("product").getAsJsonObject().get("files").getAsJsonArray().get(0).getAsJsonObject().get("size");

		inputKey.add("productSize", sizeElement);
		inputKey.remove("product");
		inputKey.add("response", getResponseObject(exception));

		TimeZone tz = TimeZone.getTimeZone("UTC");
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
		df.setTimeZone(tz);
		String nowAsISO = df.format(new Date());

		inputKey.addProperty("processCompleteTime", nowAsISO);
		return new Gson().toJson(inputKey);
	}

	/**
	 * @param response The message to send to the kinesis stream
	 * @param region an AWS region, probably us-west-2 or us-east-1
	 * @param topicArn The SNS topic ARN to which the message should be sent
	 */
	public static void sendMessageSNS(String response, String region, String topicArn){
		AmazonSNS snsClient = AmazonSNSClientBuilder.standard().withRegion(region).build();
		final PublishRequest publishRequest = new PublishRequest(topicArn, response);
		/*final PublishResult publishResponse =*/ snsClient.publish(publishRequest);
	}
	
	/**
	 * @param response The message to send to the kinesis stream
	 * @param region an AWS region, probably us-west-2 or us-east-1
	 * @param streamName - the stream name, not ARN, of the kinesis stream
	 */
	public static void sendMessageKinesis(String response, String region, String streamName) {

        //AWSCredentials credentials = CredentialUtils.getCredentialsProvider().getCredentials();
        AmazonKinesis kinesisClient = new AmazonKinesisClient();
        kinesisClient.setRegion(RegionUtils.getRegion(region));

	    //byte[] bytes = new Gson().toJson(response).getBytes();
	    //we already have the json as a string, so we don't need the above command to re-string it.
        byte[] bytes = response.getBytes();

	    if (bytes == null) {
	        return;
	    }


	    PutRecordRequest putRecord = new PutRecordRequest();
	    putRecord.setStreamName(streamName);
	    putRecord.setPartitionKey("1");
	    putRecord.setData(ByteBuffer.wrap(bytes));
	    kinesisClient.putRecord(putRecord);
	}


        public String getError(JsonObject input, String key){

		String exception = null;
		System.out.println("WorkflowException: " + input.get(key));

		if(input.get(key) != null){
			System.out.println("Step 3.5");
			exception = input.get(key).toString();
		}
		return exception;
	}

	//inputs
	// OriginalCNM
	// CNMResponseStream
	// WorkflowException
	// region
	public String PerformFunction(String input, Context context) throws Exception {

		System.out.println("Processing " + input);

		JsonElement jelement = new JsonParser().parse(input);
		JsonObject inputKey = jelement.getAsJsonObject();


		JsonObject  inputConfig = inputKey.getAsJsonObject("config");
		String cnm = new Gson().toJson(inputConfig.get("OriginalCNM"));
		String exception = getError(inputConfig, "WorkflowException");


		String output = CNMResponse.generateOutput(cnm,exception);
		String method = inputConfig.get("type").getAsString();
		String region = inputConfig.get("region").getAsString();
		String endpoint = inputConfig.get("response-endpoint").getAsString();
		
		/*
		 * This needs to be refactored into a factory taking 'type' as an input
		 */
		if(method != null && method.equals("kinesis")){
			CNMResponse.sendMessageKinesis(output, region, endpoint);
		}else if(method != null && method.equals("sns")){
			CNMResponse.sendMessageSNS(output, region, endpoint);
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
