package models.attribute;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;

import javax.persistence.Entity;

import com.google.gson.JsonObject;

import play.Logger;
import play.data.parsing.UrlEncodedParser;
import util.JSONType;
import util.Util;

/**
 * Superclass for the (currently) two general-purpose, built-in attributes that
 * can extract data from different underlying data structures. As they're not
 * true attributes they don't have real sub-attributes, but can report a name
 * pattern that valid sub-attributes can have. Although the built-in attributes
 * don't <em>have</em> to be persisted, since there's only ever one
 * <em>persistent</em> instance of each class, it helps when retrieving
 * attributes based on name to keep things consistent.
 *  
 * @author jmontgomery
 *
 */
@Entity
public abstract class PrimitiveAttribute extends Attribute {
	private static final long serialVersionUID = 1L;
	
	private static final String CSV_COLUMN = "csv";
	private static final String OBJECT_PROPERTY = "property";
	/**
	 * In internal attribute creation requests this can be used in place of
	 * {@code http://local_domain_name/_primitive} as was used previously.
	 */
	public static final String PRIMITIVE_URI_SCHEME = "primitive";
	
	/** The JSON type to which to convert CSV entries. */
	protected JSONType type;

	public PrimitiveAttribute() { this(null, null, null); }
	
	public PrimitiveAttribute(final String compositeName, String attrName, JSONType type, String... values) {
		super(compositeName, attrName);
		this.type = type == null ? JSONType.STRING : type;
		this.emits = "$" + this.type.toString().toLowerCase();

		if (values.length > 0) {
			JsonObject tempSchema = new JsonObject();
			tempSchema.add(this.emits, type.addEnumSchemaProperty(null, values) );
			this.emits = Util.GSON.toJson(tempSchema);		
			Logger.trace("In PrimitiveAttribute.<init>: Generated emits for enumerated field is: %s", this.emits);
		}
	}
	
	//--Attribute creation-----------------------------------------------------
	
	/**
	 * Returns a new {@code PrimitiveAttribute} iff the given attrURI path
	 * matches one of the currently supported built-in attributes, {@code null}
	 * otherwise.
	 * @throws IllegalArgumentException if no {@code type} argument is present
	 * 			in the request or the {@code type} argument does not correspond
	 * 			to a value of {@link JSONType}. 
	 */
	protected static PrimitiveAttribute create(final String compositeName, String attrName, URI attrURI) {
		String primitiveName = attrURI.getHost();
		String path = attrURI.getPath().substring(1); //ignore leading /
		Logger.trace("PrimitiveAttribute.create(): attrURI.getQuery() == %s", attrURI.getQuery());
		
		Map<String,String> args = Util.extractQueryArgs(attrURI);
		JSONType type = args.containsKey("type") ? JSONType.valueOf(args.get("type").toUpperCase()) : null;  
		if (type == null)
			throw new IllegalArgumentException("Internal attributes require that a type be specified.");

		String[] values = args.containsKey("values") ? args.get("values").split(",") : new String[0];
		
		PrimitiveAttribute attr = null;
		if (primitiveName.equals(CSV_COLUMN)) {
			//Names for array elements start at 1, but internally will be zero-indexed
			int index = Integer.parseInt(path) - 1;  
			attr = new CSVColumnAttribute(compositeName, attrName, index, type, values);
		} else if (primitiveName.equals(OBJECT_PROPERTY)) {
			attr = new ObjectPropertyAttribute(compositeName, attrName, path.split("/"), type, values);
		}
		//Now support descriptions for primitive attributes (for instance, when defined as part of a single, large, structured attribute)
		if (attr != null && args.containsKey("description"))
			attr.description = args.get("description");
		return attr;
	}
	
}
