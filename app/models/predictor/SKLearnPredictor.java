package models.predictor;

import java.util.Iterator;

import javax.persistence.Entity;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import play.Logger;
import play.Play;
import play.libs.WS;
import play.libs.WS.HttpResponse;
import util.ConfKeys;
import util.HttpUtil;
import util.SKLearnTrainer;
import util.TrainingException;

/**
 * A PSI {@link Predictor} that uses web.py-hosted scikit-learn.
 * 
 * @author jmontgomery
 *
 */
@Entity
public class SKLearnPredictor extends Predictor {
	private static final long serialVersionUID = 1L;
	
	/** Operations on SKLearn service predictor resources. */
	public static enum Operation {
		DELETE, INFER, UPDATE;
		
		public String getURIExtension() {
			switch (this) {
			case DELETE :
			case INFER : return "";
			case UPDATE : return "/" + this.name().toLowerCase();
			}
			return null;
		}
	}
	
	//Loading appears to have to be delayed, at least when running in dev mode
	protected static String predictorRoot = null;
	
	/**
	 * Name of the sklearn predictor in the web.py service; an anti-pattern
	 * since it must then create the URI later, but that is actually more flexible.
	 */
	public String predictorName;
	
	public SKLearnPredictor(String name, String description, String learnerName, String trainingStatus)
	{
		super(name, description, learnerName, trainingStatus);		
	}
	
	public void trainingComplete(String sklearnPredName, String accepts, String emits,
			JsonObject provenance, String updateSchema)
	{
		((SKLearnPredictor) getOwnModel()).predictorName = sklearnPredName;
		super.trainingComplete(accepts, emits, provenance, updateSchema);
	}


	protected String getPredictorURI(Operation op) {
		if (predictorRoot == null)
			predictorRoot = Play.configuration.getProperty(ConfKeys.SKLEARN_SERVICE_ROOT) + "predictor/";
		return predictorRoot + predictorName + op.getURIExtension();
	}
	
	/**
	 * Throws a {@link play.mvc.results.Error} if the sklearn service is not
	 * running, which allows the error to be immediately presented as a 500
	 * internal server error rather than revealing the Java exception's type.
	 */
	private void assertServiceIsUp() {
		if (! SKLearnTrainer.serviceIsUp())
			throw new play.mvc.results.Error("This predictor is backed by scikit-learn, but the internal scikit-learn service is not currently running");
	}


	//--Entity-related methods-------------------------------------------------
	
	public SKLearnPredictor delete() {
		assertServiceIsUp();
		//Note that since not persisting joined transformers have no way of knowing if
		//(at some point in the past) a joined transformer was created that used this one. 
		Logger.trace("Deleting sklearn predictor model (%s) associated with SKLearnPredictor %s", predictorName, name);
		HttpResponse response = WS.url(getPredictorURI(Operation.DELETE)).delete();
		if (! response.success())
			throw new RuntimeException("Unable to delete predictor in scikit-learn service. Details: " + response.getString());
		return super.delete();
	}
	
	//--Transformer interface--------------------------------------------------

	@Override
	public JsonElement apply(JsonElement value) throws ArrayLengthMismatchException {
		assertServiceIsUp();
		return apply(value, false);
	}
	
	@Override
	public JsonArray apply(Iterator<JsonElement> values) throws ArrayLengthMismatchException {
		assertServiceIsUp();
		//Ugh, tedious, but will still be more efficient to package all the values up for transmission to the scikit learn service.
		JsonArray arrayValue = new JsonArray();
		while (values.hasNext())
			arrayValue.add(values.next());
		return apply(arrayValue, true).getAsJsonArray();
	}
	
	/**
	 * The actual connection to prediction in the sklearn service; can deal
	 * with single or multiple values. Returns either a single prediction
	 * (presumably some JsonPrimitive) or a JsonArray of predictions.
	 * @throws ArrayLengthMismatchException if given value has wrong number of
	 * features (error is detected by sklearn service)
	 */
	private JsonElement apply(JsonElement value, boolean multiple) throws ArrayLengthMismatchException {
		//Repack request into JSON wrapper for sending to sklearn service
		JsonObject req = new JsonObject();
		req.add(VALUE_ARG, value);
		if (multiple)
			req.add("multiple", new JsonPrimitive(true));
		JsonObject response = HttpUtil.postJSON(getPredictorURI(Operation.INFER), req);
		if (response.has("badRequest"))
			throw new ArrayLengthMismatchException(response.get("expected").getAsInt(), response.get("actual").getAsInt());
		if (! response.has(VALUE_ARG))
			throw new RuntimeException("Scikit-Learn service did not return a value reponse as expected");
		
		return response.get(VALUE_ARG);
	}
	
	//--Predictor-specific methods---------------------------------------------
	
	public void update(JsonArray updateValues) {
		assertServiceIsUp();
		JsonObject request = new JsonObject();
		request.add("updates", updateValues);
		JsonObject response = HttpUtil.postJSON(getPredictorURI(Operation.UPDATE), request);
		if (! response.get("success").getAsBoolean() )
			throw new RuntimeException("Error updating scikit learn predictor. Details: " + response.get("body").getAsString());
		reviseUpdatedDate();
	}

}