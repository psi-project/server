package controllers;

import play.*;

import play.vfs.VirtualFile;
import util.SubattributeRelationMismatchException;

import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.filefilter.SuffixFileFilter;

import models.attribute.ArrayAttribute;
import models.attribute.Attribute;
import models.data.*;
import util.Util;

public class Data extends CORSController {
	private static Pattern RELATION_ID_PATTERN = Pattern.compile("/(?:(?:data)|(?:view))/([^/?]+)");
	
	public static void listAll() { listAll(Relation.class); }

	public static void describe(String id) {
		try {
			renderJSON( new Relation.Description(find(id), new Query(params.data), Util.requestPath()) );
		} catch (QueryException qe) {
			badRequest(qe.getMessage());
		}
	}
	
	public static String getReverseRoute(Relation relation, Query query) {
		return Util.getReverseRoute("Data.describe", (query == null ? Query.getEmptyQuery() : query).makeMap("id", relation.name));
	}

	protected static Relation find(final String id) {
		Relation relation = Relation.findById(id);
		if (relation == null)
			notFoundSeeList("relation", "Data.listAll");
		return relation;
	}
	
	public static Relation findByURI(String relationURI) {
		Matcher idMatcher = RELATION_ID_PATTERN.matcher(relationURI);
		if (idMatcher.find())
			return find( idMatcher.group(1) );
		return null;
	}
	
	//--Admin tasks; no route leads directly to these--------------------------
	
	public static String _deleteAll() {
		//Cascading delete cannot handle potentially complex object graphs that have been persisted, so manually unlink models first
		for (Relation relation : Relation.all().<Relation>fetch())
			relation.prepareForDeletion();
		return String.format("%s\nRemoved %d relation(s) and %d attribute(s)", new Date(), Relation.deleteAll(), Attribute.deleteAll());
	}
	
	public static String _initialiseRelations() {
		long relationCountBefore = Relation.count();
		long attrCountBefore = Attribute.count();

		File dataDir = VirtualFile.fromRelativePath("/private/datasets").getRealFile();
		File[] relationFiles = dataDir.listFiles( (FilenameFilter) new SuffixFileFilter(".js") );
		for (File relationFile : relationFiles) {
			Logger.trace("Processing relation file: " + relationFile);
			initialiseRelation(relationFile);
		}
		long addedRelations = Relation.count() - relationCountBefore;
		long addedAttributes = Attribute.count() - attrCountBefore;
		long ignored = relationFiles.length - addedRelations;
		
		return String.format("%s\nSuccessfully created and persisted %d relation(s) and %d distinct attribute(s).%s", new Date(), addedRelations, addedAttributes,
				ignored == 0? "" : String.format("\nIgnored %d definitions corresponding to existing relation models.", ignored));
	}
	
	private static void initialiseRelation(File relationFile) {
		try {
			//New relation will also create any locally defined attributes
			Logger.info("Loading relation from %s", relationFile);
			Relation.Create req = loadRelationCreationRequest(relationFile);
			failOnInvalidMessage(req); //even though used internally, still check it
			if ( Relation.findById(req.name) == null)
				Relation.create(req);
		} catch (IOException ioe) {
			error(ioe);
		} catch (SubattributeRelationMismatchException srme) {
			badRequest(srme.getMessage());
		}
	}
	
	private static Relation.Create loadRelationCreationRequest(File relationFile) throws IOException {
		//Just load the entire file all at once
		FileReader in = null;
		try {
			in = new FileReader( relationFile );
			char[] buffer = new char[ (int) relationFile.length() ];
			in.read(buffer);
			
			return Util.GSON.fromJson(new String(buffer), Relation.Create.class);
		} finally {
			if (in != null) in.close();
		}
	}
	
}
