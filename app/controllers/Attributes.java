package controllers;

import play.*;
import play.mvc.Http;
import util.SubattributeRelationMismatchException;

import java.util.*;
import java.util.regex.Pattern;

import javax.persistence.PersistenceException;

import com.google.gson.JsonObject;

import models.PSIResponse;
import models.attribute.*;
import models.data.*;
import models.predictor.ArrayLengthMismatchException;
import models.transformer.BadValueException;
import models.transformer.EncodedTransformerChain;
import models.transformer.Function;
import models.transformer.TransformationEncodingException;
import models.transformer.Transformer;
import util.Util;

public class Attributes extends CORSController {
	private static final String INSTANCE_ARG = "instance";
	private static final Pattern VALID_INSTANCE = Pattern.compile("^(?:all|\\d+)$");
	
	public static void create(String id, JsonObject body) {
		try {
			Attribute.Create createReq = parseAndCheckRequestBody(body, Attribute.Create.class);
			//Not essential, but select something more meaningful than a UUID; the terminal digit ensures that the ultimate name will be numbered (sequentially among available identifiers)
			createReq.name = createReq.attribute.isJsonArray() ? "array1" : "object1";
			Attribute attr = Attribute.create(id, createReq.name, createReq.description, createReq.attribute, Attribute.Persistence.USER);

			renderCreated(Attributes.getReverseRoute(attr), response);
		} catch (IllegalArgumentException | SubattributeRelationMismatchException e) { //yes, should not catch RuntimeExceptions, but this is currently thrown due to client mistakes
			badRequest(e.getMessage());
		}
	}
	
	public static void describeOrApply(String relID, String id) {
		Logger.trace("Attributes.describeOrApply(relID = %s, id = %s)", relID, id);
		try {
			Attribute attr = find(relID, id);
			EncodedTransformerChain transformation = Transformers.parseTransformation(params);
			Query query = new Query(params.data);
			if (params._contains(INSTANCE_ARG))
				renderJSON( apply(attr, transformation, query).toJsonWithNullableValue() );
			else
				renderJSON( new Attribute.Description(attr, id, transformation, query) );
		} catch (TransformationEncodingException | QueryException e) {
			badRequest(e.getMessage());
		}
	}
	
	
	private static Transformer.Value apply(Attribute attr, EncodedTransformerChain transformation, Query query) {
		try {
			String instance = params.get(INSTANCE_ARG).toLowerCase(); //be kind to clients not following spec exactly
			if (! VALID_INSTANCE.matcher(instance).matches())
				badRequest("Instance value " + instance + " is not valid. Must be a non-negative integer or 'all'");
			Integer i = instance.equals("all") ? null : Integer.parseInt(instance);
			Function f = transformation == null ? attr : transformation.constructProcessingPipeline(attr);
			if (i == null)
				return new Transformer.Value( f.apply( attr.getRelation().iterator(query) ) );
			return new Transformer.Value( f.apply( attr.getRelation().readInstance(query, i) ) );
		} catch (TransformationEncodingException tee) {
			error("Unable to create processing pipeline for transformed attribute. Details: " + tee.getMessage());
		} catch (NoSuchInstanceException | BadValueException e) {
			badRequest(e.getMessage());
		}
		return null; //unreachable as error() and badRequest() throw exceptions
	}
	
	public static void join(String relID, String id, JsonObject body) {
		try {
			final Http.Response origResponse = response; //Just in case Play talks to itself and thus changes this global(!) response variable.
			Logger.trace("Attributes.join(relID = %s, id = %s)", relID, id);
			Attribute attr = find(relID, id);
			Transformer.Join joinRequest = parseAndCheckRequestBody(body, Transformer.Join.class);
			EncodedTransformerChain chain = EncodedTransformerChain.create(attr, Transformers.parseTransformation(params), joinRequest.join, joinRequest.description);
			//TODO Currently stripping query from attribute (as transformed-attribute can have new queries applied), but perhaps client's _expected_ behaviour is that query is preserved
			renderCreated(getRoute(relID, id, chain.toBase64JSON(), null /*new Query(request.querystring)*/ ), origResponse);
//		} catch (QueryException qe) {
//			badRequest("Bad query arguments to attribute: " + qe.getMessage());
		} catch (TransformationEncodingException tee) {
			badRequest(tee.getMessage());
		}
	}
	
	public static void delete(String relID, String id) {
		if (Transformers.getTransformation(params) != null) {
			badRequest("A transformed attribute cannot be deleted directly. " +
					"Its URI will be valid as long as the attribute and transformers with which it is joined exist. " +
					"Perhaps you meant to request DELETE " + getRoute(relID, id, null, null));
		}

		if (id.indexOf('/') >= 2) //is a 'true' sub-attribute, in that it was created along with another attribute that needs it
			forbidden("Sub-attributes cannot be deleted. To delete a sub-attribute its parent attribute must be deleted.");
		Attribute target = find(relID, id);
		if (target.isDeletable()) {
			Collection<Attribute> toDelete = new Vector<>();
			target.prepareForDeletion(toDelete);
			try {
				for (Attribute attr : toDelete)
					attr.delete();
				Logger.info("Deleted %d attribute(s), starting with '%s'", toDelete.size(), id);
				renderDeleted();
			} catch (PersistenceException pe) { //simpler to catch this than to perform the check
				Throwable cause = pe;
				while (cause.getCause() != null)
					cause = cause.getCause();
				Logger.warn(cause, "Couldn't delete attribute '%s', probably because it or one of its sub-attributes is referenced by another", id);
				if (cause.getMessage().startsWith("Referential integrity constraint violation"))
					forbidden("Attribute (or one of its sub-attributes) is used by another structured attribute, which must be deleted first");
				error("Unexpected error when deleting attribute " + cause);
			}
		} else {
			forbidden("The referenced attribute is permanently attached to a relation and cannot be deleted");
		}
	}

	public static Attribute find(final String relID, final String id) {
		Attribute attribute = Attribute.findById(relID, id);
		if (attribute == null) {
			Relation r = Relation.findById(relID); 
			if (r == null)
				notFound("You appear to be looking for an attribute of a relation, but neither can be found. The list of available relations is at: " + Util.getReverseRoute("Data.listAll"));
			notFound("You appear to be looking for an attribute of " + Data.getReverseRoute(r, null) + " but the requested attribute does not exist. GET the relation at that URI to discover its attributes.");
		}
		return attribute;
	}
	
	//--Reverse routing--------------------------------------------------------
	
	/** Returns the URI (as a String) that can be used to GET the given unqueried Attribute. */
	public static String getReverseRoute(Attribute attr) { return getReverseRoute(attr, null, null); }
	
	/** Returns the URI (as a String) that can be used to GET the given Attribute (with the given query arguments and transformation (both can be null)). */
	public static String getReverseRoute(Attribute attr, String transformation, Query query) { return getRoute(attr.getRelation().name, attr.nameNoRelation, transformation, query); }
	
	private static String getRoute(String relationID, String id, String transformation, Query query) {
		Map<String, Object> args = Util.makeMap("relID", relationID, "id", id);
		if (transformation != null)
			args.put(EncodedTransformerChain.URI_KEY, transformation);
		if (query != null)
			query.makeMap(args);
		return Util.getReverseRoute("Attributes.describeOrApply", args).replaceAll("%2F", "/");
	}
	
}