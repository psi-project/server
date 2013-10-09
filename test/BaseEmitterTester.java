import java.util.Map;
import java.util.Set;

import models.transformer.Transformer;
import play.Logger;
import play.mvc.Http.Response;
import util.JSONValueGenerator;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * Testing functions common to testing transformers and attributes; should be
 * extended by more specific test classes.
 */
public abstract class BaseEmitterTester extends PSIFunctionalTest {

	/**
	 * Applies the attribute or transformer using the given URI (to which the
	 * given instance=n or value=v string will be added)and tests that (1) it
	 * works and (2) the 'value' property contains a value that is valid against
	 * the {@code emits} JSON schema. Null values in responses will be checked
	 * strictly against the given schema. If testing attributes then will be
	 * better to use {@link #testApplyAll(String, JsonObject, JsonElement, int)}
	 * as this can deal with {@code null} (i.e., missing) values.
	 * @param emits The attribute or transformer's emits schema compiled to
	 * pure JSON schema.
	 */
	public static void testApply(final String uri, final String arg, JsonObject emits) {
		testApply(uri, arg, emits, null, null);
	}
	
	/**
	 * Applies the attribute twice, once with "instance=1" and again with
	 * "instance=all", and tests that both single and multiple instance value
	 * responses are correct. Null components in the value response are
	 * substituted using a value generated based on {@code emits}, thereby
	 * allowing missing values to be emitted by attributes. Unfortunately this
	 * is fairly computationally costly.
	 * @param emits The attribute or transformer's emits schema compiled to
	 * pure JSON schema.
	 */
	public static void testApplyAttribute(final String uri, JsonObject emits, int expectedNumber) {
		JsonElement substitute = JSONValueGenerator.generate(emits);
		testApply(uri, "instance=1", emits, substitute, null);
		testApply(uri, "instance=all", emits, substitute, new Integer(expectedNumber));
	}
	
	private static void testApply(final String uri, String arg, JsonObject emitsJSONSchema, JsonElement substitute, Integer expectedNumber) {
		JsonObject valueMessage = GET_JSON(uri + (uri.contains("?") ? '&' : '?') + arg);
		assertValidPSIMessage(valueMessage, "value");
		//PSI API validation should have taken care of mutual exclusivity of 'value' and 'valueList' property (although an invalid value for 'valueList' will allow object to pass even if it has both properties)
		JsonElement value = valueMessage.get(expectedNumber == null ? "value" : "valueList"); 
		assertNotNull( value );
		if (expectedNumber == null)
			assertValidJSON( complete(value, substitute), emitsJSONSchema, "emits");
		else {
			assertEquals("Number of values returned ", expectedNumber.intValue(), value.getAsJsonArray().size());
			for (JsonElement v : value.getAsJsonArray())
				assertValidJSON( complete(v, substitute), emitsJSONSchema, "emits");
		}
	}
	
	/**
	 * Complete the given value with value of or from {@code using}.
	 * Unfortunately computationally costly, since rebuilds complex structures.
	 */
	private static JsonElement complete(JsonElement value, JsonElement using) {
		if (value.isJsonNull())
			return using;
		else if (value.isJsonArray() && using.isJsonArray()) { 
			JsonArray arrayValue = value.getAsJsonArray(), usingArray = using.getAsJsonArray();
			if (arrayValue.size() == usingArray.size()) {
				JsonArray completed = new JsonArray();
				for (int i = 0, c = arrayValue.size(); i < c; i++)
					completed.add( complete( arrayValue.get(i), usingArray.get(i) ) );
				value = completed;
			}
		} else if (value.isJsonObject() && using.isJsonObject()) {
			JsonObject objectValue = value.getAsJsonObject(), usingObject = using.getAsJsonObject();
			for (Map.Entry<String,JsonElement> prop : objectValue.entrySet())
				if (usingObject.has(prop.getKey()))
					prop.setValue( complete(prop.getValue(), usingObject.get(prop.getKey())) );
		}
		return value;
	}
	
	/**
	 * As the service is clever enough to push (transformed) 'enum' values
	 * through to the schema of a joined transformer, its emits schema may not
	 * match that of the last transformer in the join; this function is a
	 * simple test that returns {@code true} if {@code s1.equals(s2)} or
	 * {@code s1} is a string such as {@code "s" and {@code s2} is an object of
	 * the form <pre>{ "$s": { "enum": [...] } }</pre>, {@code false} otherwise.
	 */
	private static boolean schemaSameOrEnumAdded(JsonElement s1, JsonElement s2) {
		if (s1.equals(s2))
			return true;
		if (s1.isJsonPrimitive() && s1.getAsJsonPrimitive().isString() && s2.isJsonObject()) {
			Set<Map.Entry<String,JsonElement>> props = s2.getAsJsonObject().entrySet();
			if (props.size() == 1) {
				Map.Entry<String,JsonElement> prop = props.iterator().next();
				return s1.getAsString().equals(prop.getKey()) && prop.getValue().isJsonObject() &&
						prop.getValue().getAsJsonObject().has("enum") && prop.getValue().getAsJsonObject().get("enum").isJsonArray();
			}
		}
		return false;
	}
	
	/**
	 * Makes a join request to the attribute or transformer {@code t}, verifies
	 * it was successful, tests that joined resource's {@code emits} is the
	 * same as {@code s}'s and that its description is as expected. Returns the
	 * joined resource's representation.
	 */
	public static JsonObject doJoin(JsonObject t, JsonObject s) {
		Transformer.Join body = new Transformer.Join();
		body.description = String.format("Test join of %s and %s", getProp(t,"uri"), getProp(s,"uri"));
		body.join = makeFull(getProp(s,"uri"));
		
		Response response = POST_JSON(getProp(t,"uri"), body);
		assertStatus(201, response);
		String location = response.getHeader("location"); 
		assertNotNull(location);
		
		JsonObject ts = GET_JSON( location );
		assertTrue("Joined resource's emits should match that of second transformer.", schemaSameOrEnumAdded(s.get("emits"), ts.get("emits")));
		assertEquals("Joined resources's description in request and after creation should be the same.", body.description, getProp(ts, "description"));
		
		return ts; 
	}

	/**
	 * As utility of the join tests depend on what transformers are available,
	 * log how many combinations were actually tested.
	 */
	protected void logJoinTestDetails(int total, String testFunctionName, String firstType, String secondType) {
		String testName = getClass().getSimpleName() + "." + testFunctionName + "()";
		if (total == 0)
			Logger.warn("%s test trivially passed as could not find identify any suitable %s-%s join combinations", testName, firstType, secondType);
		else
			Logger.info("%s test was able to examine %d %s-%s join combination(s)", testName, total, firstType, secondType);

	}
}