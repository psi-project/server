package models.attribute;

import java.util.Collection;
import java.util.List;
import java.util.Vector;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.OrderColumn;
import javax.persistence.Transient;

import org.hibernate.annotations.OrderBy;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import models.data.Relation;
import models.transformer.BadValueException;

import play.Logger;
import util.SubattributeRelationMismatchException;
import util.Util;

/**
 * Structured attribute that produces array values.
 * 
 * @author jmontgomery
 *
 */
@Entity
public class ArrayAttribute extends Attribute {
	private static final long serialVersionUID = 1L;
	
	private static final String ARRAY_SCHEMA_TEMPLATE = Util.singleQuotesToDouble("{ '$array\' : { 'items' : [] } }");
	
	/**
	 * Sub-attributes for an array are ordered and numerically indexed,
	 * starting from 1, as far as the outside world is concerned.
	 */
	@ManyToMany(cascade=CascadeType.ALL)
	@JoinTable(name = "ArrayAttrElements",
		joinColumns = @JoinColumn(name="ArrayAttr_name"),
		inverseJoinColumns = @JoinColumn(name="ElementAttr_name")
	)
	@OrderColumn
	public List<Attribute> elements;
	

	public ArrayAttribute() { super(""); }
	
	public ArrayAttribute(final String compositeName, String attrName, JsonArray arrayDef, final Persistence persistence)
			throws SubattributeRelationMismatchException
	{
		super(compositeName, attrName);
		Logger.trace("Creating new ArrayAttribute");

		JsonObject schema = Util.parseJSON(ARRAY_SCHEMA_TEMPLATE).getAsJsonObject();
		JsonArray itemsSchema = schema.get("$array").getAsJsonObject().get("items").getAsJsonArray();

		elements = new Vector<Attribute>( arrayDef == null ? 10 : arrayDef.size() );
		for (int i = 0; i < arrayDef.size(); i++) {
			String extension = "/" + (i+1);
			Attribute el = create(compositeName + extension, attrName + extension, arrayDef.get(i), persistence);
			elements.add( el );
			itemsSchema.add( el.getEmitsInJSON() );
		}
		emits = schema.toString();
	}
	
	public void setRelation(Relation relation) {
		super.setRelation(relation);
		for (Attribute subattr : elements)
			subattr.setRelation(relation);
	}
	
	protected Collection<Attribute> getSubattributes() {
		return elements;
	}
	
	//--Persistence assistance-------------------------------------------------
	
	public void prepareForDeletion(Collection<Attribute> detached) {
		int i = 1;
		for (Attribute el : elements)
			if (el.name.equals(name + "/" + (i++))) //then was created concurrently with this attribute and can be deleted with it
				el.prepareForDeletion(detached);
		elements.clear();
		super.prepareForDeletion(detached);
	}
	
	//--Attribute as a function interface--------------------------------------
	
	public JsonElement apply(JsonElement instance) throws BadValueException {
		JsonArray array = new JsonArray();
		for (Attribute subattribute : elements) {
			array.add( subattribute.apply(instance) );
		}
		return array;
	}
	
}