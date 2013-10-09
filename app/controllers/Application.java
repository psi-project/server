package controllers;

import java.util.*;

import play.mvc.With;

import util.Util;

import models.*;

public class Application extends CORSController {
	
	public static void index() {
		String representation = Util.PP_GSON.toJson( new PSI.Service( Util.requestPathNoQuery() ) );
		if (request.format == null || request.format.equals("json"))
			renderJSON( representation );
		else if (request.format.equals("html")) {
			String root = Util.requestPathNoQuery();
			render(root, representation);
		}
		renderText("Cannot represent this PSI Service in " + request.format);
	}
   
    /**
     * Creates a JSON URI list of the given resource names; for use by other
     * {@code Controller}s to ensure consistency in list presentation.
     */
	public static String generateResourceListAsJSON(String uri, List<String> modelNames) {
		return Util.PP_GSON.toJson( new PSI.ResourceList( uri, Util.requestPathNoQuery(), modelNames) );
	}
    
}
