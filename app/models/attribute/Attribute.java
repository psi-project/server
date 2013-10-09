package models.attribute;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.Entity;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import controllers.Attributes;
import controllers.Data;
import models.transformer.EncodedTransformerChain;
import models.transformer.AbstractFunction;
import util.Util;

import models.PSIMessage;
import models.PSIResource;
import models.data.Relation;
import models.data.Query;

import play.Logger;
import play.mvc.Router;
import util.SubattributeRelationMismatchException;

@Entity
@Inheritance(strategy=InheritanceType.SINGLE_TABLE)
public abstract class Attribute extends AbstractFunction {
	private static final long serialVersionUID = 1L;
	/**
	 * In internal attribute creation requests this can be used to refer to
	 * just-created attributes without having to know the local service's host
	 * name. URIs with this fake scheme can have any host name desired (as it
	 * will be ignored) but should probably use {@code localhost} with no port
	 * number for consistency.
	 * @see PrimitiveAttribute#PRIMITIVE_URI_SCHEME
	 */
	private static final String LOCAL_URI_SCHEME = "local";

	/**
	 * When creating attributes they may either be created by the SYSTEM along
	 * with a relation (and hence cannot be deleted) or created by the USER.
	 */
	public static enum Persistence {
		SYSTEM, USER;
		public boolean isDeletable() { return this != SYSTEM; }
	};
	
	/** A predicate that identifies top-level attributes. */
	private static final Predicate TOP_LEVEL_ATTR_PREDICATE = new Predicate() {
		public boolean evaluate(Object attr) { return ((Attribute)attr).isParent(); } 
	};
	
	/** Link to its relation. */
	@ManyToOne
	@JoinColumn(name="relation_fk")
	protected Relation relation;
	
	/** Supports securing a name before an Attribute is actually persisted. */
	private static Set<String> attributesBeingCreated = new HashSet<>();
	
	/**
	 * The attribute's name without the relation name included (which is
	 * necessary in the primary key {@link #name} to avoid conflicts).
	 */
	public String nameNoRelation;
	
	/**
	 * Is this Attribute a top-level attribute as opposed to a sub-attribute?
	 * Note that if backlinks are added between sub- and parent attributes then
	 * a {@code null} parent link would replace this.
	 */
	protected boolean isParent;
	
	
	public Attribute() { this(""); }
	
	public Attribute(String name) { this(name, null); }
	
	public Attribute(String compositeName, String attrName) {
		super(compositeName);
		this.nameNoRelation = attrName;
	}
	
	/**
	 * Support function for a poor man's version of a composite key, which
	 * would have been a mild annoyance except for one part of the key being a
	 * foreign key, turning it into a giant headache. This way the composite
	 * key is just 'relationID/attributeID'
	 */
	public static String generateCompositeName(final String relationID, final String attributeID) {
		return relationID + '/' + attributeID;
	}

	public String toString() {
		Description result = new Description();
		result.uri = "/" + name;
		result.description = description;
		result.emits = getEmitsInJSON();
		return Util.GSON.toJson( result );
	}
	
	public boolean isParent() { return isParent; }
	
	public Relation getRelation() { return relation; }
	
	/**
	 * Sets the associated {@code Relation} in this and any sub-attributes.
	 * Must be overridden by structure attribute types to achieve the second
	 * aim. Only required so that sub-attributes created at the same time as a
	 * {@code Relation} will have their relation property set (<em>after</em>
	 * the relation's database model is persistent).
	 */
	public void setRelation(Relation relation) {
		if (this.relation == null) {
			this.relation = relation;
			save();
		} else if ( ! this.relation.equals( relation ) ) {
			throw new IllegalStateException("Cannot change relation associated with this attribute. Currently '" +
						this.relation.name + "' and attempted to change to '" + relation.name + "'");
		}
	}

	/**
	 * Checks that all given sub-attributes have either the same, non-null
	 * value in their {@link #externalRelation} fields or the same value in
	 * their {@link #relation} fields; sets the appropriate one of those in
	 * this Attribute if non-null values are encountered but the appropriate
	 * field in this Attribute is null. 
	 * @throws SubattributeRelationMismatchException if any of the sub-
	 * attributes has a different relation. 
	 */
	protected void checkSubattributeRelationsMatch() throws SubattributeRelationMismatchException {
		for (Attribute subattr : getSubattributes()) {
			subattr.checkSubattributeRelationsMatch();
			if (this.relation == null)
				this.relation = subattr.relation; //we're new, but sub-attributes appear to exist already, so use their relation (leave save() to others though)
			else if (! this.relation.equals(subattr.relation))
				throw new SubattributeRelationMismatchException(subattr.name, this.relation.name, subattr.relation.name);
		}
	}
	
	/**
	 * Structured and external attribute types should override this method to
	 * return a flat collection of their immediate sub-attributes. The
	 * collection is used internally for checking consistency in their linked
	 * relations.
	 */
	protected Collection<Attribute> getSubattributes() {
		return Collections.<Attribute>emptyList();
	}
	
	/**
	 * The attribute will unlink itself from its relation and any sub-
	 * attributes, and add itself and any sub-attributes that were created
	 * concurrently with it to the given {@code detached} collection, if it is
	 * not {@code null}.
	 */
	public void prepareForDeletion(Collection<Attribute> detached) {
		this.relation = null;
		save();
		if (detached != null) detached.add(this);
	}
	
	/**
	 * An attribute is potentially deletable if it was user-created and is a
	 * top-level attribute; if it is obviously a sub-attribute then its
	 * deletion cannot be requested from outside (this method isn't used
	 * internally).  
	 */
	public boolean isDeletable() {
		return super.isDeletable() && isParent();
	}
	
	//--Utility methods--------------------------------------------------------
	
	/**
	 * Similar to issuing a {@code fetch()} on the result of {@link #all()},
	 * but restricts the returned {@code Attributes} to be those that are not
	 * solely sub-attributes.
	 */
	public static List<Attribute> allTopLevel() {
		return find("from Attribute where isParent = true").fetch();
	}
	
	/**
	 * Filters a collection of {@code Attribute}s so that it contains only top-
	 * level attributes (i.e., where {@link #isParent()}. Useful if you already
	 * have a list of attributes. If you want <em>all</em> top-level attributes
	 * then use {@link #allTopLevel()} instead.
	 * @see #allTopLevel()
	 */
	public static Collection<Attribute> keepTopLevelAttributes(Collection<Attribute> attributes) {
		CollectionUtils.filter(attributes, TOP_LEVEL_ATTR_PREDICATE);
		return attributes;
	}
	
	/**
	 * Alternative to generic {@link #findById(Object)} that uses the composite
	 * relation ID + attribute ID to find a given attribute.
	 */
	public static Attribute findById(final String relationID, final String attributeID) {
		return Attribute.<Attribute>findById(generateCompositeName(relationID, attributeID));
	}
	
	//--Attribute construction-------------------------------------------------

	/**
	 * Finds a name that is not in use and records it in
	 * {@link #attributesBeingCreated} so it cannot be used by any other
	 * attributes currently being created. For sequentially numbered names
	 * ensure the {@code initialName} ends with a sequence of numerals; the
	 * search for an unused name will begin at 1, however, regardless of what
	 * sequence is at the end of {@code initialName}.
	 * <p>
	 * Yes, this duplicates such unique identity finding code as is already in
	 * the database, but this allows the use of names, rather than simple
	 * integer IDs.
	 */
	private static String secureName(String relationName, String initialName) {
		synchronized (attributesBeingCreated) {
			//Numbered names can be accessed by providing an number at the end of 
			String format = (initialName.matches("[^\\d]+\\d+$") ? initialName.replaceFirst("\\d+$", "") : initialName) + "%d";
			String finalName = initialName;
			int i = 1;
			String compositeName = generateCompositeName(relationName, finalName);
			while (Attribute.findById(compositeName) != null || attributesBeingCreated.contains(compositeName)) {
				finalName = String.format(format, i);
				compositeName = generateCompositeName(relationName, finalName);
				i++;
			}
			attributesBeingCreated.add(compositeName);
			return finalName;
		}
	}

	/**
	 * Removes the given name from the set of attributes currently being
	 * created. Should be called if the attempt to persist the attribute
	 * failed, too.
	 */
	private static void nameSecured(String relationName, String name) {
		synchronized (attributesBeingCreated) {
			attributesBeingCreated.remove( generateCompositeName(relationName, name) );
		}
	}
	
	/**
	 * Creates a new structured {@code Attribute} and persists it iff
	 * {@code persistence != Persistence.TEMPORARY}.
	 * @throws SubattributeRelationMismatchException if any of the sub-
	 * attributes referenced in the {@code attrDefinition} do not belong to the
	 * same relation.
	 * <p>
	 * If {@code relationName == null} then it will be determined from the
	 * extant attributes referenced in the definition.
	 * TODO Currently create definition cannot refer to a (lightweight) transformed attribute, even though such a use case seems quite natural (transform value to derive a new feature, then include it in a feature vector for training). However, including them requires careful thought and potentially big changes to the database model.
	 */
	public static Attribute create(String relationName, final String attributeName, final String description,
			JsonElement attrDefinition, Persistence persistence) throws SubattributeRelationMismatchException
	{
		assert relationName != null;
		if (persistence == Persistence.USER && ! (attrDefinition.isJsonArray() || attrDefinition.isJsonObject()))
			throw new IllegalArgumentException("Attribute definition must be an array or an object; no external request path should allow call create with a definition such as " + attrDefinition);
		Logger.trace("Attribute.create(relationName == %s)", relationName);
		Attribute attr = null;
		final String uniqueName = secureName(relationName, attributeName);
		try {
			//Use pseudo composite key made up of relation and attribute's names
			attr = create(generateCompositeName(relationName, uniqueName), uniqueName, attrDefinition, persistence);
			//A malfunctioning client could submit an internally-consistent definition to the wrong relation, which is not otherwise checked, so do so now
			if (attr.relation != null && ! attr.relation.name.equals(relationName))
				throw new SubattributeRelationMismatchException(attr.name, attr.relation, relationName);
			if (description != null)
				attr.description = description;
			attr.isParent = true;
			attr.isDeletable = persistence.isDeletable();
			attr.save();
			Logger.trace("Creating attribute: attr.isDeletable == %s", attr.isDeletable);
			return attr;
		} finally {
			nameSecured(relationName, uniqueName);
		}
	}
	
	/**
	 * Creates a new {@code Attribute} using the given JSON {@code definition}.
	 * If the definition is a {@code JsonArray} then a new
	 * {@link ArrayAttribute} is returned, whereas if it is a
	 * {@code JsonObject} then a new {@link ObjectAttribute} is returned. 
	 * At the lowest level, if the definition is a string then it is
	 * interpreted as the URI for an existing attribute or one of the
	 * built-in, parameterised attributes. In the first case the existing
	 * attribute is returned, while in the second a new
	 * {@link PrimitiveAttribute} is created with its relevant settings taken
	 * from the URI string.
	 * <p>
	 * The {@code commonRelation} will always be {@code null} when this method
	 * is entered for the first time, but as soon as an existing attribute is
	 * referred to in the given attribute definition {@code commonRelation}
	 * will be set. If an existing attribute is referred to later that has a
	 * different associated relation then an exception will be thrown. If the
	 * definition has a combination of existing and primitive attributes, then
	 * any mismatch will be picked up when the {@code Relation} that instigated
	 * this attribute creation attempts to set the relation property of all
	 * attributes. Any externally instigated create attempt can only refer to
	 * existing attributes.
	 * @throws SubattributeRelationMismatchException if existing attributes
	 * referenced in the {@code definition} do not belong to the same relation
	 */
	protected static Attribute create(final String compositeName, final String attrName,
			JsonElement definition, final Persistence persistence)
			throws SubattributeRelationMismatchException
	{
		Logger.trace("Entering Attribute.create(%s, %s)", compositeName, definition);
		Attribute attr = null;
		try {
			if (definition.isJsonArray()) {
				attr = new ArrayAttribute(compositeName, attrName, definition.getAsJsonArray(), persistence);
			} else if (definition.isJsonObject()) {
				attr = new ObjectAttribute(compositeName, attrName, definition.getAsJsonObject(), persistence);
			} else if (definition.isJsonPrimitive() && definition.getAsJsonPrimitive().isString()) {
				Logger.trace("Encountered string value in definition: %s", definition.getAsString());
				URI attrURI = new URI( definition.getAsString() );
				if ( attrURI.getScheme().equals(PrimitiveAttribute.PRIMITIVE_URI_SCHEME) ) {
					if ( persistence != Persistence.SYSTEM ) 
						throw new IllegalArgumentException("External clients may not create internal attributes that directly access data sources");
					Logger.trace("Attribute URI is for a primitive attribute; calling PrimitiveAttribute.create()");
					attr = PrimitiveAttribute.create(compositeName, attrName, attrURI);
				} else 	if ( Util.hostIsLocal(attrURI) || attrURI.getScheme().equals(LOCAL_URI_SCHEME) ) {
					Logger.trace("Attribute believed to be local");
					Map<String,String> routeArgs = Router.route("GET", attrURI.getPath());
					Logger.trace("Play! route arguments extracted from URI are: %s", routeArgs);
					Attribute existingAttr = Attribute.findById(routeArgs.get("relID"), routeArgs.get("id"));
					if (existingAttr != null) {
						Logger.trace("Found existing attribute and will return immediately with a reference to it: " + existingAttr);
						return existingAttr;
					}
				}
				if (attr == null)
					throw new IllegalArgumentException("Referenced attribute not found at this service: " + attrURI);
			} else {
				throw new IllegalArgumentException("Structured attribute definition must be an array or object. Was given: " + definition);
			}
		} catch (URISyntaxException urise) {
			throw new RuntimeException (urise);
		}
		attr.save();
		attr.checkSubattributeRelationsMatch();
		
		return attr;
	}
	
	//--Requests---------------------------------------------------------------
	
	public static class Create extends PSIMessage {
		/** Used in internal attribute creation only; will be ignored if present in external creation requests. */
		public String name;
		/** Definition of the attribute in JSON. */
		public JsonElement attribute;
		/** [opt] Human-readable description of the new attribute. */
		public String description;

		public boolean isValid() {
			if (super.isValid() && validateAllNonNull(attribute)) {
				if (attribute.isJsonArray() || attribute.isJsonObject())
					return true;
				addBadMessage("Attribute definition must be an array or object, but received " + attribute);
			}
			return false;
		}
	}


	//--Responses--------------------------------------------------------------
	
	public static class Description extends PSIResource {
		/** [opt] A short, human-readable description of the attribute. */
		public String description;
		/** Schema describing the output of the attribute; may be a string reference to a predefined schema, hence not JsonObject. */
		public JsonElement emits;
		//Pure attributes only
		/** URI of the default relation for the attribute. */
		public String relation;
		/** URIs for sub-attributes if this attribute is structured. */
		public Object subattributes;
		/** Schema of valid queries for this attribute (i.e., for its relation). */
		public JsonObject querySchema;

		public Description() { }
		
		@SuppressWarnings("unchecked") //There are only two execution paths and the type is guaranteed correct in both
		public Description(Attribute attribute, final String apparentID, EncodedTransformerChain transformation, Query query) {
			this.uri = Attributes.getReverseRoute(attribute, transformation == null ? null : transformation.toBase64JSON(), query);
			if (transformation == null)
				this.description = attribute.description;
			else
				this.description = transformation.getDescription() == null ? attribute.description + " (transformed)" : transformation.getDescription();
			this.emits = transformation == null ? Util.parseJSON( attribute.emits ) : transformation.getEmits();
			if (attribute.relation != null)
				this.relation = Data.getReverseRoute(attribute.relation, query);
			if ( transformation == null && (attribute instanceof ArrayAttribute || attribute instanceof ObjectAttribute) ) {
				if (attribute instanceof ArrayAttribute) {
					Collection<Attribute> els = attribute.getSubattributes();
					this.subattributes = new ArrayList<String>(els.size());
					for (Attribute subAttr : els)
						((List<String>)this.subattributes).add( Attributes.getReverseRoute( subAttr, null, query ));
				} else {
					Map<String,Attribute> props = ((ObjectAttribute)attribute).getProperties();
					this.subattributes = new HashMap<String,String>();
					for (Map.Entry<String,Attribute> prop : props.entrySet())
						((Map<String,String>)this.subattributes).put(prop.getKey(), Attributes.getReverseRoute( prop.getValue(), null, query ) );
				}
			}
			this.querySchema = Query.getQuerySchema(); //currently all relations in service support same queries
		}
		
	}
	
}
