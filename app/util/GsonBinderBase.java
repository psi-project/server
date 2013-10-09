package util;

import com.google.gson.JsonObject;

/**
 * General support for binding JSON objects and generic JSON; base class for
 * the {@link GsonBinder} and {@link GeneralGsonBinder}. Note: Cannot have this
 * parameterised and implementing TypeBinder, because Play won't correctly
 * register the subclasses to the types they are meant to bind. 
 */
public abstract class GsonBinderBase {
	/**
	 * A property with this name is set in a JsonObject result as the only
	 * property if there was an exception when parsing the given JSON string.
	 * The value of the property is an appropriate error message.
	 */
	public static final String ERROR_PROPERTY = "__error__";
	
	protected static final String EMPTY_OBJECT = "{}";
	
	/**
	 * Allows the binder to pass JSON syntax errors to the controller;
	 * otherwise the default behaviour of Play is to swallow errors or set the
	 * body argument to {@code null}, which obscures an error that should be
	 * reported back to the client as 400 bad request.
	 */
	protected JsonObject createErrorObject(Throwable error) {
		//Narrow down to actual cause of the error, which should contain just the message that a
		// client could find useful without revealing details of our server-side implementation.
		while (error.getCause() != null)
			error = error.getCause();
		JsonObject result = new JsonObject();
		result.addProperty(ERROR_PROPERTY, "Unable to parse the JSON request: " + error.getMessage());
		return result;
	}
}
