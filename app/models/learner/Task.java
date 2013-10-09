/**
 * 
 */
package models.learner;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import models.PSIResponse;

import play.Logger;

import util.ExternalResourceException;
import util.HttpUtil;
import util.JSONType;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * Holds details about a generic learning task. Can extract some information
 * from the task given in a process request, mapping entries in the 'resources'
 * property to appropriate description response types, where possible.
 * 
 * @author jmontgomery
 *
 */
public class Task {
	/** Commonly occurring names for resources. */
	public static enum CommonResource {
		source, target, weight, first, second, preferred, not_preferred, relation;
		public boolean isAttribute() { return this != relation; }
	}
	
	private JsonObject taskInJSON;
	private Map<String,PSIResponse> resources; 
	private Map<String,Object> settings;
	/** Optional copy of the PSI Schema for this task. */
	private JsonObject psiSchema;
	
	public Task(JsonObject taskInJSON, JsonObject psiSchema) throws ExternalResourceException {
		this.taskInJSON = taskInJSON;
		this.psiSchema = psiSchema;				
		extractResources();
		extractSettings();
	}
	
	public JsonObject getTaskInJSON() { return taskInJSON; }
	
	public boolean hasResource(CommonResource res) { return hasResource(res.name()); }
	
	public boolean hasResource(String id) { return resources.containsKey(id); }
	
	public <T extends PSIResponse> T getResource(CommonResource res) { return getResource(res.name()); }
	
	@SuppressWarnings("unchecked") //Other problems will occur before the wrong type is ever removed from this collection
	public <T extends PSIResponse> T getResource(String id) { return (T) resources.get(id); }

	/**
	 * Adds a resource description to the task (but does not modify the JSON
	 * representation that was originally received from the client). Can be
	 * useful for holding state between calls to otherwise stateless
	 * {@code Trainer} methods.
	 */
	public void setResource(String id, PSIResponse resourceDesc) { resources.put(id, resourceDesc); }
	
	public Map<String,PSIResponse> getResources() { return resources; }
	
	public Map<String,Object> getSettings() { return settings; }
	
	private void extractSettings() {
		settings = new HashMap<String,Object>();
		//Assumes that every property other than 'resources' is a primitive-valued parameter
		for (Entry<String,JsonElement> property : taskInJSON.entrySet()) {
			if (! property.getKey().equals("resources"))
				settings.put( property.getKey(), JSONType.extractValueFromJsonPrimitive( property.getValue().getAsJsonPrimitive(), deduceType(property.getKey()) ) );
		}
	}

	//Note: This special handling currently only needed to deal with integer-valued parameters so that correct conversion is done later.
	private JSONType deduceType(final String key) {
		if (psiSchema != null) {
			JsonElement schema = psiSchema.has("?" + key) ? psiSchema.get("?" + key) : psiSchema.get("/" + key);
			if (schema != null) {
				String typeName = null;
				if (schema.isJsonPrimitive() && schema.getAsJsonPrimitive().isString())
					typeName = schema.getAsString();
				else if (schema.isJsonObject()) {
					for (Map.Entry<String,JsonElement> entry : schema.getAsJsonObject().entrySet())
						if (typeName == null && entry.getKey().startsWith("$"))
							typeName = entry.getKey();
				}
				if (typeName != null)
					return JSONType.fromJSONTypeName( typeName.substring(1) );
			}
		}
		return null;
	}
	
	private void extractResources() throws ExternalResourceException {
		resources = new HashMap<String, PSIResponse>();
		if (! taskInJSON.has("resources"))
			return;
		JsonObject resourcesInJSON = taskInJSON.get("resources").getAsJsonObject();
		for (Map.Entry<String,JsonElement> entry : resourcesInJSON.entrySet()) {
			Logger.trace("Processing given resource named '%s'", entry.getKey());
			PSIResponse psiResponse = null;
			if (entry.getValue().isJsonObject()) {
				psiResponse = PSIResponse.jsonToMessage(entry.getValue().getAsJsonObject());
			} else if (HttpUtil.jsonElementIsReference(entry.getValue())) {
				psiResponse = HttpUtil.getPSIResponse( entry.getValue().getAsString().substring(1) );
			} else {
				throw new RuntimeException("Resource '" + entry.getKey() + "' was neither a complete description nor a reference to one");
			}
			//Note: At this point the response object can be any of the PSI responses and hence *might* not be a resource description
			resources.put( entry.getKey(), psiResponse );
		}
		
	}
	
	public String toString() {
		return "Resources: " + resources + "\n" + "Settings: " + settings; 
	}
	
}
