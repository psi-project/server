package controllers;

import play.*;

import play.jobs.Job;
import play.jobs.OnApplicationStart;
import play.mvc.*;
import util.ExternalResourceException;
import util.Schema;

import java.net.HttpURLConnection;
import java.util.*;

import org.apache.commons.lang.StringUtils;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import models.predictor.Predictor;
import models.transformer.Transformer;
import util.Util;

public class Predictors extends Transformers {

	//--Update interface-------------------------------------------------------
	
	public static void updateSchema(String id) {
		renderJSON( getUpdatablePredictor(id).updateSchema );
	}

	public static void update(String id, JsonObject body) {
		//Http.Response origResponse = response; //Note: If responses start being 'lost' then reinstate this and use below (internal requests can generate a new global response object in Play)
		Predictor predictor = getUpdatablePredictor(id);
		Transformer.Value updateValues = parseAndCheckRequestBody(body, Transformer.Value.class);
		validateUpdate(predictor, updateValues);
		JsonArray updateList = updateValues.valueList != null ? updateValues.valueList : new JsonArray();
		if (updateValues.value != null)
			updateList.add(updateValues.value);
		predictor.update(updateList);
		predictor.save();
		renderSimpleResponse(HttpURLConnection.HTTP_SEE_OTHER, LOCATION, getReverseRoute(Kind.PREDICTOR, predictor.name), response /*origResponse*/);
	}
	
	private static Predictor getUpdatablePredictor(String id) {
		Predictor predictor = (Predictor)find(Kind.PREDICTOR, id);
		if (! predictor.isUpdatable())
			notFound("This predictor cannot be updated");
		checkReady(predictor);
		return predictor;
	}
	
	private static void validateUpdate(Predictor predictor, Transformer.Value updateValues) {
		try {
			Logger.trace("Received the following update value(s): %s", updateValues.value != null ? updateValues.value : updateValues.valueList);
			JsonObject updateSchema = Schema.compileToJSONSchema( Util.parseJSON(predictor.updateSchema) ); //if validating many values then want this already in appropriate form
			List<String> validationMessages = updateValues.valueList == null ?
					Schema.validateJSON(updateSchema, updateValues.value) : Schema.validateJSON(updateSchema, updateValues.valueList);
			if (! validationMessages.isEmpty())
				badRequest("Validation errors" + (updateValues.valueList != null ? " in at least one update value": "") + ":\n" + StringUtils.join(validationMessages, "\n"));
		} catch (ExternalResourceException ere) {
			error("Could not validate update task due to problems resolving the validation schema. Details: " + ere.getMessage());
		}
	}

	//--DELETE p---------------------------------------------------------------
	
	public static void delete(String id) {
		if (getTransformation(params) != null) {
			badRequest("A joined transformer cannot be deleted directly. " +
					"Its URI will be valid as long as its constituent transformers exist. " +
					"Perhaps you meant to request DELETE " + getReverseRoute(Kind.PREDICTOR, id));
		}
		Predictor target = (Predictor) find(Kind.PREDICTOR, id);
		checkReady(target);
		target.delete();
		renderText("Deleted predictor '%s'. Note that this may have broken an existing joined transformer.", id);
	}
	
	//--Start up jobs----------------------------------------------------------
	/**
	 * Marks as failed those predictors that have not completed training (and
	 * never will since the server has just been (re)started).
	 */
	@OnApplicationStart
	public static class MarkInterruptedTrainingJobs extends Job<Object> {
		public void doJob() {
			Logger.info("Performing startup job to mark interrupted training jobs as failed due to server restart");
			//Interrupted jobs have a null accepts schema
			List<Predictor> failedJobs = Predictor.find("from Predictor where accepts is NULL").<Predictor>fetch();
			if (failedJobs.size() > 0) {
				Logger.info("Found %d interrupted training jobs; updating their status messages to indicate failure.", failedJobs.size());
				for (Predictor failure : failedJobs)
					failure.trainingFailed("Learning interrupted. Resubmit task. This resource may be removed at any point in the future.", null);
			}
		}
	}
	
	//--Admin------------------------------------------------------------------
	
	public static String _deleteAll() {
		List<Predictor> trainedPredictors = Predictor.find("from Predictor where accepts <> 'null'").<Predictor>fetch();
		deletePredictors(trainedPredictors);
		return String.format("%s\nDeleted %d predictors.", new Date(), trainedPredictors.size());
	}
	
	public static String _deleteFailed() {
		//Failed jobs have accepts and emits schema that equal "null"
		List<Predictor> failedJobs = Predictor.find("from Predictor where accepts = 'null' and emits = 'null'").<Predictor>fetch();
		deletePredictors(failedJobs);
		return String.format("%s\nDeleted %d failed jobs.", new Date(), failedJobs.size());
	}
	
	private static void deletePredictors(List<Predictor> targets) {
		for (Predictor p : targets)
			p.delete();
	}

}
