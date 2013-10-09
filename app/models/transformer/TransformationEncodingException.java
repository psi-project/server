/**
 * 
 */
package models.transformer;


/**
 * A catchable exception for when a base64 string representing a chain of
 * transformations cannot be decoded by {@link EncodedTransformerChain}.
 * 
 * @author jmontgomery
 *
 */
public class TransformationEncodingException extends Exception {
	private static final long serialVersionUID = 1L;

	public TransformationEncodingException(String message) { super(message); }

}
