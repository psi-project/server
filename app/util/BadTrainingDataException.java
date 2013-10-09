/**
 * 
 */
package util;

/**
 * Indicates that the data given to a training process had some problem with
 * it, such as two attributes not returning the same number of values when that
 * was expected.
 *
 */
public class BadTrainingDataException extends Exception {
	private static final long serialVersionUID = 1L;
	
	//This is the only constructor because bad data (at least currently) should be identified as such without encountering some other Exception
	public BadTrainingDataException(String message) { super(message); }

}
