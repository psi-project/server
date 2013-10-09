import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import models.transformer.Transformer;

import org.junit.*;
import org.junit.contrib.assumes.Assumes;
import org.junit.contrib.assumes.Corollaries;
import org.junit.runner.RunWith;

import play.Logger;
import play.mvc.Http;
import play.mvc.Http.Response;
import play.test.Fixtures;

import util.ExternalResourceException;
import util.JSONValueGenerator;
import util.Schema;
import util.Util;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import controllers.Data;
import controllers.Learners;

/**
 * Learner, training and predictor tests, including joining attributes with
 * predictors. Extends {@link BaseEmitterTester} because it tests the
 * predictors produced.
 * <p>
 * If you review the log after a test you will see that
 * {@link #assertValidPSIMessage(JsonObject, String)} on predictor
 * representations triggers additional internal GET requests to the attributes
 * used in training. This is because that assertion function 'compiles' the
 * representation to pure JSON before it is validated, which includes the
 * nested {@code provenance} and its stored {@code task}, which includes $
 * references to the training attributes.
 * 
 */
@RunWith(Corollaries.class)
public class LearnerAndPredictorTests extends BaseEmitterTester {
	
	/**
	 * The descriptions for currently-implemented learners that use sci-kit
	 * learn, which cannot be used in testing because that requires the sklearn
	 * service to also be running, which cannot be guaranteed.
	 */
	private static final List<String> KNOWN_SKLEARN_LEANERS = Arrays.asList(new String[]{
		"C-Support Vector Classification",
		"Gaussian Mixture Model",
		"Simple preference function learning algorithm. Uses logistic regression on pairs of preferred-less preferred instances to induce the preference function for single instances",
		"Stochastic Gradient Descent Classifier"
	});
	
	private static final int LARGE_RELATION_THRESHOLD = 2000;
	/** Repoll an accepted training task after this many seconds. */
	private static final int REPOLL_TRAINING_AFTER = 5;

	/** Cached learner resource URIs. */
	private static List<String> learnerURIs;
	/** Cached learner representations. */
	private static List<JsonObject> learners;
	/** Cached relation representations. */
	private static List<JsonObject> relations;
	/** Cached predictor representations. */
	private static List<JsonObject> predictors;
	
	/**
	 * Loads data and learner models into the database; preloads all relation
	 * and learner URIs.
	 * Assumes success of {@link DiscoveryTests#collectionsOK()}. 
	 */
	@BeforeClass
	public static void loadLearnersAndRelations() {
		Fixtures.deleteDatabase();
		Data._initialiseRelations(); //YAML is insufficient to properly build database contents
		Learners._initialiseLearners();

		learnerURIs = getResourceList("learners", true);
		learners = relations = predictors = null;
	}
	
	/**
	 * Tests all learner's representations; caches the representations in
	 * {@link #learners}.
	 */
	@Test public void learnerRepresentation() throws ExternalResourceException {
		//Yes, tests should be independent, but use of Assumes means this will be run first
		learners = new Vector<>(learnerURIs.size());
		for (String uri : learnerURIs) {
			JsonObject l = GET_JSON(uri);
			assertValidPSIMessage(l, "learner");
			learners.add(l);
		}
	}
	
	/**
	 * Trains a predictor for each combination of learner and relation that
	 * has some attributes that satisfy the learner's needs; caches all
	 * relation representations.
	 * <p>
	 * Notes:
	 * <ul>
	 *   <li> If performing supervised learning this could well select an
	 *   attribute that includes the label, but since we don't test the
	 *   predictor's accuracy later that shouldn't matter.</li>
	 *   <li>Whether a training-status representation is ever tested depends on
	 *   the availability of a large enough data set to trigger the 202 response.
	 *   </li>
	 * </ul>
	 * @throws ExternalResourceException if couldn't resolve an entity
	 * referenced by a learner's task schema.
	 */
	@Assumes("learnerRepresentation")
	@Test public void trainSomething() throws ExternalResourceException {
		int combinations = 0, longRuns = 0;
		List<String> internalServerErrors = new ArrayList<>();
		
		Map<String,JsonObject> resourceSchema = new HashMap<>();
		Map<String,String> taskTemplates = new HashMap<>(); //since Gson doesn't support cloning JsonObject, it's quicker to store task requests as text and re-parse later *sigh*
		List<JsonObject> safeLearners = new Vector<>(learners.size() - KNOWN_SKLEARN_LEANERS.size());
		for (JsonObject learner : learners) {
			if (! KNOWN_SKLEARN_LEANERS.contains(getProp(learner,"description"))) {
				safeLearners.add(learner);
				String uri = getProp(learner,"uri");
				JsonObject taskSchema = Schema.compileToJSONSchema(learner.get("taskSchema"));
				// Less efficient, but creates shorter code than walking the chain below *sigh*
				JsonObject resourcesSchema = Schema.compileToJSONSchema(learner.get("taskSchema").getAsJsonObject().get("/resources"));
				resourceSchema.put( uri, resourcesSchema.get("properties").getAsJsonObject() );
				JsonObject bareTask = JSONValueGenerator.generate( taskSchema ).getAsJsonObject();
				for (Map.Entry<String,JsonElement> resource : bareTask.get("resources").getAsJsonObject().entrySet())
					resource.setValue( JsonNull.INSTANCE );
				taskTemplates.put(uri, bareTask.toString());
			}
		}
		
		relations = getResources("relations");
		List<JsonObject> smallerRelations = new Vector<>(relations.size());
		for (JsonObject relation : relations)
			if (relation.get("size").getAsInt() < LARGE_RELATION_THRESHOLD)
				smallerRelations.add(relation);
		
		JsonObject body = stringToJSON("{'psiType':'task'}").getAsJsonObject();
		for (JsonObject relation : smallerRelations) {
			List<JsonObject> attributes = getResources( relation.get("attributes") );
			for (int i = 0; i < attributes.size(); i++) //cache compiled versions, since that's what will be used during validation anyway
				attributes.set(i, Schema.compileToJSON( attributes.get(i) ).getAsJsonObject() );
			for (JsonObject learner : safeLearners) {
				String uri = getProp(learner,"uri");
				JsonObject task = Util.parseJSON( taskTemplates.get(uri) ).getAsJsonObject();
				JsonObject perResourceSchema = resourceSchema.get(uri);
				int matched = 0;
				Set<Map.Entry<String,JsonElement>> resources = task.get("resources").getAsJsonObject().entrySet();
				for (Map.Entry<String,JsonElement> resource : resources) {
					for (int i = 0; i < attributes.size() && resource.getValue().isJsonNull(); i++) {
						if ( Schema.validateJSON( perResourceSchema.get(resource.getKey()).getAsJsonObject(), attributes.get(i) ).isEmpty() ) {
							matched++;
							resource.setValue( new JsonPrimitive( "$" + getProp(attributes.get(i),"uri") ) ); //could actually drop attribute structure in, but this more closely matches expected way of constrcuting tasks
						}
					}
				}
				if (matched == resources.size()) {
					combinations++;
					body.add("task", task);
					Response response = POST_JSON(uri, body);
					if (response.status == Http.StatusCode.CREATED || response.status == Http.StatusCode.ACCEPTED) {
						String location = response.getHeader("location"); 
						assertNotNull(location);
						if (response.status == Http.StatusCode.ACCEPTED) { //test will be slow, but wait 1s at a time and retest
							longRuns++;
							JsonObject p;
							do {
								p = GET_JSON(location);
								assertTrue(p.has("psiType"));
								if (getProp(p,"psiType").equals("training-status")) {
									assertValidPSIMessage(p, "training-status");
									Logger.info("Training accepted; waiting %d second(s) before polling predictor/status URI again: %s", REPOLL_TRAINING_AFTER, location);
									try { Thread.sleep(REPOLL_TRAINING_AFTER * 1000); } catch (InterruptedException e) { throw new RuntimeException("Interrupted while waiting to repoll training status URI", e); }
								}
							} while (getProp(p,"psiType").equals("training-status"));
							assertValidPSIMessage(p, "transformer"); //although will be tested later, this captures the unexpected situation of a status representation being replaced with something other than a predictor
						} else {
							assertStatus(Http.StatusCode.CREATED, response);
						}
					} else {
						assertStatus(Http.StatusCode.INTERNAL_ERROR, response);
						internalServerErrors.add( String.format("%s with task %s", uri, task) );
					}
				}
			}
		}
		
		if (combinations == 0)
			Logger.warn("trainSomething() trivially passed: Couldn't match any relations' attributes to the needs of any learner among %d potential combinations", safeLearners.size() * smallerRelations.size());
		else {
			Logger.info("Trained %d predictors using combinations of available learners and relations. %d possible combinations considered. %d tasks were long runs.", combinations, safeLearners.size() * smallerRelations.size(), longRuns);
			if (! internalServerErrors.isEmpty())
				Logger.warn("Note that %s internal server error response(s) were encountered, for the following combinations: %s", internalServerErrors.size(), prettyList(internalServerErrors));
		}
	}
	
	/**
	 * Tests basic functionality for all predictors listed by the system (even
	 * if {@link #trainSomething()} only trains one) by using the testing
	 * functions for standard transformers.
	 */
	@Assumes("trainSomething")
	@Test public void predictorBasics() throws ExternalResourceException {
		List<JsonObject> predictors = getPredictorsToTest("predictorBasics()");
		if (! predictors.isEmpty()) {
			for (JsonObject p : predictors)
				TransformerTests.testTransformer(p);
		}
	}
	
	/** Tests various attribute-predictor joins. */
	@Assumes("predictorBasics")
	@Test public void attributePredictorJoins() throws ExternalResourceException {
		List<JsonObject> predictors = getPredictorsToTest("attributePredictorJoins()");
		if (! predictors.isEmpty()) {
			logJoinTestDetails( TransformedAttributeTests.testAllAttributeTransformerJoins(relations, predictors),
				"testPredictors", "attribute", "predictor" );
		}
	}
	
	/** Tests various attribute-predictor joins. */
	@Assumes("attributePredictorJoins")
	@Test public void updatablePredictors() throws ExternalResourceException {
		List<JsonObject> predictors = getPredictorsToTest("updatablePredictors()");
		if (! predictors.isEmpty()) {
			for (JsonObject p : predictors)
				if (p.has("update"))
					testPredictorUpdating(p);
		}
	}
	
	/**
	 * Tests that a predictor's update resource works and submits a single value
	 * and multi-valued update to it.
	 */
	private void testPredictorUpdating(JsonObject p) throws ExternalResourceException {
		assert p.has("update");
		String uri = getProp(p,"update");
		JsonElement updateValue = JSONValueGenerator.generate( Schema.compileToJSONSchema( GET_JSON(uri) ) );
		JsonArray updateValues = new JsonArray();
		for (int i = 0; i < 10; i++)
			updateValues.add(updateValue);

		testPredictorUpdateRequest(p, new Transformer.Value(updateValue));
		testPredictorUpdateRequest(p, new Transformer.Value(updateValues));
	}

	/**
	 * Submits the given update value(s) to the predictor's update resource and
	 * checks that the predictor has actually changed (at least, that it's
	 * provenance has changed).
	 */
	private void testPredictorUpdateRequest(JsonObject p, Transformer.Value request) {
		assert p.has("update");
		Response response = POST_JSON(getProp(p,"update"), request);
		//Play's name for the constant 303 is METHOD rather than SEE_OTHER, which is confusing, so not used
		assertTrue("Expected 201 Created or 303 See Other response to update request, but received " + response.status,
				response.status == Http.StatusCode.CREATED || response.status == 303);
		String location = response.getHeader("location"); 
		assertNotNull(location);
		JsonObject pPrime = GET_JSON(location);
		assertValidPSIMessage(pPrime, "transformer");
		JsonElement oldUpdated = p.get("provenance").getAsJsonObject().get("updated");
		JsonElement updated = pPrime.get("provenance").getAsJsonObject().get("updated");
		assertNotNull("Expected update property in predictor's provenance, but not found", updated);
		assertTrue("Expected update property in predictor's provenace to have changed, but is the same as before",
				oldUpdated == null || !oldUpdated.equals(updated));
	}
	
	/** Cleans up and tests that predictors can be deleted. */
	@Assumes("updatablePredictors")
	@Test public void deletePredictors() {
		List<JsonObject> predictors = getPredictorsToTest("deletePredictors()");
		if (! predictors.isEmpty()) {
			for (JsonObject p : predictors)
				DELETE_OK( getProp(p,"uri") );
		}
	}
	
	/**
	 * Returns the list of predictor representations and, if it is empty, logs
	 * a warning that the given named test has been passed trivially. 
	 */
	private static synchronized List<JsonObject> getPredictorsToTest(String testName) {
		if (predictors == null)
			predictors = getResources("predictors");
		if (predictors.isEmpty())
			Logger.warn("%s trivially passed as no predictors available to test", testName);
		return predictors;
	}

}