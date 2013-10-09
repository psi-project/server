package models;

/**
 * Representations of PSI resources are {@link PSIResponse}s which include the
 * URI of the resource being presented.
 *
 */
public abstract class PSIResource extends PSIResponse {
	/**
	 * URI of the resource that generated this response.
	 */
	public String uri;
	
}