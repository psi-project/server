/**
 * 
 */
package util;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

/**
 * Deterministically generates sample values using a JSON schema as a guide,
 * but uses only a subset of schema constraints, so the results are not
 * guaranteed to be valid against that schema. However, the result can be used
 * for simple schema compatibility checking, by attempting to validate the
 * generated value against another schema. The generation algorithm for objects
 * assumes JSON Schema draft version 4, even if the system-wide setting is for
 * version 3. It also assumes valid JSON schema, in order to simplify
 * processing; if an invalid schema is provided then type casting errors will
 * result.
 *
 */
public final class JSONValueGenerator {
	
	private JSONValueGenerator() { }
	
	/**
	 * Generates a JSON value based on the given JSON (not PSI) schema.
	 */
	public static JsonElement generate(JsonObject schema) {
		if (schema.has("enum"))
			return schema.get("enum").getAsJsonArray().get(0);
		return basicGenerate(schema);
	}
	
	private static JsonElement basicGenerate(JsonObject schema) {
		JsonElement typeEl = schema.get("type");
		//if no type then could produce *any* value; integer is a subset of anything.
		String type = typeEl != null ? (typeEl.isJsonArray() ? typeEl.getAsJsonArray().get(0) : typeEl).getAsString() : "integer";
		switch (type) {
		case "boolean": return new JsonPrimitive(false);
		case "integer": return new JsonPrimitive( generateInteger(schema) ); 
		case "number":	return new JsonPrimitive( generateRealNumber(schema) );
		case "string":	return new JsonPrimitive(""); 
		case "array":	return generateArray(schema);
		case "object":	return generateObject(schema);
		case "null":	return JsonNull.INSTANCE;
		default:		return null;
		}		
	}
	
	/**
	 * If the given schema specifies a minimum then returns that value (or one
	 * more if 'exclusiveMinimum' is set and {@code true}), otherwise returns
	 * zero.
	 */
	private static int generateInteger(JsonObject schema) {
		int value = 0;
		if (schema.has("minimum")) {
			value = schema.get("minimum").getAsInt();
			if (schema.has("exclusiveMinimum") && schema.get("exclusiveMinimum").getAsBoolean())
				value++;
		}
		return value;
	}
	
	/**
	 * If the given schema specifies a minimum then returns that value (or 1e-5
	 * more if 'exclusiveMinimum' is set and {@code true}), otherwise returns
	 * zero. <em>May</em> generate a value greater than 'maximum', if set and
	 * the bounds imposed are very tight.
	 */
	private static double generateRealNumber(JsonObject schema) {
		double value = 0;
		if (schema.has("minimum")) {
			value = schema.get("minimum").getAsDouble();
			if (schema.has("exclusiveMinimum") && schema.get("exclusiveMinimum").getAsBoolean())
				value += 1e-5;
		}
		return value;
	}
	
	/**
	 * Generates a {@code JsonArray} with either one element, if {@code items}
	 * is a schema, or as many elements as in {@code items}. Ignores both
	 * {@code minItems} and {@code maxItems}, assuming that the {@code items}
	 * array should provide sufficient guidance for creating a valid value.
	 */
	private static JsonArray generateArray(JsonObject schema) {
		JsonArray array = new JsonArray();
		JsonElement items = schema.get("items");
		if (items != null) {
			if (items.isJsonArray()) {
				for (JsonElement itemSchema : items.getAsJsonArray())
					array.add( generate(itemSchema.getAsJsonObject()) );
			} else if (items.isJsonObject()) {
				array.add( generate(items.getAsJsonObject()) );
			}
		}
		return array;
	}
	
	//Note: Assumes JSON Schema version 4, even if system setting is for version 3
	private static JsonObject generateObject(JsonObject schema) {
		JsonObject object = new JsonObject();
		if (schema.has("properties") && schema.has("required")) {
			JsonObject props = schema.get("properties").getAsJsonObject();
			for (JsonElement requiredProp : schema.get("required").getAsJsonArray())
				object.add(requiredProp.getAsString(), generate(props.get(requiredProp.getAsString()).getAsJsonObject()));
		}
		return object;
	}
	
}
