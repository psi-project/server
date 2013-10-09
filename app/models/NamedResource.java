/**
 * 
 */
package models;

import util.SchemaTemplate;

/**
 * Any resource identified (and partially located) by its name should implement
 * this interface. {@link NamedModel}s and {@link SchemaTemplate}s, which are
 * constant and hence do not need to be persisted like other models, are two
 * examples.
 * 
 * @author jmontgomery
 *
 */
public interface NamedResource {
	
	/**
	 * Returns the name for this resource. The name will be unique only within
	 * the resource's local context, e.g., within all attributes, or all
	 * predictors.
	 */
	public String getName();
	
}
