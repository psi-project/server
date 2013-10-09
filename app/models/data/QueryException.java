/**
 * 
 */
package models.data;

import models.data.Query;


/**
 * A catchable exception for when {@link Query} objects are created with
 * conflicting or erroneous arguments.
 * 
 * @author jmontgomery
 *
 */
public class QueryException extends Exception {
	private static final long serialVersionUID = 1L;

	public QueryException(String detail) { this(null, detail); }

	public QueryException(Query query, String detail) {
		super("Bad relation query" + 
			(query == null ? "" : " '" + query.toQueryString() + "'") +
			": " + detail);
	}

}
