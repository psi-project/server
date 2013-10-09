import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.*;
import org.junit.contrib.assumes.Assumes;
import org.junit.contrib.assumes.Corollaries;
import org.junit.runner.RunWith;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * Schema resource tests. Far from exhaustive, in that it doesn't test that
 * every predefined schema template can be completed correctly, but should be
 * sufficient.
 */
@RunWith(Corollaries.class)
public class SchemaTests extends PSIFunctionalTest {
	
	/** Names of predefined schema in version 2 spec. */
	private static List<String> PREDEFINED_SCHEMA_NAMES = Collections.unmodifiableList(
			Arrays.asList(new String[]{
					"integer",
					"number",
					"boolean",
					"string",
					"object",
					"array",
					"atomicValue",
					"atomicValueSchema",
					"numberSchema",
					"nominalValueSchema",
					"uri",
					"richValueSchema",
					"relation",
					"attribute",
					"arrayAttribute",
					"numberAttribute",
					"fixedAttribute",
					"nominalAttribute",
					"atomicAttribute",
					"richValueAttribute"
			}
	));
	
	private static String schemaRootURI;
	
	/**
	 * Determines the schema root URI. Assumes success of
	 * {@link DiscoveryTests#rootOK()}.
	 */
	@BeforeClass
	public static void loadSchemaRoot() {
		schemaRootURI = makeRelative( getProp(GET_JSON("/"), "schema") ).replaceAll("\\/$", "");
	}
	
	/**
	 * Tests all predefined schema are present.
	 * Assumes success of {@link DiscoveryTests#collectionsOK()}.
	 */
	@Test public void testPredefinedSchema() {
		JsonObject schemaRoot = GET_JSON(schemaRootURI);
		List<String> missing = new ArrayList<>( PREDEFINED_SCHEMA_NAMES );
		for (JsonElement el : schemaRoot.get("resources").getAsJsonArray())
			missing.remove( el.getAsString().replaceFirst(".*\\/", "") );
		assertTrue("Not all predefined schema are present. Missing: " + prettyList(missing), missing.isEmpty());
	}

	@Assumes("testPredefinedSchema")
	@Test public void testInteger() {
		testPredefinedSchema("integer", "{'type':'integer'}");
		testPredefinedSchema("integer?template=true",
			"{'type':'integer','default':'%default','minimum':'%min','maximum':'%max'}");
		testPredefinedSchema("integer?min=2&max=5&description=\"2To5\"",
			"{'type':'integer','minimum':2,'maximum':5,'description':'2To5'}");
	}
	
	@Assumes("testPredefinedSchema")
	@Test public void testNominalAttribute() {
		testPredefinedSchema("nominalAttribute?template=true",
				"{'allOf':['$attribute'],'/emits':{'/enum':{'$array':{'allItems':'%allItems'}}}}");
		testPredefinedSchema("nominalAttribute?allItems=\"$string\"",
				"{'allOf':['$attribute'],'/emits':{'/enum':{'$array':{'allItems':'$string'}}}}");

	}
	
	private void testPredefinedSchema(String uriPart, String singleQuotedTestJSON) {
		assertExpectedJSON( GET_JSON( schemaRootURI + '/' + uriPart), singleQuotedTestJSON );
	}

}