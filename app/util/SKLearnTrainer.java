/**
 * 
 */
package util;

import java.net.ConnectException;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import util.Util;
import weka.core.WeightedInstancesHandler;

import models.data.Relation;
import models.learner.Learner;
import models.learner.SKLearnLearner;
import models.learner.Task;
import models.learner.WekaLearner;
import models.learner.Task.CommonResource;
import models.predictor.Predictor;
import models.predictor.SKLearnPredictor;
import models.transformer.Transformer;
import models.attribute.Attribute;
import play.Logger;
import play.Play;
import play.libs.WS;
import play.libs.WS.HttpResponse;
import play.mvc.Http;

/**
 * A collection of methods for interfacing with the training part of the
 * sklearn micro web service.
 * 
 * @author jmontgomery
 *
 */
public final class SKLearnTrainer extends Trainer {
	
	private static String SKLEAN_SERVICE_URI = Play.configuration.getProperty(ConfKeys.SKLEARN_SERVICE_ROOT);

	private static SKLearnTrainer instance;
	
	private SKLearnTrainer() { }
	
	public static synchronized SKLearnTrainer instance() {
		if (instance == null)
			instance = new SKLearnTrainer();
		return instance;
	}
	
	public Predictor newUntrainedPredictor(Learner learner, Task task) throws TrainingException {
		try {
			assertServiceIsUp();
			//Note that this is sufficient for classification and clustering using scikit learn, and for the simple ranking algorithm implemented in the sklearn Python service, but nothing else at the moment
			Attribute.Description instanceAttrDesc = task.getResource( task.hasResource(CommonResource.source) ? CommonResource.source : CommonResource.first);
			Relation.Description relationDesc = HttpUtil.getPSIResponse( instanceAttrDesc.relation );
			String relationURIFragment = relationDesc.uri.replaceAll("\\?.*$","").replaceAll("\\/$","").replaceAll(".*\\/", "");
			
			String name = Predictor.generateName(learner.name, relationURIFragment);
			Predictor predictor = new SKLearnPredictor(name, learner.name + " trained predictor (trained on " + relationURIFragment + ")",
					learner.name, DEFAULT_STATUS);
			predictor.save();
			return predictor;
		} catch (ExternalResourceException ere) {
			throw new TrainingException(ere);
		}

	}
	
	/**
	 * Trains the scikit learn classifier or clusterer specified by
	 * {@code learner} on the given {@code Task}. (Basically obtains the
	 * training data and then submits the task with resources replaced by
	 * that data to the sklearn service.
	 * @throws TrainingException if there is an error communicating with the
	 * sklearn service.
	 * @throws BadTrainingDataException if not all given attribute resources
	 * produce the same number of values.
	 */
	public void trainPredictor(Predictor predictor, Learner learner, Task task) throws TrainingException, BadTrainingDataException {
		try {
			assertServiceIsUp();
			assert predictor instanceof SKLearnPredictor;
			//Note that this is sufficient for classification and clustering using scikit learn, and for the simple ranking algorithm implemented in the sklearn Python service, but nothing else at the moment
			Attribute.Description instanceAttrDesc = task.getResource( task.hasResource(CommonResource.source) ? CommonResource.source : CommonResource.first);
			JsonElement labelSchema = task.hasResource(CommonResource.target) ? getAttribute(task, CommonResource.target).emits : null;

			Map<CommonResource,JsonArray> allValues = retrieveValues(task, true);
			
			//Replace resources in original JSON task with their values (which means creating duplicate of task)
			JsonObject newTask = new JsonObject();
			for (Map.Entry<String,JsonElement> prop : task.getTaskInJSON().entrySet()) {
				if (! prop.getKey().equals("resources")) //this roundabout way allows the externally-visible parameter names to differ from the scikit learner parameter names 
					newTask.add( learner.getParameter( prop.getKey() ).toolkitName, prop.getValue() );
			}
			JsonObject newResources = new JsonObject();
			for (Map.Entry<CommonResource,JsonArray> values : allValues.entrySet())
				newResources.add(values.getKey().name(), values.getValue());
			if (labelSchema != null)
				newResources.add( "targetLabels", Schema.compileToJSONSchema( labelSchema ).getAsJsonObject().get("enum") );
			newTask.add("resources", newResources);

			JsonObject status = HttpUtil.postJSON( ((SKLearnLearner)learner).getLearnerURI(), newTask);

			if (! status.has("predictor"))
				throw new TrainingException(status.has("status") ? status.get("status").getAsString() : "Unknown error occurred is scikit-learn");

			((SKLearnPredictor)predictor).trainingComplete(status.get("predictor").getAsString(),
					instanceAttrDesc.emits.toString(),
					generatePredictorOutputSchema(labelSchema, status, task.hasResource(CommonResource.first)),
					Predictor.newProvenance(task, new Date()),
					generateUpdateSchema(learner, instanceAttrDesc.emits, labelSchema, false)
			);
		} catch (ExternalResourceException ere) {
			throw new TrainingException(ere);
		}
	}

	/**
	 * Returns {@code true} if the Python-base sklearn service is running,
	 * {@code false} otherwise.
	 */
	public static boolean serviceIsUp() {
		//Unfortunately must look for a specific exception to test if service is not running, adding considerable complication
		try {
			return WS.url( SKLEAN_SERVICE_URI ).get().success();
		} catch (RuntimeException e) { //yes, these should never be caught, but Play rethrows *all* exceptions caught in .get() as runtime exceptions
			Throwable cause = e;
			while (cause.getCause() != null)
				cause = cause.getCause();
			if (! (cause instanceof ConnectException))
				throw e; //genuine unexpected error
		}
		return false;
	}

	/** Throws a {@code TrainingException} if the sklearn service is not running. */
	private void assertServiceIsUp() throws TrainingException {
		if (! serviceIsUp())
			throw new TrainingException("Internal scikit-learn service is not currently running so cannot proceed with training");
	}
	
	private static String generatePredictorOutputSchema(JsonElement targetAttrSchema, JsonObject status, boolean isRanking) {
		if (targetAttrSchema != null) //is a classifier; output is (probably) same as target attribute
			return generateClassifierEmitsSchema(targetAttrSchema);
		else if (isRanking) {
			return Util.singleQuotesToDouble("{ '$number' : { 'min': 0, 'max': 1 } }"); //simple enough to use JSON literal
		} else { //assume some sort of clusterer
			return generateClustererEmitsSchema(status.has("clusters") ? status.get("clusters").getAsInt() : null).toString(); 
		}
		
	}

}
