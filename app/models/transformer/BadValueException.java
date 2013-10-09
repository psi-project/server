package models.transformer;

/**
 * A catchable exception representing any error that occurs during
 * {@code Function} application due to a bad input value.
 */
public class BadValueException extends Exception {
	
	public BadValueException() { super(); }
	
	public BadValueException(String message) { super(message); }
	
	public BadValueException(Throwable cause) { super(cause); }
	
	public BadValueException(String message, Throwable cause) { super(message, cause); }
	
}
