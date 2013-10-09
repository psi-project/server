package models.learner;

import java.lang.reflect.Constructor;

import javax.persistence.Entity;
import javax.persistence.Transient;

import util.Trainer;
import util.WekaTrainer;
import weka.classifiers.UpdateableClassifier;

/**
 * Model of a Weka {@link Learner}.
 * 		
 * @author jmontgomery
 *
 */
@Entity
public class WekaLearner extends Learner {
	private static final long serialVersionUID = 1L;

	protected static final Trainer WEKA_TRAINER = WekaTrainer.instance();
	
	/** The actual WEKA learner class. */
	@Transient
	private Class<?> learnerClass;
	
	/** The actual learner's constructor. */
	@Transient
	private Constructor<?> learnerConstructor;
	
	public WekaLearner() { this(""); }
	
	public WekaLearner(String name) { super(name); }
	
	/** Used internally to initialise a learner model. */
	public WekaLearner(InternalCreate req) {
		super(req);
		getLearnerConstructor(); //forces checking of implementation class name
	}
	
	public Trainer getTrainer() { return WEKA_TRAINER; }

	protected Parameter createParameter(String name, Parameter.Definition def) {
		//Yes, this converts the map of constraints *back* into its JSON representation, but overall this is easier... I think
		return new WekaParameter(name, def.toolkitName, def.type, def.constraints == null ? null : def.constraints.toString(), def.defaultValue, def.description, def.required);
	}

	protected boolean implementationMakesUpdatablePredictors(InternalCreate createReq) {
		return UpdateableClassifier.class.isAssignableFrom( getLearnerClass() );
	}

	//--Accessors--------------------------------------------------------------
	
	public synchronized Class<?> getLearnerClass() {
		if (learnerClass == null) {
			try {
				this.learnerClass = Class.forName(learnerImplementation);
			} catch (ClassNotFoundException cnfe) {
				throw new IllegalArgumentException("Class name not valid", cnfe);
			}
		}
		return learnerClass;
	}
	
	public synchronized Constructor<?> getLearnerConstructor() {
		if (learnerConstructor == null) {
			try {
				learnerConstructor = getLearnerClass().getConstructor();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
		return learnerConstructor;
	}
	
}