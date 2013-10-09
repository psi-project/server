/**
 * 
 */
package models.data;

/**
 * @author jmontgomery
 *
 */
public class NoSuchInstanceException extends Exception {
	private static final long serialVersionUID = 1L;
	
	public NoSuchInstanceException(final int instance, final int relationSize) {
		super("Instance " + instance + " does not exist in relation, must be in [1," + relationSize + "]");
	}
}
