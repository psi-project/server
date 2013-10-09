package models.predictor;

import java.io.Serializable;
import java.util.Arrays;

import javax.persistence.Entity;
import javax.persistence.Lob;
import javax.persistence.Transient;

import org.apache.commons.lang.SerializationUtils;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import play.Logger;
import util.WekaTrainer;
import weka.classifiers.Classifier;
import weka.classifiers.UpdateableClassifier;
import weka.clusterers.Clusterer;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.WeightedInstancesHandler;

/**
 * A PSI {@link Predictor} that uses Weka.
 * 
 * @author jmontgomery
 *
 */
@Entity
public class WekaPredictor extends Predictor {
	private static final long serialVersionUID = 1L;
	
	/**
	 * Serialised empty {@code Instances} describing WEKA Attributes and
	 * indicating which is the class attribute.
	 */
	@Lob
	public byte[] wekaDatasetBytes;
	/** Serialised trained WEKA predictor. */
	@Lob
	public byte[] wekaPredictorBytes;

	/** Actual WEKA {@code Instances} object; create copies when using. */
	@Transient
	private Instances wekaDataset;
	/** Actual trained WEKA predictor. */
	@Transient
	private Serializable wekaPredictor;
	
	public WekaPredictor(String name, String description, String learnerName, String trainingStatus)
	{
		super(name, description, learnerName, trainingStatus);
	}
	
	public void trainingComplete(Serializable wekaPredictor, Instances wekaDatasetForReference,
			String accepts, String emits, JsonObject provenance, String updateSchema)
	{
		WekaPredictor model = (WekaPredictor) getOwnModel();
		model.setWekaPredictor(wekaPredictor);
		model.setWekaAttributeDetails(wekaDatasetForReference);
		super.trainingComplete(accepts, emits, provenance, updateSchema);
	}

	//--Entity-related methods-------------------------------------------------
	
	public synchronized Instances getWekaAttributeDetails() {
		if (wekaDataset == null)
			wekaDataset = (Instances) SerializationUtils.deserialize(wekaDatasetBytes);
		return wekaDataset;
	}

	public void setWekaAttributeDetails(Instances wekaDataset) {
		this.wekaDataset = new Instances(wekaDataset, 0);
		this.wekaDatasetBytes = SerializationUtils.serialize(this.wekaDataset);
	}
	
	public synchronized Serializable getWekaPredictor() {
		if (wekaPredictor == null)
			wekaPredictor = (Serializable) SerializationUtils.deserialize(wekaPredictorBytes);
		return wekaPredictor;
	}
	
	public void setWekaPredictor(Serializable wekaPredictor) {
		this.wekaPredictor = wekaPredictor;
		this.wekaPredictorBytes = SerializationUtils.serialize(wekaPredictor);
	}

	//--Transformer interface--------------------------------------------------

	public JsonElement apply(JsonElement value) throws ArrayLengthMismatchException {
		try {
			Instances wekaDataset = getWekaAttributeDetails();
			Instance wekaInstance = constructWekaInstance(null, (JsonArray) value, wekaDataset, 1);
			
			Serializable wekaPredictor = getWekaPredictor();
			if (wekaPredictor instanceof Classifier)
				return predictClass((Classifier) wekaPredictor, wekaInstance);
			else if (wekaPredictor instanceof Clusterer)
				return predictCluster((Clusterer) wekaPredictor, wekaInstance);
			throw new UnsupportedOperationException("Only classifiers and clustering algorithms currently supported");
		} catch (ArrayLengthMismatchException e) {
			throw e;
		} catch (Exception e) {
			throw new RuntimeException("Possibly WEKA generated an exception during prediction", e);
		}
	}
	
	private JsonPrimitive predictClass(Classifier wekaClassifier, Instance wekaInstance) throws Exception {
		double pred = wekaClassifier.classifyInstance(wekaInstance);
		
		weka.core.Attribute wekaClassAttr = wekaDataset.attribute( wekaDataset.classIndex() );
		//Assumes output is either nominal or numerical; if supporting a wider range of learners this will need to be modified (does Weka support a wider range?)
		if (wekaClassAttr.isNumeric())
			return new JsonPrimitive( pred );
		else if (wekaClassAttr.isNominal())
			return new JsonPrimitive( wekaClassAttr.value((int) pred) );
		throw new UnsupportedOperationException("Unable to process predictions that are neither numerical nor nominal");
	}
	
	private JsonPrimitive predictCluster(Clusterer wekaClusterer, Instance wekaInstance) throws Exception {
		return new JsonPrimitive( wekaClusterer.clusterInstance(wekaInstance) );
	}
	
	//--Predictor-specific methods---------------------------------------------
	
	public void update(JsonArray updateValues) {
		try {
			assert getWekaPredictor() instanceof UpdateableClassifier;
			UpdateableClassifier wekaClassifier = (UpdateableClassifier) getWekaPredictor();
			Instances wekaDataset = new Instances(getWekaAttributeDetails(), 0);
			for (JsonElement update : updateValues)
				updateOne(wekaClassifier, wekaDataset, update.getAsJsonObject());
			setWekaPredictor( (Serializable) wekaClassifier ); //revise the serialised representation
			reviseUpdatedDate();
		} catch (Exception e) { //thrown by Weka if couldn't deal with new instance
			throw new RuntimeException("Weka generated an exception during update", e);
		}
	}
	
	private void updateOne(UpdateableClassifier wekaClassifier, Instances wekaDataset, JsonObject update) throws Exception {
		Instance wekaInstance = constructWekaInstance(update.get("target"),
				update.get("source").getAsJsonArray(),
				wekaDataset,
				update.has("weight") ? update.get("weight").getAsDouble() : 1);
		wekaClassifier.updateClassifier(wekaInstance);
	}
	
	/**
	 * Constructs a WEKA {@code Instance} suitable for classification and
	 * regression tasks from the given JSON encoded source vector and target
	 * value; if not target value is given then the returned instance will be
	 * suitable for clustering tasks only. The generated {@code Instance} is
	 * associated with the given {@code Instances} collection (i.e., so it
	 * knows the meaning of its attributes).
	 * @throws ArrayLengthMismatchException if {@code sourceArray.size()} <
	 * {@code wekaDataset.numAttributes()} (values in {@code sourceArray}
	 * beyond the length of the number of attributes are merely ignored).
	 */
	protected static Instance constructWekaInstance(JsonElement targetValue, JsonArray sourceArray,
			Instances wekaDataset, final double weight) throws ArrayLengthMismatchException
	{
		final boolean isForClassification = wekaDataset.classIndex() >= 0;
		final int instanceSize = wekaDataset.numAttributes() - (isForClassification ? 1 : 0);
		if (sourceArray.size() < instanceSize)
			throw new ArrayLengthMismatchException(instanceSize, sourceArray.size());
			
		double[] attrValues = new double[ wekaDataset.numAttributes() ];
		int sourceOffset = 0;
		if (isForClassification) {
			attrValues[0] = WekaTrainer.generateWekaValue(wekaDataset.attribute(0), targetValue);
			sourceOffset++;
		}
		for (int i = 0; i < instanceSize; i++) {
			//Note: conversion is based primarily on type of JSON value, not on previously recorded Weka attribute
			// details, except for nominal values where the WEKA attribute is used to determine the numeric value.
			attrValues[sourceOffset] = WekaTrainer.generateWekaValue(wekaDataset.attribute(sourceOffset), sourceArray.get(i));
			sourceOffset++;
		}
		Logger.trace("In Predictor.constructWekaInstance(): constructed instance is %s", Arrays.toString(attrValues));
		Instance wekaInstance = new DenseInstance(weight, attrValues);
		wekaInstance.setDataset(wekaDataset);
		return wekaInstance;
	}

}