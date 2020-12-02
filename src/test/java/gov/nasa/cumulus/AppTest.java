package gov.nasa.cumulus;

import com.amazonaws.services.sns.model.NotFoundException;
import com.google.gson.*;
import gov.nasa.cumulus.bo.MessageAttribute;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import com.amazonaws.services.sns.AmazonSNS;
import org.apache.http.client.utils.URIBuilder;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import java.net.URI;
import java.nio.file.Files;
import java.util.Map;
import java.util.Scanner;
import java.util.UUID;
import java.io.File;
import java.io.IOException;


/**
 * Unit test for simple App.
 */
public class AppTest
    extends TestCase implements IConstants
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

    public void testError() throws Exception{

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
		JsonElement product = outputElement.getAsJsonObject().get("product");
		assertNotSame("SUCCESS", response.get("status").getAsString());
		assertEquals("FAILURE", response.get("status").getAsString());
		assertEquals(product, null);
    }
    
    /**
     * this portable as of PODAAC-2549; we now mock AWS using Mockito
     */
    public void testSNS(){
        // Test and make sure the factory returns the correct sender class
        Sender sender = SenderFactory.getSender("us-west-2", "sns");
        if(!(sender instanceof SNSSender)) {
            fail("Sender class should be SNSSender");
        }
        // configure our mocks for aws
        AmazonSNS snsClient = Mockito.mock(AmazonSNS.class);
        // create our 'valid topic'
        final String topicARN = "MyTopic-" + UUID.randomUUID().toString();
        // Print the topic ARN.
        System.out.println("TopicArn:" + topicARN);

        // configure our mocks for SNSSender now, using our mocked aws
        sender = new SNSSender("us-west-2", snsClient);
        // configure our mock for aws
        Mockito.doThrow(NotFoundException.class).when(snsClient).publish(
                ArgumentMatchers.argThat(p -> !p.getTopicArn().equalsIgnoreCase(topicARN)));
        // now test sending with a bad topic
        try {
            sender.sendMessage("testMessage", "badTopic");
    		fail("Should have failed with invalid topic");
    	}catch(Exception e){}

        // finally, test with our valid topic
        try {
            sender.sendMessage("testMessage", topicARN);
        } catch(Exception e) {
            e.printStackTrace();
            fail("Should not have failed with valid topic");
        }
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
	public void testSuccessCnm() throws Exception{
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
		JsonArray files = product.get("files").getAsJsonArray();
		assertEquals("1.0", product.get("dataVersion").getAsString());
		assertEquals("Merged_TOPEX_Jason_OSTM_Jason-3_Cycle_945.V4_2", product.get("name").getAsString());
		assertEquals("https://te31m541y2.execute-api.us-west-2.amazonaws.com:9001/DEV/test-protected/Merged_TOPEX_Jason_OSTM_Jason-3_Cycle_945.V4_2.nc",
				files.get(0).getAsJsonObject().getAsJsonPrimitive("uri").getAsString());
		assertEquals("https://te31m541y2.execute-api.us-west-2.amazonaws.com:9001/DEV/test-public/Merged_TOPEX_Jason_OSTM_Jason-3_Cycle_945.V4_2.cmr.json",
				files.get(1).getAsJsonObject().getAsJsonPrimitive("uri").getAsString());
		// product.name should be the granuleId
		assertEquals("Merged_TOPEX_Jason_OSTM_Jason-3_Cycle_945.V4_2", product.get("name").getAsString());

		JsonObject ingestionMetadata = response.get("ingestionMetadata").getAsJsonObject();
		assertNotNull(ingestionMetadata);
		assertEquals("G1234313662-POCUMULUS", ingestionMetadata.get("catalogId").getAsString());
		assertEquals("https://cmr.uat.earthdata.nasa.gov/search/granules.json?concept_id=G1234313662-POCUMULUS", ingestionMetadata.get("catalogUrl").getAsString());
	}

	public void testBuildMessageAttributesHash() {
		CNMResponse cnmResponse = new CNMResponse();
		Map<String, MessageAttribute> attributeBOMap =  cnmResponse.buildMessageAttributesHash("JASON_C1", "E","SUCCESS");
		MessageAttribute collectionBO = attributeBOMap.get(this.COLLECTION_SHORT_NAME_ATTRIBUTE_KEY);
		MessageAttribute statusBO = attributeBOMap.get(this.CNM_RESPONSE_STATUS_ATTRIBUTE_KEY);
		MessageAttribute dataVersionBO = attributeBOMap.get(this.DATA_VERSION);
		assertEquals(MessageFilterTypeEnum.String.name(), collectionBO.getType());
		assertEquals("JASON_C1", collectionBO.getValue());
		assertEquals(MessageFilterTypeEnum.String.name(), statusBO.getType());
		assertEquals("SUCCESS", statusBO.getValue());
		assertEquals(MessageFilterTypeEnum.String.name(), dataVersionBO.getType());
		assertEquals("E", dataVersionBO.getValue());
	}

	/**
	 * This unit test case to prove Apache URIBuilder is slash "/" safe when concatenate URI
	 * @throws Exception
	 */
	public void testApacheURIbuilder() throws  Exception{
		String uriString = null;
		URIBuilder uriBuilder = null;
		URI uri = null;
		// distribution_endpoint does not end with slash and key starts with slash
		uriBuilder = new URIBuilder("http://distribution-uri:9000/DEV");
		uri = uriBuilder.setPath(uriBuilder.getPath() + "/protected-bucket/granule_id.nc")
				.build()
				.normalize();
		uriString = uri.toString();
		assertEquals(uriString,"http://distribution-uri:9000/DEV/protected-bucket/granule_id.nc");

		// distribution_endpoint does end with slash and key starts with slash (double slash case)
		uriBuilder = new URIBuilder("http://distribution-uri:9000/DEV/");
		uri = uriBuilder.setPath(uriBuilder.getPath() + "/protected-bucket/granule_id.nc")
				.build()
				.normalize();
		uriString = uri.toString();
		assertEquals(uriString,"http://distribution-uri:9000/DEV/protected-bucket/granule_id.nc");

		// distribution_endpoint does not end with slash and key does not start with double slash (double slash case)
		uriBuilder = new URIBuilder("http://distribution-uri:9000/DEV");
		uri = uriBuilder.setPath(uriBuilder.getPath() + "//protected-bucket/granule_id.nc")
				.build()
				.normalize();
		uriString = uri.toString();
		assertEquals(uriString,"http://distribution-uri:9000/DEV/protected-bucket/granule_id.nc");

	}
}
