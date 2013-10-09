package models.learner;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.Lob;
import javax.persistence.OneToMany;
import javax.persistence.Transient;

import models.NamedModel;
import models.PSIMessage;
import models.PSIResource;

import com.google.gson.JsonObject;

import util.Util;

import play.Logger;
import util.Trainer;

/**
 * Problems with the current implementation (well, some of them): it assumes
 * that all learners, including derived ones, will reside within the same
 * system, hence we can use the one model to represent both an original
 * learner or one derived from it. This makes any derived learners independent,
 * so they can be used to instantiate an actual learner from the underlying
 * toolkit, but requires that they be based on the same underlying toolkit.
 * 		
 * @author jmontgomery
 *
 */
@Entity
@Inheritance(strategy=InheritanceType.SINGLE_TABLE)
public abstract class Learner extends NamedModel {
	private static final long serialVersionUID = 1L;
	
	/** Short, human-readable description of learner. */
	public String description;

	/**
	 * Is the Learner deletable (i.e., produced by currying an existing
	 * learner)? This is not yet supported but is planned for a future revision
	 * to the PSI specification.
	 */
	private boolean isDeletable;
	
	/**
	 * Schema specifying format of the "resources" property in this learner's
	 * task schema.
	 */
	@Lob //since could be very large
	protected String resourcesSchema;
	
	/** Learner's parameters; mapped as property */
	@OneToMany
	private List<Parameter> parameters;
	
	/** Does this learner generate updatable predictors? */
	private boolean makesUpdatablePredictors;
	
	/**
	 * Fully-qualified name of the class or other language construct in the
	 * underlying toolkit that implements the learning algorithm.
	 */
	public String learnerImplementation;

	/** Provides faster access to and checking for specific parameters. */
	@Transient
	protected Map<String,Parameter> parametersMap = new HashMap<String, Parameter>();
	
	@Transient
	protected JsonObject taskSchema;

	
	public Learner() { super(""); }
	
	public Learner(String name) { super(name); }
	
	/** Used internally to create one of the default learners. */
	public Learner(InternalCreate req) {
		super(req.name);
		this.description = req.description;
		this.isDeletable = false;
		this.learnerImplementation = req.implementation;
		this.makesUpdatablePredictors = implementationMakesUpdatablePredictors(req);
		this.resourcesSchema = Util.GSON.toJson(req.resources);
		Logger.trace("In Learner.<init>: name = '%s', description = '%s', implementation = '%s', resourcesSchema = '%s'", name, description, learnerImplementation, resourcesSchema);
		
		Map<String,Parameter.Definition> paramDefs = req.getParameters(); 
		if (paramDefs != null) {
			List<Parameter> newParams = new Vector<Parameter>( paramDefs.size() );
			for (Map.Entry<String,Parameter.Definition> paramDef : paramDefs.entrySet()) {
				newParams.add( createParameter( paramDef.getKey(), completeDefinition( paramDef.getValue() ) ) );
			}
			setParameters(newParams);
		}
	}
	
	/**
	 * Modifies the given definition with concrete values for defaults that are
	 * not specified; returns the definition.
	 */
	protected Parameter.Definition completeDefinition(Parameter.Definition def) {
		if (def.defaultValue == null && def.type == null)
			def.defaultValue = false;
		if (def.type == null)
			def.type = "$boolean";
		if (name.startsWith("/") || name.startsWith("?")) {
			def.required = name.startsWith("/");
			name = name.substring(1);
		}
		return def;
	}

	/** Subclasses of Learner can modify this to create the appropriate Parameter subclass. */
	protected Parameter createParameter(String name, Parameter.Definition def) {
		Logger.trace("In createParameter(%s): decription = '%s', type = '%s', defaultValue (of type %s) = %s, required = %s, toolkitName = '%s', constraints = '%s'",
				name, def.description, def.type, def.defaultValue != null ? def.defaultValue.getClass().getSimpleName() : "?",
						def.defaultValue, def.required, def.toolkitName, def.constraints);
		completeDefinition(def);
		//Yes, this converts the map of constraints *back* into its JSON representation, but overall this is easier... I think
		return new Parameter(name, def.toolkitName, def.type, def.constraints == null ? null : def.constraints.toString(), def.defaultValue, def.description, def.required);
	}
	
	/**
	 * Subclasses should implement this method to report if the current
	 * {@link #learnerImplementation} corresponds to a learning algorithm that
	 * creates updatable predictors.
	 */
	protected abstract boolean implementationMakesUpdatablePredictors(InternalCreate createReq);

	/** Used internally to set parameters in a new Learner. */
	@Access(value=AccessType.PROPERTY)
	public void setParameters(List<Parameter> parameters) {
		this.parameters = parameters;
		parametersMap.clear();
		for (Parameter param : this.parameters)
			parametersMap.put(param.name, param);
	}

	@OneToMany(cascade=CascadeType.ALL)
	@Access(value=AccessType.PROPERTY)
	public List<Parameter> getParameters() { return parameters; }

	public Parameter getParameter(String name) {
		return parametersMap.get(name);
	}

	public boolean isDeletable() { return isDeletable; }

	public boolean makesUpdateablePredictors() { return makesUpdatablePredictors; }

	/**
	 * Constructs and returns the task schema for this learner.
	 */
	public synchronized JsonObject getTaskSchema() {
		if (taskSchema == null) {
			taskSchema = new JsonObject();
			taskSchema.add("/resources", Util.parseJSON( resourcesSchema ) );
			for (Parameter param : parameters) {
				taskSchema.add( param.getTaskSchemaKey(), param.getTaskSchemaValue() );
			}
		}
		return taskSchema;
	}

	/** 
	 * Returns the toolkit-appropriate {@link Trainer} for this Learner.
	 */
	public abstract Trainer getTrainer();

	//--Requests---------------------------------------------------------------

	/**
	 * An internal request message corresponding to the structure in the
	 * learner definition files; used to create the concrete Learners in
	 * the system.
	 */
	public static class InternalCreate extends PSIMessage {
		/** Name for this learner. */
		public String name;
		/** Human-readable description of the learner. */
		public String description;
		/**
		 * Fully-qualified name of the class or other language construct
		 * in the underlying toolkit that implements the learning algorithm.
		 */
		public String implementation;
		/**
		 * Fully-qualified Java class name for the subclass of Learner that
		 * can communicate with the required toolkit. Currently supported
		 * are {@link WekaLearner} and {@link SKLearnLearner}.
		 */
		public String learnerModelClass;
		/**
		 * Allows this 'create' request to be used with toolkits that
		 * require options outside what it currently allows to be
		 * specified.
		 */
		public Map<String,Object> toolkitOptions;
		/** PSI Schema for resources part of task schema. */
		public JsonObject resources;
		/** Learner parameters by name. */
		public Map<String,Parameter.Definition> parameters;

		public boolean isValid() {
			return /* has no psiType, so just */validateAllNonNull(name, implementation, learnerModelClass, resources);
		}

		/**
		 * Subclasses that provide alternative implementations of
		 * Parameter.Definition (which will necessarily be under a
		 * different name to 'parameters' [otherwise parameters will not be
		 * masked correctly]) can return the correct map here.
		 */
		public Map<String,Parameter.Definition> getParameters() { return parameters; }

		/**
		 * Has to have this unusual name (obtain instead of get) because Play
		 * attempts to be helpful by replacing any attempt to read the String-
		 * valued public property with a call to this method.
		 */
		public Class<?> obtainLearnerModelClass() throws ClassNotFoundException {
			return Class.forName(learnerModelClass);
		}
	}

	public static class Process extends PSIMessage {
		public JsonObject task;

		public boolean isValid() { return super.isValid() && validateAllNonNull(task); }
	}


	/**
	 * Performs the binding of JSON to the appropriate POJO (which will probably extend
	 * {@link Request.InternalCreate}).
	 */
	public static Learner.InternalCreate bindLearnerCreationRequest(String json) {
		return Util.GSON.fromJson(json, Learner.InternalCreate.class);
	}

	//--Responses--------------------------------------------------------------

	public static class Description extends PSIResource {
		/** Short, human-readable description of learner. */
		public String description;
		/** Schema specifying format of tasks this learner can process. */
		public JsonObject taskSchema;
		/**
		 * Provenance of the learner if it was derived from another.
		 * This is not yet part of the spec, but is planned for a future
		 * revision.
		 */
		public JsonObject provenance;

		public Description() { }

		public Description(String uri, Learner learner) {
			this.uri = uri;
			this.description = learner.description;
			this.taskSchema = learner.getTaskSchema();
		}


	}
	
}
