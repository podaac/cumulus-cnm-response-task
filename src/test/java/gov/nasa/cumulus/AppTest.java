package gov.nasa.cumulus;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.Scanner;
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

}
