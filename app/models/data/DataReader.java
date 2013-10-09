/**
 * 
 */
package models.data;

import com.google.gson.JsonElement;

import models.data.NoSuchInstanceException;
import models.data.Relation;

/**
 * Base class for all instance readers.
 * 
 * @author jmontgomery
 */
public abstract class DataReader {
	protected Relation relation;
	protected Query query;
	
	public DataReader(Relation relation, Query query) {
		this.relation = relation;
		this.query = query;
	}
	
	/**
	 * Returns the number of instances in the underlying dataset.
	 */
	public abstract int refreshInstanceCount();

	/**
	 * Returns the i^th instance and converts it to an appropriate JSON
	 * representation. This could be modified (early implementations just used
	 * Object as the return type) to allow exotic formats to be drawn from the
	 * dataset and then appropriately encoded by low-level {@code Attribute}s
	 * later, but the efficiency gains will be small if mostly used to service
	 * external requests.
	 * @throws NoSuchInstanceException if {@code i} is less than 1 or larger
	 * than the size of the relation.
	 */
	public abstract JsonElement readInstance(final int i) throws NoSuchInstanceException;
	
}