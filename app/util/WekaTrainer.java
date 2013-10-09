/**
 * 
 */
package util;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import util.Util;

import models.data.Relation;
import models.learner.Learner;
import models.learner.Task;
import models.learner.Task.CommonResource;
import models.learner.WekaLearner;
import models.learner.WekaParameter;
import models.predictor.Predictor;
import models.predictor.WekaPredictor;
import models.transformer.Transformer;
import models.attribute.Attribute;
import play.Logger;
import weka.classifiers.Classifier;
import weka.clusterers.Clusterer;
import weka.core.DenseInstance;
import weka.core.Instances;
import weka.core.OptionHandler;
import weka.core.WeightedInstancesHandler;

/**
 * A collection of methods for training Weka classifiers and clusters.
 * 
 * @author jmontgomery
 *
 */
public final class WekaTrainer extends Trainer {
	public static final double MISSING_VALUE = weka.core.Utils.missingValue();

	private final static List<String> BOOLEAN_NOMINAL_VALUES = Arrays.asList( new String[]{"false", "true"} );
	private final static double FALSE_VALUE_INDEX = 0;
	private final static double TRUE_VALUE_INDEX = 1;

	private static WekaTrainer instance;
	
	private WekaTrainer() { }
	
	public static synchronized WekaTrainer instance() {
		if (instance == null)
			instance = new WekaTrainer();
		return instance;
	}
	
	public Predictor newUntrainedPredictor(Learner learner, Task task) throws TrainingException {
		try {
		Attribute.Description sourceAttrDesc = task.getResource(CommonResource.source);
		Relation.Description relationDesc = HttpUtil.getPSIResponse( sourceAttrDesc.relation );
		String relationURIFragment = relationDesc.uri.replaceAll("\\?.*$","").replaceAll("\\/$","").replaceAll(".*\\/", ""); 
		String name = Predictor.generateName(learner.name, relationURIFragment);
		Predictor predictor = new WekaPredictor(name, learner.name + " trained predictor (trained on " + relationURIFragment + ")",
				learner.name, DEFAULT_STATUS);
		predictor.save();
		return predictor;
		} catch (ExternalResourceException ere) {
			throw new TrainingException(ere);
		}
	}
	
	/**
	 * Trains the Weka Classifer or Clusterer specified by {@code learner} on
	 * the given {@code Task}, storing the predictive model in {@code predictor}.
	 * @throws TrainingException if there is an error setting Weka options or
	 * obtaining values from an external attribute.
	 * @throws BadTrainingDataException if the given attributes do not generate
	 * the same number of values (for instance, when they have different query
	 * arguments).
	 */
	public void trainPredictor(Predictor predictor, Learner learner, Task task) throws TrainingException, BadTrainingDataException {
		assert predictor instanceof WekaPredictor;

		JsonElement sourceSchema = getAttribute(task, CommonResource.source).emits;
		JsonElement labelSchema = task.hasResource(CommonResource.target) ? getAttribute(task, CommonResource.target).emits : null;
		
		ArrayList<weka.core.Attribute> wekaAttrs = generateWekaAttributes(sourceSchema, labelSchema);
		Logger.trace("Based on source and target attribute schema, produced the following list of WEKA attributes: %s", Arrays.toString(wekaAttrs.toArray()));

		Map<CommonResource,JsonArray> allValues = retrieveValues(task, true);
		Instances wekaData = generateWekaClassificationData(wekaAttrs, allValues);
		Serializable wekaPredictor = performTraining(learner, task, wekaData, labelSchema != null);
		
		((WekaPredictor)predictor).trainingComplete(wekaPredictor, wekaData, sourceSchema.toString(), generatePredictorOutputSchema(wekaPredictor, labelSchema),
				Predictor.newProvenance(task, new Date()),
				generateUpdateSchema(learner, sourceSchema, labelSchema, wekaPredictor instanceof WeightedInstancesHandler) );
	}

	/**
	 * Generates a Weka value suitable for training or prediction based on the
	 * given Weka attribute details and JSON value.
	 */
	public static double generateWekaValue(weka.core.Attribute wekaAttribute, JsonElement value) {
		if (value == null || value.isJsonNull()) //external missing values will be represented as a JSON null value, internally generated ones may actually be null
			return MISSING_VALUE;
		assert value instanceof JsonPrimitive;
		JsonPrimitive primitive = value.getAsJsonPrimitive();
		if (primitive.isBoolean())
			return primitive.getAsBoolean() ? TRUE_VALUE_INDEX : FALSE_VALUE_INDEX;
		else if (primitive.isNumber())
			return primitive.getAsDouble();
		else if (primitive.isString())
			return wekaAttribute.indexOfValue(primitive.getAsString());
		throw new IllegalArgumentException("Unable to convert given value to double: " + value);
	}
	
	/**
	 * Trains either a Weka Classifer or Clusterer, depending on the value of
	 * {@code isClassifier}. Will fail if the class specified by
	 * {@code learner.className} is neither a Classifier nor a Clusterer.
	 * @throws TrainingException if there is an error when setting the Weka
	 * 			learner's options. 
	 */
	protected static Serializable performTraining(Learner learner, Task task, Instances wekaData,
			final boolean isClassifier) throws TrainingException
	{
		if (! (learner instanceof WekaLearner))
			throw new IllegalArgumentException("Given Learner (of type " + learner.getClass().getCanonicalName() + ") is not suitable this Trainer (type " + WekaTrainer.class.getCanonicalName() + ")");
		OptionHandler wekaPredictor = newWekaPredictor( (WekaLearner)learner, task);
		Logger.trace("Instantiated WEKA predictor successfully, with its options");
		
		try {
			if (isClassifier)
				((Classifier)wekaPredictor).buildClassifier(wekaData);
			else //is Clusterer
				((Clusterer)wekaPredictor).buildClusterer(wekaData);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		Logger.trace("Trained WEKA classifier successfully");
		return (Serializable) wekaPredictor;
	}
	
	/**
	 * Creates a new instance of the Weka class specified by the Learner and
	 * sets its options according to those in the Task.
	 * @throws TrainingException 
	 */
	private static OptionHandler newWekaPredictor(WekaLearner learner, Task task) throws TrainingException {
		try {
			OptionHandler wekaPredictor = (OptionHandler) learner.getLearnerConstructor().newInstance();
			String[] wekaOptions = WekaParameter.formatSettingsForToolkit(learner, task.getSettings());
			Logger.trace("Passing these options to WEKA predictor: %s", Arrays.toString(wekaOptions));
			((OptionHandler)wekaPredictor).setOptions( wekaOptions );
			return wekaPredictor;
		} catch (InstantiationException ie) {
			throw new RuntimeException(ie);
		} catch (IllegalAccessException iae) {
			throw new RuntimeException(iae);
		} catch (ClassNotFoundException cnfe) {
			throw new RuntimeException(cnfe);
		} catch (Exception e) {
			//Most likely thrown by setOptions(); srsly, they couldn't narrow it down a little given it's only thrown if an option isn't supported?
			throw new TrainingException("Error while setting parameter values: " + e.getMessage(), e);
		}
	}
		
	private static String generatePredictorOutputSchema(Serializable wekaPredictor, final JsonElement targetAttrSchema) {
		if (targetAttrSchema != null) //is a Classifier
			return generateClassifierEmitsSchema(targetAttrSchema);
		else { //assume is a clusterer, provide more specific output schema
			try {
				return generateClustererEmitsSchema( ((Clusterer)wekaPredictor).numberOfClusters() );
			} catch (Exception e) {
				Logger.warn("Unable to obtain number of clusters from clusterer '%s', cause: %s", wekaPredictor, e);
				return generateClustererEmitsSchema(null);
			}
		}
	}
	
	//--Data wrangling---------------------------------------------------------

	/**
	 * Maps from <em>atomic</em> JSON types to Weka attribute types; although
	 * any JSON schema can specify an 'enum' of allowed values, this is only
	 * translated to a Weka nominal type if the JSON type is 'string'.
	 */
	protected static weka.core.Attribute generateWekaAttributeFromSchema(final String name, JsonObject schema) {
		JSONType type = schema.has("type") ? JSONType.valueOf(schema.get("type").getAsString().toUpperCase()) : null;
		JsonArray jsonValues = schema.has("enum") ? schema.get("enum").getAsJsonArray() : null;
		if (jsonValues != null && type == JSONType.STRING) {
			List<String> enumValues = new ArrayList<>(jsonValues.size());
			for (JsonElement el : jsonValues)
				enumValues.add(el.getAsString()); //safe as we know they're Strings already
			return new weka.core.Attribute(name, enumValues);
		} else if (type != null) {
			if (type == JSONType.BOOLEAN)
				return new weka.core.Attribute(name, BOOLEAN_NOMINAL_VALUES);
			else if (type == JSONType.INTEGER) // no distinction between integers and (real-valued) numbers
				return new weka.core.Attribute(name);
			else if (type == JSONType.NUMBER)
				return new weka.core.Attribute(name);
			else if (type == JSONType.STRING)
				return new weka.core.Attribute(name, (List<String>) null);
			throw new UnsupportedOperationException("Don't know how to create attribute for given type: " + schema.get("type").getAsString());
		}
		throw new IllegalArgumentException("Neither type nor enum given in schema, so cannot construct Weka attribute for schema: " + schema);
	}
	
	/**
	 * Generate Weka attribute details for either supervised or unsupervised
	 * learning, setting {@code targetSchema} to {@code null} in the second
	 * case.
	 * @throws TrainingException if there was a problem compiling the given
	 * PSI schema.
	 */
	public static ArrayList<weka.core.Attribute> generateWekaAttributes(JsonElement sourceAttrSchema, JsonElement targetSchema)
			throws TrainingException
	{
		try {
			JsonArray sourceItemsSchema = Schema.compileToJSONSchema( sourceAttrSchema ).getAsJsonObject().get("items").getAsJsonArray();
			final int numAttributes = (targetSchema == null ? 0 : 1) + sourceItemsSchema.size();
			ArrayList<weka.core.Attribute> wekaAttrs = new ArrayList<>(numAttributes);
			if (targetSchema != null)
				wekaAttrs.add( generateWekaAttributeFromSchema("target", Schema.compileToJSONSchema( targetSchema )) );
			for (int i = 0; i < sourceItemsSchema.size(); i++)
				wekaAttrs.add( generateWekaAttributeFromSchema( "attribute " + (i+1), sourceItemsSchema.get(i).getAsJsonObject() ) );
			return wekaAttrs;
		} catch (ExternalResourceException ere) {
			throw new TrainingException(ere);
		}
	}
	
	/**
	 * Generates Weka's {@code Instances} from source and (optionally) target
	 * attributes. Also supports weight attributes.
	 * @throws TrainingException if was unable to contact an external attribute
	 */
	public static Instances generateWekaClassificationData(ArrayList<weka.core.Attribute> wekaAttrs,
			Map<CommonResource,JsonArray> allValues)throws TrainingException
	{
		JsonArray sourceValues = allValues.get(CommonResource.source);
		boolean hasTarget = allValues.containsKey(CommonResource.target); //|-will ask these questions a lot, so cache answers
		boolean hasWeight = allValues.containsKey(CommonResource.weight); //|
		JsonArray targetValues = hasTarget ? allValues.get(CommonResource.target) : null;
		JsonArray weightValues = hasWeight? allValues.get(CommonResource.weight) : null;
		
		final int size = allValues.get(CommonResource.source).size();
		Instances wekaData = new Instances("training data", wekaAttrs, size);
		if (hasTarget)
			wekaData.setClassIndex(0);

		//Combine results into the single table of values expected
		final int numAttributes = wekaAttrs.size();
		for (int i = 0; i < size; i++) {
			//Manually construct double-valued array of attribute values to avoid
			// Weka's infuriatingly stupid copying of its internal array upon
			// *every single call to setValue()*
			double[] attrValues = new double[numAttributes];
			int sourceOffset = 0;
			if (hasTarget) {
				attrValues[sourceOffset] = generateWekaValue( wekaAttrs.get(0), targetValues.get(i) );
				sourceOffset++;
			}
			JsonArray source = sourceValues.get(i).getAsJsonArray();
			for (int j = sourceOffset; j < numAttributes; j++)
				attrValues[j] = generateWekaValue( wekaAttrs.get(j), source.get(j-sourceOffset) );
			wekaData.add( new DenseInstance(hasWeight ? weightValues.get(i).getAsDouble() : 1, attrValues) );
		}
		return wekaData;
	}

}
