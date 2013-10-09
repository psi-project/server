package models.predictor;

import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.Lob;
import javax.persistence.Transient;

import models.PSIResource;
import models.learner.Task;
import models.transformer.EncodedTransformerChain;
import models.transformer.Transformer;

import com.google.gson.JsonArray;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;

import controllers.Learners;

import util.Util;


/**
 * A PSI Predictor.
 * 
 * @author jmontgomery
 *
 */
@Entity
@Inheritance(strategy=InheritanceType.SINGLE_TABLE) //Note that if subclasses become very different then should use separate tables instead
public abstract class Predictor extends Transformer {
	private static final long serialVersionUID = 1L;
	
	/**
	 * If the predictor is still being trained then this holds a description of
	 * training progress. 
	 */
	@Lob //since could be longer than standard Db string field
	public String trainingStatus;
	/**
	 * Name of the learner model that created this predictor. (This is more
	 * robust than storing the full URI and easier than linking to the actual
	 * learner instance, since only need this for reporting to a client.
	 */
	public String learnerName;
	/**
	 * PSI schema (as text) defining the structure of update objects used in
	 * update requests.
	 */
	@Lob //since could be very long
	public String updateSchema;
	
	/**
	 * If training this predictor has been accepted by the server and the
	 * predictor model is genuinely persistent now.
	 */
	@Transient
	public boolean trainingNowOffline;
	/**
	 * The cause of a training error, which may be caused by a bad request. If
	 * this happens quickly enough then there's still time to send back an
	 * appropriate response to the client, rather than merely storing the error
	 * message.
	 */
	@Transient
	public Throwable trainingErrorCause;

	/**
	 * Should be used by subclasses to initialise a Predictor model's common
	 * properties when it is first created, which may be before training has
	 * completed, in which case {@code trainingStatus} should be non-null.
	 */
	protected Predictor(String name, String description, String learnerName, String trainingStatus)
	{
		super(name);
		this.description = description;
		this.learnerName = learnerName;
		this.trainingStatus = trainingStatus; //set it, but DO NOT call updateTrainingStatus (formerlly called setTrainingStatus) Play.
	}
	
	public PSIResource getDescription(final String uri, EncodedTransformerChain transformation) {
		return isTrained() ? super.getDescription(uri, transformation) : new Status(this, uri);
	}

	public synchronized void setTrainingNowOffline(boolean isNowOffline) { this.trainingNowOffline = isNowOffline; }
	
	/**
	 * Called on the predictor when training has completed to set its custom
	 * provenance information, set its update schema if it is updatable, and
	 * remove its training status information. The model is saved after these
	 * are set. Subclasses should provide their own public implementation
	 * (which will typically take additional arguments) and call this.
	 */
	protected synchronized void trainingComplete(String accepts, String emits, JsonObject provenance, String updateSchema) {
		Predictor model = getOwnModel();
		model.accepts = accepts;
		model.emits = emits;
		model.provenance = provenance.toString();
		model.updateSchema = updateSchema;
		model.trainingStatus = null;
		model.save();
	}
	
	/**
	 * A training process that encounters an error that prevented the predictor
	 * from being trained should call this and provide human-readable details
	 * of the reason; the predictor model is saved at the end. The accepts,
	 * emits and update schema, as well as provenance, are all set to the
	 * string representation of {@link JsonNull#INSTANCE}.
	 */
	public synchronized void trainingFailed(String details, Throwable cause) {
		this.trainingErrorCause = cause;
		Predictor model = getOwnModel();
		this.accepts = this.emits = this.provenance = this.updateSchema = model.accepts = model.emits = model.provenance = model.updateSchema = JsonNull.INSTANCE.toString();
		this.trainingStatus = model.trainingStatus = details + (trainingNowOffline ? " Resource will be removed at some point in the future." : "");
		model.save();
	}
	
	/**
	 * Returns {@code true} if the accepts, emits and update schema are all
	 * equivalent to {@link JsonNull#INSTANCE} (i.e., {@code "null"}).
	 */
	public synchronized boolean isTrainingFailed() {
		Predictor model = getOwnModel();
		final String jsonNull = JsonNull.INSTANCE.toString();
		return (jsonNull.equals(this.accepts) && jsonNull.equals(this.emits) && jsonNull.equals(this.updateSchema)) ||
				(jsonNull.equals(model.accepts) && jsonNull.equals(model.emits) && jsonNull.equals(model.updateSchema));
	}
	
	/** Returns {@code true} iff this predictor appears ready to process data. */
	public synchronized boolean isTrained() {
		Predictor model = getOwnModel();
		final String jsonNull = JsonNull.INSTANCE.toString();
		return !(model.accepts == null || jsonNull.equals(model.accepts) || this.accepts == null || jsonNull.equals(this.accepts));
	}
	
	/**
	 * Sets the training status message and immediately saves the model.
	 * Deliberately not called {@code setTrainingStatus} because, if it is,
	 * then Play will call it on <em>every</em> write to the field
	 * {@code trainingStatus}, including the one in the constructor, which
	 * <em>should not cause the object to be persisted to the database</em>.
	 * */
	public void updateTrainingStatus(String message) {
		Predictor model = getOwnModel();
		model.trainingStatus = message;
		model.save();
	}
	
	/**
	 * Since working with the database models across multiple threads seems to
	 * cause them to become detached from the database, this should retrieve
	 * the persistent model that this object used to (or may still) represent.
	 */
	public Predictor getOwnModel() {
		Predictor model = findById(name);
		return model == null ? this : model;
	}
	
	public boolean isUpdatable() { return updateSchema != null; }

	/**
	 * Generates a timestamp based name incorporating the given
	 * {@code learnerName} and {@code relationName}. Generates names until
	 * it finds a unique one, which is only an issue when servicing two
	 * concurrent requests.
	 */
	public static synchronized String generateName(String learnerName, String relationName) {
		String lead = learnerName + "_" + relationName;
		String name = null;
		do {
			name = Util.generateTimeStampBasedName(lead, new Date());
		} while (findById(name) != null);
		return name;
	}
	
	public JsonObject getReportableProvenance() {
		JsonObject prov = super.getReportableProvenance();
		prov.addProperty("learner", Learners.getReverseRoute(learnerName));
		return prov;
	}

	/**
	 * Generates a standardised provenance object that incorporates the
	 * original task in JSON and a creation date and time. Can be used by
	 * subclasses when instantiating Predictor model for the first time.
	 */
	public static JsonObject newProvenance(Task task, Date created) {
		JsonObject provenance = new JsonObject();
		provenance.add("task", task.getTaskInJSON());
		provenance.addProperty("created", Util.UTC_DATETIME_FORMAT.format(created) );
		return provenance;
	}

	//--Predictor-specific methods---------------------------------------------

	/**
	 * Attempt to update the predictor with the given additional training
	 * examples. If passing a single value then wrap it in a JsonArray first.
	 */
	public abstract void update(JsonArray updateValues);
	
	/**
	 * Should be used by subclasses to modify the updated date in the
	 * predictor's provenance.
	 */
	protected void reviseUpdatedDate() {
		addToProvenance("updated", Util.UTC_DATETIME_FORMAT.format( new Date() ));
	}

	public static class Status extends PSIResource {
		/** URI of the learner doing the processing. */
		public String learner;
		/** Text describing the status of the learning process. */
		public String status;

		public Status(Predictor p, String uri) {
			this.uri = uri;
			this.learner = Learners.getReverseRoute(p.learnerName);
			this.status = p.trainingStatus;
		}
	}

}