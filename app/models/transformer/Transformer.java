package models.transformer;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

import javax.persistence.Lob;
import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;

import util.ExternalResourceException;
import util.Util;

import models.predictor.Predictor;
import models.PSI;
import models.PSIMessage;
import models.PSIResource;
import models.PSIResponse;

import util.Schema;

/**
 * Superclass of {@link BuiltinTransformer}s, {@link Predictor}s and
 * {@link JoinedTransformer}s.
 * 
 * @author jmontgomery
 *
 */
@MappedSuperclass
public abstract class Transformer extends AbstractFunction {
	private static final long serialVersionUID = 1L;
	
	/** GET argument name for an input value. */
	public static final String VALUE_ARG = "value";
	
	/** Schema describing values that the Transformation accepts. */
	@Lob //since could be very large
	public String accepts;
	/** JSON object (as text) describing provenance of this transformer. */
	@Lob
	public String provenance;
	/** The {@link #accepts} field as parsed JSON. */
	@Transient
	protected JsonElement acceptsInJSON;
	/** The {@link #accepts} field in pure JSON Schema. */ 
	@Transient
	private JsonObject acceptsJSONSchema;


	public Transformer() { this(""); }
	
	public Transformer(String name) { super(name); }

	/**
	 * Returns this Transformer's accepts schema as a parsed {@link
	 * JsonElement}, but does not compile the schema represented.
	 */ 
	protected synchronized JsonElement getAcceptsInJSON()  {
		if (acceptsInJSON == null)
			acceptsInJSON = Util.parseJSON(accepts);
		return acceptsInJSON;
	}

	/**
	 * Returns this Transformer's accepts schema as pure JSON schema.
	 * @throws ExternalResourceException if there is a problem resolving an
	 * external resource referenced in the {@link #accepts} schema.
	 */ 
	protected synchronized JsonObject getAcceptsJSONSchema() throws ExternalResourceException {
		if (acceptsJSONSchema == null)
			acceptsJSONSchema = new Schema(accepts).asCompiledJSON();
		return acceptsJSONSchema;
	}
	
	/**
	 * Validates the given value against this transformers's accepts schema and
	 * returns {@code true} if so if it is valid, {@code false} otherwise.
	 * Currently does not provide a way of reporting the actual errors found,
	 * if any.
	 * @throws ExternalResourceException if there is a problem resolving an
	 * external resource referenced in the {@link #accepts} schema.
	 */
	public boolean isAcceptableValue(JsonElement value) throws ExternalResourceException {
		return Schema.validateJSON(getAcceptsJSONSchema(), value).isEmpty();
	}
	
	/**
	 * Returns a PSI response describing (i.e., representing) this transformer.
	 */
	public PSIResource getDescription(final String uri, EncodedTransformerChain transformation) {
		return new Description(this, uri, transformation);
	}
	
	/**
	 * Returns a {@code JsonObject} containing provenance information to form
	 * part of the transformer's JSON representation. This is separate from the
	 * helper methods used for adding fields to the provenance to be persisted.
	 * The default implementation merely parses the {@link #provenance} field,
	 * but subclasses may wish to inject additional information stored
	 * elsewhere.
	 */
	public JsonObject getReportableProvenance() {
		return Util.parseJSON( provenance ).getAsJsonObject();
	}
	
	/**
	 * Convenience interface to {@link #addToProvenance(Map)} that uses
	 * {@link Util#makeMap(Object...)} to produce the map of properties.
	 */
	protected void addToProvenance(Object... propertyPairs) { addToProvenance(Util.makeMap(propertyPairs)); }
	
	/**
	 * Adds string-valued properties to the provenance object (i.e., all values
	 * have their toString() value stored.
	 */
	protected void addToProvenance(Map<String,Object> properties) {
		JsonObject jsonProvenance = Util.parseJSON(provenance == null ? "{}" : provenance).getAsJsonObject();
		for (Map.Entry<String,Object> property : properties.entrySet())
			jsonProvenance.addProperty(property.getKey(), property.getValue().toString());
		provenance = jsonProvenance.toString();
	}

	//--Requests---------------------------------------------------------------
	
	public static class Join extends PSIMessage {
		/** URI of transformer with which to join this transformation. */
		public String join;
		/** Optional human-readable description of the joined transformation. */
		public String description;

		public boolean isValid() {
			if (super.isValid()) {
				try {
					new URI(this.join);
				} catch (URISyntaxException e) {
					addBadMessage("Given join property is not a valid URI: " + this.join);
					return false;
				}
				return true;
			}
			return false;
		}
	}

	//--Responses--------------------------------------------------------------

	public static class Description extends PSIResource {
		/** [opt] A short, human-readable description of the transformer. */
		public String description;
		/** Schema describing valid input values for the transformer; may be a string reference to a predefined schema, hence not JsonObject. */
		public JsonElement accepts;
		/** Schema describing the output values of the transformer; may be a string reference to a predefined schema, hence not JsonObject. */
		public JsonElement emits;
		/** [opt] Structure describing how this transformer was created or later modified. */
		public JsonObject provenance;
		/** [opt] URI of update resource for updatable predictors. */
		public String update;

		public Description(Transformer t, final String uri, EncodedTransformerChain transformation) {
			this.uri = uri;
			if (transformation == null)
				this.description = t.description;
			else
				this.description = transformation.getDescription() == null ? t.description + " (transformed)" : transformation.getDescription();
			this.accepts = Util.parseJSON( t.accepts );
			this.emits = transformation == null ? Util.parseJSON( t.emits ) : transformation.getEmits();
			this.provenance = t.getReportableProvenance();
			if (transformation != null) {
				final String tKey = "transformer", joinKey = "joinedWith";
				this.provenance.getAsJsonObject().addProperty(tKey, this.uri.replaceAll("\\?.*$", ""));
				this.provenance.getAsJsonObject().add( joinKey, transformation.constructProvenanceEntry(tKey, joinKey) );
			}
			if ((t instanceof Predictor) && ((Predictor)t).isUpdatable())
				this.update = this.uri + PSI.UPDATE_PATH;
		}

	}

	public static class Value extends PSIResponse {
		/** Output of a transformer (predictor, attribute) applied to some value. */
		public JsonElement value;
		/** Output of an attribute (at this stage) applied to many instances/values. */
		public JsonArray valueList;

		public Value(JsonElement value) { this.value = value; }
		
		public Value(JsonArray values) { this.valueList = values; }
		
		/**
		 * Converts this {@code Value} into a {@code JsonObject} where the
		 * {@code value} field may have the value {@code JsonNull.INSTANCE},
		 * which would ordinarily be excluded during Gson serialization.
		 */
		public JsonObject toJsonWithNullableValue() {
			JsonObject jsonValue = Util.GSON.toJsonTree(this).getAsJsonObject();
			if (value == JsonNull.INSTANCE) //really should have been included
				jsonValue.add("value", value);
			return jsonValue;
		}
		
		public boolean isValid() {
			if (super.isValid()) {
				if (this.value == null && this.valueList == null || this.value != null && this.valueList != null) {
					addBadMessage("Exactly one of 'value' and 'valueList' must be set.");
					return false;
				}
				return true;
			}
			return false;
		}
	}
	
}