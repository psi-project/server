/**
 * 
 */
package util;

import java.util.List;

/**
 * Convenience methods for dealing with lists.
 * 
 * @author jmontgomery
 *
 */
public final class ListUtil {
	private ListUtil() { }
	
	/**
	 * Unnecessary alias method to get the first element in a list.
	 */
	public static <E> E head(List<E> list) {
		return list.get(0);
	}

	/**
	 * Returns the tail of the given list.
	 */
	public static <E> List<E> tail(List<E> list) {
		return list.subList(1, list.size());
	}


}
