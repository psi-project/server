/**
 * 
 */
package models.data;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonPrimitive;

import models.data.NoSuchInstanceException;
import models.data.Relation;
import play.Logger;
import play.vfs.VirtualFile;
import util.Util;

/**
 * Base class for text file instance readers.
 * 
 * @author jmontgomery
 */
public abstract class TextFileDataReader extends DataReader {
	protected BufferedReader in;
	protected int currPos;
	
	public TextFileDataReader(Relation relation, Query query) {
		super(relation, query);
		try {
			in = new BufferedReader( new FileReader( VirtualFile.fromRelativePath(relation.getPath()).getRealFile() ) );
			currPos = 0;
		} catch (IOException ioe) {
			throw new RuntimeException(ioe);
		}
	}
	
	//Since can't guarantee that mark() will work, allow entirely new reader to be created if necessary
	private synchronized void checkReader(boolean reset) throws FileNotFoundException {
		if (in == null || reset)
			in = new BufferedReader( new FileReader( VirtualFile.fromRelativePath(relation.getPath()).getRealFile() ) );
	}
	
	/**
	 * Returns the number of instances in the underlying data file; the
	 * default implementation counts the number of lines in the file.
	 */
	public int refreshInstanceCount() {
		try {
			checkReader(true);
			int count = 0;
			String line = in.readLine();
			while (line != null) {
				count++;
				line = in.readLine();
			}
			return count;
		} catch (IOException ioe) {
			throw new RuntimeException(ioe);
		}
	}
	
	/** Read the i^th line from the data file. */
	protected String seek(final int to) throws IOException {
		checkReader(to <= currPos); //may have already read line, in which case reset reader
		String line = "";
		while (line != null && currPos < to) {
			currPos++;
			line = in.readLine();
		}
		return line;
	}
	
	protected String readLine(final int logicalPos) throws NoSuchInstanceException {
		try {
			int pos = query == null ? logicalPos : query.translateIndex(logicalPos);
			if (! relation.containsInstance(pos))
				throw new NoSuchInstanceException(logicalPos, query.size(relation));
			return seek( pos );
		} catch (IOException ioe) {
			throw new RuntimeException(ioe);
		}
	}
	
	//--Nested classes---------------------------------------------------------
	
	/** {@link TextFileDataReader} for CSV files. */
	public static class CSVReader extends TextFileDataReader {
		public CSVReader(Relation relation, Query query) { super(relation, query); }

		public JsonElement readInstance(final int i) throws NoSuchInstanceException {
			String line = readLine(i);
			if (line != null) {
				JsonArray cells = new JsonArray();
				for (String text : line.split(",", -1))
					cells.add( text.isEmpty() ? JsonNull.INSTANCE : new JsonPrimitive(text) );
				return cells;
			}
			return null;
		}
	}

	/**
	 * {@link TextFileDataReader} for files with one JSON object per line (i.e., not
	 * 'true' JSON).
	 */
	public static class JSONReader extends TextFileDataReader {
		public JSONReader(Relation relation, Query query) { super(relation, query); }

		public JsonElement readInstance(final int i) throws NoSuchInstanceException {
			return Util.parseJSON( readLine(i) );
		}
	}

}