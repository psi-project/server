import java.util.Collection;
import java.util.List;

import org.junit.*;
import org.junit.contrib.assumes.Corollaries;
import org.junit.runner.RunWith;

import play.test.Fixtures;
import util.ExternalResourceException;
import util.Schema;

import com.google.gson.JsonObject;

import controllers.Data;

/**
 * Attribute-transformer join tests.
 */
@RunWith(Corollaries.class)
public class TransformedAttributeTests extends BaseEmitterTester {

	private static List<JsonObject> relations;
	private static List<JsonObject> transformers;
	
	/**
	 * Loads representations of all relations and all transformers.
	 * Assumes success of {@link DiscoveryTests#collectionsOK()}. 
	 */
	@BeforeClass
	public static void setupRelations() {
		Fixtures.deleteDatabase();
		Data._initialiseRelations(); //YAML is insufficient to populate the database correctly
		
		relations = getResources("relations");
		transformers = getResources("transformers");
	}
	
	/**
	 * Tests all likely-viable attribute-transformer joins.
	 * Assumes success of {@link RelationAndAttributeTests#relationsAndAttributesOK()}
	 * and {@link TransformerTests#transformerRepresentationAndApplication()}. 
	 */
	@Test public void attributeTransformerJoins() throws ExternalResourceException {
		logJoinTestDetails( testAllAttributeTransformerJoins(relations, transformers), "attributeTransformerJoins", "attribute", "transformer" );
	}

	/**
	 * Tests all likely-viable joins between attributes of the given relations
	 * and the given transformers or predictors. Returns the number of
	 * combinations tested.
 	 * Assumes success of {@link RelationAndAttributeTests#relationsAndAttributesOK()}
	 * and {@link TransformerTests#transformerRepresentationAndApplication()}. 
	 */
	public static int testAllAttributeTransformerJoins(Collection<JsonObject> attributesOf, Collection<JsonObject> others) throws ExternalResourceException {
		if (others.size() == 0) return 0;
		int viableCombinations = 0;
		for (JsonObject relation : attributesOf) {
			for (JsonObject a : getResources(jsonArrayToStringList(relation.get("attributes"))) ) {
				for (JsonObject s : others) {
					if (! Schema.isIncompatible(a.get("emits"), s.get("accepts"))) {
						viableCombinations++;
						JsonObject transformed = doJoin(a, s);
						RelationAndAttributeTests.testApplyAttribute(transformed, relation);
					}
				}
			}
		}
		return viableCombinations;
	}
	
}