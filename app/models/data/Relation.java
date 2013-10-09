package models.data;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Vector;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Transient;

import play.Logger;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import models.NamedModel;
import models.PSIMessage;
import models.PSIResource;
import models.attribute.ArrayAttribute;
import models.attribute.Attribute;
import models.data.DataReader;
import models.data.Query;
import models.data.TextFileDataReader;

import controllers.Attributes;

import util.SubattributeRelationMismatchException;

/**
 * Describes an immutable (from an outside user's perspective) relation of
 * instances.
 * 
 * @author jmontgomery
 *
 */
@Entity
public class Relation extends NamedModel implements Iterable<JsonElement> {
	private static final long serialVersionUID = 1L;
	
	/** Supported formats for datasets in this PSI service. */
	protected enum Format { CSV, JSON };

	/** Human-readable description of this relation. */
	protected String description;
	/** Number of instances in this relation. */
	protected int size;
	/** The 'default' attribute that should return an entire instance. */
	@OneToOne(cascade={CascadeType.ALL})
	public Attribute defaultAttribute;
	/** (Top-level) attributes for reading instances from this relation. */
	@OneToMany(cascade={CascadeType.ALL})
	@JoinColumn(name="relation_fk")
	public List<Attribute> attributes;
	/** File format of the data file. */ 
	protected Format format;
	/**
	 * Relative path to the data file, to be resolved by Play!. Note that this
	 * is highly restrictive, assuming a single data <em>file</em>.
	 */
	protected String path;
	
	public Relation() { super(""); }
	
	/**
	 * Used internally to create a new {@code Relation}.
	 * @throws SubattributeRelationMismatchException if any of the attributes
	 * for this relation refer to existing attributes for another relation.
	 */
	private Relation(Create createRequest) throws SubattributeRelationMismatchException {
		super(createRequest.name);
		this.format = Format.valueOf(createRequest.format);
		this.path = createRequest.path;
		this.description = createRequest.description;
		this.size = getDataReader().refreshInstanceCount();
		
		attributes = new Vector<Attribute>(createRequest.attributes.size());
		for (Attribute.Create createReq : createRequest.attributes) {
			Attribute attr = Attribute.create(name, createReq.name, createReq.description, createReq.attribute, Attribute.Persistence.SYSTEM);
			attributes.add(attr);
			if (createReq.name.equals(createRequest.defaultAttribute))
				defaultAttribute = attr;
		}
		if (defaultAttribute == null && !attributes.isEmpty())
			defaultAttribute = attributes.get(0);
	}

	/**
	 * Creates a new {@code Relation} and its {@link Attribute}s and persists
	 * it to the database.
	 * @throws SubattributeRelationMismatchException if any of the attributes
	 * for this relation refer to existing attributes for another relation.
	 */
	public static Relation create(Create createRequest) throws SubattributeRelationMismatchException {
		Relation r = new Relation( createRequest );
		r.save();
		//Any sub-attributes just created (and hence not appearing in r.attributes, need to be told this is their relation
		for (Attribute attr : r.attributes)
			attr.setRelation(r);
		return r;
	}

	public Attribute getDefaultAttribute() {
		return defaultAttribute;
	}
	
	/** Returns the 'path' to the file (table, etc.) that holds this Relation's data. */
	public String getPath() { return path; }
	
	/** Returns the total number of instances in this Relation. */ 
	public int getSize() { return size; }
	
	//--Persistence assistance-------------------------------------------------
	
	/**
	 * Requests that all attribute's prepare to be deleted and unlinks this
	 * relation from them; the potentially complicated object-model that
	 * attributes define cannot be easily cascade deleted by Hibernate. 
	 */
	public void prepareForDeletion() {
		defaultAttribute = null;
		for (Attribute attr : attributes)
			attr.prepareForDeletion(null);
		attributes.clear();
		save();
    }

	public boolean containsInstance(final int instance) { return instance >= 1 && instance <= size; }
	
	public JsonElement readInstance(Query query, final int i) throws NoSuchInstanceException {
		return getDataReader(query).readInstance(i);
	}
	
	public Iterator<JsonElement> iterator() {
		return iterator(null);
	}
	
	/** An extension to the standard Iterable interface that supports a query. */
	public Iterator<JsonElement> iterator(Query query) {
		return new DataIterator(this, query);
	}
	
	public DataReader getDataReader() { return getDataReader(null); }
	
	public DataReader getDataReader(Query query) {
		switch (format) {
		case CSV : return new TextFileDataReader.CSVReader(this, query);
		case JSON : return new TextFileDataReader.JSONReader(this, query);
		default: return null;
		}
	}
	
	//--Nested classes---------------------------------------------------------
	
	/**
	 * Wraps up a concrete {@link DataReader} and the current instance within
	 * the relation induced by a given {@link Query}.
	 */
	protected static class DataIterator implements Iterator<JsonElement> {
		private DataReader dataReader;
		private int virtualPos = 1;
		private int lastVirtualPos;
		private JsonElement nextInstance;
		
		public DataIterator(Relation relation, Query query) {
			this.dataReader = relation.getDataReader(query);
			lastVirtualPos = query.size(relation);
			loadNext(); //assumes at least one instance is selected by query and relation not empty
		}
		
		public boolean hasNext() { return nextInstance != null; }
		
		private void loadNext() {
			try {
				nextInstance = virtualPos > lastVirtualPos ? null : dataReader.readInstance(virtualPos);
			} catch (NoSuchInstanceException nsie) {
				throw new RuntimeException("Avoidable error occurred", nsie);
			}
		}
		
		public JsonElement next() {
			if (! hasNext())
				throw new NoSuchElementException(String.valueOf(virtualPos)); //report the *apparent* index that doesn't exist
			JsonElement toReturn = nextInstance;
			virtualPos++;
			loadNext();
			return toReturn;
		}
		
		public void remove() {
			throw new UnsupportedOperationException();
		}
	}

	//--Requests---------------------------------------------------------------

	//Internal requests are supported; ensure no public path can allow a suitably formatted request to be acted on though
	/** Internal relation creation message. */
	public static class Create extends PSIMessage {
		/** Relation name; note that, unlike a real external PUT request, the desired URI (or part of it) is part of this request object. */
		public String name;
		/** Human-readable description of the relation. */
		public String description;
		/** Underlying data format; must be the string representation of an element of {@link Relation.Format}. */
		public String format;
		/** Local path to data file; note that this is not generalisable as it assumes that data is in a single <em>file</em>, rather than a database, etc. */
		public String path;
		/** Name of the default attribute if it isn't the first entry in the {@link #attributes} list. */
		public String defaultAttribute;
		/** List of {@linkplain Attribute.Request.Create attribute create requests }. */
		public List<Attribute.Create> attributes;

		public boolean isValid() { return /* has no psiType, so just */validateAllNonNull(name, format, path, attributes); }

	}

	//--Responses--------------------------------------------------------------

	public static class Description extends PSIResource {
		/** [opt] Human-readable description of the relation. */
		public String description;
		/** The number of instances in the relation. */
		public int size;
		/** URI of the default attribute of this relation. */
		public String defaultAttribute;
		/** [opt] URI of all attributes for this relation. */
		public List<String> attributes;
		/**
		 * Query schema for this relation. Must be a {@link JsonElement}
		 * rather than the more-specific {@link JsonObject} for
		 * {@link com.google.gson.Gson#toJsonTree(Object)} to render it
		 * correctly. 
		 */
		public JsonObject querySchema;

		public Description(Relation relation, Query query, String uri) {
			this.uri = uri;
			this.description = relation.description + (query.isEmpty() ? "" : " " + query);
			this.size = query.size(relation);
			if ( ! relation.attributes.isEmpty() ) {
				this.defaultAttribute = Attributes.getReverseRoute(relation.getDefaultAttribute(), null, query);
				this.attributes = new ArrayList<String>(relation.attributes.size());
				for (Attribute attr : Attribute.keepTopLevelAttributes( new ArrayList<>( relation.attributes ) ))
					this.attributes.add( Attributes.getReverseRoute(attr, null, query) );
			}
			this.querySchema = Query.getQuerySchema();
		}
	}

}