package gov.nasa.cumulus;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.AmazonSNSClientBuilder;
import com.amazonaws.services.sns.model.CreateTopicRequest;
import com.amazonaws.services.sns.model.CreateTopicResult;
import com.amazonaws.services.sns.model.DeleteTopicRequest;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.nio.file.Files;
import java.util.Scanner;
import java.util.UUID;
import java.io.File;
import java.io.IOException;


/**
 * Unit test for simple App.
 */
public class AppTest
    extends TestCase
{
    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public AppTest( String testName )
    {
        super( testName );
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite()
    {
        return new TestSuite( AppTest.class );
    }

    public void testError() throws IOException{

      StringBuilder sb = new StringBuilder();
      Scanner scanner  = new Scanner(new File(getClass().getClassLoader().getResource("workflow.error.json").getFile()));
      //String text = new Scanner(ClassLoader.getSystemResource("workflow.error.json")).useDelimiter("\\A").next();
      while (scanner.hasNextLine()) {
			     String line = scanner.nextLine();
			     sb.append(line).append("\n");
		  }

		  scanner.close();
      String text = sb.toString();
      System.out.println("Processing " + text);

  		JsonElement jelement = new JsonParser().parse(text);
  		JsonObject inputKey = jelement.getAsJsonObject();

      JsonObject  inputConfig = inputKey.getAsJsonObject("config");

      CNMResponse cnm = new CNMResponse();
      String ex = cnm.getError(inputConfig, "WorkflowException");
      System.out.println("Exception: " + ex);
      assertNotNull(ex);
      // Further testing with generateOutput
      JsonObject granule = inputKey.get("input").getAsJsonObject().get("granules").getAsJsonArray().get(0).getAsJsonObject();
      String output = CNMResponse.generateOutput(new Gson().toJson(inputConfig.get("OriginalCNM")), ex, granule, inputConfig);
		JsonElement outputElement = new JsonParser().parse(output);
		JsonObject response = outputElement.getAsJsonObject().get("response").getAsJsonObject();
		JsonObject product = outputElement.getAsJsonObject().get("product").getAsJsonObject();
		assertNotSame("SUCCESS", response.get("status").getAsString());
		assertEquals("FAILURE", response.get("status").getAsString());
		assertEquals("1.0", product.get("dataVersion").getAsString());
		assertEquals(1, product.get("files").getAsJsonArray().size());
		assertEquals("L1B_HR_SLC_product_0001-of-4154", product.get("name").getAsString());

    }
    
    /**
     * this is not portable! relies on the default profile for AWS connectivity. 
     */
    public void testSNS(){
    	
    	try{
			SenderFactory.getSender("us-west-2", "sns").sendMessage("testMessage", "badTopic");
    		fail("Should have failed with invalid topic");
    	}catch(Exception e){}
    	
    	AmazonSNS snsClient = AmazonSNSClientBuilder.standard().withRegion("us-west-2").build();
    	
    	final CreateTopicRequest createTopicRequest = new CreateTopicRequest("MyTopic-" + UUID.randomUUID().toString());
    	final CreateTopicResult createTopicResponse = snsClient.createTopic(createTopicRequest);

    	final String topicARN = createTopicResponse.getTopicArn();
    	
    	// Print the topic ARN.
    	System.out.println("TopicArn:" + topicARN);
    	
    	try{
			SenderFactory.getSender("us-west-2","sns").sendMessage("testMessage", topicARN);
    	}catch(Exception e){
    		e.printStackTrace();
    		fail("Should not have failed with valid topic");
    	}
    	
    	final DeleteTopicRequest deleteTopicRequest = new DeleteTopicRequest(topicARN);
    	snsClient.deleteTopic(deleteTopicRequest);
    }

    public void testErrorCode() {
    	String fileNotFound = "{\n" +
				"    \"Error\": \"FileNotFound\",\n" +
				"    \"Cause\": \"{\\\"errorMessage\\\":\\\"Source file not found s3://podaac-sndbx-cumulus-test-input/L2_HR_PIXC/SWOT_L2_HR_PIXC_001_005_012R_20210612T000000_20210612T000022_PGA2_03.nc.h5\\\",\\\"errorType\\\":\\\"FileNotFound\\\",\\\"stackTrace\\\":[\\\"S3Granule.sync (/var/task/index.js:127702:13)\\\",\\\"<anonymous>\\\",\\\"process._tickDomainCallback (internal/process/next_tick.js:228:7)\\\"]}\"\n" +
				"  }";

		JsonObject responseFileNotFound = CNMResponse.getResponseObject(fileNotFound);
		assertEquals("FAILURE", responseFileNotFound.get("status").getAsString());
		assertEquals(CNMResponse.ErrorCode.TRANSFER_ERROR.toString(), responseFileNotFound.get("errorCode").getAsString());

		String invalidChecksum = "{\n" +
				"    \"Error\": \"InvalidChecksum\",\n" +
				"    \"Cause\": \"{\\\"errorMessage\\\":\\\"Invalid checksum for S3 object s3://podaac-sndbx-cumulus-internal/file-staging/podaac-sndbx-cumulus/L2_HR_PIXC___1/SWOT_L2_HR_PIXC_001_005_012R_20210612T000000_20210612T000022_PGA2_03.nc with type md5 and expected sum 4719793a1005470ac6744643cbe27b6cdd\\\",\\\"errorType\\\":\\\"InvalidChecksum\\\",\\\"stackTrace\\\":[\\\"Object.module.exports.../../packages/common/aws.js.exports.validateS3ObjectChecksum (/var/task/index.js:1695:9)\\\",\\\"<anonymous>\\\",\\\"process._tickDomainCallback (internal/process/next_tick.js:228:7)\\\"]}\"\n" +
				"  }";

		JsonObject responseInvalidChecksum = CNMResponse.getResponseObject(invalidChecksum);
		assertEquals("FAILURE", responseInvalidChecksum.get("status").getAsString());
		assertEquals(CNMResponse.ErrorCode.VALIDATION_ERROR.toString(), responseInvalidChecksum.get("errorCode").getAsString());

		String notFound = "{\n" +
				"    \"Error\": \"An error occurred (404) when calling the HeadObject operation: Not Found\",\n" +
				"    \"Cause\": \"An error occurred (404) when calling the HeadObject operation: Not Found\"\n" +
				"  }";

		JsonObject responseNotFound = CNMResponse.getResponseObject(notFound);
		assertEquals("FAILURE", responseNotFound.get("status").getAsString());
		assertEquals(CNMResponse.ErrorCode.PROCESSING_ERROR.toString(), responseNotFound.get("errorCode").getAsString());
		assertEquals("An error occurred (404) when calling the HeadObject operation: Not Found", responseNotFound.get("errorMessage").getAsString());

		String remoteResourceError = "{\n" +
				"    \"Error\": \"RemoteResourceError\",\n" +
				"    \"Cause\": \"Placeholder for RemoteResourceError message\"\n" +
				"  }";

		JsonObject responseRemoteResourceError = CNMResponse.getResponseObject(remoteResourceError);
		assertEquals("FAILURE", responseRemoteResourceError.get("status").getAsString());
		assertEquals(CNMResponse.ErrorCode.TRANSFER_ERROR.toString(), responseRemoteResourceError.get("errorCode").getAsString());
		assertEquals("Placeholder for RemoteResourceError message", responseRemoteResourceError.get("errorMessage").getAsString());

		String connectionTimeout = "{\n" +
				"    \"Error\": \"ConnectionTimeout\",\n" +
				"    \"Cause\": \"Placeholder for ConnectionTimeout message\"\n" +
				"  }";

		JsonObject responseConnectionTimeout = CNMResponse.getResponseObject(connectionTimeout);
		assertEquals("FAILURE", responseConnectionTimeout.get("status").getAsString());
		assertEquals(CNMResponse.ErrorCode.TRANSFER_ERROR.toString(), responseConnectionTimeout.get("errorCode").getAsString());
		assertEquals("Placeholder for ConnectionTimeout message", responseConnectionTimeout.get("errorMessage").getAsString());

		String unexpectedFileSize = "{\n" +
				"    \"Error\": \"UnexpectedFileSize\",\n" +
				"    \"Cause\": \"Placeholder for UnexpectedFileSize message\"\n" +
				"  }";

		JsonObject responseUnexpectedFileSize = CNMResponse.getResponseObject(unexpectedFileSize);
		assertEquals("FAILURE", responseUnexpectedFileSize.get("status").getAsString());
		assertEquals(CNMResponse.ErrorCode.VALIDATION_ERROR.toString(), responseUnexpectedFileSize.get("errorCode").getAsString());
		assertEquals("Placeholder for UnexpectedFileSize message", responseUnexpectedFileSize.get("errorMessage").getAsString());
	}

	/**
	 * Test success CNM response
	 */
	public void testSuccessCnm() {
		ClassLoader classLoader = getClass().getClassLoader();
		File inputJsonFile = new File(classLoader.getResource("workflow.success.json").getFile());

		String input = "";
		try {
			input = new String(Files.readAllBytes(inputJsonFile.toPath()));
		} catch (IOException e) {
			e.printStackTrace();
		}

		JsonElement jelement = new JsonParser().parse(input);
		JsonObject inputKey = jelement.getAsJsonObject();

		JsonObject  inputConfig = inputKey.getAsJsonObject("config");
		String cnm = new Gson().toJson(inputConfig.get("OriginalCNM"));

		JsonObject granule = inputKey.get("input").getAsJsonObject().get("granules").getAsJsonArray().get(0).getAsJsonObject();

		String output = CNMResponse.generateOutput(cnm, null, granule, inputConfig);
		JsonElement outputElement = new JsonParser().parse(output);
		JsonObject response = outputElement.getAsJsonObject().get("response").getAsJsonObject();
		JsonObject product = outputElement.getAsJsonObject().get("product").getAsJsonObject();
		assertEquals("SUCCESS", response.get("status").getAsString());
		assertEquals("1.0", product.get("dataVersion").getAsString());
		assertEquals(2, product.get("files").getAsJsonArray().size());
		// product.name should be the granuleId
		assertEquals("Merged_TOPEX_Jason_OSTM_Jason-3_Cycle_945.V4_2", product.get("name").getAsString());

		JsonObject ingestionMetadata = response.get("ingestionMetadata").getAsJsonObject();
		assertNotNull(ingestionMetadata);
		assertEquals("G1234313662-POCUMULUS", ingestionMetadata.get("catalogId").getAsString());
		assertEquals("https://cmr.uat.earthdata.nasa.gov/search/granules.json?concept_id=G1234313662-POCUMULUS", ingestionMetadata.get("catalogUrl").getAsString());
	}
}
