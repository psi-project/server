package controllers;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import play.data.parsing.UrlEncodedParser;
import util.ExternalResourceException;
import util.Schema;
import util.SchemaTemplate;
import util.SchemaTemplateLibrary;
import util.Util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;


public class Schemas extends CORSController {

	public static void listAll() { listAll( SchemaTemplateLibrary.instance().sortedNames() ); }
	
	/**
	 * Retrieves the schema text associated with the named schema; supports
	 * returning the schema text with all template variables shown and quoted
	 * as well as setting the values of template variables. If template is
	 * requested then no template variables will be set, even if present in
	 * request. Currently supported switches and their expected behaviour:
	 * 
	 * <ul>
	 *   <li><strong>template</strong>={true,false}: if present and
	 *   	{@code true} then returns the schema text with all template
	 *   	variables presented and quoted.</li>
	 *   <li><strong>&lt;name&gt;</strong>=&lt;value&gt;: Replaces the template
	 *   	variable &lt;name&gt; with &lt;value&gt;. The properties for unset
	 *  	template variables are removed from the schema string that is
	 *  	returned.</li>
	 * </ul>
	 */
	public static void describe(String s) {
		SchemaTemplate template = retrieveTemplate(s);
		if (Util.hasSwitch(params, "template"))
			renderJSON( Util.PP_GSON.toJson( template.asJSON() ) );
		renderJSON( Util.GSON.toJson( template.asJSON( queryToArgs() ) ) );
	}
		
	private static Map<String,Object> queryToArgs() {
		Map<String,Object> args = new HashMap<>();
		for (Map.Entry<String,String[]> entry : UrlEncodedParser.parseQueryString(new ByteArrayInputStream(request.querystring.getBytes())).entrySet())
			if (!entry.getKey().equals("template"))
				args.put(entry.getKey(), entry.getValue()[0]);
		return args;
	}
	
	//--Out of spec additions--------------------------------------------------
	
	public static void validate(String s, JsonElement body) {
		try {
			Schema validationSchema = new Schema( retrieveTemplate(s), queryToArgs() );
			List<String> validationErrors = validationSchema.validateWithResolution(body);
			renderJSON( new Schema.Validation(validationErrors, body) );
		} catch (ExternalResourceException ere) {
			error("There was an unexpected error while resolving either the schema or JSON value. Details: " + ere.getMessage());
		}
	}

	private static SchemaTemplate retrieveTemplate(String id) {
		SchemaTemplate template = SchemaTemplateLibrary.instance().get(id);
		if (template == null)
			notFound();
		return template;
	}
	
	//Non-publicly accessible operations (i.e., although currently publicly accessible routes do point to these methods)
	
	public static void compile(String s) {
		try {
			renderJSON( new Schema(retrieveTemplate(s), queryToArgs()).asCompiledJSON() );
		} catch (ExternalResourceException ere) {
			error("There was an unexpected error compling the PSI schema to JSON schema. Details: " + ere.getMessage());
		}
	}

	public static void compileAny(String schemaText, JsonObject schemaArgs) {
		if (schemaText == null)
			render();
		try {
			renderJSON( new Schema(schemaText, schemaArgs).asCompiledJSON()	);
		} catch (ExternalResourceException ere) {
			error("There was an unexpected error compiling the PSI schema to JSON schema. Details: " + ere.getMessage());
		}
	}

}