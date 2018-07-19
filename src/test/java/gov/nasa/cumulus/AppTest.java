package gov.nasa.cumulus;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

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
}
