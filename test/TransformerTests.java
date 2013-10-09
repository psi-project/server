import java.util.Collection;
import java.util.List;
import java.util.Vector;

import org.junit.*;
import org.junit.contrib.assumes.Assumes;
import org.junit.contrib.assumes.Corollaries;
import org.junit.runner.RunWith;

import util.ExternalResourceException;
import util.JSONValueGenerator;
import util.Schema;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * Transformer resource tests. Tests representations, application (using
 * generated values) and transformer-transformer joins.
 */
@RunWith(Corollaries.class)
public class TransformerTests extends BaseEmitterTester {

	private static List<String> transformerURIs;
	private static List<JsonObject> transformers;
	
	/**
	 * Loads all transformer URIs.
	 * Assumes success of {@link DiscoveryTests#collectionsOK()}. 
	 */
	@BeforeClass
	public static void loadTransformerURIs() {
		transformerURIs = getResourceList("transformers", true);
		transformers = null;
	}
	
	/**
	 * Tests all transformer's representations and applies them to generated
	 * values; caches the representations in {@link #transformers}.
	 */
	@Test public void transformerRepresentationAndApplication() throws ExternalResourceException {
		//Yes, tests should be independent, but use of Assumes means this will be run first
		transformers = new Vector<>(transformerURIs.size());
		for (String uri : transformerURIs) {
			JsonObject t = GET_JSON(uri);
			testTransformer(t);
			transformers.add(t);
		}
	}
	
	@Assumes("transformerRepresentationAndApplication")
	@Test public void transformerTransformerJoin() throws ExternalResourceException {
		int viableCombinations = 0;
		for (JsonObject t : transformers)
			viableCombinations += testJoinsTo(t, transformers);
		logJoinTestDetails(viableCombinations, "transformerTransformerJoin", "transformer", "transformer");
	}
	
	/** Tests joins of {@code t} with all other transformers that appear compatible. */
	private int testJoinsTo(JsonObject t, Collection<JsonObject> transformers) throws ExternalResourceException {
		int viableCombinations = 0;
		for (JsonObject s : transformers) {
			if (! Schema.isIncompatible(t.get("emits"), s.get("accepts"))) {
				viableCombinations++;
				JsonObject ts = doJoin(t, s);
				testTransformer(ts);
			}
		}
		return viableCombinations;
	}
	
	/**
	 * Tests that the given transformer representation is valid, then applies
	 * the transformer to a {@linkplain JSONValueGenerator#generate(JsonObject)
	 * generated value}.
	 * @throws ExternalResourceException if there's a problem compiling the
	 * transformer's {@code accepts} or {@code emits} schema.
	 */
	public static void testTransformer(JsonObject t) throws ExternalResourceException {
		assertValidPSIMessage(t, "transformer");
		JsonElement input = JSONValueGenerator.generate( Schema.compileToJSONSchema(t.get("accepts")) );
		testApply(getProp(t,"uri"), "value=" + input.toString(), Schema.compileToJSONSchema(t.get("emits")));
	}

}