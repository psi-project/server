package models;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import models.attribute.Attribute;
import models.data.Relation;
import models.learner.Learner;
import models.predictor.Predictor;
import models.transformer.Transformer;

import com.google.gson.JsonObject;

import util.Util;

/**
 * Abstract superclass of {@link PSIRequest} and {@link PSIResponse}, providing
 * whatever common code can actually be shared.
 * 
 * @author jmontgomery
 */
public abstract class PSIMessage {
	private static final String PSI_TYPE = "psiType";
	
	/**
	 * Since eschewing original approach in which message type names could
	 * mirror Java inheritance hierarchies, must manually map between
	 * {@code psiType} an the concrete Java class.
	 */
	protected static final Map<String,Class<? extends PSIMessage>> psiTypeToPOJO;
	protected static final Map<Class<? extends PSIMessage>,String> pojoToPSIType;
	static {
		Map<String,Class<? extends PSIMessage>> toPOJO = new HashMap<>();
		toPOJO.put("service",				PSI.Service.class);
		toPOJO.put("resource-list",			PSI.ResourceList.class);
		toPOJO.put("transformer",			Transformer.Description.class);
		toPOJO.put("relation",				Relation.Description.class);
		toPOJO.put("attribute",				Attribute.Description.class);
		toPOJO.put("learner",				Learner.Description.class);
		toPOJO.put("training-status",		Predictor.Status.class);
		toPOJO.put("attribute-definition",	Attribute.Create.class);
		toPOJO.put("task",					Learner.Process.class);
		toPOJO.put("composition",			Transformer.Join.class);
		toPOJO.put("value",					Transformer.Value.class);
		psiTypeToPOJO = Collections.unmodifiableMap(toPOJO);
		Map<Class<? extends PSIMessage>,String> toPSIType = new HashMap<>();
		for (String psiType : psiTypeToPOJO.keySet())
			toPSIType.put(psiTypeToPOJO.get(psiType), psiType);
		pojoToPSIType = Collections.unmodifiableMap(toPSIType);
	}
	
	/**
	 * If basic validation of the message fails this will be set to a non-null
	 * string holding details to report back to the client along with the 400
	 * status.
	 */
	private transient String badMessage;
	/** The message 'type' indicator, common to all PSI messages. */
	public String psiType;
	
	public PSIMessage() {
		psiType = pojoToPSIType.get(getClass());
	}
	
	public String toString() { return Util.GSON.toJsonTree(this).toString(); }
	
	/**
	 * Appends the given bad message details to the {@link #badMessage} field;
	 * should be used inside {@link #isValid()}. It is recommended to make each
	 * message a sentence ending in a full stop as each additional message is
	 * appended after a single space character. 
	 */
	protected void addBadMessage(String message) {
		badMessage = badMessage == null ? message : badMessage + " " + message;
	}
	
	/**
	 * Convenience validation method for subclasses that checks that all the
	 * field values given as arguments are not {@code null}; returns {@code
	 * true} iff all arguments are not {@code null}, {@code false} otherwise.
	 * If any are {@code null} then adds a message to the error details but
	 * cannot indicate <em>which</em> field was left blank in the message (a
	 * trade-off between simpler code validation code and informativeness of
	 * message).
	 */
	protected boolean validateAllNonNull(Object... fields) {
		for (Object value : fields)
			if (value == null) {
				addBadMessage("Not all required properties were included.");
				return false;
			}
		return true;	
	}
	
	/**
	 * Returns {@code true} iff the message appears to be 'valid', in that
	 * {@link #psiType} == {@link #expectedType} and required fields are
	 * non-null, etc. The top-most implementation only checks that the type
	 * field is valid. If the method returns {@code false} then details of the
	 * error will available from {@link #getValidationErrorMessage()}.
	 * <em>Could, perhaps should, be replaced by validation using the psi-api
	 * JSON schema, but this is probably a bit quicker for the time-being.</em>
	 */
	public boolean isValid() {
		Class<? extends PSIMessage> pojoForType = psiTypeToPOJO.get(psiType);
		if (pojoForType != null) {
			if (getClass().equals( pojoForType ))
				return true;
			else
				addBadMessage("PSI message type '" + psiType + "' + doesn't match expected '" + pojoToPSIType.get(getClass()) + "'.");
		}
		addBadMessage("Unknown psiType '" + psiType + "'");
		return false;
	}
	
	public String getValidationErrorMessage() { return badMessage; }

	
	/**
	 * Maps the given JSON object to a {@code PSIMessage} object, if possible.
	 */
	@SuppressWarnings("unchecked") //the only possible Java objects that can be created extend PSIMessage
	public static <T extends PSIMessage> T jsonToMessage(JsonObject json) {
		if (!json.has(PSI_TYPE))
			throw new IllegalArgumentException("No psiType found");
		Class<? extends PSIMessage> pojoClass = psiTypeToPOJO.get(json.get(PSI_TYPE).getAsString());
		if (pojoClass == null)
			throw new RuntimeException("Given psiType of '" + json.get(PSI_TYPE).getAsString() + "' does not match any known message type");
		return (T) Util.GSON.fromJson(json, pojoClass);
	}
	
}