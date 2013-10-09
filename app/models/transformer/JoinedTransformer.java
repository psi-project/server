/**
 * 
 */
package models.transformer;

import java.util.Iterator;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;


/**
 * A {@link Function} whose output is transformed by another {@link Function}.
 *
 */
public class JoinedTransformer implements Function {
	/** The underlying function. */
	private Function f;
	/** The function to be applied to that transformer's output. */
	private Function g;
	
	public JoinedTransformer(Function f, Function g) {
		this.f = f;
		this.g = g;
	}
	
	@Override
	public JsonElement apply(JsonElement value) throws BadValueException {
		return g.apply( f.apply(value) );
	}
	
	@Override
	public JsonArray apply(Iterator<JsonElement> values) throws BadValueException {
		return g.apply( f.apply(values).iterator() );
	}
	
}
