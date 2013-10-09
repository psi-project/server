/**
 * 
 */
package util;


/**
 * A catchable exception for when the service attempts to contact an external
 * resource and finds that it's missing or its representation cannot be parsed.
 * 
 * @author jmontgomery
 *
 */
public class ExternalResourceException extends Exception {
	private static final long serialVersionUID = 1L;

	public ExternalResourceException(String message) { super(message); }

}
