/**
 * 
 */
package util;

import java.util.HashMap;

import java.util.Iterator;
import java.util.Map;

import weka.core.WeightedInstancesHandler;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import models.attribute.Attribute;
import models.data.Relation;
import models.learner.Learner;
import models.learner.Task;
import models.learner.WekaLearner;
import models.learner.Task.CommonResource;
import models.predictor.Predictor;
import models.transformer.Transformer;

/**
 * Base type for toolkit-specific trainers responsible for performing training
 * and persisting the created predictor model. Although it cannot be enforced,
 * all {@code Trainers} should be stateless so that a single instance may be
 * created and reused.
 *
 */
public abstract class Trainer {
	
	/** A schema for weights in update values (the schema of any actual weight attribute used during training is not used). */ 
	protected static final JsonObject UPDATE_WEIGHT_SCHEMA = Util.parseJSON(Util.singleQuotesToDouble("{'$number':{'min':0,'description':'Relative weight to give the new instance'}}")).getAsJsonObject();
	
	public static final String DEFAULT_STATUS = "Learning in progress.";
	
	/**
	 * Returns a new untrained {@code Predictor} model with some metadata set
	 * based on the learner and training task, and {@code save()}s the new
	 * model to the database. The task may be modified by this request to
	 * better support subsequent {@linkplain #trainPredictor(Learner, Task,
	 * String) training}.
	 * @throws TrainingException if there is an error obtaining details of a
	 * 	source relation.
	 */
	public abstract Predictor newUntrainedPredictor(Learner learner, Task task)
			throws TrainingException;
	
	/**
	 * Trains the given {@code Predictor}, which <em>must</em> have been
	 * {@linkplain #newUntrainedPredictor(Learner, Task) prepared} using the
	 * same learner and task, by applying the given {@link Learner} to the
	 * given training {@link Task}.
	 * @throws TrainingException if there is an error during training.
	 * @throws BadTrainingDataException if there is a problem with the data
	 * produced by that the resources given in the task.
	 */
	public abstract void trainPredictor(Predictor predictor, Learner learner, Task task)
			throws TrainingException, BadTrainingDataException;
	
	protected static Attribute.Description getAttribute(Task task, CommonResource id) {
		return task.<Attribute.Description>getResource( id.name() );
	}
	
	/**
	 * Retrieves the values for each attribute resource present in the given
	 * task and, {@code checkCountsEqual}, checks that each produces the same
	 * number of values.
	 * @throws TrainingException if unable to retrieve values from an external
	 * attribute. 
	 * @throws BadTrainingDataException if {@code checkCountsEqual} but at
	 * least one pair of attributes produced a different number of values. 
	 */
	protected static Map<CommonResource,JsonArray> retrieveValues(Task task, boolean checkCountsEqual)
			throws TrainingException, BadTrainingDataException
	{
		try {
			final Map<String,Object> allInstancesArgs = Util.makeMap("instance", "all");
			Map<CommonResource,JsonArray> values = new HashMap<>();
			for (CommonResource res : CommonResource.values()) {
				if (res.isAttribute() && task.hasResource(res.name())) {
					Transformer.Value valueResponse = HttpUtil.getPSIResponse( getAttribute(task, res).uri, allInstancesArgs);  
					values.put(res, valueResponse.valueList);
				}
			}
			if (checkCountsEqual)
				checkValueCountsMatch(values);
			return values;
		} catch (ExternalResourceException ere) {
			throw new TrainingException(ere);
		}
	}
	
	/**
	 * Verifies that all value lists present in resources have the same number
	 * of values and throws a {@link BadTrainingDataException} if there's a
	 * mismatch.
	 */
	public static void checkValueCountsMatch(Map<CommonResource,JsonArray> resources) throws BadTrainingDataException {
		Iterator<Map.Entry<CommonResource,JsonArray>> entries = resources.entrySet().iterator();
		Map.Entry<CommonResource,JsonArray> first = entries.next();
		while (entries.hasNext()) {
			Map.Entry<CommonResource,JsonArray> other = entries.next();
			assertValueCountsMatch(first.getValue(), first.getKey().name(), other.getValue(), other.getKey().name());
		}
	}
	
	/**
	 * Tests that the value arrays have the same size and throws a
	 * {@link BadTrainingDataException} if they do not, using the given labels
	 * {@code name1} and {@code name2} to customise the error message.
	 * @throws BadTrainingDataException if {@code values1.size() != values2.size() }
	 */
	public static void assertValueCountsMatch(JsonArray values1, String name1,
			JsonArray values2, String name2) throws BadTrainingDataException
	{
		if (values1.size() != values2.size())
			throw new BadTrainingDataException( String.format("Attributes must produce same number of values, but received %d %s values and %d %s values.", values1.size(), name1, values2.size(), name2) );
	}
	
	/**
	 * Uses the following heuristic to determine the most appropriate emits
	 * schema for the predictor: if {@code targetEmits} describes an integer
	 * type then returns {@code "$number"}, otherwise returns
	 * {@code targetEmits.toString()}. This is suitable for the current range
	 * of learners in the system (both Weka and scitkit-learn based), since
	 * nominal values must also be strings, but is not a general solution,
	 * which requires knowledge of what values the actual <em>learner</em> will
	 * emit.
	 */
	protected static String generateClassifierEmitsSchema(JsonElement targetEmits) {
		return targetEmits.isJsonObject() && targetEmits.getAsJsonObject().has("$integer") || !targetEmits.isJsonObject() && targetEmits.getAsString().equals("$integer") ?
				"$number" : targetEmits.toString();
	}
	
	/**
	 * Returns an emits schema for a typical cluster that outputs values
	 * between 0 and {@code clusterCount - 1}; if {@code clusterCount} is
	 * {@code null} then only the minimum value will be set in the schema.
	 */
	protected static String generateClustererEmitsSchema(Integer clusterCount) {
		JsonObject schema = Util.parseJSON( Util.singleQuotesToDouble("{ '$integer' : { 'min': 0 } }") ).getAsJsonObject();
		if (clusterCount != null)
			schema.get("$integer").getAsJsonObject().add("max", new JsonPrimitive( clusterCount - 1 ));
		return schema.toString();
	}
	
	protected static String generateUpdateSchema(Learner learner, JsonElement sourceAttrSchema, JsonElement targetAttrSchema, boolean usesWeights) {
		if ( learner.makesUpdateablePredictors() ) {
			JsonObject updateSchema = new JsonObject();
			updateSchema.add("/source", sourceAttrSchema);
			if (targetAttrSchema != null) //is a Classifier
				updateSchema.add("/target", targetAttrSchema);
			if (usesWeights)
				updateSchema.add("/weight", UPDATE_WEIGHT_SCHEMA);
			return updateSchema.toString(); 
		}
		return null;
	}


}
