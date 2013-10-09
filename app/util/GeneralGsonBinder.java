package util;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import com.google.gson.JsonElement;
import com.google.gson.JsonSyntaxException;

import util.Util;

import play.data.binding.Global;
import play.data.binding.TypeBinder;

/**
 * For the (rare, in PSI) automatic binding of JSON elements from the request
 * body (not just JSON objects like {@link GsonBinder}).
 */
@Global
public class GeneralGsonBinder extends GsonBinderBase implements TypeBinder<JsonElement> {

	/** Returns the parsed JsonElement. */
	@SuppressWarnings("rawtypes") //Since Play!'s TypeBinder does not include type wildcard in Class parameter, can't do so here
	public Object bind(String name, Annotation[] annotations, String value, Class actualClass, Type genericType) throws Exception {
		try {
			return Util.parseJSON(value);
		} catch (JsonSyntaxException e) {
			return createErrorObject(e);
		}
    }
	
}
