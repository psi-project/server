package util;

import java.util.Collection;
import java.util.Map;
import java.util.Vector;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import util.Util;

/**
 * A mutable schema template, represented as plain text, that supports template
 * variable quoting, substitution and removal (if no substituted value given).
 * 
 * @author jmontgomery
 * @version 1
 *
 */
public class MutableSchemaTemplate extends SchemaTemplate {
	
	public MutableSchemaTemplate(SchemaTemplate template) {
		this(template, null);
	}
	
	public MutableSchemaTemplate(SchemaTemplate template, Map<String,Object> args) {
		super(template);
		if (args != null)
			setVariables(args);
	}

	
	//--Accessors--------------------------------------------------------------
	
	
	//--Template behaviour methods---------------------------------------------

	/**
	 * Sets the value of all matching template variables in the schema
	 * template, <em>then</em> adds any additional properties that do not
	 * already exist in the schema.
	 */
	public void setVariables(Map<String,Object> namedValues) {
		Collection<String> additionalKeys = new Vector<String>();
		for (Map.Entry<String,Object> pair : namedValues.entrySet()) {
			if (! setVariable(pair.getKey(), pair.getValue()))
				additionalKeys.add( pair.getKey() );
		}
		removeUnsetProperties(schema); //avoids revising schema text at this point
		for (String additionalKey : additionalKeys)
			insertProperty(additionalKey, namedValues.get(additionalKey));
	}

	/**
	 * Replaces all occurrences of the template variable {@code %name} with the
	 * given {@code value}, which is assumed to be either a String to be parsed
	 * into JSON or a {@code JsonElement}, which will be added to the schema
	 * object's tree as is. Returns {@code true} if a corresponding template
	 * variable was found and replaced, {@code false} if {@code %name} is not a
	 * variable in this template.
	 * @throws ClassCastException if {@code value} isn't a {@code String} or
	 * 			{@code JsonObject}.
	 */
	public boolean setVariable(String name, Object value) {
		name = "%" + name;
		if (! variableToFieldNames.containsKey(name))
			return false;
		for (String key : variableToFieldNames.get(name)) //unfortunate complication of when template variable is used multiple times
			setVariable(schema, key, name, value);
		return true;
	}
	
	/**
	 * Performs {@link #setVariable(String, Object)} within the given schema
	 * object (and any nested objects).
	 */
	private void setVariable(JsonObject schema, String key, String name, Object value) {
		if (schema.has(key)) {
			schema.add(key, processValue(value) );
		} else { //must look through all properties in search of object values that may contain propName
			for (Map.Entry<String,JsonElement> property : schema.entrySet()) {
				if (property.getValue().isJsonObject())
					setVariable(property.getValue().getAsJsonObject(), key, name, value);
			}
		}
	}
	
	private void insertProperty(String name, Object value) {
		if (!schema.has(name))
			schema.add(name, processValue( value ) );
	}
	
	/**
	 * @throws ClassCastException if {@code value} isn't a {@code String} or
	 * 			{@code JsonObject}.
	 */
	private JsonElement processValue(Object value) {
		return value instanceof String ? Util.parseJSON((String)value) : (JsonElement) value;
	}
	
	/** Removed properties with template variables that have not been set. */
	public void removeUnsetProperties() {
		removeUnsetProperties(schema);
	}
	
	private void removeUnsetProperties(JsonObject schema) {
		Collection<String> toRemove = new Vector<String>();
		for (Map.Entry<String,JsonElement> property : schema.entrySet()) {
			JsonElement value = property.getValue();
			if (value.isJsonObject())
				removeUnsetProperties( value.getAsJsonObject() );
			else if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isString() && value.getAsString().startsWith("%")) {
				//Do final check to make sure it really *was* a template variable
				String varName = value.getAsString();
				if (variableToFieldNames.containsKey(varName)) {
					for (String key : variableToFieldNames.get(varName))
						if (key.equals(property.getKey())) {
							toRemove.add(property.getKey());
						}
				}
			}
		}
		for (String key : toRemove)
			schema.remove(key);
	}
	
}