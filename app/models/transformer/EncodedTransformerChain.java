/**
 * 
 */
package models.transformer;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.Vector;


import models.PSI;

import org.apache.commons.codec.binary.Base64;
import org.hibernate.cfg.NotYetImplementedException;

import play.mvc.Router;

import util.ExternalResourceException;
import util.Schema;
import util.Util;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

import controllers.Transformers;


/**
 * Converts between an ugly but relatively space-efficient JSON representation
 * of a chain of transformers (that uses arrays where objects are clearly
 * indicated) encoded in base64 and a Java list of the same information. Can
 * load the actual transformers referred to by the JSON representation.
 * 
 * @author jmontgomery
 *
 */
public class EncodedTransformerChain {
	private static final String NO_EMITS_AT_END_MESSAGE = "End of existing transformer chain should have stored emits schema, but is actually null";
	private static final String ROUTE_ACTION = Transformers.class.getSimpleName() + ".describeOrApply";
	private static final String TRAN_ID = "T";
	private static final String PRED_ID = "I";
	
	/** The URI key name used to identify an encoded transformer chain. */
	public static final String URI_KEY = "t";
	
	private List<Link> list = new Vector<>();
	private String base64;
	
	private enum TransformerType { TRANSFORMER, PREDICTOR, EXTERNAL };
	
	private EncodedTransformerChain() {
		
	}
	
	private EncodedTransformerChain(JsonArray chain) throws TransformationEncodingException {
		this(chain, null);
	}
	
	private EncodedTransformerChain(JsonArray chain, String base64Chain) throws TransformationEncodingException {
		base64 = base64Chain;
		for (JsonElement jsonEl : chain) {
			String f, desc = null;
			JsonElement emits = null;
			if (jsonEl.isJsonArray()) {
				JsonArray a = jsonEl.getAsJsonArray();
				f = a.get(0).getAsString();
				if (a.size() > 1) emits = a.get(1);
				if (a.size() > 2) desc = a.get(2).getAsString();
			} else {
				f = jsonEl.getAsString();
			}
			list.add( new Link(f, emits, desc) );
		}		
	}
	
	/**
	 * Returns a new transformer chain containing {@code g}, whose accepts
	 * schema is tested for compatibility against {@code f.emits}. If the given
	 * {@code priorTransformation} is not {@code null} then it is extended by
	 * the given function {@code g} and returned as the result; otherwise a new
	 * {@code EncodedTransformerChain is returned}.
	 * @throws TransformationEncodingException 
	 */
	public static EncodedTransformerChain create(AbstractFunction f, EncodedTransformerChain priorTransformation,
			final String g, final String description) throws TransformationEncodingException
	{
		EncodedTransformerChain chain = priorTransformation == null ? new EncodedTransformerChain() : priorTransformation;
		chain.add(chain.list.isEmpty() ? Util.parseJSON(f.emits) : chain.getEnd().emits, g, description);
		return chain;
	}
	
	public static EncodedTransformerChain unpack(final String base64Chain) throws TransformationEncodingException {
		try {
			JsonElement json = Util.parseJSON( new String( Base64.decodeBase64(base64Chain) ) );
			if (! json.isJsonArray())
				throw new TransformationEncodingException("Badly encoded joined transformer information");
			return new EncodedTransformerChain( json.getAsJsonArray(), base64Chain );
		} catch (JsonSyntaxException jse) {
			throw new TransformationEncodingException("Unable to interpret joined transformer information");
		}
	}
	
	public Function constructProcessingPipeline(AbstractFunction start) throws TransformationEncodingException {
		checkCompatible( start.getEmitsInJSON(), list.get(0).fetchTransformer(true).getAcceptsInJSON() );
		return new JoinedTransformer(start, constructProcessingPipeline(0));
	}
	
	private Function constructProcessingPipeline(int pos) throws TransformationEncodingException {
		Transformer f = list.get(pos).fetchTransformer(false); 
		if (pos < list.size() - 1)
			return new JoinedTransformer(f, constructProcessingPipeline(pos+1));
		return f;
	}
	
	/**
	 * Returns an object containing nested objects representing the chain of
	 * transformers this chain represents, with each nested object containing
	 * a {@code transformerKey} with the transformer and a {code joinKey}
	 * containing any nested join. 
	 */
	public JsonObject constructProvenanceEntry(final String transformerKey, final String joinKey) {
		return constructProvenanceEntry(transformerKey, joinKey, 0); 
	}
	
	private JsonObject constructProvenanceEntry(final String transformerKey, final String joinKey, int level) {
		JsonObject entry = new JsonObject();
		entry.addProperty(transformerKey, list.get(level).getURI());
		if (level < list.size()-1)
			entry.add(joinKey, constructProvenanceEntry(transformerKey, joinKey, level + 1));
		return entry;
	}
	
	public synchronized String toBase64JSON() {
		if (base64 == null) {
			base64 = Base64.encodeBase64URLSafeString( toJSON().toString().getBytes() ); 
		}
		return base64;
	}
	
	public JsonArray toJSON() {
		JsonArray array = new JsonArray();
		for (int i = 0, last = list.size() - 1; i <= last; i++)
			array.add( list.get(i).toJSON(i == last) );
		return array;
	}
	
	public String toString() { return toJSON().toString(); }
	
	/** Returns the emits schema of the final part of the transformer chain. */
	public JsonElement getEmits() {
		assert getEnd().emits != null : NO_EMITS_AT_END_MESSAGE;
		return getEnd().emits;
	}

	/** Returns the description of the transformer chain (may be null). */
	public String getDescription() { return getEnd().description; }
	
	public synchronized void add(String g, String description) throws TransformationEncodingException {
		assert getEnd().emits != null : NO_EMITS_AT_END_MESSAGE;
		add(getEnd().emits, g, description);
	}
	
	private void add(JsonElement mustAccept, String g, String description) throws TransformationEncodingException {
		base64 = null;
		String gt = null;
		if (g.startsWith("http://")) {
			try {
				URI gURI = new URI(g);
				if (Util.hostIsLocal(gURI)) {
					Map<String,String> routeArgs = Router.route("GET", gURI.getPath());
					if (routeArgs.isEmpty())
						throw new TransformationEncodingException("Seemingly local resource '" + g + "' not found at this service");
					if (! routeArgs.get("action").equals(ROUTE_ACTION) )
						throw new TransformationEncodingException("Cannot create join. Given URI is hosted at this service but does not appear to correspond to a transformer or predictor. URI: " + g);
					g = (routeArgs.get("kind").equals(PSI.PREDICTOR_BASE) ? PRED_ID : TRAN_ID) + routeArgs.get("id");
					if (gURI.getQuery() != null) { //Play only extracts path arguments it can unify with the list of routes, so do quick and dirty search for URI_KEY
						String[] args = gURI.getQuery().split("[&=]");
						for (int i = 0; i < args.length - 1 && gt == null; i += 2)
							if (args[i].equals(URI_KEY))
								gt = args[i+1];
					}
				}
			} catch (URISyntaxException urise) {
				throw new TransformationEncodingException("Join transformer's URI begins with http:// but is not a valid URI: " + urise.getMessage());
			}
		}
	
		int prevSize = list.size();
		Link entry = new Link(g, description);
		checkCompatible(mustAccept, entry.t.getAcceptsInJSON());
		list.add( entry );
		
		//Add g's transformer chain if present; assume it is a valid extension to g
		if (gt != null) {
			list.addAll( EncodedTransformerChain.unpack(gt).list );
			list.get(list.size()-1).description = description; //only kept by last entry
		}
		
		deriveEnumValues(mustAccept, prevSize);
	}
	
	/**
	 * Although the use case for this is rare, it would be nice if, when
	 * joining one transformer to another that enumerates the values it emits,
	 * that the joined transformer's emits schema enumerates the values that
	 * <em>it</em> emits. The modification is only made if the previous end
	 * enumerates its values, the present end does not, and each link in the
	 * chain appears to be an immutable function.
	 * @throws TransformationEncodingException if was unable to compile the old
	 * or current end point's emits schema, or if one of the enumerated values
	 * from the old end is not acceptable to the rest of the chain (which
	 * indicates that the join should not be created anyway).
	 */
	private void deriveEnumValues(JsonElement prevEmits, int startRest) throws TransformationEncodingException {
		try {
			JsonObject prevEndEmits = Schema.compileToJSONSchema(prevEmits);
			if (prevEndEmits.has("enum")) {
				JsonObject newEndEmits = Schema.compileToJSONSchema(getEmits());
				if (! newEndEmits.has("enum")) {
					for (int i = startRest, c = list.size(); i < c; i++)
						if (! (list.get(i).fetchTransformer(false) instanceof BuiltinTransformer))
							return; //could expand test to allow non-updatable predictors too
					Function fRest = constructProcessingPipeline(startRest);
					JsonArray enumValues = fRest.apply( prevEndEmits.get("enum").getAsJsonArray().iterator() );
					getEnd().emits = Schema.addEnumToPSISchema( getEmits(), enumValues );
				}
			}
		} catch (ExternalResourceException ere) {
			throw new TransformationEncodingException("Could not compile schema to determine if joined transformation has an enumerable set of possible emitable values: " + ere.getMessage());
		} catch (BadValueException bve) { //but this should not happen as already passed compatibility checking 
			throw new TransformationEncodingException("At least one value emitted by first part of join is not acceptable to the second half, so join cannot proceed");
		}
	}
	
	/** Throws an exception if {@code Schema.isIncompatible(emits, accepts)}. */
	protected void checkCompatible(JsonElement emits, JsonElement accepts) throws TransformationEncodingException {
		try {
			if (Schema.isIncompatible(emits, accepts))
				throw new TransformationEncodingException("Cannot perform join. Request recipient does not appear to emit acceptable values.\nemits: " + emits + "\naccepts: " + accepts);
		} catch (ExternalResourceException ere) {
			throw new TransformationEncodingException("Could not compile schema for compatibility testing: " + ere.getMessage());
		}
	}
	
	private Link getEnd() { return list.get( list.size() - 1 ); }
	
	//--Nested classes---------------------------------------------------------
	
	private static class Link {
		/** Function's URI (possibly truncated). */
		private String f;
		/** Cached schema (may be null). */
		private JsonElement emits;
		/** Optional description of joined transformer ending at this link. */
		private String description;
		/** Built-in transformer? Predictor? External? */
		private TransformerType type;
		/** Cached transformer model. */
		private Transformer t;

		public Link(String f, JsonElement emits, String description) throws TransformationEncodingException {
			this.f = f;
			this.emits = emits;
			this.description = description;
			this.type = determineType(f);
		}
		
		public Link(String f, String description) throws TransformationEncodingException {
			this(f, null, description);
			fetchTransformer(true);
		}
		
		public static TransformerType determineType(final String f) throws TransformationEncodingException {
			if (f != null) {
				if (f.startsWith(TRAN_ID)) return TransformerType.TRANSFORMER;
				else if (f.startsWith(PRED_ID)) return TransformerType.PREDICTOR;
				else if (f.startsWith("http://")) return TransformerType.EXTERNAL;
			}
			throw new TransformationEncodingException("Part of the joined transformer cannot be interpreted as a transformer: " + f);
		}
		
		public Transformer fetchTransformer(boolean fetchExternal) throws TransformationEncodingException {
			try {
				if (t != null && !(type == TransformerType.EXTERNAL && fetchExternal))
					return t;
				switch(type) {
				case TRANSFORMER: t = BuiltinTransformer.load( extractID(f) ); break;
				case PREDICTOR: t = Transformers.find(Transformers.Kind.PREDICTOR, extractID(f) );  break;
				case EXTERNAL: t = new ExternalTransformer(f, fetchExternal); break;
				default: t = null;
				}
				if (emits == null && t != null)
					this.emits = Util.parseJSON( t.emits );
				return t;
			} catch (ExternalResourceException ere) {
				throw new TransformationEncodingException("Unable to retrieve details from externally referenced transformer. " + ere.getMessage());
			}
			
		}
		
		/** Returns the (possibly reconstructed) URI of {@code f}. */
		public String getURI() {
			//Warning: Uses knowledge of URI construction to extract the ID for local transformers
			switch(type) {
			case TRANSFORMER: return Transformers.getReverseRoute(Transformers.Kind.TRANSFORMER, extractID(f));
			case PREDICTOR: return Transformers.getReverseRoute(Transformers.Kind.PREDICTOR, extractID(f));
			case EXTERNAL: return f;
			default: return null;
			}
		}
		
		private String extractID(String partURI) { return partURI.substring(1); }
		
		public JsonElement toJSON(boolean isLast) {
			JsonElement jsonF = Util.GSON.toJsonTree(f);
			if (isLast || type == TransformerType.EXTERNAL) { //optimistically assume that have emits and, possibly, description
				JsonArray asArray = new JsonArray();
				asArray.add( jsonF );
				if (emits != null) {
					asArray.add( emits );
					if (isLast && description != null)
						asArray.add( Util.GSON.toJsonTree( description ) );
				}
				return asArray;
			}
			return jsonF;
		}
	}
	
}
