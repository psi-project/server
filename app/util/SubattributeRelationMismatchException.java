/**
 * 
 */
package util;

import models.data.Relation;

/**
 * Indicates that an attempt was made to create a structured attribute using
 * existing attributes that do belong to the same underlying relation.
 * 
 * @author jmontgomery
 *
 */
public class SubattributeRelationMismatchException extends Exception {
	private static final long serialVersionUID = 1L;

	/** @see RuntimeException#RuntimeException() */
	public SubattributeRelationMismatchException() { super(); }
	
	/** @see RuntimeException#RuntimeException(String) */
	public SubattributeRelationMismatchException(String message) { super(message); }

	/**
	 * Creates a new sub-attribute relation mismatch exception with a
	 * meaningful message that refers to the relevant sub-attribute's name and
	 * indicates both its and the main attribute's relations' names.
	 */
	public SubattributeRelationMismatchException(String subattrName, String attrRelation, String subattrRelation) {
		this("Mismatch between attribute's relation and at least one sub-attribute (" +
				subattrName + ")'s relation: " + attrRelation + " != " + subattrRelation);
	}
	
	/**
	 * Creates a new sub-attribute relation mismatch exception with a
	 * meaningful message that refers to the mismatch between the
	 * sub-attributes used in the attribute's definition and the relation to
	 * which the create request was sent. 
	 */
	public SubattributeRelationMismatchException(String attrName, Relation attrRelation, String incomingRelation) {
		this("Referenced or newly defined attribute (" + attrName + ") is for a different relation: " +
				attrRelation.name + " != " + incomingRelation + " (which received the create request)");
	}
}
