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
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import cumulus_message_adapter.message_parser.ITask;
import cumulus_message_adapter.message_parser.MessageAdapterException;
import cumulus_message_adapter.message_parser.MessageParser;


public class CNMResponse implements  ITask, RequestHandler<String, String>{

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

	/*
	 *
{
  "version":"v1.0",
  "provider": "PODAAC",
  "deliveryTime":"2018-03-12T16:50:23.458100",
  "collection": "L2_HR_LAKE_AVG",
  "identifier": ""c5c828ac328c97b5d3d1036d08898b30-12"",
  "product":
    {
      "name": "L2_HR_LAKE_AVG/product_0001-of-0019.h5",
      "dataVersion": "1",
      "files": [
        {
          "type": "data",
          "uri": "s3://podaac-dev-cumulus-test-input/L2_HR_LAKE_AVG/product_0001-of-0019.h5",
          "name":"product_0001-of-0019.h5",
          "checksumType": "md5",
          "checksum": "123454321abc",
          "size": 96772640
        }
      ]
    }
}
	 */


	/*
	 * {
  "version": "v1.0",
  "provider": "PODAAC_SWOT",
  "collection": "SWOT_Prod_l2:1",
  "processCompleteTime":"2017-09-30T03:45:29.791198",
  "receivedTime":"2017-09-30T03:42:31.634552",
  "deliveryTime":"2017-09-30T03:42:29.791198",
  "identifier": "1234-abcd-efg0-9876",
  "response": {
    "status":"SUCCESS"
  }
}
	 */
	public static String generateOutput(String inputCnm, String exception){
    	//+ "  \"processCompleteTime\":\"2017-09-30T03:45:29.791198\","
    	//+ "  \"receivedTime\":\"2017-09-30T03:42:31.634552\","
		//convert CNM to GranuleObject
		JsonElement jelement = new JsonParser().parse(inputCnm);
		JsonObject inputKey = jelement.getAsJsonObject();

		JsonObject response = new JsonObject();

		if(exception == null || new String("").equals(exception) ||  new String("None").equals(exception)){
			//success
			response.addProperty("status", "SUCCESS");
		}else{
			//fail
			response.addProperty("status", "FAILURE");

			//logic for failure types here
		}

		JsonElement sizeElement = inputKey.get("product").getAsJsonObject().get("files").getAsJsonArray().get(0).getAsJsonObject().get("size");

		inputKey.add("productSize", sizeElement);
		inputKey.remove("product");
		inputKey.add("response", response);

		TimeZone tz = TimeZone.getTimeZone("UTC");
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
		df.setTimeZone(tz);
		String nowAsISO = df.format(new Date());

		inputKey.addProperty("processCompleteTime", nowAsISO);
		return new Gson().toJson(inputKey);
	}

	private static void sendMessage(String response, String region, String streamName) {

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
		System.out.println("WorkflowException:" + input.get(key));

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
		System.out.println("Step 2");

		String cnm = new Gson().toJson(inputConfig.get("OriginalCNM"));
		System.out.println("Step 3");

		String exception = getError(inputConfig, "WorkflowException");

		/*if(inputKey.get("WorkflowException") != null){
			System.out.println("Step 3.5");
			exception = inputKey.get("WorkflowException").getAsString();
		}*/

		System.out.println("Step 4");
		System.out.println("Exception" + exception);

		String output = CNMResponse.generateOutput(cnm,exception);
		System.out.println("Step 5");
		System.out.println("got: " + output);


		String region = inputConfig.get("region").getAsString();
		String cnmResponseStream = inputConfig.get("CNMResponseStream").getAsString();
		CNMResponse.sendMessage(output, region, cnmResponseStream);

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
