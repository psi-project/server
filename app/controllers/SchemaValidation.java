package controllers;

import java.util.List;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import util.ExternalResourceException;
import util.Schema;
import util.Util;

/**
 * This is for testing schema validation on arbitrary (PSI) schema and JSON
 * values.
 * 
 * @author jmontgomery
 *
 */
public class SchemaValidation extends CORSController {

	public static void testSchemaValidation(String jsontext, String schematext) {
		if (jsontext == null || schematext == null) {
			render();
		} else {
			try {
				JsonElement json = Util.parseJSON(jsontext);
				JsonObject schema = Util.parseJSON(schematext).getAsJsonObject();
				List<String> validationErrors = Schema.validateWithResolutionAgainstPSISchema(schema, json);
				render(jsontext, schematext, validationErrors);
			} catch (ExternalResourceException ere) {
				error("There was an unexpected error compiling the PSI schema to JSON schema. Details: " + ere.getMessage());
			}
		}
	}
    
}