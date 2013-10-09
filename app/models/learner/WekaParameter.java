/**
 * 
 */
package models.learner;

import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.persistence.Entity;


/**
 * Describes an option for a Weka {@link Learner}.
 *
 * @author jmontgomery
 *
 */
@Entity
public class WekaParameter extends Parameter {
	private static final long serialVersionUID = 1L;

	public WekaParameter() { super(); }
	
	public WekaParameter(String name, String toolkitName, String type, String constraints,
			Object defaultValue, String description, boolean required)
	{
		super(name, toolkitName, type, constraints, defaultValue, description, required);
	}

	/**
	 * Returns a String value suitable for passing (perhaps with some further
	 * processing) to Weka, e.g. if the toolkitName is '-N' and value is the
	 * integer 3, then returns {@code "-N 3"}. If this parameter has type
	 * boolean and the value is {@code false}, then returns {@code null}.
	 * @throws ClassCastException if {@code value} has the wrong type.  
	 */
	public String formatForToolkit(Object value) {
		if (isBoolean())
			return ((Boolean)value) ? toolkitName : null;
		return toolkitName + " " + value;
	}
	
	public String toString() { return toolkitName + (isBoolean()? "" : " " + defaultValue); }
	
	public static String[] formatSettingsForToolkit(Learner learner, Map<String,Object> settings) {
		List<String> options = new Vector<String>(settings.size());
		for (Map.Entry<String, Object> setting : settings.entrySet()) {
			//This will generate an exception if there is no corresponding parameter, but the schema should have verified that before now
			String option = learner.getParameter(setting.getKey()).formatForToolkit( setting.getValue() );
			if (option != null) //if parameter is boolean and explicit value of false given, then option will be null
				for (String optionPart : option.split("\\s"))
					options.add( optionPart ); 
		}
		return options.toArray(new String[0]);
	}

}
