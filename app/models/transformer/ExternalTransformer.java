/**
 * 
 */
package models.transformer;

import com.google.gson.JsonElement;

import util.ExternalResourceException;
import util.HttpUtil;
import util.Util;

/**
 * Wrapper for an external transformer that can interact with it to obtain
 * its metadata or apply it to a value.
 * <p>
 * A possible future improvement could be to override the
 * {@link #apply(java.util.Iterator)} operation to issue a number of external
 * requests simultaneously. But this would add substantial complication as the
 * responses would need to be ordered correctly. 
 *
 */
public class ExternalTransformer extends Transformer {
	private static final long serialVersionUID = 1L;
	
	/** URI of the external transformer. */
	private String uri;
	/** Cached JSON representation of provenance. */
	public JsonElement provenanceInJSON;

	public ExternalTransformer(String uri, boolean fetchProperties) throws ExternalResourceException {
		this.uri = uri;
		if (fetchProperties)
			fetchProperties();
	}
	
	public void fetchProperties() throws ExternalResourceException {
		Description r = HttpUtil.getPSIResponse(uri);
		this.acceptsInJSON = r.accepts;
		this.accepts = r.accepts.toString();
		this.emitsInJSON = r.emits;
		this.emits = r.emits.toString();
		this.description = r.description;
		this.provenanceInJSON = r.provenance;
		this.provenance = r.provenance.toString();
	}
	
	@Override
	public JsonElement apply(JsonElement value) {
		try {
			return ((Value) HttpUtil.getPSIResponse(uri, Util.makeMap(VALUE_ARG, value))).value;
		} catch (ExternalResourceException e) {
			throw new RuntimeException(e); //Downgrade exception; will result in 500 status elsewhere, but that's the most appropriate since it's not the client's fault
		}
	}
}
