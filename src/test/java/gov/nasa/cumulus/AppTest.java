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
import com.google.gson.JsonArray;
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

    /**
     * Rigourous Test :-)
     */
    public void testApp()
    {


    	String cnm = "{"
    	+ "  \"version\": \"v1.0\","
    	+ "  \"provider\": \"PODAAC_SWOT\","
    	+ "  \"collection\": \"SWOT_Prod_l2:1\","
    	+ "  \"deliveryTime\":\"2017-09-30T03:42:29.791198\","
    	+ "  \"identifier\": \"1234-abcd-efg0-9876\","
    	+ "  \"product\": {"
    	+ "    \"files\": ["
    	+ "      { \"size\":53205914864} ]"

    	+ "  }"
    	+ "}";

    	String output = CNMResponse.generateOutput(cnm, "");
    	System.out.println(output);
        assertNotNull(output);
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

    }
    
    /**
     * this is not portable! relies on the default profile for AWS connectivity. 
     */
    public void testSNS(){
    	
    	try{
    		CNMResponse.sendMessageSNS("testMessage","us-west-2", "badTopic");
    		fail("Should have failed with invalid topic");
    	}catch(Exception e){}
    	
    	AmazonSNS snsClient = AmazonSNSClientBuilder.standard().withRegion("us-west-2").build();
    	
    	final CreateTopicRequest createTopicRequest = new CreateTopicRequest("MyTopic-" + UUID.randomUUID().toString());
    	final CreateTopicResult createTopicResponse = snsClient.createTopic(createTopicRequest);

    	final String topicARN = createTopicResponse.getTopicArn();
    	
    	// Print the topic ARN.
    	System.out.println("TopicArn:" + topicARN);
    	
    	try{
    		CNMResponse.sendMessageSNS("testMessage","us-west-2", topicARN);
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
	}
}
