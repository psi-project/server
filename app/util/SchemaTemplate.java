package util;

import util.Util;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import models.NamedResource;

import com.google.gson.JsonObject;

/**
 * An immutable schema loaded from a plain text representation that includes
 * template variables. Variable substitution is supported by the subclass
 * {@link MutableSchemaTemplate}.
 *
 */
public class SchemaTemplate implements NamedResource {
	protected String name;
	private String originalJSONText;
	protected JsonObject schema;
	protected Map<String,List<String>> variableToFieldNames;
	
	public SchemaTemplate(String name, String schemaText) {
		setName(name);
		setSchemaFromText(schemaText);
	}
	
	public SchemaTemplate(SchemaTemplate template) {
		this.name = template.name;
		this.originalJSONText = template.originalJSONText;
		this.schema = Util.parseJSON(template.originalJSONText).getAsJsonObject(); //all pre-defined schema are objects
		this.variableToFieldNames = template.variableToFieldNames;
	}
	
	//--Accessors--------------------------------------------------------------
	
	public String getName() { return name; }
	
	public void setName(String name) { this.name = name; } 
	
	private void setSchemaFromText(String schemaText) {
		this.originalJSONText = schemaText;
		schema = Util.parseJSON(schemaText).getAsJsonObject();
		//Although could inspect the JsonObject structure, will use regexes to identify variable fields since this avoids nested object traversal
		variableToFieldNames = new HashMap<String,List<String>>();
		Matcher m = Pattern.compile("\"([^\"]+)\"\\s*:\\s*\"(%\\w+)\"").matcher(schemaText);
		while (m.find()) {
			List<String> names = variableToFieldNames.get(m.group(2));
			if (names == null) {
				names = new Vector<String>();
				variableToFieldNames.put(m.group(2), names);
			}
			names.add( m.group(1) );
		}
		variableToFieldNames = Collections.unmodifiableMap(variableToFieldNames);
	}
	
	public int hashCode() { return name.hashCode(); }
	
	public boolean equals(Object o) {
		if (o instanceof SchemaTemplate) {
			SchemaTemplate other = (SchemaTemplate) o;
			return name.equals(other.name) && schema.equals(other.schema);
		}
		return false;
	}
	
	//--Template behaviour methods---------------------------------------------

	/**
	 * Returns a modifiable template object based on this {@code SchemaTemplate}.
	 */
	public MutableSchemaTemplate asMutableTemplate() {
		return asMutableTemplate(null);
	}

	/**
	 * Returns a modifiable template object based on this {@code SchemaTemplate}
	 * with template variables replaced by the given arguments; any template
	 * variables that are not set will have their corresponding properties
	 * removed from the returned template.
	 */
	public MutableSchemaTemplate asMutableTemplate(Map<String,Object> args) {
		return new MutableSchemaTemplate(this, args);
	}

	/**
	 * Returns a {@code JsonObject} representing this template with all
	 * template variables displayed.
	 */
	public JsonObject asJSON() { return schema; }
	
	/**
	 * Returns a {@code JsonObject} representing this template with all
	 * template variables replaced by their values in {@code args} and
	 * variables without an entry in args removed from the JSON structure.
	 */
	public JsonObject asJSON(Map<String,Object> args) {
		MutableSchemaTemplate mutableTemplate = asMutableTemplate(args);
		if (args == null)
			mutableTemplate.removeUnsetProperties();
		return mutableTemplate.schema;
	}
	
}