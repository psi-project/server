package util;

import org.junit.*;
import org.junit.contrib.assumes.Assumes;
import org.junit.contrib.assumes.Corollaries;
import org.junit.runner.RunWith;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import play.test.*;

/**
 * Tests PSI to JSON schema compilation, as described in
 * <a href="http://psi.cecs.anu.edu.au/spec#compilation-to-json-schema">
 * Section 5.3 of the PSI Specification</a>.
 * <p>
 * Where expected result is an object (i.e., a complete schema) then the
 * function under test is {@link Schema#compileToJSONSchema(JsonElement)},
 * while where the expected result is not then the function under test is
 * {@link Schema#compileToJSON(JsonElement)}.
 * 
 */
@RunWith(Corollaries.class)
public class SchemaCompilationTests extends UnitTest {
	
	private static JsonParser JSON_PARSER = new JsonParser();
	
	private final JsonObject integerSchema = stringToJSON("{'type':'integer'}").getAsJsonObject();
	private final JsonObject booleanSchema = stringToJSON("{'type':'boolean'}").getAsJsonObject();
	
	private static final String JSON_METASCHEMA = "http://json-schema.org/draft-04/schema#";

	/** {@code If S is an integer, number, boolean then return S.} */
	@Test public void primitiveCompilation() throws ExternalResourceException {
		JsonPrimitive integer = new JsonPrimitive(1), number = new JsonPrimitive(1.1), bool = new JsonPrimitive(true);  
		assertEquals(integer, Schema.compileToJSON(integer));
		assertEquals(number, Schema.compileToJSON(number));
		assertEquals(bool, Schema.compileToJSON(bool));
	}
	
	/**
	 * <pre>If S is a string of the form "$R" then
	 *    Resolve the reference R using context C to get R’ and return Compile(R’,C)
	 *    Otherwise return S.</pre>
	 * Note that passing this test requires an external website
	 * (json-schema.org) to be operational.
	 */
	@Test public void dollarReferenceResolution() throws ExternalResourceException {
		assertEquals( integerSchema, compilerWrapper("'$integer'") );
		assertEquals( booleanSchema, compilerWrapper("'$boolean'") );
		
		JsonObject externalSchema = compilerWrapper(new JsonPrimitive("$" + JSON_METASCHEMA));
		assertTrue( externalSchema.has("id") && externalSchema.get("id").isJsonPrimitive() &&
				externalSchema.get("id").getAsJsonPrimitive().isString() &&
				externalSchema.get("id").getAsString().startsWith("http://json-schema.org/") );
		
		JsonPrimitive jsonURI = new JsonPrimitive(JSON_METASCHEMA);
		JsonElement compiledString = Schema.compileToJSON(jsonURI);
		assertEquals(jsonURI, compiledString);
	}
	
	/**
	 * <pre>If S is a string of the form "@T" then return the JSON object
	 * { "type" : "string", "format" : "uri", "mediaType" : T }</pre>
	 */
	@Test public void richValueCompilation() throws ExternalResourceException {
		final String mediaType = "image/jpg";
		assertEquals( stringToJSON("{'type':'string','format':'uri','mediaType':'" + mediaType + "'}"),
				compilerWrapper( new JsonPrimitive("@" + mediaType) ) );
	}
	
	/**
	 * <pre>If S is an array then
	 *    Initialise an empty array S’ and for each item I in S add the value Compile(I,C) to S’
	 *    Return S’</pre>
	 */
	@Assumes({"primitiveCompilation","dollarReferenceResolution"})
	@Test public void arrayCompilation() throws ExternalResourceException {
		JsonArray simpleArray = stringToJSON("[1, 1.1, 'a string', true]").getAsJsonArray();
		assertEquals(simpleArray, Schema.compileToJSON(simpleArray));
		
		JsonArray psiSchemaArray = stringToJSON("['$integer','$boolean','$integer']").getAsJsonArray();
		JsonArray schemaArray = new JsonArray();
		schemaArray.add(integerSchema);
		schemaArray.add(booleanSchema);
		schemaArray.add(integerSchema);
		assertEquals(schemaArray, Schema.compileToJSON(psiSchemaArray));
	}

	/**
	 * Tests that locally defined schema are not added to the JSON schema. 
	 * <pre>If S is an object then initialise an empty JSON object S’
	 *    For each property P in S with key K of the form "#F" and value V
	 *       Add the pair (F,V) to the resolution context C</pre>
	 */
	@Test public void localSchemaNotAddedToSchema() throws ExternalResourceException {
		JsonElement psiSchema = stringToJSON("{'#local1':{'type':'integer'}, '#local2':'$integer'}");
		assertEquals( new JsonObject(), compilerWrapper(psiSchema));
	}

	/**
	 * <pre>If S is an object then initialise an empty JSON object S’
	 *    ...
	 *    If S contains a key of the form "$R" which does not equal "$ref" or
	 *      "$schema", with the object value V, then resolve the reference R in
	 *      context C with arguments V to obtain R’ and return Compile(R’,C)}
	 *    ...
	 *       Otherwise add the property K with value Compile(V,C) to S’</pre>
	 */
	@Assumes({"primitiveCompilation","dollarReferenceResolution"})
	@Test public void referencePropertyCompilation() throws ExternalResourceException {
		JsonElement dollarRefSchema = stringToJSON("{'$ref':'%s'}", JSON_METASCHEMA);
		assertEquals( dollarRefSchema, compilerWrapper(dollarRefSchema));
		
		JsonElement dollarSchemaRefSchema = stringToJSON("{'$schema':'%s'}", JSON_METASCHEMA);
		assertEquals( dollarSchemaRefSchema, Schema.compileToJSONSchema(dollarSchemaRefSchema)); //don't use wrapper, actually want to check that property is still present
		
		final int min = 1, max = 10;
		assertEquals( stringToJSON("{'type':'integer','minimum':%d,'maximum':%d}", min, max),
				compilerWrapper("{'$integer':{'min':%d,'max':%d},'thisProperty':'will not appear in JSON schema'}", min, max) );
	}
	
	/**
	 * <pre>If S is an object then initialise an empty JSON object S’
	 *    ...
	 *    For each other property P in S with key K and value V
	 *       If K is "allItems" then add the property with key "items" and value Compile(V,C) to S’
	 *       If K is equal to "/*" then add the property with key "additionalProperties" and value Compile(V,C) to S’. Add the property "type": "object" to S’
	 *       If K is of the form "/F", "/F=", "?F", "?F=" then
	 *          Add the property with key F to the object T associated with the key "properties" in S’, creating a new property in S’ with key "properties" and empty object value T if necessary.
	 *          If the key K ended with = then associate the value I = { "enum": V } with F in T, otherwise associate the value I = Compile(V,C) with F in T.
	 *          If the key K started with / then add F to the array Q associated with the key "required" in S’, creating a new property in S’ with key "required" and empty array value Q if necessary.
	 *          Add the property "type": "object" to S’.</pre>
	 */
	@Assumes({"primitiveCompilation","dollarReferenceResolution"})
	@Test public void objectPropertyCompilation() throws ExternalResourceException {
		assertEquals("'allItems' should become items",
				stringToJSON("{'items':%s}", integerSchema.toString()),
				compilerWrapper("{'allItems':'$integer'}") );
		assertEquals("'/*' should become additionaProperties",
				stringToJSON("{'type':'object','additionalProperties':%s}", booleanSchema.toString()),
				compilerWrapper("{'/*':'$boolean'}") );
		assertEquals("'/key' should become a required property",
				stringToJSON("{'type':'object','properties':{'key':%s},'required':['key']}", integerSchema.toString()),
				compilerWrapper("{'/key':'$integer'}") );
		assertEquals("'?key' should become an optional property",
				stringToJSON("{'type':'object','properties':{'key':%s}}", integerSchema.toString()),
				compilerWrapper("{'?key':'$integer'}") );
		assertEquals("'/key=' should become a fixed-value required property",
				stringToJSON("{'type':'object','properties':{'key':{'enum':['constant']}},'required':['key']}") ,
				compilerWrapper("{'/key=':'constant'}") );
		assertEquals("'?key=' should become a fixed-value optional property",
				stringToJSON("{'type':'object','properties':{'key':{'enum':['constant']}}}"),
				compilerWrapper("{'?key=':'constant'}") );
		assertEquals("'key' should become part of the compiled schema",
				stringToJSON("{'key':'value'}"),
				compilerWrapper("{'key':'value'}") );
	}
	
	/**
	 * Constructs a PSI schema exhibiting all of the features of the PSI schema
	 * language (and the JSON Schema $ref and $schema that compilation must work
	 * around) and confirms that they are compiled to the correct (and rather
	 * complex) JSON schema.
	 */
	@Assumes({"primitiveCompilation","dollarReferenceResolution","richValueCompilation","arrayCompilation","referencePropertyCompilation","objectPropertyCompilation"})
	@Test public void testAllCases() throws ExternalResourceException {
		JsonElement psiSchema = stringToJSON(
			"{" +
			"  '/a' : { '$schema':'%s', 'type':'integer' }," +
			"  '?b' : { '$ref':'%s', 'not':'removed yet' }," +
			"  '/c' : { '$integer' : { 'min' : 1 }, 'will' : 'be removed' }," +
			"  '?d' : '$integer'," +
			"  '/e' : '$local'," +
			"  '?f' : '@image/png'," +
			"  '/g=' : 'required fixed value'," +
			"  '?h=' : 'optional fixed value'," +
			"  '/*' : '$boolean'," +
			"  'pass' : 'through'," +
			"  '#local' : { 'type' : 'boolean', 'default': true }" +
			"}"
			, JSON_METASCHEMA, JSON_METASCHEMA);
		
		JsonObject expectedSchema = stringToJSON(
			"{" +
			"  'type':'object'," +
			"  'properties': {" +
			"    'a' : { '$schema':'%s', 'type':'integer' }," +
			"    'b' : { '$ref':'%s', 'not':'removed yet' }," +
			"    'c' : { 'type':'integer', 'minimum' : 1 }," +
			"    'd' : { 'type':'integer'}," +
			"    'e' : { 'type':'boolean', 'default':true }," +
			"    'f' : { 'type':'string','format':'uri','mediaType':'image/png'}," +
			"    'g' : { 'enum':['required fixed value']}," +
			"    'h' : { 'enum':['optional fixed value']}" +
			"  }," +
			"  'additionalProperties' : { 'type':'boolean' }," +
			"  'pass' : 'through'," +
			"  'required' : ['a','c','e','g']" +
			"}"
			, JSON_METASCHEMA, JSON_METASCHEMA).getAsJsonObject();
		
		assertEquals(expectedSchema, compilerWrapper(psiSchema));
	}

	/**
	 * Parses the JSON text using {@link #stringToJSON(String, Object...)} and
	 * then compiles it using {@link #compilerWrapper(JsonElement)}.
	 * @see #compilerWrapper(JsonElement)
	 */
	private static JsonObject compilerWrapper(String singleQuotedJSON, Object... args) throws ExternalResourceException {
		return compilerWrapper( stringToJSON(singleQuotedJSON, args) );
	}
	
	/**
	 * Calls {@link Schema#compileToJSONSchema(JsonElement)}, checks that the
	 * returned object contains a valid {@code $schema} property, then removes
	 * that property from the returned object so that later tests can focus on
	 * testing the structure they're interested in rather than having to
	 * repeatedly test the {@code $schema} property.
	 * @throws ExternalResourceException 
	 */
	private static JsonObject compilerWrapper(JsonElement psiSchema) throws ExternalResourceException {
		JsonObject schema = Schema.compileToJSONSchema(psiSchema);
		assertEquals("Top-level compiled schema must have $schema property", new JsonPrimitive(JSON_METASCHEMA), schema.get("$schema"));
		schema.remove("$schema");
		return schema;
	}
	
	/**
	 * Converts the given (invalid) JSON text that uses single quotes instead
	 * of double quotes into a {@code JsonObject}, after first substituting
	 * values in the string using {@link String#format(String, Object...)}. 
	 */
	protected static JsonElement stringToJSON(final String singleQuotedJSON, Object... args) {
		return JSON_PARSER.parse( String.format(singleQuotedJSON, args).replaceAll("'","\"") );
	}

}