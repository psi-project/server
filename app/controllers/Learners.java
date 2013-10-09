package controllers;

import play.*;

import play.db.jpa.JPA;
import play.jobs.Job;
import play.libs.F.Promise;
import play.mvc.Http;
import play.vfs.VirtualFile;
import util.BadTrainingDataException;
import util.ConfKeys;
import util.ExternalResourceException;
import util.Schema;
import util.TrainingException;

import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.persistence.EntityTransaction;

import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.apache.commons.lang.StringUtils;

import com.google.gson.JsonObject;

import models.learner.Learner;
import models.learner.Task;
import models.predictor.Predictor;
import util.Util;

public class Learners extends CORSController {
	/**
	 * After this many seconds processing return 202 Accepted to the client.
	 */
	public static final int CREATE_STATUS_RESOURCE_AFTER_N_SECONDS = Integer.parseInt( Play.configuration.getProperty( ConfKeys.TRAINING_TIMEOUT ) );
	
	public static void listAll() { listAll(Learner.class); }

	public static void describe(String id) {
		renderJSON( new Learner.Description(Util.requestPathNoQuery(), find(id)) );
	}

	private static Learner find(final String id) {
		Learner learner = Learner.findById(id);
		if (learner == null)
			notFoundSeeList("learner","Learners.listAll");
		return learner;
	}

	/**
	 * Trains the predictor based on the given processing request.
	 */
	public static void trainPredictor(String id, JsonObject body) {
		Learner learner = find(id);
		Learner.Process processReq = parseAndCheckRequestBody(body, Learner.Process.class);
		final String learnerURI = Util.requestPathNoQuery();
		final String baseURI = request.getBase();
		Http.Response origResponse = response;
		try {
			//Validate task against learner's task schema as soon as possible
			validateTask(learner, processReq.task);

			Logger.trace("Processing request task field looks like: %s", processReq.task);
			doTraining(learner, new Task( processReq.task, learner.getTaskSchema() ), baseURI, learnerURI, origResponse);
		} catch (ExternalResourceException ere) {
			//Note cannot really distinguish between poor information from the client and a problem at the externally referenced service
			error("Encountered an unexpected error while processing learning task. " + ere.getMessage());
		}
	}
	
	private static void validateTask(Learner learner, JsonObject taskInJSON) throws ExternalResourceException {
		List<String> validationMessages = Schema.validateWithResolutionAgainstPSISchema( learner.getTaskSchema(), taskInJSON );
		if (! validationMessages.isEmpty()) {
			badRequest("Validation errors:\n" + StringUtils.join(validationMessages, "\n"));
		}
	}
	
	/**
	 * Masking static {@code response} object is deliberate because previous
	 * internal requests will have replaced it with a fresh one, so setting any
	 * headers in it would be utterly futile. Way to go, Play!
	 */
	protected static void doTraining(Learner learner, Task task, final String baseURI, final String learnerURI, Http.Response response) {
		try {
			TrainingJob trainingJob = new TrainingJob(learner, task, learnerURI);
			Promise<Predictor> trainingPromise = trainingJob.now();
			Predictor predictor = null;
			try {
				predictor = trainingPromise.get(CREATE_STATUS_RESOURCE_AFTER_N_SECONDS, TimeUnit.SECONDS);
				Logger.trace("trainingPromise returned Predictor (may be null if timed out): %s", predictor);
				if (predictor != null) {
					if (predictor.isTrainingFailed())
						dealWithFailedTraining(predictor);
					renderCreated( Transformers.getReverseRoute(Transformers.Kind.PREDICTOR, predictor), response);
				}
			} catch (TimeoutException te) {
				Logger.info("Training timed out (as per the docs), so will return 202 Accepted");
			} catch (InterruptedException | ExecutionException e) {
				error(e);
			}
			predictor = trainingJob.getEvenThoughNotFinished();
			if (predictor.isTrainingFailed()) { //an error condition prevented training from completing; details will be in predictor.trainingStatus
				dealWithFailedTraining(predictor);
			} else {
				Logger.info("Training process took 'too long'; reporting back 202 Accepted");
				predictor.setTrainingNowOffline(true);
				renderSimpleResponse(Http.StatusCode.ACCEPTED, LOCATION, Transformers.getReverseRoute(Transformers.Kind.PREDICTOR, predictor), response);
			}
		} catch (TrainingException te) { //only thrown (and not otherwise caught) when creating the TrainingJob
			error("There was a problem while initialising training. Details: " + te.getMessage());
		}
	}
	
	/**
	 * Deletes the predictor's model and reports either a bad request (if the
	 * cause of the failure was a {@link BadTrainingDataException}) or an
	 * internal server error.
	 */
	private static void dealWithFailedTraining(Predictor predictor) {
		removeFailedPredictor(predictor.name);
		if (predictor.trainingErrorCause instanceof BadTrainingDataException)
			badRequest(predictor.trainingErrorCause.getMessage() );
		error( predictor.trainingStatus );
	}
	
	private static void removeFailedPredictor(final String id) {
		EntityTransaction transaction = JPA.em().getTransaction();
		if (! transaction.isActive())
			transaction.begin();
		Predictor.<Predictor>findById(id).delete();
	}
	
	/**
	 * An asynchronous job to train predictors. Has a {@link Predictor} from
	 * the outset, which will hold training status information until training
	 * is complete. If the job takes 'too long' then the controller can send
	 * back 202 (Accepted) instead of 201 (Created).
	 */
	private static class TrainingJob extends Job<Predictor> {
		private final Learner learner;
		private final Task task;
		private Predictor predictor;
		
		/**
		 * Prepares a new training job and immediately attempts to create an
		 * untrained predictor. If successful the model will be persisted to
		 * the database. If creating the new predictor fails (if, for instance,
		 * a source relation cannot be reached) then a
		 * {@link TrainingException} will be thrown and no predictor model will
		 * be persisted. The time to create the untrained predictor is not
		 * counted towards the total time before sending back 202 Accepted.
		 * Note that this <em>might</em> be a problem if the client has a
		 * similar timeout to this service contacting another holding a remote
		 * relation.
		 */
		public TrainingJob(Learner learner, Task task, final String learnerURI) throws TrainingException {
			this.learner = learner;
			this.task = task;
			predictor = learner.getTrainer().newUntrainedPredictor(learner, task);
			//Apparently need to commit the current transaction so that the model can be updated later by other parts of the training process.
			//If an error occurred during its creation then normal automatic rollback should be performed by Play
			JPA.em().getTransaction().commit();
		}
		
		/** Returns the predictor, even if the job hasn't finished. */
		public synchronized Predictor getEvenThoughNotFinished() { return predictor; }
		
		public void doJob() throws Exception {
			try {
				learner.getTrainer().trainPredictor(predictor, learner, task);
			} catch (TrainingException | BadTrainingDataException e) {
				predictor.trainingFailed("Training failed: " + e.getMessage(), e);
			} catch (Exception e) {
				predictor.trainingFailed("Training failed due to an unexpected error: " + (e instanceof RuntimeException ? e.getMessage() : e), e);
				Logger.error(e, "Unexpected error during training of %s.", predictor.name);
			}
		}

		public Predictor doJobWithResult() throws Exception {
			doJob();
			return getEvenThoughNotFinished();
		}

	}
	
	public static String getReverseRoute(String learnerName) {
		return Util.getReverseRoute("Learners.describe", "id", learnerName);
	}


	//--Admin functions--------------------------------------------------------
	
	public static String _deleteAll() {
		long count = Learner.count();
		for (Learner learner: Learner.all().<Learner>fetch())
			learner.delete();
		return String.format("%s\nDeleted %d learners.", new Date(), count);
	}
	
	public static String _initialiseLearners() {
		long learnerCountBefore = Learner.count();
		Map<Class<?>, Constructor<?>> learnerModelConstructors = new HashMap<>();

		File learnersDir = VirtualFile.fromRelativePath("/private/learners").getRealFile();
		File[] learnerFiles = learnersDir.listFiles( (FilenameFilter) new SuffixFileFilter(".js") );
		for (File learnerFile : learnerFiles) {
			initialiseLearner(learnerFile, learnerModelConstructors);
		}
		final long added = Learner.count() - learnerCountBefore;
		final long ignored = learnerFiles.length - added;
		return String.format("%s\nSuccessfully created and persisted %d learner(s).%s", new Date(), added,
				ignored == 0? "" : String.format("\nIgnored %d definitions corresponding to existing learner models.", ignored));
	}
	
	private static void initialiseLearner(File learnerFile, Map<Class<?>, Constructor<?>> learnerModelConstructors) {
		try {
			Logger.info("Loading learner from %s", learnerFile);
			Learner.InternalCreate req = loadLearnerCreationRequest(learnerFile);
			failOnInvalidMessage(req); //even though used internally still check it is valid
			
			if ( Learner.findById(req.name) == null ) {
				Class<?> modelClass = req.obtainLearnerModelClass();
				Constructor<?> modelCons = learnerModelConstructors.get( modelClass );
				if (modelCons == null) {
					modelCons = modelClass.getConstructor(req.getClass());
					learnerModelConstructors.put(modelClass, modelCons);
				}
				Learner learner = (Learner) modelCons.newInstance(req);
				learner.save();
			}
		} catch (IOException | ClassNotFoundException | InstantiationException | IllegalAccessException |
				IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e) {
			error(e);
		}
	}

	private static Learner.InternalCreate loadLearnerCreationRequest(File learnerFile) throws IOException {
		FileReader in = null;
		try {
			//Just load the entire file all at once
			in = new FileReader( learnerFile );
			char[] buffer = new char[ (int) learnerFile.length() ];
			in.read(buffer);
			return Learner.bindLearnerCreationRequest( new String(buffer) );
		} finally {
			if (in != null) in.close();
		}
	}

}
