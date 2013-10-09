/**
 * 
 */
package util;

import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.filefilter.SuffixFileFilter;

import play.Logger;
import play.vfs.VirtualFile;

/**
 * A singleton collection of predefined {@link SchemaTemplate}s, which are
 * loaded from file; other parts of the system may keep some of these (without
 * template variables) in compiled, full JSON.
 * 
 * @author jmontgomery
 *
 */
public class SchemaTemplateLibrary implements Iterable<SchemaTemplate> {
	private static SchemaTemplateLibrary instance;
	
	/** The actual collection of {@code SchemaTemplate}s. */
	private Map<String,SchemaTemplate> collection;
	
	private SchemaTemplateLibrary() {
		collection = new HashMap<String, SchemaTemplate>();
		populateFromFiles();
	}
	
	public static synchronized SchemaTemplateLibrary instance() {
		if (instance == null) {
			instance = new SchemaTemplateLibrary();
		}
		return instance;
	}

	@Override
	public Iterator<SchemaTemplate> iterator() { return collection.values().iterator(); }
	
	public List<String> names() { return new ArrayList<String>( collection.keySet() ); }
	
	public List<String> sortedNames() {
		List<String> names = names();
		Collections.sort(names);
		return names;
	}
	
	public boolean contains(String name) { return collection.containsKey(name); }
	
	public SchemaTemplate get(String name) { return collection.get(name); }
	
	private void populateFromFiles() {
		collection.clear();

		final String schemaPath;
		switch (Schema.SCHEMA_VERSION) {
			case DRAFTV3 : schemaPath = "/private/schema/v3"; break;
			case DRAFTV4 : 
			default:	   schemaPath = "/private/schema/v4"; //default needed to keep Java lint happy; you know, if Schema.SCHEMA_VERSION == null [rolls eyes] 
		}
		
		File schemaDir = VirtualFile.fromRelativePath(schemaPath).getRealFile();
		File[] schemaFiles = schemaDir.listFiles( (FilenameFilter) new SuffixFileFilter(".js") );
		for (File schemaFile : schemaFiles) {
			try {
				Logger.trace("Loading schema from %s", schemaFile);
				SchemaTemplate template = loadTemplateFromFile(schemaFile);
				collection.put( template.getName(), template );
			} catch (IOException ioe) {
				throw new RuntimeException(ioe);
			}
		}
	}
	
	private SchemaTemplate loadTemplateFromFile(File schemaFile) throws IOException {
		//Just load the entire file all at once
		FileReader in = null;
		try {
			in = new FileReader( schemaFile );
			char[] buffer = new char[ (int) schemaFile.length() ];
			in.read(buffer);
			return new SchemaTemplate( FilenameUtils.getBaseName(schemaFile.getPath()), new String(buffer) );
		} finally {
			if (in != null) in.close();
		}
		
	}
}
