import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import models.attribute.Attribute;

import org.junit.*;
import org.junit.contrib.assumes.Assumes;
import org.junit.contrib.assumes.Corollaries;
import org.junit.runner.RunWith;

import play.mvc.Http;
import play.mvc.Http.Response;
import play.test.Fixtures;
import util.ExternalResourceException;
import util.JSONValueGenerator;
import util.Schema;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import controllers.Data;

/**
 * Relation and attribute resource tests. Many individual tests actually test
 * several distinct behaviours, but since these behaviours require us to fetch
 * information from the server first, it's simpler to combine them into one.
 * Also, the required set up (clearing and repopulating the database) is so
 * costly it's better to do it fewer times.
 * <p>
 * <strong>Tests that have been made less strict:</strong>
 * <ol>
 *   <li>Attributes may emit {@code null} values to represent missing values,
 *   but are not required to express this in their schema (i.e., the schema do
 *   not describe a union of some JSON type and {@code null}). When attribute
 *   results are tested, {@code null} values are replaced with generated values
 *   matching the expected schema so that they will pass strict validation.
 *   There is a risk that this will cause misleading failures if the generated
 *   value is, for some reason, not valid against the schema.</li>
 * </ol>
 */
@RunWith(Corollaries.class)
public class RelationAndAttributeTests extends BaseEmitterTester {

	private static List<JsonObject> relations;
	
	/**
	 * Loads the list of relation URIs and retrieves the first relation it lists.
	 * Assumes success of {@link DiscoveryTests#collectionsOK()}.
	 */
	@BeforeClass
	public static void loadServiceRootAndFirstRelation() {
		Fixtures.deleteDatabase();
		Data._initialiseRelations(); //YAML is insufficient to properly build database contents

		List<String> relationURIs = getResourceList("relations", true);
		relations = getResources(relationURIs);
	}

	/**
	 * Tests first listed relation and its attributes.
	 * Assumes success of {@link DiscoveryTests#collectionsOK()}.
	 */
	@Test public void relationsAndAttributesOK() throws ExternalResourceException {
		for (JsonObject relation : relations) {
			assertValidPSIMessage(relation, "relation");
			JsonObject attr = GET_JSON( getProp(relation, "defaultAttribute") );
			assertValidPSIMessage(attr, "attribute");
			testApplyAttribute(attr, relation);
			testAttributes( jsonArrayToStringList( relation.get("attributes") ), relation.get("size").getAsInt() );
		}
	}
	
	private void testAttributes(List<String> uris, final int relationSize) throws ExternalResourceException {
		for (JsonObject attr : getResources(uris))
			testOneAttribute(attr, relationSize);
	}
	
	private void testOneAttribute(JsonObject attr, final int relationSize) throws ExternalResourceException {
		assertValidPSIMessage(attr, "attribute");
		testApplyAttribute(attr, relationSize);
		
		if (attr.has("subattributes")) {
			JsonElement subattrs = attr.get("subattributes");
			String errorLead = "Attribute " + getProp(attr,"uri");
			String jsonType = Schema.compileToJSONSchema( attr.get("emits") ).get("type").getAsString();
			assertTrue(errorLead + " has sub-attributes, but does not emit array or object; actually emits " + jsonType,
					jsonType.equals("array") || jsonType.equals("object"));
			
			boolean isArray = jsonType.equals("array");
			assertSubattributesType(subattrs, errorLead, jsonType, isArray ? subattrs.isJsonArray() : subattrs.isJsonObject());
			List<String> subattrURIs;
			if (isArray) {
				subattrURIs = jsonArrayToStringList(subattrs);
			} else {
				subattrURIs = new ArrayList<>();
				for (Map.Entry<String,JsonElement> prop : subattrs.getAsJsonObject().entrySet())
					subattrURIs.add( prop.getValue().getAsString() );
			}
			testAttributes( subattrURIs, relationSize );
		}
	}
	
	private void assertSubattributesType(JsonElement subattrs, String errorLead, String type, boolean test) {
		assertTrue(errorLead + " emits " + type + " values but sub-attributes are not presented in an " + type + ". Sub-attributes property is " + subattrs, test);	
	}
	
	@Assumes("relationsAndAttributesOK")
	/** Tests querying of first listed relation. */
	@Test public void relationQuerying() throws ExternalResourceException {
		/*
		 * Have removed generation of a query value using the query schema since the test
		 * below requires knowledge of how query operates anyway, so had gained little in generality. 
		 */
		/*
		JsonObject query = JSONValueGenerator.generate( Schema.compileToJSONSchema( relation.get("querySchema") ) ).getAsJsonObject();
		StringBuilder sb = new StringBuilder("?");
		for (Map.Entry<String,JsonElement> queryProp : query.entrySet()) {
			String value = queryProp.getValue().toString().replaceAll("(?:^\\\"|\\\"$)", "");
			sb.append(queryProp.getKey()).append("=").append(value).append("&");
		}
		String queryString = sb.deleteCharAt(sb.length()-1).toString();
		JsonObject queried = GET_JSON( getProp(relation,"uri") + queryString);
		int expectedSize = relation.get("size").getAsInt() / query.get("numfolds").getAsInt();
		*/
		for (JsonObject relation : relations) {
			int folds = 10; 
			JsonObject queried = GET_JSON( getProp(relation,"uri") + "?fold=1&numfolds=" + folds);
			int expectedSize = relation.get("size").getAsInt() / folds;
			assertValidPSIMessage(queried, "relation");
			int size = queried.get("size").getAsInt();
			assertTrue("Queried relation size " + size + " different from expected " + expectedSize + " by more than 1.", Math.abs(expectedSize - size) <= 1);
		}
	}

	@Assumes("relationsAndAttributesOK")
	@Test public void attributeCreation() throws ExternalResourceException {
		for (JsonObject relation : relations)
			testAttributeCreation(relation);
	}
	
	private void testAttributeCreation(JsonObject relation) throws ExternalResourceException {
		List<String> attrURIs = jsonArrayToStringList( relation.get("attributes") );
		//Extract some attributes to use in new structured attribute definition
		JsonObject[] attrs = new JsonObject[3];
		for (int i = 0; i < attrs.length; i++)
			attrs[i] = GET_JSON( attrURIs.get(i % attrURIs.size()) );
		Attribute.Create body = new Attribute.Create();
		body.description = "New attribute created during testing";
		JsonObject def = new JsonObject();
		def.addProperty("first", makeFull(getProp(attrs[0],"uri")));
			JsonArray array = new JsonArray();
			array.add( new JsonPrimitive(makeFull(getProp(attrs[1],"uri")) ) );
			array.add( new JsonPrimitive(makeFull(getProp(attrs[2],"uri")) ) );
		def.add("second", array);
		body.attribute = def;
		Response response = POST_JSON(getProp(relation,"uri"), body);
		assertStatus(Http.StatusCode.CREATED, response);
		String location = response.getHeader("location"); 
		assertNotNull(location);
		
		//New attribute should be in revised list of attributes
		List<String> newAttrURIs = jsonArrayToStringList( GET_JSON( getProp(relation,"uri") ).get("attributes") ); 
		assertTrue("Just-created attribute is not in latest list of attributes returned by relation. Returned list: " + newAttrURIs, newAttrURIs.contains(location));

		//New attribute's schema can be predicted
		JsonObject newAttr = GET_JSON( location );
		assertValidPSIMessage(newAttr, "attribute");
		JsonObject expectedEmits = new JsonObject();
		expectedEmits.add("/first", attrs[0].get("emits") );
		expectedEmits.add("/second", stringToJSON("{'$array':{'items':[" + attrs[1].get("emits") + "," + attrs[2].get("emits") + "]}}"));
		assertEquals("New attribute's emits schema not what expected. ", expectedEmits, newAttr.get("emits"));
		
		//Check other property values are as expected
		assertEquals("Attribute description in request and after creation.", body.description, getProp(newAttr, "description"));
		JsonElement subattrs = newAttr.get("subattributes");
		assertNotNull("New attribute's sub-attributes", subattrs);
		assertTrue("Sub-attributes expected to be in an object structure", subattrs.isJsonObject());
		assertHasProperties(subattrs.getAsJsonObject(), "first", "second");
				
		testApplyAttribute(newAttr, relation);
		
		DELETE_OK( location );
	}
	
	public static void testApplyAttribute(JsonObject attr, JsonObject relation) throws ExternalResourceException {
		testApplyAttribute(attr, relation.get("size").getAsInt());
	}
	
	/** Tests applying the attribute to the first instance and all instances. */
	public static void testApplyAttribute(JsonObject attr, final int relationSize) throws ExternalResourceException {
		final String uri = getProp(attr,"uri");
		JsonObject emits = Schema.compileToJSONSchema( attr.get("emits") ); //since will be used twice, compile it now
		testApplyAttribute(uri, emits, relationSize);
	}

}