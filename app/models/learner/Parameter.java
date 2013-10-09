/**
 * 
 */
package models.learner;

import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.persistence.Entity;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.Lob;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import util.Util;

import play.db.jpa.Model;


/**
 * Describes an option or control parameter for a {@link Learner}. Must be
 * subclassed for different toolkits.
 * 
 * @author jmontgomery
 *
 */
@Entity
@Inheritance(strategy=InheritanceType.SINGLE_TABLE)
public class Parameter extends Model {
	private static final long serialVersionUID = 1L;
	
	/** Parameter's name; shown externally. */
	public String name;
	/** The name used by the toolkit to refer to this parameter. */
	public String toolkitName;
	/** Name of locally-defined schema template. */
	public String type;
	/** JSON-formatted schema type constraints; can include its description. */
	public String constraints;
	/**
	 * Default value, stored as its string representation. Kept outside
	 * {@link #constraints} so can be accessed more easily.
	 */
	public String defaultValue;
	/**
	 * Description of the parameter. Maintained outside {@link #constraints}
	 * so it's easier to define in the learner description files.
	 */
	@Lob
	public String description;
	
	/**
	 * Required or not? Not considered part of {@link #constraints} as it is
	 * indicated differently in the PSI schema language and is not immediately
	 * part of the other schema constraints.
	 */
	public boolean required;

	
	public Parameter() { super(); }
	
	public Parameter(String name, String toolkitName, String type, String constraints,
			Object defaultValue, String description, boolean required)
	{
		this.name = name;
		this.toolkitName = toolkitName;
		this.type = type;
		this.constraints = constraints;
		//Must check type as integers are parsed into doubles when parsed from JSON
		this.defaultValue = defaultValue == null ? null : type.equals("$integer") ? String.valueOf( ((Double)defaultValue).intValue() ) : defaultValue.toString();
		this.description = description;
		this.required = required;
	}

	/**
	 * Returns the property name for this parameter when part of a task schema.
	 */
	public String getTaskSchemaKey() { return (required ? "/" : "?") + name; }
	
	/**
	 * Constructs and returns the JSON representation of the schema for this
	 * parameter, suitable as the value for its task schema
	 * {@linkplain #getTaskSchemaKey() key}.
	 */
	public JsonElement getTaskSchemaValue() {
		if (constraints == null && defaultValue == null && description == null)
			return Util.parseJSON(type);
		//Must build more complex schema object
		JsonObject args = constraints == null ? new JsonObject() : Util.parseJSON(constraints).getAsJsonObject();
		if (description != null)
			args.add( "description", Util.GSON.toJsonTree(description) );
		if (defaultValue != null)
			args.add( "default", Util.GSON.toJsonTree( getTypedDefaultValue() ) );
		
		JsonObject schemaValue = new JsonObject();
		schemaValue.add(type, args);
		return schemaValue;
	}

	/**
	 * Returns {@code true} if this parameter is boolean, which may be
	 * indicated by a simple {@code "boolean"} type or a parameterised
	 * {@code "$boolean"} reference.
	 */
	public boolean isBoolean() { return type.equals("boolean") || type.equals("$boolean"); }
	
	/** Returns the parameter's default value in the most appropriate type. */
	public Object getTypedDefaultValue() {
		return getTypedValue( defaultValue );
	}
	
	/**
	 * Returns an appropriately typed value for this parameter derived from the
	 * given String. Requires that all primitive types are specified as
	 * $-references to predefined PSI schema.
	 */
	public Object getTypedValue(String valueAsString) {
		switch (type) {
		case "$integer":	return Integer.parseInt( valueAsString );
		case "$number":		return Double.parseDouble( valueAsString );
		case "$boolean":	return Boolean.parseBoolean( valueAsString );
		case "$string": 	return valueAsString;
		default:			throw new UnsupportedOperationException("Unable to convert from string value '" + valueAsString + "' to appropriate value of type " + type);
		}
	}

	/**
	 * Returns a String value suitable for passing (perhaps with some further
	 * processing) to the underlying toolkit, e.g. {@code "-N 3"} for Weka or
	 * {@code "k=3"} for scikit-learn. This default implementation produces a
	 * string of the form this.toolkitName=value.toString(). 
	 */
	public String formatForToolkit(Object value) { return toolkitName + '=' + value; }

	/**
	 * Generates an array of parameter settings as they choose to format
	 * themselves using {@link #formatForToolkit(Object)}; this may not be
	 * appropriate for all toolkits. Subclasses can replace this behaviour.
	 */
	public static String[] formatSettingsForToolkit(Learner learner, Map<String,Object> settings) {
		List<String> options = new Vector<String>(settings.size());
		for (Map.Entry<String, Object> setting : settings.entrySet())
			options.add( learner.getParameter(setting.getKey()).formatForToolkit( setting.getValue() ) );
		return options.toArray(new String[0]);
	}
	
	public String toString() { return toolkitName + "=" + defaultValue; }
	
	public int hashCode() { return name.hashCode(); }
	
	/**
	 * Parameters are considered equal if they have the same name; it is
	 * assumed that comparisons will only take place for parameters belonging
	 * to a single learner, so names should be unique.
	 */
	public boolean equals(Object o) {
		return (o instanceof Parameter) && ((Parameter)o).name.equals(name);
	}
	
	//--JSON definition POJO--------------------------------------------------
	
	public static class Definition {
		/** Description of parameter that will be seen by the world. */
		public String description;
		/** Type, using one of the PSI schema predefined values; currently assumed to be any primitive. */
		public String type;
		/** A JSON object representing constraints; the arguments passed to a predefined schema. */
		public JsonElement constraints;
		/** The default value in any compatible type. */
		public Object defaultValue;
		/** Is the parameter mandatory? */
		public boolean required;
		/** The name as used in the underlying toolkit. */
		public String toolkitName;
	}
	
}
