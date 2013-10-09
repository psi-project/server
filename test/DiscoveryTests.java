import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.*;
import org.junit.contrib.assumes.Assumes;
import org.junit.contrib.assumes.Corollaries;
import org.junit.runner.RunWith;

import com.google.gson.JsonObject;

/**
 * Tests of the service entry point and collection resources. Note that tests
 * that are not run because one or more of their dependencies failed will not
 * be shown on Play's testing web interface, but will if running tests through
 * JUnit directly.
 */
@RunWith(Corollaries.class)
public class DiscoveryTests extends PSIFunctionalTest {
	
	private static final List<String> EXPECTED_COLLECTIONS = Arrays.asList( new String[]{ "schema", "relations", "learners", "predictors", "transformers" } );
	
	@Test public void rootOK() {
		JsonObject json  = GET_JSON("/");
		testAllCollectionsPresent(json);
		assertValidPSIMessage(json, "service");
	}
	
	/**
	 * Subordinate test: passes if all possible collections are present in
	 * service description; if implementation is changed to make remove some
	 * collections then do not call this from {@link #rootOK()}.
	 */
	private void testAllCollectionsPresent(JsonObject json) {
		List<String> missing = new ArrayList<>( EXPECTED_COLLECTIONS );
		for (String name : EXPECTED_COLLECTIONS)
			if (json.has(name))
				missing.remove(name);
		assertTrue("Service as currently implemented should have all collection resources present. Missing: " + prettyList(missing), missing.isEmpty());
	}

	@Assumes("rootOK")
	/** Browser-based GET on root should fall back to HTML. */
	@Test public void rootHTMLFallback() {
		okHTMLResponse( GET("/") );
	}

	@Assumes("rootOK")
	@Test public void collectionsOK() {
		JsonObject json = GET_JSON("/");
		for (String name : new String[]{"schema", "relations", "learners", "predictors", "transformers"}) {
			String uri = getProp(json, name);
			testCollection(uri);
			testCollectionHTMLFallback(uri);
		}
	}

	private void testCollection(final String uri) {
		if (uri != null) {
			JsonObject json = GET_JSON(uri);
			assertValidPSIMessage(json, "resource-list");
		}
	}
	
	private void testCollectionHTMLFallback(final String uri) {
		if (uri != null)
			okHTMLResponse( GET(makeRelative(uri)) );
	}

}