/**
 * 
 */
package models;

import com.google.gson.JsonElement;

/**
 * A lightweight POJO that can be serialized into a JSON Schema link
 * description object.
 * 
 * @see <a href="http://json-schema.org/latest/json-schema-hypermedia.html#anchor17">
 * Link Description Object specification in JSON Hyper Schema specification</a>
 *
 */
public final class LinkDescriptionObject {
	/** Link relationship. Required. */
	public String rel;
	/** Link hyper reference. Required. */
	public String href;
	/** Title of the link; could be used as a decorator in a client. */
	public String title;
	/** Schema describing expected structure of any JSON representation of resource at {@link #href}. */
	public JsonElement targetSchema;
	/** Expected media type when fetching resource at {@link #href}. */
	public String mediaType;
	/** Which HTTP method should be used to access resource at {@link #href}. */
	public String method;
	/** Media type that server expects request bodies to be in. */
	public String encType;
	/** JSON Schema defining the acceptable structure of a request submitted to {@link #href}. */
	public JsonElement schema;

	/** Create a new {@code LinkDescriptionObject} with the two required fields filled in. */
	public LinkDescriptionObject(String rel, String href) {
		if (rel == null || rel.isEmpty() || href == null || href.isEmpty())
			throw new IllegalArgumentException("Both rel and href are required. Received rel == '" + rel + "' and href == '" + href + "'");
		this.rel = rel;
		this.href = href;
	}
}
