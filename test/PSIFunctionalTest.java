import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import models.PSIMessage;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonIOException;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import play.Play;
import play.test.*;
import play.mvc.Http;
import play.mvc.Http.*;
import util.ExternalResourceException;
import util.Schema;
import util.Util;

/**
 * A variant of Play's {@link FunctionalTest} in which the {@code newRequest}
 * method returns a request that accepts application/json.
 * 
 * Note that JSON validation requires that the PSI API schema actually reside
 * at the URI given by its 'id' property.
 *
 */
public abstract class PSIFunctionalTest extends FunctionalTest {
	protected static final Gson GSON = new Gson();

	/**
	 * Common headers for PSI requests are currently limited to just
	 * {@code Accept:application/json} . 
	 */
	protected static final Map<String,Header> COMMON_HEADERS;
	static {
		Header[] headers = { new Header("accept","application/json") };
		Map<String,Header> headerMap = new HashMap<>();
		for (Header header : headers)
			headerMap.put(header.name, header);
		COMMON_HEADERS = Collections.unmodifiableMap( headerMap ); 
	}

	/**
	 * A pure JSON schema for JSON messages used in the PSI API. Loading this
	 * is, however, largely a waste of time, since json-schema-validator
	 * appears to immediately go off to the web to retrieve the entire schema
	 * when it encounters one of the message type schema fragment pointers
	 * (even though they all point to within the schema it's already got).
	 */ 
	protected static final JsonObject PSI_API_SCHEMA;
	static {
		try {
			PSI_API_SCHEMA = Util.JSON_PARSER.parse( new FileReader( Play.getFile("private/schema/v4/psi-api") )).getAsJsonObject();
		} catch (JsonIOException | JsonSyntaxException | FileNotFoundException e) {
			throw new Error("Unable to load PSI API schema for use in testing", e);
		}
	}

	/**
	 * Version of {@link FunctionalTest#newRequest()} that ensures the headers
	 * are set to {@link #COMMON_HEADERS}.
	 */
	public static Request newJSONRequest() {
		Request request = Request.createRequest(
				null,
				"GET",
				"/",
				"",
				null,
				null,
				null,
				null,
				false,
				80,
				"localhost",
				false,
				COMMON_HEADERS,
				null
				);
		return request;
	}
	
	/**
	 * A combination of {@link #GET(Object)} that accepts application/json
	 * and an {@link #okResponse(Response)} test; the request URI is passed
	 * through {@link #makeRelative(String)} first and the response is
	 * immediately converted to a JSON object if status was OK.
	 */
	public static JsonObject GET_JSON(String url) {
		Response response = GET(newJSONRequest(), makeRelative(url));
		okResponse(response);
		return asJsonObject(response);
	}

	/**
	 * See {@link #POST_JSON(String, JsonObject)}; given pseudo-JSON text is
	 * converted to a JSON object first using {@link #stringToJSON(String)}.
	 */
	public static Response POST_JSON(String url, String singleQuotedJSON) {
		return POST_JSON(url, stringToJSON(singleQuotedJSON));
	}

	/**
	 * See {@link #POST_JSON(String, JsonObject)}; given PSIMessage is
	 * serialzed to a JSON string first by Gson.
	 */
	public static Response POST_JSON(String url, PSIMessage body) {
		return POST_JSON(url, GSON.toJsonTree(body).getAsJsonObject());
	}
	
	/**
	 * A version of {@link #POST(Request, Object, String, String)} that
	 * sends and accepts application/json; the request URI is passed through
	 * {@link #makeRelative(String)} first. Unlike {@link #GET_JSON(String)}
	 * the response is not automatically converted to a JSON object, since
	 * many expected responses are status code plus header information only.
	 */
	public static Response POST_JSON(String url, JsonObject body) {
		return POST(newJSONRequest(), makeRelative(url), "application/json", body.toString());
	}
	
	/**
	 * A variant of Play's {@link #DELETE(String)} that also verifies the
	 * status-only response is as expected; the request URI is passed through
	 * {@link #makeRelative(String)} first.
	 */
	public static void DELETE_OK(String url) {
		Response response = DELETE( makeRelative(url) );
		assertTrue("Response to DELETE should be a suitalbe success status (200 OK or 204 No Content). Instead it was "
					+ response.status + " with response body of\n" + getContent(response), response.status == Http.StatusCode.OK || response.status == Http.StatusCode.NO_RESPONSE);
	}
	
	/**
	 * Retrieve a resource-list representation and extract the list of URIs it
	 * contains. Can optionally also test that the list is not empty.
	 */
	public static List<String> getResourceList(String collection, boolean assertNotEmpty) {
		JsonArray resourceList = GET_JSON( getProp(GET_JSON("/"), collection) ).get("resources").getAsJsonArray();
		if (assertNotEmpty)
			assertTrue("Expected " + collection + " to contain at least one resource", resourceList.size() > 0 );
		List<String> uris = new Vector<>(resourceList.size());
		for (JsonElement el : resourceList)
			uris.add( el.getAsString() );
		return uris;
	}
	
	/**
	 * Retrieve the representations of each resource in the resource-list for
	 * the stated collection. Does no testing other than for 200 OK on each
	 * resource representation retrieved.
	 * @see #getResources(Collection<String>)
	 */
	public static List<JsonObject> getResources(String collection) {
		return getResources(getResourceList(collection, false));
	}

	/**
	 * Retrieve the representations of each resource referred to in the given
	 * JsonArray of URIs. Does no testing other than for 200 OK on each
	 * resource representation retrieved. Accepts JsonElement rather than
	 * JsonArray to save the many callers who would otherwise have to call
	 * array.getAsJsonArray().
	 * @see #getResource(List<String>)
	 * @throws IllegalArgumentException if the argument is not a JsonArray.
	 */
	public static List<JsonObject> getResources(JsonElement uriArray) {
		return getResources( jsonArrayToStringList(uriArray) );
	}
	
	/**
	 * Retrieve the representations of each resource referred to in the given
	 * collection of URIs. Does no testing other than for 200 OK on each
	 * resource representation retrieved.
	 * @see #getResourceList(String, boolean)
	 * @see #GET_JSON(String)
	 */
	public static List<JsonObject> getResources(List<String> uris) {
		List<JsonObject> resources = new Vector<>(uris.size());
		for (String uri : uris)
			resources.add( GET_JSON(uri) );
		return resources;
	}
	

	/**
	 * Returns the given URI without any leading "http://localhost". This is
	 * required because Play's GET() cannot retrieve 'external' URIs, which
	 * means that (1) the request URI received by the system lacks the port
	 * number and so subsequently generated URIs it returns won't be valid,
	 * and (2) it wouldn't be able to retrieve them even if they were valid.
	 */
	protected static String makeRelative(final String uri) {
		return uri.replaceAll("^http://localhost(?:\\:9000)?","");
	}
	
	protected static List<String> makeRelative(JsonArray uriArray) {
		List<String> relativeURIs = new Vector<>(uriArray.size());
		for (JsonElement el : uriArray)
			relativeURIs.add( makeRelative(el.getAsString() )  );
		return relativeURIs;
	}
	
	/**
	 * The opposite of {@link #makeRelative(String)} in that it ensures the URI
	 * is complete and includes the 9000 port number that Play's test harness
	 * has probably removed from requests.
	 */
	protected static String makeFull(String uri) {
		final String fullHost = "http://localhost:9000/";
		return uri.replaceAll("^http://localhost/", fullHost).replaceAll("^([^h])", fullHost + "\\1");
	}
	
	/**
	 * Accepts JsonElement rather than JsonArray to save the many callers who
	 * would otherwise have to call array.getAsJsonArray().
	 * @throws IllegalArgumentException if the argument is not a JsonArray.
	 */
	protected static List<String> jsonArrayToStringList(JsonElement array) {
		if (! array.isJsonArray())
			throw new IllegalArgumentException("Argument must be a JsonArray. Received this: " + array);
		JsonArray asArray = array.getAsJsonArray();
		List<String> strings = new Vector<>(asArray.size());
		for (JsonElement el : asArray)
			strings.add( el.getAsString() );
		return strings;
	}
	
	/** Convenience method equivalent to {@code json.get(property).getAsString()}. */
	protected static String getProp(JsonObject json, String property) {
		return json.get(property).getAsString();
	}

	/** Is response 200 OK, the content type and encoding what are expected? */ 
	protected static void okResponse(Response response, final String expectedContentType) {
		assertIsOk(response);
		assertContentType(expectedContentType, response);
		assertCharset(play.Play.defaultWebEncoding, response);
	}

	/** Is response 200 OK, the content type 'application/json' and encoding what's expected? */
	protected static void okResponse(Response response) {
		okResponse(response, "application/json");
	}

	/** Is response 200 OK, the content type 'text/html' and encoding what's expected? */
	protected static void okHTMLResponse(Response response) {
		okResponse(response, "text/html");
	}
	
	/** An improved version of Play's {@link FunctionalTest#assertStatus(int, Response)}
	 * that displays the response body if the status is not what is expected.
	 */
	public static void assertStatus(int status, Response response) {
		assertEquals("Response body: " + getContent(response) + "\nResponse status ", (Object) status, response.status);
	}

	/**
	 * Returns the response's content as a {@link JsonObject}; fails if there
	 * are JSON syntax errors or the response is valid JSON but does not
	 * represent an object.
	 */
	protected static JsonObject asJsonObject(Response response) {
		try {
			JsonElement json = Util.parseJSON(getContent(response));
			assertTrue("Response is valid JSON but is not a JSON object.", json.isJsonObject());
			return json.getAsJsonObject();
		} catch (JsonSyntaxException e) {
			fail("Response is not valid JSON: " + e.getMessage());
			return null; //unreachable code, but function is more elegantly written with content entirely inside try block. 
		}
	}

	/**
	 * Converts the given (invalid) JSON text that uses single quotes instead
	 * of double quotes into a {@code JsonObject}. 
	 */
	protected static JsonObject stringToJSON(final String singleQuotedJSON) {
		return Util.parseJSON( singleQuotedJSON.replaceAll("'","\"") ).getAsJsonObject();
	}
	
	/**
	 * Tests that the given JSON is equal to the JSON object defined by the
	 * given almost-JSON text (i.e., the text can use single instead of double
	 * quotes.
	 */
	public static void assertExpectedJSON(JsonObject json, String expectedSingleQuoteJSON) {
		assertEquals("JSON structure mismatch.", json, stringToJSON(expectedSingleQuoteJSON));
	}
	
	/**
	 * A weaker test than {@link #assertExpectedJSON(JsonObject, String)} that
	 * merely tests for the presence of all named properties (the names of which
	 * presumably are not given in the PSI spec.
	 */
	public static void assertHasProperties(JsonObject json, String... properties) {
		List<String> missing = new ArrayList<String>( Arrays.asList(properties) );
		for (Map.Entry<String,JsonElement> p : json.entrySet())
			missing.remove(p.getKey());
		assertTrue("Missing expected properties: " + prettyList(missing), missing.isEmpty());
	}

	/**
	 * Tests that the given JSON object is valid against the PSI API. Because
	 * the JSON Schema validator is passed the schema to use (i.e., the schema
	 * is not accessible over the web, even if the validator were to support
	 * that) we can't use fragment identifiers to restrict the validation to
	 * the expected type, which is why that is given as a second argument.
	 */
	protected static void assertValidPSIMessage(JsonObject json, final String expectedPSIType) {
		assertValidJSON(json, PSI_API_SCHEMA, "PSI API");
		assertEquals("response.psiType not what expected", expectedPSIType, getProp(json, "psiType"));
	}

	/**
	 * Tests that the given JSON object is valid against the given JSON schema;
	 * values are compiled first so that, if they are resource representations
	 * containing PSI schema, that PSI schema is transformed into valid JSON
	 * schema. The given schema, however, must be JSON, not PSI, schema.
	 * @param schemaName the label injected into the failure message to 'name'
	 * the schema used in validation (e.g., 'PSI API', 'emits', 'accepts').
	 */
	protected static void assertValidJSON(JsonElement json, JsonObject schema, final String schemaName) {
		try {
			List<String> errors = Schema.validateWithResolution(schema, json);
			assertEquals("JSON is not valid against " + schemaName + " schema.\nJSON: " + json + "\nSchema: " + schema + "\nErrors: " + errors, 0, errors.size());
		} catch (ExternalResourceException e) {
			fail("Unable to test valid JSON assertion because of problem resolving an external resource referenced by the test value: " + e.getMessage());
		}
	}
	
	/** Returns a string representation of the list without the default bounding brackets [ ] .*/
	protected static String prettyList(List<String> list) {
		return list.toString().replaceAll("(?:^\\[|\\]$)","");
	}

}