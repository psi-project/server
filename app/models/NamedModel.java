/**
 * 
 */
package models;

import javax.persistence.Id;
import javax.persistence.MappedSuperclass;

import play.db.jpa.GenericModel;
import util.SubattributeRelationMismatchException;

/**
 * An extensible model that is identified by a string <em>name</em>. Similar to
 * the <em>play!</em> provided {@link play.db.jpa.Model Model}, but does not use
 * an automatically generated long integer identifier.
 * 
 * @author jmontgomery
 *
 */
@MappedSuperclass
public class NamedModel extends GenericModel implements Comparable<NamedModel>, NamedResource {
	private static final long serialVersionUID = 1L;

	/** Name for this model instance. */
	@Id
	public String name;

	public NamedModel() { this(null); }

	public NamedModel(String name) { this.name = name; }

	public String getName() { return name; }

	@Override
	public Object _key() { return getName(); }
	
	/**
	 * {@code NamedModel}s' natural order is based on their name.
	 * @see Comparable#compareTo(Object)
	 */
	@Override
	public int compareTo(NamedModel o) { return name.compareTo(o.name); }

}
