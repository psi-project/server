/**
 * 
 */
package models;

import java.util.ArrayList;
import java.util.List;

import util.Util;

/**
 * A pseudo-model for the PSI service as a whole. Currently requires the routes
 * file and information here to be kept in sync; perhaps this can be improved.
 * 
 * @author jmontgomery
 *
 */
public final class PSI {
	//Warning: These must be kept in sync with routes file
	public static final String RELATION_BASE	= "data";
	public static final String SCHEMA_BASE		= "schema";
	public static final String LEARNER_BASE		= "learn";
	public static final String PREDICTOR_BASE	= "infer";
	public static final String TRANSFORMER_BASE	= "transform";

	/** Path from predictor's URI to its update interface. */
	public static final String UPDATE_PATH = "/update";

	private PSI() { }

	public static class Service extends PSIResource {
		/** URI for collection of relations. */
		public String relations;
		/** URI for collection of pre-defined schema. */
		public String schema;
		/** URI for collection of learners. */
		public String learners;
		/** URI for collection of predictors. */
		public String predictors;
		/** URI for collection of transformers. */
		public String transformers;

		public Service(String hostURI) {
			final String hostSlash = hostURI.endsWith("/") ? hostURI : hostURI + "/";
			hostURI = hostSlash.substring(0, hostSlash.length() - 1);
			uri = hostURI;
			relations = hostSlash + RELATION_BASE;
			schema = hostSlash + SCHEMA_BASE;
			learners = hostSlash + LEARNER_BASE;
			predictors = hostSlash + PREDICTOR_BASE;
			transformers = hostSlash + TRANSFORMER_BASE;
			//Illustrate a simple use of the relatedServices field
			this.relatedServices = new ArrayList<>();
			this.relatedServices.add( Util.GSON.toJsonTree( new LinkDescriptionObject("help", "http://psi.cecs.anu.edu.au/spec") ) );
		}

	}

	public static class ResourceList extends PSIResource {
		/** URIs of resources in this collection. */
		public List<String> resources;

		/**
		 * Generate a new list of resource URIs for the given names, rooted
		 * rooted at {@code anchorPoint}.
		 * @param anchorPoint should be a combination of the host's URI and
		 * one of the static constants defined in {@link PSI}, such as
		 * {@link PSI#LEARNER_BASE}, although this requirement is not
		 * enforced.
		 */
		public ResourceList(String uri, String anchorPoint, List<String> names) {
			this.uri = uri;
			if (! anchorPoint.endsWith("/"))
				anchorPoint += "/";
			resources = new ArrayList<String>( names.size() );
			for (String name : names) 
				resources.add( anchorPoint + name );
		}
	}

}
