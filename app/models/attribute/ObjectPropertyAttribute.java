package models.attribute;

import java.util.Arrays;

import javax.persistence.Entity;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import util.JSONType;

/**
 * Built-in, fairly general-purpose {@link Attribute} that can extract the
 * value of object properties.
 *  
 * @author jmontgomery
 *
 */
@Entity
public class ObjectPropertyAttribute extends PrimitiveAttribute {
	private static final long serialVersionUID = 1L;
	
	/** Path to property to read. */
	protected String[] propertyPath;
	
	public ObjectPropertyAttribute() { this("", "", new String[0], null, null); }
	
	public ObjectPropertyAttribute(final String compositeName, String attrName,
			String[] propertyPath, JSONType type, String richType, String... values) {
		super(compositeName, attrName, type, richType, values);
		this.propertyPath = Arrays.copyOf(propertyPath, propertyPath.length);
	}
	
	//--Attribute as a function interface--------------------------------------
	
	/**
	 * Returns the {@code JsonElement} holding this attribute's property.
	 * Requires that {@code instance} be a {@link JsonObject}.
	 */
	public JsonElement apply(JsonElement instance) {
		JsonElement curr = instance.getAsJsonObject();
		for (String nextProp : propertyPath) {
			curr = curr.getAsJsonObject().get(nextProp);
		}
		//Note that PrimitiveAttribute.type is currently not used, since it should be implied by instance's JSON structure
		return curr;
	}
	
}