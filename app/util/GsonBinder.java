package util;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

import util.Util;

import play.data.binding.Global;
import play.data.binding.TypeBinder;

/**
 * Should support automatic binding of JSON contents in a request body to a
 * JSON object.
 * <p>
 * Taken from <a href="http://www.playframework.org/community/snippets/6">
 * http://www.playframework.org/community/snippets/6</a>.
 */
@Global
public class GsonBinder extends GsonBinderBase implements TypeBinder<JsonObject> {

	/**
	 * Returns the parsed JsonObject or {@code null} if {@code value == null},
	 * even though that is a valid JSON object, there are no valid PSI requests
	 * that consist of such an object; requests that work on GET as well as
	 * POST can safely expect a {@code null} body if the Play-introduced "{}"
	 * is bound by this method. 
	 */
	@SuppressWarnings("rawtypes") //Since Play!'s TypeBinder does not include type wildcard in Class parameter, can't do so here
	public Object bind(String name, Annotation[] annotations, String value, Class actualClass, Type genericType) throws Exception {
		try {
			return value.equals(EMPTY_OBJECT) ? null : Util.parseJSON(value);
		} catch (JsonSyntaxException e) {
			return createErrorObject(e);
		}
    }
	
}
