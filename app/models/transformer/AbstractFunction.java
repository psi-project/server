package models.transformer;

import java.util.Iterator;

import javax.persistence.Lob;
import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import util.ExternalResourceException;
import util.Util;

import models.NamedModel;

import util.Schema;

/**
 * The superclass of {@link Attribute}s and {@link Transformer}s, providing
 * some shared fields and a common interface. Transformations are
 * {@link Function}s, in that they can be applied to a value to produce a
 * result, but the format of that input value is left for subclasses and their
 * instances to define.
 * 
 * @author jmontgomery
 *
 */
@MappedSuperclass
public abstract class AbstractFunction extends NamedModel implements Function {
	private static final long serialVersionUID = 1L;
	/** Human-readable description of this attribute. */
	public String description;
	/** Schema describing values that the Transformation emits. */
	@Lob //since could be very large
	public String emits;
	/** If object is a database model, can it be deleted by the client? */
	protected boolean isDeletable;
	/** The {@link #emits} field as parsed JSON. */
	@Transient
	protected JsonElement emitsInJSON;
	/** The {@link #emits} field in pure JSON Schema. */ 
	@Transient
	private JsonObject emitsJSONSchema;

	
	public AbstractFunction() { this(""); }
	
	public AbstractFunction(String name) {
		super();
		this.name = name;
		this.isDeletable = true;
	}
	
	public boolean isDeletable() { return isDeletable; }
	
	/**
	 * Returns this Transformer's emits schema as a parsed {@link JsonElement},
	 * but does not compile the schema represented.
	 */ 
	public synchronized JsonElement getEmitsInJSON()  {
		if (emitsInJSON == null)
			emitsInJSON = Util.parseJSON(emits);
		return emitsInJSON;
	}

	/**
	 * Returns this Transformation's emits schema as pure JSON schema.
	 * @throws ExternalResourceException if there is a problem resolving an
	 * external resource referenced in the {@link #emits} schema.
	 */
	protected synchronized JsonObject getEmitsJSONSchema() throws ExternalResourceException {
		if (emitsJSONSchema == null)
			emitsJSONSchema = new Schema(emits).asCompiledJSON();
		return emitsJSONSchema;
	}
	
	/**
	 * Validates the given value against this attribute's output schema and
	 * returns {@code true} if so, {@code false} otherwise. Currently does not
	 * provide a way of reporting the actual errors found, if any.
	 */
	public boolean isValidOutputValue(JsonElement value) throws ExternalResourceException {
		return Schema.validateJSON(getEmitsJSONSchema(), value).isEmpty();
	}
	
	//--Partial implementation of the Function interface-----------------------
	
	public JsonArray apply(Iterator<JsonElement> values) throws BadValueException {
		JsonArray result = new JsonArray();
		while (values.hasNext())
			result.add( apply( values.next() ) );
		return result;
	}
	
}