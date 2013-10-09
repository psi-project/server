/**
 * 
 */
package util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import play.Logger;
import play.Play;
import play.libs.WS;
import play.libs.WS.HttpResponse;
import play.mvc.ActionInvoker;
import play.mvc.Http;
import play.mvc.Http.Header;
import play.utils.HTTP;

import models.PSIResponse;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import util.Util;

/**
 * A collection of utilities for connecting to a server over HTTP and mapping
 * the response into JSON or a {@link PSIResponse}.
 * 
 * @author jmontgomery
 *
 */
public final class HttpUtil {
	/** Default encoding used. */
	public static final String ENCODING = "UTF-8";

	/**
	 * Used in internal HTTP requests so that any errors generated use the
	 * correct (JSON) template.
	 */
	private static final Map<String,Header> REQUEST_HEADERS;
	static {
		Map<String,Header> requestHeaders = new HashMap<String, Http.Header>();
		requestHeaders.put("Accept", new Header("Accept", "application/json"));
		REQUEST_HEADERS = Collections.unmodifiableMap(requestHeaders);
	}
	
	private HttpUtil() { }
	
	public static boolean jsonElementIsReference(JsonElement el) {
		return el.isJsonPrimitive() && el.getAsJsonPrimitive().isString() && el.getAsString().charAt(0) == '$';
	}
	
	/**
	 * Sends an argumentless GET request to the given URI, retrieves the
	 * response and maps it into the corresponding {@link PSIResponse}; throws
	 * a range of runtime exceptions if the connection failed, the response
	 * wasn't in JSON or the response didn't correspond to a known
	 * {@code PSIResponse}.
	 * @param uri URI of PSI service to send GET request to
	 * @throws ExternalResourceException 
	 */
	@SuppressWarnings("unchecked") //Can't be sure, but jsonToResponse will throw an exception if it isn't the right type
	public static <T extends PSIResponse> T getPSIResponse(String uri) throws ExternalResourceException {
		return (T) PSIResponse.jsonToMessage( getJSON(uri, null) );
	}

	
	/**
	 * Sends a GET request to the given URI, retrieves the response and maps it
	 * into the corresponding {@link PSIResponse}; throws a range of runtime
	 * exceptions if the connection failed, the response wasn't in JSON or the
	 * response didn't correspond to a known {@code PSIResponse}.
	 * @param uri URI of PSI service to send GET request to
	 * @param args GET arguments, can be {@code null} if no arguments required
	 * @throws ExternalResourceException 
	 */
	@SuppressWarnings("unchecked") //Can't be sure, but jsonToResponse will throw an exception if it isn't the right type
	public static <T extends PSIResponse> T getPSIResponse(String uri, Map<String,Object> args) throws ExternalResourceException {
		Logger.trace("Retrieving PSIResponse from %s", uri);
		return (T) PSIResponse.jsonToMessage( getJSON(uri, args) );
	}
	
	/**
	 * Sends a GET request to the given URI, retrieves the response and maps it
	 * into a {@code JsonObject}.
	 * @param uri URI to send GET request to
	 * @param args GET arguments, can be {@code null} if no arguments required
	 * @throws ExternalResourceException If the resource referenced by
	 * 	{@code uriString} could not be retrieved at all or has an unsuitable
	 *  representation (not JSON or not a JSON object).
	 */
	public static JsonObject getJSON(String uriString, Map<String,Object> args) throws ExternalResourceException {
		try {
			Logger.trace("Retrieving JSON response from %s, with given query arguments %s", uriString, args);
			JsonElement jsonResponse;
			String queryString = createQueryString(args);
			URI uri = new URI(uriString + (queryString.isEmpty() ? "" : (uriString.contains("?") ? "&" : "?") + queryString));
	
			//In DEV mode only have a single thread, which appears to cause HTTP requests to self to fail
			if ( Play.mode == Play.Mode.DEV && Util.hostIsLocal(new URI(uriString)) ) {
				Logger.trace("Host believed to be local when retrieving JSON response from %s", uri);
				jsonResponse = getLocal(uri);
			} else {
				HttpResponse response = WS.url(uri.toString()).get();
				if (response.getStatus() < 200 || response.getStatus() > 299)
					throw new ExternalResourceException("Unable to retrieve resource at " + uriString);
				if (! HTTP.parseContentType( response.getContentType() ).contentType.equals("application/json"))
					Logger.warn("Expected content type application/json for resource at %s, but reported content type is %s", uri, response.getContentType());
				jsonResponse = response.getJson();
				Logger.trace("Received this JSON reponse from %s:\n%s", uriString, jsonResponse.toString());
			}
			
			if (! jsonResponse.isJsonObject())
				throw new ExternalResourceException("Response from resource at '" + uriString + "' is valid JSON but is not a JSON object, which was expected. Response: " + jsonResponse);
			return jsonResponse.getAsJsonObject();
		} catch (URISyntaxException urise) {
			throw new RuntimeException(urise);
		}
	}
	
	/**
	 * Sends a POST request to the given URI, retrieves the response and maps
	 * it into a {@code JsonObject}. Makes no distinction between 'local' and
	 * 'external' service URIs. If the response is empty or not in JSON then a
	 * new JSON object is created with a 'body' property that contains the
	 * textual content returned and a 'success' property indicting if the
	 * response indicates success.
	 * @param uri URI to POST the request to
	 * @param request the JSON request to send; if {@code null} then an empty
	 * request body will be sent.
	 */
	public static JsonObject postJSON(String uri, JsonObject request) {
		 HttpResponse response = WS.url(uri)
				.body( request == null ? "" : request.toString())
				.mimeType("application/json")
				.post();
		 JsonElement jsonResponse = response.getJson(); 
		if (! jsonResponse.isJsonObject()) {
			JsonObject jsonObj = new JsonObject();
			jsonObj.add("body", jsonResponse);
			jsonObj.add("success", new JsonPrimitive( response.success() ));
			jsonResponse = jsonObj;
		}
		return jsonResponse.getAsJsonObject();
	}
	
	private static String createQueryString(Map<String,Object> args) {
		try {
			if (args != null) {
				StringBuilder sb = new StringBuilder();
				for (Map.Entry<String,Object> arg : args.entrySet())
					sb.append(arg.getKey()).append("=").append(URLEncoder.encode(arg.getValue().toString(), ENCODING)).append("&");
				sb.deleteCharAt(sb.length() - 1); //remove additional &, since Java doens't provide a nice way of knowing if at last element
				return sb.toString();
			}
			return "";
		} catch (UnsupportedEncodingException uee) {
			throw new RuntimeException("Error encoding query string. Why is UTF8 not supported? Message: " + uee);
		}
	}

	/**
	 * Uses the play router to send request to appropriate local controller,
	 * rather than issuing a genuine HTTP request to localhost.
	 * @param url URL, including any query parameters, to send GET request to
	 */
	public static JsonElement getLocal(URI uri) {
		Logger.trace("HttpUtil.getLocal(): uri.getPath() == '%s', uri.getQuery() == '%s'", uri.getPath(), uri.getQuery());
		Http.Request httpRequest =
			Http.Request.createRequest("HttpUtil.getLocal()", "GET", uri.getPath(), Util.emptyIfNull( uri.getRawQuery() ),
					"text/plain", new ByteArrayInputStream( "".getBytes() ), uri.toString(), uri.getHost(),
					false, uri.getPort() == -1 ? 80 : uri.getPort(), uri.getHost(), false, REQUEST_HEADERS, null);
		Http.Response httpResponse = new Http.Response();
		httpResponse.out = new ByteArrayOutputStream(4096);
	
		Logger.trace("Attempting to invoke internal action at uri %s", uri);
		ActionInvoker.invoke(httpRequest, httpResponse);
		Logger.trace("Response status: " + httpResponse.status);
		
		try {
			String content = httpResponse.out.toString( httpResponse.encoding );
			if (httpResponse.status != Http.StatusCode.OK)
				throw new RuntimeException("Unable to retrieve content at " + uri + ". Status " + httpResponse.status);// + ", with message starting:\n" + content.substring(0,200));
			return Util.parseJSON(content);
		} catch (UnsupportedEncodingException uee) {
			throw new RuntimeException("Unable to decode response using its own specified encoding.", uee);
		}
		
	}
	
}
