/**
 * 
 */
package controllers;

import java.util.List;

import models.NamedModel;
import models.PSIMessage;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import play.Logger;
import play.Play;
import play.db.jpa.JPA;
import play.mvc.Before;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Http.Header;
import play.mvc.results.Status;
import util.ConfKeys;
import util.GsonBinder;
import util.Util;

/**
 * Superclass for all PSI controllers that sets the HTTP header
 * {@code Access-Control-Allow-Origin} to {@code *} to allow JavaScript
 * clients.
 * <p>
 * Also centralises support for checking if a JSON request was badly
 * formed (since Play!'s mechanism for dealing with these is to fail
 * silently without telling my code), and for rendering simple responses
 * consisting of a non-200 (but successful) status code and a single
 * information-carrying header field.
 */
public abstract class CORSController extends Controller {

	/** A Gson instance which will escape characters in strings. */
	private static final Gson GSON_W_ESCAPING = new Gson();
	
	/** Header property name for location (of created or other resource). */
	protected static final String LOCATION = "Location";
	
	//Note that Play! converts any received request headers to lowercase, so ones we check for must also be lowercase
	private static final String ACRH = "access-control-request-headers"; 
	private static final String ACAH = "Access-Control-Allow-Headers";
	private static final String ACEH = "Access-Control-Expose-Headers";
	private static final String ACRM = "access-control-request-method";
	private static final String ACAM = "Access-Control-Allow-Methods";
	
	/**
	 * Generates NOT FOUND with a template message that indicates the system
	 * thinks it knows what the user was looking for, and which points to the
	 * URI where a list of existing items of that type can be found.
	 * @param what name of the thing the client was looking for (must start
	 * with a consonant for the message to make sense at the moment)
	 * @param listAction the ControllerName.methodName action that would
	 * generate the required resource list.
	 */
	protected static void notFoundSeeList(final String what, final String listAction, Object... actionArgs) {
		notFound("You appear to be looking for a " + what + " that does not exist. The list of available " + what + "s is at: " + Util.getReverseRoute(listAction, actionArgs));
	}
	
	/**
	 * Supports sending back details of why the request was bad, something that
	 * the designers of Play didn't seem to think was important.
	 */
	protected static void badRequest(String message) {
		JPA.setRollbackOnly(); //Since Play DOES NOT do this automatically, as I previously believed (and, frankly, as it should on an error())
		if (request.format.equals("json")) //since (1) Gson does not expose character escaping functions and (2) the 400.json file is just a text template
			message = GSON_W_ESCAPING.toJsonTree(message).toString().replaceAll("(^\\\"|\\\"$)", "");
		error(Http.StatusCode.BAD_REQUEST, message);
	}

	/**
	 * Subclasses that expect a non-null, valid JSON request should call this
	 * with the GsonBinder-parsed request (which may be a specially-created
	 * object with a single error property set) to check that the request was
	 * syntactically valid JSON. If it isn't then this method will use
	 * {@link #error(int, String)} to report a bad request to the client.
	 */
	protected static void failOnJSONSyntaxError(JsonObject req) {
		if (req.has(GsonBinder.ERROR_PROPERTY))
			badRequest(req.get(GsonBinder.ERROR_PROPERTY).getAsString());
	}
	
	/**
	 * Subclasses should call this immediately after parsing a JSON request
	 * into a specific subclass of {@link PSIRequest} to ensure that the
	 * request appears to be valid (has the correct type field, etc.).
	 */
	protected static void failOnInvalidMessage(PSIMessage req) {
		if (! req.isValid())
			badRequest(req.getValidationErrorMessage());
	}
	
	/**
	 * Subclasses can use this method to perform the following common sequence:
	 * {@linkplain #failOnJSONSyntaxError(JsonObject) check the received JSON
	 * request was free from errors}, extract the
	 * specified POJO from the JSON request body, and {@linkplain
	 * #failOnInvalidMessage(PSIRequest) perform basic validation on the
	 * request message}.
	 */
	protected static <T extends PSIMessage> T parseAndCheckRequestBody(JsonObject body, Class<T> messageClass) {
		failOnJSONSyntaxError(body);
		T req = Util.GSON.fromJson(body, messageClass);
		failOnInvalidMessage(req);
		return req;
	}
	
	@Before 
	public static void setCORS() {
		Logger.info("Received request: %s %s from %s", request.method, Util.requestPath(), request.remoteAddress);
		if (request.headers.containsKey("origin")) {
			Logger.trace("CORS request headers: %s", request.headers);
			
			Http.Response.current().accessControl("*");
			if (Http.Request.current().headers.containsKey(ACRH)) {
				response.headers.put(ACAH, Http.Request.current().headers.get(ACRH));
			}
			//Allow Location header field to be seen in CORS requests; may need to expand this list (Actually, it's on the whitelist, so this shouldn't be necessary)
			Http.Response.current().setHeader(ACEH, "Location");
			Logger.trace("After setting CORS headers, Http.Response.current().headers == %s", Http.Response.current().headers);
		}
	}
	
	public static void basicOptions() {
		Logger.trace("Received an OPTIONS request: Request headers are %s", Http.Request.current().headers);
		//The CORS should be handled by the superclass's @Before handler
		response.status = Http.StatusCode.OK;
		//FIXME Should probably be more restrictive than this
		if (request.headers.containsKey(ACRH)) {
			response.headers.put(ACAH, request.headers.get(ACRH));
		}
		if (request.headers.containsKey(ACRM)) {
			//Required to support responses to POSTs that redirect (to GET) when the request is preceded by a preflight OPTIONS request
			Header acrmHeader = request.headers.get(ACRM);
			response.headers.put(ACAM, acrmHeader);
		}
		Logger.trace("Response to OPTIONS request has these headers: %s", Http.Response.current().headers);
	}
	
	/**
	 * Performs pretty printing using Gson before returning an
	 * {@code application/json} response. Include {@code null}-valued fields,
	 * since if they are still present in the given {@code JsonElement} then
	 * whoever performed the original serialization clearly intended for them
	 * to be kept.
	 */
	protected static void renderJSON(JsonElement json) {
		renderJSON( Util.PP_GSON_W_NULLS.toJson(json) );
	}

	/**
	 * Serializes the given object and then pretty prints it using Gson before
	 * returning an {@code application/json} response. Null-valued fields are
	 * not serialized.
	 */
	protected static void renderJSON(Object o) {
		renderJSON( Util.PP_GSON.toJson(o) );
	}
	
	/** Convenience method to set the status, set a single header field and respond to the client. */
	protected static void renderSimpleResponse(final int status, final String header, final String value, Http.Response responseToUse) {
		responseToUse = responseToUse == null ? Http.Response.current() : responseToUse;
		if (header != null) responseToUse.setHeader(header, value);

		responseToUse.headers.put(ACAM, new Header(ACAM, java.util.Arrays.asList(new String[]{"GET", "OPTIONS"})));	
		
		Logger.trace("Prior to sending, the status only response (%d) has these headers: %s", status, responseToUse.headers);
		throw new Status(status);
	}
	
	protected static void renderCreated(final String location, Http.Response responseToUse) {
		renderSimpleResponse(Http.StatusCode.CREATED, "Location", location, responseToUse);
	}
	
	protected static void renderDeleted() {
		renderSimpleResponse(Http.StatusCode.NO_RESPONSE, null, null, null);
	}

	/**
	 * Renders a sorted URI list in JSON or HTML using the current request path
	 * and names of all persistent models for the given {@code modelClass}.
	 * @see #listAll(List, boolean) 
	 */
	protected static void listAll(Class<? extends NamedModel> modelClass) {
		listAll(modelClass, null);
	}
	
	/**
	 * Renders a URI list in JSON or HTML using the current request path and
	 * names of all persistent models for the given {@code modelClass}.
	 * @param kind An additional parameter that will be passed to the HTML
	 * template renderer and thus will be available for use in the relevant
	 * controller's template, if needed.
	 * @see #listAll(Class, String) 
	 */
	@SuppressWarnings("unchecked") //The 'name' property is of type string in every NamedModel in this system
	protected static void listAll(Class<? extends NamedModel> modelClass, String kind) {
		listAll((List<String>) JPA.em().createQuery("select name from " + modelClass.getSimpleName() + " order by name").getResultList(), kind);
	}

	/**
	 * Renders a URI list in JSON or HTML using the current request path and
	 * given resource names. If resources are database models then use
	 * {@link #listAll(Class) or #listAll(Class, String)} instead.
	 * @see #listAll(List, String)
	 */
	protected static void listAll(final List<String> resourceNames) {
		listAll(resourceNames, null);
	}

	/**
	 * Renders a URI list in JSON or HTML using the current request path and
	 * given resource names. If resources are database models then use
	 * {@link #listAll(Class) or #listAll(Class, String)} instead. Resource
	 * collections are all rendered the same way, only the content of the
	 * collection changes. If rendering HTML, an appropriate {@code
	 * listAll.html} template needs to be defined for the appropriate
	 * controller though, in which the resource list must be called {@code
	 * resources} (since Play does some automagical matching using the argument
	 * variable's <em>name</em>).
	 */
	protected static void listAll(final List<String> resourceNames, String kind) {
		String representation = Application.generateResourceListAsJSON( Util.requestPath(), resourceNames);
		if (request.format == null || request.format.equals("json"))
			renderJSON( representation );
		else if (request.format.equals("html"))
			render(resourceNames, kind, representation);
		renderText("Cannot represent request PSI Resource List in " + request.format);
	}
	
}
