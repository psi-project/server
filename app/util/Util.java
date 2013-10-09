/**
 * 
 */
package util;

import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;
import java.util.Vector;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import models.NamedModel;
import models.data.Query;

import play.Logger;
import play.Play;
import play.Play.Mode;
import play.data.parsing.UrlEncodedParser;
import play.mvc.Http.Request;
import play.mvc.Router;
import play.mvc.Scope.Params;

/**
 * Common utility methods, including some that Play framework should really supply. 
 * 
 * @author jmontgomery
 *
 */
public final class Util {
	/**
	 * A shared Gson instance with default settings and no escaping of
	 * characters (which interferes with PSI schema presentation while also
	 * being unnecessary).
	 */
	public static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();
	/**
	 * A shared Gson instance that pretty prints JSON output and no escaping of
	 * characters (which interferes with PSI schema presentation while also
	 * being unnecessary).
	 */
	public static final Gson PP_GSON = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();
	/**
	 * A shared Gson instance that pretty prints JSON output, does not escape
	 * characters and which includes {@code null} and, more importantly,
	 * {@code JsonNull} valued fields in the serialized structures it creates.
	 */
	public static final Gson PP_GSON_W_NULLS = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().serializeNulls().create();
	/** A shared JSON parser. */
	public static final JsonParser JSON_PARSER = new JsonParser();
	/** A date-time format that creates valid HTTP dates (such as for the Last-Modified header). */
	public static final DateFormat HTTP_DATE_FORMAT = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss 'GMT'");
	/** A date-time format that creates valid ISO8601 strings for UTC time. */
	public static final DateFormat UTC_DATETIME_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'");
	/** A date format suitable for including the date only as a compact string in a name. */
	public static final DateFormat TIME_BASED_NAME_FORMAT = new SimpleDateFormat("yyyyMMddHHmmssSSS");
	/** Base URL for the app; cached at start. */
	private static String BASE_URL = Play.configuration.getProperty( "application.baseUrl" );
	private static String BASE_URL_NO_SLASH;
	/** App URI, read from the configuration and cached upon first get. */
	private static URI APP_URI;
	
	static {
		BASE_URL_NO_SLASH = BASE_URL.replaceAll("\\/$","");
		TimeZone tz = TimeZone.getTimeZone("UTC");
		UTC_DATETIME_FORMAT.setTimeZone(tz);
		HTTP_DATE_FORMAT.setTimeZone(tz);
	}
	
	
	private Util() { }
	
	public static String generateTimeStampBasedName(String prefix, Date date) {
		return prefix + "_" + TIME_BASED_NAME_FORMAT.format(date);
	}
	
	public static String generateUUIDBasedName(String prefix) {
		return prefix + "_" + UUID.randomUUID();
	}
	
	/** Returns the configuration-set base URL for the application. */
	public synchronized static String getBaseURL() { return BASE_URL; }
	
	public synchronized static URI getAppURI() {
		if (APP_URI == null) {
			try {
				APP_URI = new URI(getBaseURL());
			} catch (URISyntaxException urise) {
				throw new RuntimeException("Base URL cannot be parsed as URI: " + getBaseURL(), urise);
			}
		}
		return APP_URI;
	}
		
	public static String emptyIfNull(Object o) { return o == null ? "" : o.toString(); }
	
	/**
	 * Reconstructs the full query path used in the given request.
	 */
	public static String requestPath() { return requestPath(true, null); }
	
	public static String requestPathNoQuery() { return requestPath(false, null); }
	
	/**
	 * Regenerates the request path which was broken down by Play; the result
	 * always <em>excludes</em> a trailing slash /. (The routes file may not
	 * care about the presence of trailing slashes in some paths, but for
	 * reporting to the client let's keep it consistent.
	 */
	public static String requestPath(boolean includeQuery, String extension) {
		Request request = Request.current();
		String path = request.path.replaceFirst("\\/$", "");
		return request.getBase() + path +
				( extension != null && ! extension.isEmpty() ? "/" + extension : "" ) +
				( !includeQuery || request.querystring.isEmpty() ? "" : "?" + request.querystring );
	}
	
	public static List<String> generateListAllPaths(List<? extends NamedModel> resources) {
		List<String> resourceURIs = new Vector<String>();
		for (NamedModel resource : resources)
			resourceURIs.add( Util.requestPath( false, resource.getName() ) );
		return resourceURIs;
	}

	/**
	 * Version of {@link #getReverseRoute(String, Object...)} that augments the
	 * map with query parameters from the given {@linkplain Query query}. 
	 */
	public static String getReverseRoute(String action, Query query, Object... otherMapParams) {
		return getReverseRoute(action, query.makeMap(otherMapParams));
	}
	
	/**
	 * Prerequisite: There must be an even number of {@code mapParams} as these
	 * are used to generate the {@code Map} passed to Play!'s reverse router. 
	 */
	public static String getReverseRoute(String action, Object... mapParams) {
		return getReverseRoute(action, makeMap(mapParams));
	}
	
	public static String getReverseRoute(String action, Map<String,Object> args) {
		//This doesn't always work, so just use configuration setting
		//return Request.current().getBase() + Router.reverse(action, args).url;
		return BASE_URL_NO_SLASH + Router.reverse(action, args).url;
	}
	
	/**
	 * Prerequisite: There must be an even number of items in {@code pairs} and
	 * every odd item must be a string, or it will be converted to a string.
	 * Null values will not be entered into the generated map.
	 */
	public static Map<String,Object> makeMap(Object... pairs) {
		Map<String,Object> map = new HashMap<String, Object>();
		for (int i = 0; i < pairs.length - 1; i += 2) {
			if (pairs[i+1] != null) 
				map.put(pairs[i].toString(), pairs[i+1]);
		}
		return map;
	}
	
	public static Map<String,Object> queryStringToMap(String queryString) {
		Map<String,Object> result = new HashMap<>();
		for (Map.Entry<String,String[]> pair : UrlEncodedParser.parseQueryString( new ByteArrayInputStream( queryString.getBytes() ) ).entrySet() )
			result.put(pair.getKey(), pair.getValue()[0]);
		return result;
	}
	
	/**
	 * Returns a list containing the given {@code items}.
	 */
	@SafeVarargs
	public static <T> List<T> makeList(T... items) {
		return Arrays.asList(items);
	}
	
	/**
	 * Returns {@code true} if the named parameter is set and has the value
	 * {@code "true"}, {@code false} otherwise.
	 * @throws ClassCastException if the named parameter is set but does not
	 * 		have a Boolean value.
	 */
	public static boolean hasSwitch(Params params, String name) {
		return params._contains(name) && params.get(name, Boolean.class);
	}
	
	public static boolean hostIsLocal(String uriString) throws URISyntaxException {
		return hostIsLocal( new URI(uriString) );
	}
	
	public static boolean hostIsLocal(URI uri) {
		final String host = uri.getHost();
		final int port = uri.getPort();
		final URI appURI = getAppURI();
		Logger.trace("In hostIsLocal(): app's URI read from configuration gives host:port = %s:%d; received host:port = %s:%d", appURI.getHost(), appURI.getPort(), host, port);
		return (Play.mode == Mode.DEV && (host.equals("localhost") || host.equals("127.0.0.1")) || appURI.getHost().equals(host))
				&& (appURI.getPort() + port == -2 || appURI.getPort() == port);
	}
	
	/**
	 * Binds the given JSON to an object of the stated class.
	 */
	public static <T> T jsonToPOJO(String json, Class<T> classOfT) {
		return GSON.fromJson(json, classOfT);
	}
	
	/** Parses the given (JSON) text into a {@link JsonElement}. */
	public static JsonElement parseJSON(String json) { return Util.JSON_PARSER.parse( json ); }
	
	public static String singleQuotesToDouble(String singleQuoted) { return singleQuoted.replaceAll("\\'", "\""); }
	
	/**
	 * An alternative to Play's {@link UrlEncodedParser#parse(String)} that is
	 * robust to being used outside of an active HTTP request (i.e., currently
	 * during testing, but conceivably if using a non-HTTP mechanism for system
	 * initialisation), but otherwise extremely simple and should thus only be
	 * used on internally generated or obtained URIs.
	 */
	public static Map<String,String> extractQueryArgs(URI uri) {
		try {
			Map<String,String> args = new HashMap<>();
			for (String param : uri.getRawQuery().split("&")) {
				String[] pair = param.split("=");
				if (pair.length != 2) throw new IllegalArgumentException("Current query argument pair is malformed: " + param);
				args.put(pair[0], URLDecoder.decode(pair[1],"UTF-8"));
			}
			return args;
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException("UTF-8 isn't supported by Java's URLDecoder? Reason given: " + e.getMessage());
		} catch (Exception e) {
			Logger.error("Problematic query string is: %s", uri.getQuery());
			throw e;
		}
	}

	
}
