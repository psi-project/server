package models.predictor;

import models.transformer.BadValueException;

/**
 * As JSON schema validation may validate arrays that contain valid elements
 * according to the schema's {@code items} property but which are a different
 * length to {@code items}, this exception may be thrown when the system
 * cannot handle the discrepancy.
 */
public class ArrayLengthMismatchException extends BadValueException {
	private int expected;
	private int actual;
	
	public ArrayLengthMismatchException(int expected, int actual) {
		this.expected = expected;
		this.actual = actual;
	}
	
	public String getMessage() {
		return String.format("Expected array of size %d but was given one of size %d", expected, actual);
	}
}
