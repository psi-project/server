package models.data;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Map;

import models.data.QueryException;
import models.data.Relation;
import play.data.parsing.UrlEncodedParser;

import com.google.gson.JsonObject;

import util.Util;

/**
 * Simple wrapper for supported relation query options that provides facilities
 * for mapping to and from URL query arguments.
 */
public class Query {
	/**
	 * Deliberately breaks convention of uppercase names so string values match
	 * expected GET arguments.
	 */
	public static enum Param { fold, numfolds, invert };

	/**
	 * The PSI schema for query arguments. Assumes that <em>entire
	 * query</em> is optional, so arguments can be mandatory; otherwise
	 * must use JSON schema dependencies to enforce mutual presence of some
	 * properties.
	 */
	public static final JsonObject QUERY_SCHEMA = Util.parseJSON( Util.singleQuotesToDouble(
		"{" +
		"  '/" + Param.fold + "' : { '$integer' : { 'min' : 1, 'title' : 'Fold number', 'description' : '\u2264 number of folds' } }," +
		"  '/" + Param.numfolds + "' : { '$integer' : { 'min' : 2, 'title' : 'Total folds' } }," +
		"  '?" + Param.invert + "' : { '$boolean' : { 'title' : 'Invert selection' } } " +
		"}" ) ).getAsJsonObject();

	private static Query emptyQuery;
	
	private Map<String,Object> mapView;
	private Integer fold;
	private Integer numfolds;
	private Boolean invert;
	
	public Query(Map<String,String[]> params, Relation relation) throws QueryException {
		if (params != null) {
			fold = getIntegerParam( params, Param.fold );
			numfolds = getIntegerParam( params, Param.numfolds  );
			invert = getBooleanParam( params, Param.invert );

			checkFolds();
			if (relation != null)
				checkFoldsAgainstRelation(relation);
		}
	}
	
	public Query(Map<String,String[]> params) throws QueryException {
		this(params, null);
	}
	
	public Query(String queryString, Relation relation) throws QueryException {
		this( queryString == null ? null : UrlEncodedParser.parseQueryString( new ByteArrayInputStream( queryString.getBytes() ) ), relation );
	}
	
	public Query(String queryString) throws QueryException {
		this(queryString, null);
	}
	
	public static synchronized Query getEmptyQuery() {
		try {
			if (emptyQuery == null)
				emptyQuery = new Query((Map<String,String[]>)null);
		} catch (QueryException e) { throw new RuntimeException("Error creating empty query. This indicates a fault since it should succeed.", e); }
		return emptyQuery;
	}
	
	private synchronized Map<String,Object> asMap() {
		if (mapView == null) {
			mapView = new HashMap<String,Object>();
			addToMap(mapView, Param.fold, fold );
			addToMap(mapView, Param.numfolds, numfolds );
			addToMap(mapView, Param.invert, invert );
		}
		return mapView;
	}
	
	protected void addToMap(Map<String,Object> mapView, Query.Param param, Object value) {
		if (value != null)
			mapView.put(param.toString(), value);
	}
	
	protected String getParam(Map<String,String[]> params, Query.Param param) {
		return params.containsKey(param.toString()) ? params.get(param.toString())[0] : null;
	}
	
	/** Returns Integer value of String value, or {@code null} if {@code null}. */
	protected Integer getIntegerParam(Map<String,String[]> params, Query.Param which) throws QueryException {
		String value = getParam(params, which);
		try {
			return value == null ? null : new Integer( value );
		} catch (NumberFormatException nfe) {
			throw new QueryException("Query argument " + which + " must be an integer value. Received this: " + value);
		}
	}

	/** Returns Integer value of String value, or {@code null} if {@code null}. */
	protected Boolean getBooleanParam(Map<String,String[]> params, Query.Param which) {
		String value = getParam(params, which);
		return value == null ? null : new Boolean( value );
	}
	
	//--Query checks-----------------------------------
	
	private void checkFolds() throws QueryException {
		//Case of (fold == null && (numfolds != null || invert != null)) is stupid, but won't cause any error, so ignored
		if (fold != null) {
			if (fold != null && numfolds == null)
				throw new QueryException(this, "If specifying a fold, total number of folds must be given");
			if (fold <= 0 || fold > numfolds)
				throw new QueryException(this, "Selected fold (" + fold + ") is not in allowed range [1," + numfolds + "]");
		}
	}
	
	private void checkFoldsAgainstRelation(Relation relation) throws QueryException {
		if (fold != null && numfolds > relation.getSize())
			throw new QueryException(this, "Number of folds is larger than number of instances: " + numfolds + " > " + relation.getSize());
	}
	
	//--Conversion to query string and other interface issues--------------
	
	/**
	 * Returns a PSI schema describing supported queries. Returns a new
	 * object each time, despite the appalling overhead, so that any
	 * tampering with it is not persisted, but perhaps this can be avoided.
	 */
	public static JsonObject getQuerySchema() { return QUERY_SCHEMA; }
	
	/**
	 * Generates parameters map from given pairs of String keys and Object
	 * values, with this {@code Query}'s parameters added as well. Uses
	 * {@link Util#makeMap(Object...)} so same restrictions apply to given
	 * list of parameter values.
	 */
	public Map<String,Object> makeMap(Object... pairs) { return makeMap( Util.makeMap(pairs) ); }

	/**
	 * Adds the query's parameters to the given map.
	 * @see Query#makeMap(Object...)
	 */
	public Map<String,Object> makeMap(Map<String,Object> params) {
		params.putAll(asMap());
		return params;
	}
	
	/**
	 * Returns a naive translation of the query into a query string fragment. 
	 */
	public String toQueryString() {
		Map<String,Object> map = asMap();
		String result = "";
		for (Query.Param param : Param.values())
			if (map.containsKey(param.toString()))
				result += (result.length() > 1 ? "&" : "") + param + "=" + mapView.get(param.toString());
		return result;
	}
	
	public String toString() {
		//This must be changed if supporting more varied queries
		return isEmpty() ? "(all instances)" : "(" + (invert()? "all except " : "") + "fold " + fold + " of " + numfolds + ")";
	}
	
	/** Returns {@code true} if the given object represents the same query. */
	public boolean equals(Object o) {
		return (o == this) || (o instanceof Query) && asMap().equals( ((Query)o).asMap() );
	}
	
	/**
	 * Returns {@code true} if this query places no restrictions on
	 * selected instances, {@code false} otherwise.
	 */
	public boolean isEmpty() { return fold == null && numfolds == null; }
	
	//--Query interpretation---------------------------
	
	/** Invert only if fold specified, invert specified and {@code true}. */
	private boolean invert() { return fold != null && invert != null && invert; }
	
	private int foldSize(final int relationSize) {
		return fold == null ? relationSize :
			relationSize / numfolds + ( fold > relationSize % numfolds ? 0 : 1 );
	}
	
	public int size(Relation relation) {
		final int foldSize = foldSize(relation.getSize());
		return invert() ? relation.getSize() - foldSize : foldSize;
	}
	
	/**
	 * Translates a requested index into the actual underlying index given
	 * the query.
	 */
	public int translateIndex(final int i) {
		if (fold == null)
			return i;
		if (invert()) {
			final int offset = (i - 1) % (numfolds - 1) + 1; //offset as if weren't skipping fold's entry
			return (i - 1)/(numfolds - 1) * numfolds + offset + (offset >= fold ? 1 : 0); 
		}
		return (i - 1) * numfolds + fold;
	}
}
