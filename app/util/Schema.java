package util;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Vector;

import org.hibernate.cfg.NotYetImplementedException;

import models.PSIResponse;

import com.github.fge.jsonschema.SchemaVersion;
import com.github.fge.jsonschema.cfg.ValidationConfiguration;
import com.github.fge.jsonschema.exceptions.ProcessingException;
import com.github.fge.jsonschema.main.JsonSchema;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import com.github.fge.jsonschema.report.ProcessingMessage;
import com.github.fge.jsonschema.report.ProcessingReport;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import util.Util;

import play.Logger;
import play.Play;

/**
 * Wraps a JSON object and supports compilation from the PSI schema language to
 * full JSON schema.
 * <p>
 * Note that compiled JSON schema for arrays will often include an array-valued
 * {@code items} property, which in PSI implies that a valid array value must
 * have the same length as items and that each element of the array value is
 * valid against the schema in the corresponding position in {@code items}.
 * <strong>This is not how the {@code items} property is used in validation,
 * </strong> which only uses {@code items} to determine what schema to use to
 * valid array elements, but <em>not</em> to ensure that an array value has the
 * same number of elements. In a future revision compilation may be changed to
 * add (at least) {@code minItems=}<em>n</em> for {@code items} arrays of
 * length <em>n</em>.
 * 
 */
public class Schema {
	public static final SchemaVersion SCHEMA_VERSION;
	private static final ObjectMapper JACKSON_OBJ_MAPPER = new ObjectMapper();
	private static final JsonSchemaFactory SCHEMA_FACTORY;
	static {
		SCHEMA_VERSION = SchemaVersion.valueOf( Play.configuration.getProperty( "psi.schema-version" ) );
		ValidationConfiguration validationCfg = ValidationConfiguration.newBuilder()
				.setDefaultVersion( SCHEMA_VERSION )
				.setUseFormat(true) //this is the default, but to serve as a reminder that it *could* be switched off
				.freeze();
		SCHEMA_FACTORY = JsonSchemaFactory.newBuilder()
				.setValidationConfiguration( validationCfg )
				.freeze();
	}
		 
	
	private JsonElement schema;
	
	/**
	 * Create a new {@code Schema} object wrapping the given JSON schema.
	 */
	public Schema(JsonElement schema) {
		this.schema = schema;
	}

	/**
	 * Create a new {@code Schema} object based on the given template, using
	 * the provided template arguments.
	 */
	public Schema(SchemaTemplate template, Map<String,Object> args) {
		this( template.asJSON(args) );
	}

	/**
	 * Create a new {@code Schema} object based on the given JSON-formatted
	 * text (which may contain template variables), using the provided template
	 * arguments. This convenience constructor may be removed in the future.
	 */
	public Schema(String schemaString, JsonObject args) {
		this( new SchemaTemplate("", schemaString), jsonObjectToMap(args) );
	}

	/**
	 * Create a new {@code Schema} object by parsing the given JSON-formatted
	 * text.
	 */
	public Schema(String schemaString) {
		this( Util.parseJSON(schemaString) );
	}
	
	//--Accessors--------------------------------------------------------------
	
	public JsonElement getJson() { return schema; }
	
	//--Validation-------------------------------------------------------------
	
	/**
	 * Returns a validation report from validating the given JSON value against
	 * this schema; the given value is assumed to be valid JSON and is
	 * <em>not</em> compiled.
	 */
	public List<String> validateValue(JsonElement json) throws ExternalResourceException {
		return validateValueAgainstPSISchema(schema.getAsJsonObject(), json);
	}

	/**
	 * Returns a validation report from validating the given JSON value against
	 * this schema after "compiling" the value to resolve any embedded
	 * $-references. 
	 */
	public List<String> validateWithResolution(JsonElement json) throws ExternalResourceException {
		return validateWithResolutionAgainstPSISchema(schema.getAsJsonObject(), compileToJSON(json));
	}

	/**
	 * Returns the validation report from validating the given JSON value
	 * against the given (PSI) schema; the latter is compiled before
	 * validation.
	 */
	public static List<String> validateValueAgainstPSISchema(JsonElement psiSchema, JsonElement json) throws ExternalResourceException {
		return validateJSON( compileToJSONSchema( psiSchema ), json );
	}
	
	/**
	 * Returns the validation report from validating the given JSON value
	 * against the given PSI Schema after compiling the value to resolve any
	 * embedded $-references and convert any embedded PSI schema into JSON
	 * schema.
	 */
	public static List<String> validateWithResolutionAgainstPSISchema(JsonObject psiSchema, JsonElement json) throws ExternalResourceException {
		return validateJSON( compileToJSONSchema( psiSchema ).getAsJsonObject(), compileToJSON(json) );
	}
	
	/**
	 * Returns the validation report from validating the given PSI value
	 * against the given JSON (not PSI) schema after compiling the value to
	 * resolve any embedded $-references and convert any embedded PSI schema
	 * into JSON schema.
	 */
	public static List<String> validateWithResolution(JsonObject schema, JsonElement psiValue) throws ExternalResourceException {
		return validateJSON( schema, compileToJSON(psiValue) );
	}
	
	/**
	 * Returns the validation report from validating the given (purely) JSON
	 * value against the given JSON (not PSI) schema.
	 */
	public static List<String> validateJSON(JsonObject schema, JsonElement value) {
		return validate( schemaForValidation(schema), value);
	}
	
	/**
	 * Returns the validation report from validating each JSON value in the
	 * given {@code JsonArray} against the given JSON (not PSI) schema;
	 * validation ends either once all elements have been successfully
	 * validated or an element is encountered that is not valid.
	 */
	public static List<String> validateJSON(JsonObject schema, JsonArray values) {
		JsonSchema validationSchema = schemaForValidation(schema);
		List<String> messages = Collections.EMPTY_LIST;
		for (int i = 0, c = values.size(); i < c && messages.isEmpty(); i++)
			messages = validate(validationSchema, values.get(i));
		return messages;
	}
	
	/**
	 * Returns the given JSON schema as {@code json-schema-validator}'s
	 * {@link JsonSchema} type.
	 */
	private static JsonSchema schemaForValidation(JsonObject schema) {
		try {
			return SCHEMA_FACTORY.getJsonSchema( JACKSON_OBJ_MAPPER.readTree( schema.toString() ));
		} catch (IOException e) { //note that the one I care about (JsonProcessingException) is a subclass of IOException
			throw new RuntimeException("Error encountered while parsing JSON schema", e);
		} catch (ProcessingException pe) {
			throw new RuntimeException("Error encountered while validating schema", pe);
		}
	}
	
	/**
	 * Validates the Gson-represented JSON value against the given
	 * {@link JsonSchema}, and returns a list of validation error messages,
	 * which will be empty upon successful validation. Allows reuse of a
	 * previously generated {@code JsonSchema} object.
	 */
	//TODO Explore ways of making returned messages more informative or more human comprehensible
	@SuppressWarnings("unchecked") //Except that there exists only one empty list for every type
	private static List<String> validate(JsonSchema schema, JsonElement value) {
		try {
			ProcessingReport report = schema.validate( JACKSON_OBJ_MAPPER.readTree( value.toString() ) );
			if (report.isSuccess())
				return Collections.EMPTY_LIST;
			List<String> messages = new Vector<>();
			for (ProcessingMessage message : report) {
				Logger.trace("A 'processing' message (%s):\n%s", message.getClass(), message);
				messages.add( message.getMessage() );
			}
			return messages;
		} catch (IOException e) { //note that the one I care about (JsonProcessingException) is a subclass of IOException
			throw new RuntimeException("Error encountered while parsing JSON for validation", e);			
		} catch (ProcessingException pe) {
			throw new RuntimeException("Error encountered while validating JSON", pe);
		}
	}
	
	/**
	 * Performs basic compatibility checking between the given {@code emits}
	 * and {@code accepts} schema, returning {@code true} if they are
	 * definitely not compatible, and {@code false} if they <em>might</em> be
	 * compatible. The following approach is used:
	 * <ol>
	 *   <li>If {@code emits.equals(accepts)} then returns {@code false}
	 *   immediately.</li>
	 *   <li>The two PSI schema are compiled to pure JSON Schema.
	 *   If {@code emits} contains and {@code enum} of values then all of them
	 *   are validated against {@code accepts}, otherwise a single test value
	 *   {@linkplain JSONValueGenerator#generate(JsonObject) is generated}
	 *   based on {@code emits} and that is validated against
	 *   {@code accepts}. If validation fails then returns {@code true}
	 *   immediately.</li>
	 *   <li>If the two schema describe array values and have an {@code items}
	 *   property, then returns {@code true} if they have different lengths,
	 *   {@code false} otherwise. (Note this will only catch array-length
	 *   mismatches in the top-level structure.)</li>
	 * </ol>
	 */
	public static boolean isIncompatible(JsonElement emits, JsonElement accepts) throws ExternalResourceException {
		if (emits.equals(accepts))
			return false;
		JsonObject emitsSchema = compileToJSONSchema(emits), acceptsSchema = compileToJSONSchema(accepts);
		List<String> valueValidationMessages = emitsSchema.has("enum") ?
				validateJSON(acceptsSchema, emitsSchema.get("enum").getAsJsonArray()) :
				validateJSON(acceptsSchema, JSONValueGenerator.generate(emitsSchema)); 
		return !valueValidationMessages.isEmpty() || arraySchemaWrongLength(emitsSchema, acceptsSchema);
	}
	
	/**
	 * Returns {@code true} only if {@code emits} and {@code accepts} are JSON
	 * schema for arrays and have {@code items} properties that are differing lengths.
	 */
	private static boolean arraySchemaWrongLength(JsonObject emits, JsonObject accepts) {
		return emits.has("items") && emits.get("items").isJsonArray() &&
				accepts.has("items") && accepts.get("items").isJsonArray() &&
				emits.get("items").getAsJsonArray().size() != accepts.get("items").getAsJsonArray().size();
	}

	//--Utility----------------------------------------------------------------
	
	/**
	 * Creates a new PSI schema (or modifies the given PSI schema) to include
	 * an {@code enum} property with the given array of {@code values}.
	 * Will only work successfully in three cases, when {@code psiSchema} is:
	 * <ol>
	 *   <li>a string of the form {@code $predefinedSchema};</li>
	 *   <li>an object with a single property with a key of form
	 *   {@code $predefinedSchema}; or</li>
	 *   <li>an object with no such property,</li>
	 * </ol>
	 * which should cover most expected cases.
	 * @throws RuntimeException if it is unable to insert the {@code enum}
	 * property, as this indicates a new required case that must be supported.
	 */
	public static JsonObject addEnumToPSISchema(JsonElement psiSchema, JsonArray values) {
		if (psiSchema.isJsonPrimitive() && psiSchema.getAsString().startsWith("$")) {
			return addEnumToPSISchema( Util.parseJSON("{" + psiSchema.toString() + ":{}}"), values );
		} else if (psiSchema.isJsonObject()) {
			JsonObject addTo = psiSchema.getAsJsonObject();
			Map.Entry<String,JsonElement> firstProp = addTo.entrySet().iterator().next();
			if (firstProp.getKey().charAt(0) == '$') //valid PSI Schema of this form only contain one property with an object value
				addTo = firstProp.getValue().getAsJsonObject();
			addTo.add("enum", values);
			return psiSchema.getAsJsonObject();
		}
		throw new RuntimeException("Unable to insert 'enum' property into given PSI schema: " + psiSchema);
	}

	//--Compilation and Resolution---------------------------------------------

	public JsonObject asCompiledJSON() throws ExternalResourceException {
		return compileToJSONSchema( schema ).getAsJsonObject();
	}

	/**
	 * "Compiles" the given JSON value, which really should mean only resolving
	 * $-references. If the value to be compiled is a PSI Schema then use
	 * {@link #compileToJSONSchema(JsonElement)} instead.
	 */
	public static JsonElement compileToJSON(JsonElement psiJSON) throws ExternalResourceException {
		return compile( psiJSON, new SchemaContext() );
	}
	
	/**
	 * Compiles the given JSON value (which should be a PSI Schema) and adds
	 * the relevant $schema meta-schema property if the compiled JSON is a JSON
	 * object.
	 */
	public static JsonObject compileToJSONSchema(JsonElement psiJSON) throws ExternalResourceException {
		JsonObject jsonSchema = compile(psiJSON, new SchemaContext()).getAsJsonObject();
		jsonSchema.addProperty( "$schema", SCHEMA_VERSION.getLocation().toString() );
		return jsonSchema;
	}
	
	private static JsonElement compile(JsonElement s, SchemaContext context) throws ExternalResourceException {
		if ( s.isJsonPrimitive() ) {
			JsonPrimitive primitive = s.getAsJsonPrimitive();
			if ( ! primitive.isString() ) { //must be integer, number or boolean; return unchanged
				return primitive;
			} else {
				String value = primitive.getAsString();
				if (value.startsWith("$")) {
					return compile( resolve( value, null, context ), context );
				} else if (value.startsWith("@")) {
					return generateRichValueSchema( value.substring(1) );
				} else {
					return primitive;
				}
			}
		} else if ( s.isJsonArray() ) {
			JsonArray sPrime = new JsonArray();
			for (JsonElement i : s.getAsJsonArray())
				sPrime.add( compile(i, context) );
			return sPrime;
		} else if ( s.isJsonObject() ) {
			return compileObject( s.getAsJsonObject(), context );
		}
		return s;
	}
	
	private static JsonElement compileObject(JsonObject object, SchemaContext context) throws ExternalResourceException {
		String schemaRef = null;
		for (Entry<String,JsonElement> pair : object.entrySet()) {
			String key = pair.getKey();
			if (key.charAt(0) == '#') { //no need to compile value now; if used it will be compiled then
				context.put( pair.getKey().substring(1), pair.getValue() );
			} else if ( key.charAt(0) == '$' && ! (key.equals("$ref") || key.equals("$schema")) ) {
				schemaRef = pair.getKey();
			}
		}
		
		if (schemaRef != null) {
			JsonElement value = object.get(schemaRef);
			if (! value.isJsonObject())
				throw new RuntimeException("Cannot compile schema. Arguments to schema reference " + schemaRef + " must be in the form of an object but encountered: " + value);
			return compile( resolve( schemaRef, jsonObjectToMap( value.getAsJsonObject() ), context ), context );
		}
		
		JsonObject sPrime = new JsonObject();
		for (Entry<String,JsonElement> pair : object.entrySet()) {
			String key = pair.getKey();
			JsonElement value = pair.getValue();

			if (key.charAt(0) == '#') {
				/* Ignore it; already dealt with. */
			} else if (key.equals("allItems")) {
				sPrime.add("items", compile(value, context));
			} else if (key.equals("/*")) {
				sPrime.add("additionalProperties", compile(value, context));
				setIsObject(sPrime);
			} else if (key.startsWith("/") || key.startsWith("?")) {
				addObjectProperty(sPrime, key, value, context);
			} else {
				sPrime.add(key, compile(value, context));
			}
		}
		
		return sPrime;
	}
	
	/** Generates the rich value JSON schema for the given media type. */
	private static JsonObject generateRichValueSchema(final String mediaType) {
		JsonObject s = new JsonObject();
		s.addProperty("type", "string");
		s.addProperty("format", "uri");
		s.addProperty("mediaType", mediaType);
		return s;
	}
	
	private static void addObjectProperty(JsonObject object, String key, JsonElement value, SchemaContext context) throws ExternalResourceException {
		setIsObject(object);
		/*
		 * Adapted from the PSI specification.
		 * Add the property "type": "object" to S’ (see above).
		 * If K is of the form "/F", "/F=", "?F", "?F=" then add the property with key
		 * F to the object T associated with the key "properties" in S’, creating a new
		 * property in S’ with key "properties" and empty object value T if necessary.
		 * If the key K ended with = then associate the value I = { "enum": V } with
		 * F in T, otherwise associate the value I = Compile(V,C) with F in T.
		 * If the key K started with / then
		 *   If SCHEMA_VERSION == SchemaVersion.DRAFTV3 then add the property "required"
		 *   with value true to T.
		 *   if SCHEMA_VERSION == SchemaVersion.DRAFTV4 then add K to the array Q
		 *   associated with the key "required" in S’, creating a new property in S’
		 *   with key "required" and empty array value Q if necessary.
		 */
		boolean required = key.startsWith("/");
		assert required || key.startsWith("?");
		key = key.substring(1);
		
		boolean fixedValue = key.endsWith("=");
		if (fixedValue)
			key = key.substring(0, key.length() - 1);
		
		JsonElement valueToUse;
		if (fixedValue) {
			valueToUse = new JsonObject();
			JsonArray enumList = new JsonArray();
			//FIXME Note that we've decided that compiling the value is necessary to support some operations, but should check it doesn't 'break' any existing uses of /K=
			enumList.add( compile(value, context) );
			valueToUse.getAsJsonObject().add("enum", enumList);
		} else {
			valueToUse = compile(value, context);
		}
		
		if (required) {
			if (SCHEMA_VERSION == SchemaVersion.DRAFTV3) {
				assert valueToUse.isJsonObject();
				valueToUse.getAsJsonObject().addProperty("required", true);
			} else if (SCHEMA_VERSION == SchemaVersion.DRAFTV4) {
				//Ensure required JSON array is present
				if (! object.has("required"))
					object.add("required", new JsonArray());
				object.get("required").getAsJsonArray().add( Util.parseJSON(key) ); //silly to have to parse it, but no other way to add an element to JsonArray
			} else {
				throw new IllegalStateException("System-wide schema version " + SCHEMA_VERSION + " is not supported by PSI Schema Compiler");
			}
		}
		//Ensure properties JSON object is present
		if (! object.has("properties"))
			object.add("properties", new JsonObject());
		object.get("properties").getAsJsonObject().add(key, valueToUse);
	}
	
	private static void setIsObject(JsonObject object) {
		object.addProperty("type", "object");
	}
	
	private static JsonElement resolve(String id, Map<String,Object> args, SchemaContext context) throws ExternalResourceException {
		if (id.charAt(0) == '$')
			id = id.substring(1);

		Logger.trace("Entering resolve(id=%s, args=%s, context=%s)", id, args, context);
		if (id.startsWith("http://"))
			return HttpUtil.getJSON(id, null);
		if (! context.contains(id))
			throw new NoSuchElementException("Schema with id \"" + id + "\" not found in current context");

		return context.get(id, args);
	}
	
	/**
	 * Returns a shallow conversion of the given JsonObject to a map of
	 * string-string pairs.
	 */
	private static Map<String,Object> jsonObjectToMap(JsonObject object) {
		//Because JsonObject's map is hidden (and it doesn't implement Map interface itself), must manually copy pairs across
		Map<String,Object> map = new HashMap<String,Object>();
		for (Entry<String,JsonElement> property : object.entrySet())
			map.put( property.getKey(), property.getValue() );
		return map;
	}
	
	//--In preparation for supporting validation on request--------------------
	
	//Note that PSIMessage does not map this to a psiType at this stage
	public static class Validation extends PSIResponse {
		public boolean valid;
		public JsonElement value;
		public List<String> errors;

		public Validation(List<String> validationErrors, JsonElement value) {
			this.valid = validationErrors.isEmpty();
			this.value = value;
			this.errors = this.valid ? null : validationErrors;
		}
	}
	
}
