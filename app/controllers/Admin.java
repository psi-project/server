package controllers;

import java.security.NoSuchAlgorithmException;
import java.util.*;

import play.mvc.With;

import util.Util;

import models.*;

/**
 * All admin tasks that should not be readily accessible to the general public.
 */
@With(Secure.class)
public class Admin extends CORSController {
	
	public static void index() {
		render();
	}	
   
    public static void cleanStart() {
    	renderText(Predictors._deleteAll() + "\n\n" + Predictors._deleteFailed() + "\n\n" + Learners._deleteAll() + "\n\n" + Data._deleteAll());
    }
    
	public static void deleteAllRelations() {
		renderText( Data._deleteAll() );
	}
	
	public static void initialiseRelations() {
		renderText( Data._initialiseRelations() );
	}
	
	public static void deleteAllLearners() {
		renderText( Learners._deleteAll() );
	}

	public static void initialiseLearners() {
		renderText( Learners._initialiseLearners() );
	}
	
	public static void deleteAllPredictors() {
		renderText( Predictors._deleteAll() );
	}
	
	public static void deleteFailedJobs() {
		renderText( Predictors._deleteFailed() );
	}
	
	public static void chpass(String phrase, String salt) {
		if (phrase == null)
			render();
		try {
			if (phrase.isEmpty() || salt.isEmpty())
				badRequest("Pass phrase and salt must be non-empty");
			renderText( Security.saltedHashedBase64Encoded(phrase, salt) );
		} catch (NoSuchAlgorithmException e) {
			error(e);
		}
	}

}
