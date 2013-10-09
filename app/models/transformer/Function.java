/**
 * 
 */
package models.transformer;

import java.util.Iterator;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

/**
 * A {@code Function} may be applied to a value to generate a result. As a
 * convenience, {@code Function}s may be applied to many inputs simultaneously
 * to produce a @{link JsonArray} of results.
 * 
 * @author jmontgomery
 *
 */
public interface Function {
	/**
	 * Returns the result of some transformation of the input value, encoded in
	 * JSON (i.e., as an appropriate {@link JsonElement}}. 
	 */
	public JsonElement apply(JsonElement value) throws BadValueException;
	
	/**
	 * Returns a {@link JsonArray} of results from applying the
	 * {@code Function} to each value in the iterator
	 */
	public JsonArray apply(Iterator<JsonElement> values) throws BadValueException;
	
}
