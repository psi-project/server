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
 * An enumerated type for the different JSON types; supports conversion from
 * String to the type of the actual enum object in question, except for ARRAY
 * and OBJECT types.
 * 
 * @author jmontgomery
 *
 */
public enum JSONType {
	INTEGER, NUMBER, BOOLEAN, STRING, ARRAY, OBJECT;
	
	public static Object extractValueFromJsonPrimitive(JsonPrimitive jsonPrim, JSONType expectedType) {
		if (expectedType == null)
			return typeOfJsonElement(jsonPrim).convertValueFromJsonPrimitive(jsonPrim);
		else
			return expectedType.convertValueFromJsonPrimitive(jsonPrim);
	}
	
	public static JSONType typeOfJsonElement(JsonElement json) {
		if (json.isJsonArray())
			return ARRAY;
		else if (json.isJsonObject())
			return OBJECT;
		else if (json.isJsonPrimitive()) {
			JsonPrimitive prim = json.getAsJsonPrimitive();
			if (prim.isBoolean())
				return BOOLEAN;
			else if (prim.isNumber())
				return NUMBER;
			else if (prim.isString())
				return STRING;
		}
		return null;
	}
	
	public static JSONType fromJSONTypeName(final String jsonTypeName) { return valueOf( jsonTypeName.toUpperCase() ); }

    public Object convertValueFromJsonPrimitive(JsonPrimitive jsonPrim) {
    	if (this == JSONType.BOOLEAN)
    		return jsonPrim.getAsBoolean();
    	else if (this == JSONType.INTEGER)
    		return jsonPrim.getAsInt();
    	else if (this == JSONType.NUMBER)
    		return jsonPrim.getAsNumber();
    	else if (this == JSONType.STRING)
    		return jsonPrim.getAsString();
    	throw new IllegalArgumentException("Given JSON type is not a primitive: " + this);
    }
	
	/**
	 * Returns the appropriately-typed JSON value based on the string value
	 * represented by {@code json}, or {@code JsonNull.INSTANCE} if the
	 * argument is {@code JsonNull}.
	 * @throws UnsupportedOperationException if {@code json} is neither
	 * {@code JsonNull} nor a {@code JsonPrimitive}.
	 */
	public JsonElement jsonFromJSONString(JsonElement json) {
		return json.isJsonNull() ? json :  jsonFromString(json.getAsString());
	}
	
	public JsonElement jsonFromString(String value) {
		switch (this) {
		case INTEGER: return new JsonPrimitive( Integer.parseInt(value) );
		case NUMBER: return new JsonPrimitive( Double.parseDouble(value) );
		case BOOLEAN: return new JsonPrimitive( Boolean.parseBoolean(value) );
		case STRING: return new JsonPrimitive( value );
		case ARRAY: 
		case OBJECT:
		default: throw new IllegalArgumentException("Conversion from string to array or object not supported");
		}
	}

	/**
	 * Add the "enum":[values...] property to the given schema object, or a new
	 * object if {@code schema == null}. Returns {@code schema} if it is not
	 * {@code null}, or the new object if it is.
	 */
	public JsonObject addEnumSchemaProperty(JsonObject schema, String... values) {
		if (values.length == 0)
			throw new IllegalArgumentException("Cannot create an enumeration from zero values");
		if (schema == null)
			schema = new JsonObject();
		JsonArray enumValues = new JsonArray();
		for (String value : values)
			enumValues.add( jsonFromString(value) );
		schema.add("enum", enumValues);
		return schema;
	}

}
