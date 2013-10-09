/**
 * 
 */
package util;

/**
 * Indicates an error during training.
 */
public class TrainingException extends Exception {
	private static final long serialVersionUID = 1L;
	
	//public TrainingException() is not available as must always give some explanation
	
	public TrainingException(String message) { super(message); }
	
	public TrainingException(Throwable cause) { super(cause); }
	
	public TrainingException(String message, Throwable cause) { super(message, cause); }

}
