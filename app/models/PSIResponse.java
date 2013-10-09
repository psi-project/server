package models;

import java.util.List;

import com.google.gson.JsonElement;

/**
 * PSI messages that are emitted by a service may have a
 * {@code relatedServices} property, which is a list of Link Description
 * Objects as defined by the JSON Hyper Schema specification. 
 *
 */
public abstract class PSIResponse extends PSIMessage {
	/**
	 * Optional list of related services, specified as
	 * <a href="http://json-schema.org/latest/json-schema-hypermedia.html#anchor17">
	 * link description objects</a>.
	 */
	public List<JsonElement> relatedServices;
	
}