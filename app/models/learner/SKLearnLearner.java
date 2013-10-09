package models.learner;

import javax.persistence.Entity;

import play.Play;
import util.ConfKeys;
import util.SKLearnTrainer;
import util.Trainer;

/**
 * Model of a web-based scikit-learn {@link Learner}; the learner actually
 * resides in a web.py web service, with which this communicates, although all
 * of the learner's metadata is stored in this model.
 * 		
 * @author jmontgomery
 *
 */
@Entity
public class SKLearnLearner extends Learner {
	private static final long serialVersionUID = 1L;

	protected static String learnerRoot = Play.configuration.getProperty(ConfKeys.SKLEARN_SERVICE_ROOT) + "learner/";
	protected static final Trainer SKLEARN_TRAINER = SKLearnTrainer.instance();
	
	public SKLearnLearner() { this(""); }
	
	public SKLearnLearner(String name) { super(name); }
	
	/** Used internally to initialise a learner model. */
	public SKLearnLearner(InternalCreate req) {
		super(req);
	}
	
	public Trainer getTrainer() { return SKLEARN_TRAINER; }
	
	protected boolean implementationMakesUpdatablePredictors(InternalCreate createReq) {
		return createReq.toolkitOptions != null && createReq.toolkitOptions.containsKey("isUpdatable") && (Boolean)createReq.toolkitOptions.get("isUpdatable");
	}

	//--Accessors--------------------------------------------------------------
	
	public String getLearnerURI() {
		return learnerRoot + learnerImplementation;
	}
}