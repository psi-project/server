package controllers;

import play.mvc.*;
import util.ExternalResourceException;

import java.util.*;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import models.PSI;
import models.predictor.ArrayLengthMismatchException;
import models.predictor.Predictor;
import models.transformer.BadValueException;
import models.transformer.BuiltinTransformer;
import models.transformer.EncodedTransformerChain;
import models.transformer.Function;
import models.transformer.TransformationEncodingException;
import models.transformer.Transformer;
import util.Util;

/**
 * Controller with handlers that are common to the controllers for built-in
 * transformers and predictors.
 * 
 * @author jmontgomery
 *
 */
public class Transformers extends CORSController {
	/** Alias for Transformer.VALUE_ARG. */
	private static final String VALUE_ARG = Transformer.VALUE_ARG;
	
	/** The 'kind' of transformer model to present, a built-in transformer or predictor. */
	public static enum Kind {
		TRANSFORMER, PREDICTOR;
		public String getRoute() {
			return this == TRANSFORMER ? PSI.TRANSFORMER_BASE : PSI.PREDICTOR_BASE;
		}
		public static Kind fromRoute(final String route) {
			if (Kind.TRANSFORMER.getRoute().equals(route))
				return Kind.TRANSFORMER;
			else if (Kind.PREDICTOR.getRoute().equals(route))
				return Kind.PREDICTOR;
			return null;
		}
	}	

	public static void listAll(final String kind) {
		if (kind.equals(Kind.PREDICTOR.getRoute()))
			listAll(Predictor.class, kind);
		else //kinds file ensures that kind is for (built-in) transformers
			listAll(BuiltinTransformer.names, kind);
	}
	
	public static void describeOrApply(final String kind, String id) {
		try {
			Transformer t = find(kind, id);
			EncodedTransformerChain transformation = parseTransformation(params);
			if (params._contains(VALUE_ARG)) {
				checkReady(t);
				apply(kind, t, transformation, Util.parseJSON( params.get(VALUE_ARG) ) );
			} else
				describe(t, transformation);
		} catch (TransformationEncodingException e) {
			badRequest(e.getMessage());
		}
	}
	
	/**
	 * Subclasses should call this from their own {@code applyOrJoin},
	 * substituting {@code "Transformers"} or {@code "Predictors"} for
	 * {@code kind}, so that the appropriate reverse routing can be
	 * done later.
	 */
	public static void join(final String kind, String id, JsonObject body) {
		try {
			Transformer t = find(kind, id);
			checkReady(t);
			Http.Response origResponse = response;
			join(kind, t, parseTransformation(params),
					parseAndCheckRequestBody(body, Transformer.Join.class), origResponse);
		} catch (TransformationEncodingException e) {
			badRequest(e.getMessage());
		}
	}

	private static void describe(Transformer t, EncodedTransformerChain transformation) {
		renderJSON( t.getDescription(Util.requestPath(), transformation) );
	}
	
	private static void apply(final String kind, Transformer t, EncodedTransformerChain transformation,
			JsonElement value)
	{
		try {
			if (! t.isAcceptableValue(value))
				badRequest("Given value (" + value + ") is not an acceptable input to this transformer: " + t.accepts);
			Function f = transformation == null ? t : transformation.constructProcessingPipeline(t);
			renderJSON( new Transformer.Value(f.apply(value)).toJsonWithNullableValue() );
		} catch (TransformationEncodingException tee) {
			error("Unable to create processing pipeline for joined transformer. Details: " + tee.getMessage());
		} catch (ExternalResourceException ere) {
			error("There was an unexpected error when resolving the transformer's accepts schema. Details: " + ere.getMessage());
		} catch (ArrayLengthMismatchException alme) {
			badRequest(alme.getMessage());
		} catch (BadValueException e) {
			error("Unexpected error. Details: " + e.getMessage());
		}
	}
	
	private static void join(final String kind, Transformer t, EncodedTransformerChain transformation,
			Transformer.Join joinRequest, Http.Response responseToUse)
	{
		try {
			EncodedTransformerChain chain = EncodedTransformerChain.create(t, Transformers.parseTransformation(params), joinRequest.join, joinRequest.description);
			renderCreated( getReverseRoute(kind, t.name, chain.toBase64JSON()), responseToUse);
		} catch (TransformationEncodingException tee) {
			badRequest(tee.getMessage()); 
		}
	}
	
	/**
	 * Since a predictor model may exist before training has completed, this
	 * checks if the given transformer is (1) a predictor and (2) ready to
	 * receive apply and join requests. If not then a 403 Forbidden response is
	 * sent back.
	 */
	protected static void checkReady(Transformer t) {
		if ((t instanceof Predictor) && !((Predictor)t).isTrained())
			forbidden("Training of this predictor has not yet completed. Its status is available at " + getReverseRoute(Kind.PREDICTOR, t.name));
	}

	public static Transformer find(final String kind, final String id) { return find(Kind.fromRoute(kind), id); }

	public static Transformer find(Kind kind, final String id) { 
		Transformer t = kind == Kind.PREDICTOR ? Predictor.<Predictor>findById(id) : BuiltinTransformer.load(id);
		if (t == null)
			notFoundSeeList(kind == Kind.PREDICTOR ? "predictor" : "transformer", "Transformers.listAll", "kind", kind.getRoute());
		return t;
	}
	
	/** Returns the String-encoded transformer chain. */
	public static String getTransformation(Scope.Params params) { return params.get(EncodedTransformerChain.URI_KEY); }
	
	/** Returns the decoded transformer chain. */
	public static EncodedTransformerChain parseTransformation(Scope.Params params) throws TransformationEncodingException {
		return params._contains(EncodedTransformerChain.URI_KEY) ? EncodedTransformerChain.unpack( getTransformation(params) ) : null;
	}

	/**
	 * Returns the URI (as a String) that can be used to GET the given
	 * Transformer or Predictor (determined by {@code kind}).
	 */
	public static String getReverseRoute(Kind kind, Transformer t) {
		return getReverseRoute(kind, t.name);
	}

	/**
	 * Returns the URI (as a String) that can be used to GET the Transformer or
	 * Predictor (determined by {@code kind}) with given {@code id}.
	 */
	public static String getReverseRoute(Kind kind, String id) {
		return getReverseRoute(kind.getRoute(), id, null);
	}
	
	/** Returns the URI (as a String) that can be used to GET the Transformer or Predictor with given {@code id}. */
	protected static String getReverseRoute(final String kind, final String id, String transformation) {
		Map<String, Object> args = Util.makeMap("kind", kind, "id", id);
		if (transformation != null)
			args.put(EncodedTransformerChain.URI_KEY, transformation);
		return Util.getReverseRoute("Transformers.describeOrApply", args);
	}

}
