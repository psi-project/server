package models.attribute;

import javax.persistence.Column;
import javax.persistence.Entity;

import com.google.gson.JsonElement;

import util.JSONType;

/**
 * Built-in, fairly general-purpose {@link Attribute} that can extract elements
 * from different positions in a CSV file.
 *  
 * @author jmontgomery
 *
 */
@Entity
public class CSVColumnAttribute extends PrimitiveAttribute {
	private static final long serialVersionUID = 1L;

	/** Index within the CSV data to read. */
	@Column(name="`index`")
	protected int index;
	
	public CSVColumnAttribute() { this("", "", 0, null, null); }
	
	public CSVColumnAttribute(final String compositeName, String attrName,
			int index, JSONType type, String richType, String... values) {
		super(compositeName, attrName, type, richType, values);
		this.index = index;
	}
	
	//--Attribute as a function interface--------------------------------------
	
	/**
	 * Extracts an element, appropriately converted according this this
	 * {@code PrimitiveAttribute}'s {@link #type}, from a {@code JsonArray} of
	 * {@code JsonString}s.
	 */
	public JsonElement apply(JsonElement instance) {
		return type.jsonFromJSONString( instance.getAsJsonArray().get(index) );
	}
	
}