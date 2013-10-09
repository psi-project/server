package models.attribute;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.Transient;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import models.data.Relation;
import models.transformer.BadValueException;

import play.Logger;
import util.SubattributeRelationMismatchException;

/**
 * Structured attribute that produces object values.
 * 
 * @author jmontgomery
 *
 */
@Entity
public class ObjectAttribute extends Attribute {
	private static final long serialVersionUID = 1L;
	
	/** Sub-attributes for an object are named attributes. */
	@ManyToMany(cascade=CascadeType.ALL)
	@JoinTable(name = "ObjectAttrProperties",
		joinColumns = @JoinColumn(name="ObjectAttr_name"),
		inverseJoinColumns = @JoinColumn(name="PropertyAttr_name")
	)
	public Map<String,Attribute> properties;
	

	public ObjectAttribute() { super(""); }
	
	public ObjectAttribute(final String compositeName, final String attrName, JsonObject objectDef, final Persistence persistence)
			throws SubattributeRelationMismatchException
	{
		super(compositeName, attrName);
		
		Logger.trace("Creating new ObjectAttribute(name == %s)", compositeName);
		JsonObject schema = new JsonObject();

		properties = new HashMap<String, Attribute>();
		if (objectDef != null) {
			Set<Map.Entry<String,JsonElement>> propDefs = objectDef.entrySet();
			for (Map.Entry<String,JsonElement> propDef : propDefs) {
				String extension = "/" + propDef.getKey();
				Attribute property = create(compositeName + extension, attrName + extension, propDef.getValue(), persistence);
				properties.put(propDef.getKey(), property);
				schema.add("/" + propDef.getKey(), property.getEmitsInJSON() );
			}
		}
		emits = schema.toString();
	}

	public Map<String,Attribute> getProperties() { return properties; }

	public void setRelation(Relation relation) {
		super.setRelation(relation);
		for (Attribute subattr : properties.values())
			subattr.setRelation(relation);
	}

	protected Collection<Attribute> getSubattributes() {
		return properties.values();
	}

	//--Persistence assistance-------------------------------------------------
	
	public void prepareForDeletion(Collection<Attribute> detached) {
		for (Map.Entry<String,Attribute> property : properties.entrySet())
			if (property.getValue().name.equals( name + "/" + property.getKey() )) //then was created concurrently with this attribute and can be deleted with it
				property.getValue().prepareForDeletion(detached);
		properties.clear();
		super.prepareForDeletion(detached);
	}
	
	//--Attribute as a function interface--------------------------------------
	
	public JsonElement apply(JsonElement instance) throws BadValueException {
		JsonObject object = new JsonObject();
		for (Map.Entry<String,Attribute> namedAttr : properties.entrySet()) {
			object.add(namedAttr.getKey(), namedAttr.getValue().apply(instance) );
		}
		return object;
	}
	
}