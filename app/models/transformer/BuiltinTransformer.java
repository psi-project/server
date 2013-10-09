package models.transformer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonPrimitive;

/**
 * Represents a family of lightweight transformers which are accessed through
 * this class. A list of valid identifiers is available in {@link #names},
 * while singleton instances are accessed via {@link #load(String)}.
 * 
 * @author jmontgomery
 *
 */
@SuppressWarnings("serial") //It makes everything much more tedious if every 'lightweight' transformer needs to declare a serial UID
public abstract class BuiltinTransformer extends Transformer {
	/**
	 * Names of defined {@code BuiltinTransformer}s. If we wanted a system
	 * where such transformers could be added without restarting the service
	 * then create an entity type that can hold a serialised
	 * BuiltinTransformer.
	 */
	public static final List<String> names;
	/** Map from public names to transformer classes. */
	private static final Map<String,Class<?>> table = new HashMap<>();
	static {
		Class<?>[] classes = new Class<?>[]{ Square.class, Average.class, Product.class, Str.class };
		for (Class<?> cls : classes)
			table.put( nameFromClass(cls), cls );
		List<String> knownNames = new ArrayList<>(table.keySet());
		Collections.sort(knownNames);
		names = Collections.unmodifiableList(knownNames);
	}
	
	/** Cached built-in transformer instances. */
	private static final Map<String,BuiltinTransformer> cache = new HashMap<>();
	
	private BuiltinTransformer(String accepts, String emits, String desc) {
		this.name = nameFromClass(getClass());
		this.accepts = changeQuotes(accepts);
		this.emits = changeQuotes(emits);
		this.description = desc;
		this.isDeletable = false;
		//These toy examples all have the same creation date
		this.provenance = "{'created': '2013-08-05T00:00Z', 'author': 'James Montgomery'}".replaceAll("'","\"");
	}
	
	/** Generates a simple name from the class's name. */
	private static String nameFromClass(Class<?> cls) {
		return cls.getSimpleName().toLowerCase();
	}
	
	/**
	 * Simple schema can be written without quotes, as in {@code $number},
	 * which will be transformed into {@code "$number"}, while all other
	 * schema, which will necessarily begin with an opening brace {@code {},
	 * can use the single quote {@code '} instead of double quote {@code "}.
	 */
	private static String changeQuotes(String schema) {
		return schema.startsWith("{") ? schema.replaceAll("'", "\"") : "\"" + schema + "\"";
	}

	/**
	 * Returns the single instance of the {@code name}d built-in transformer,
	 * or {@code null} if no transformer with that name is defined.
	 * <em>Do not modify its fields!</em>
	 */
	public static synchronized BuiltinTransformer load(final String name) {
		try {
			if (! names.contains(name))
				return null;
			BuiltinTransformer t = cache.get(name);
			if (t == null) {
				t = (BuiltinTransformer) table.get(name).newInstance();
				cache.put(name, t);
			}
			return t;
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException e) {
			throw new RuntimeException("Error creating built-in transformer.", e);
		}
	}

	//--The built-in transformers----------------------------------------------
	
	private static class Square extends BuiltinTransformer {
		public Square() { super("$number", "$number", "Calculates the square of a number"); }
		public JsonElement apply(JsonElement value) {
			return value.isJsonNull() ? JsonNull.INSTANCE : new JsonPrimitive( Math.pow( ((JsonPrimitive)value).getAsDouble(), 2 ) );
		}
	}
	
	private static class Average extends BuiltinTransformer {
		public Average() { super("{'$array':{'items':'$number','minItems':1}}", "$number", "Calculates the average of an array of numbers"); }
		public JsonElement apply(JsonElement value) {
			if (value.isJsonNull())
				return JsonNull.INSTANCE; 
			double sum = 0;
			for (JsonElement el : ((JsonArray)value))
				sum += el.getAsDouble();
			return new JsonPrimitive( sum / ((JsonArray)value).size() );
		}
	}
	
	private static class Product extends BuiltinTransformer {
		public Product() { super("{'$array':{'items':'$number','minItems':1}}", "$number", "Calculates the product of an array of numbers"); }
		public JsonElement apply(JsonElement value) {
			if (value.isJsonNull())
				return JsonNull.INSTANCE; 
			double product = 1;
			for (JsonElement el : ((JsonArray)value))
				product *= el.getAsDouble();
			return new JsonPrimitive( product );
		}
	}

	private static class Str extends BuiltinTransformer {
		public Str() { super("$atomicValue", "$string", "Returns a string representation of a numerical, boolean or (already) string value."); }
		public JsonElement apply(JsonElement value) {
			return value.isJsonNull() ? JsonNull.INSTANCE : new JsonPrimitive( ((JsonPrimitive)value).getAsString() );
		}
	}


}
