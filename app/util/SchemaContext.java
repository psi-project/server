/**
 * 
 */
package util;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

import com.google.gson.JsonElement;

/**
 * A mixed collection of {@link SchemaTemplate}s and {@link JsonElement}s
 * representing both the predefined and locally defined schema used in
 * compiling PSI schema to JSON schema.
 * 
 * @author jmontgomery
 *
 */
public class SchemaContext {
	/** The actual collection of {@code SchemaTemplate}s and {@code JsonElement}s. */
	private Map<String,Object> schema;
	
	public SchemaContext() {
		schema = new HashMap<String,Object>();
		for (SchemaTemplate template : SchemaTemplateLibrary.instance())
			schema.put(template.getName(), template);
	}
	
	public boolean contains(String id) { return schema.containsKey(id); }
	
	public void put(String id, JsonElement localSchema) {
		if (schema.containsKey(id))
			throw new IllegalArgumentException("Schema context already contains a schema with id '" + id + "'; it cannot be replaced");
		schema.put(id, localSchema);
	}

	/**
	 * Locates the predefined schema template or context-bound JSON schema in
	 * this {@code SchemaContext} and returns the {@code JsonElement} produced
	 * by substituting any template variables with the given arguments.
	 * Assumes that the given {@code id} <em>not</em> begin with a dollar sign. 
	 */
	public JsonElement get(String id, Map<String,Object> args) {
		Object entry = schema.get(id);
		if (entry == null)
			throw new NoSuchElementException(id);
		
		if (entry instanceof JsonElement) {
			return (JsonElement) entry;
		} else {
			assert entry instanceof SchemaTemplate;
			return ((SchemaTemplate)entry).asJSON( args );
		}
	}
	
}
