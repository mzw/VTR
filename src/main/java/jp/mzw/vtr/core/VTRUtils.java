package jp.mzw.vtr.core;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class VTRUtils {

	/**
	 * Make Collection corresponding to given Iterable
	 * @param iter Given Iterable
	 * @return Corresponding Collection
	 */
	public static <E> Collection<E> makeCollection(Iterable<E> iter) {
		Collection<E> list = new ArrayList<>();
		for (E item : iter) {
			list.add(item);
		}
		return list;
	}

	/**
	 * Get files under given directory recursively
	 * @param dir Given Directory
	 * @return File found
	 */
	public static List<File> getFiles(File dir) {
		ArrayList<File> ret = new ArrayList<File>();
		if (!dir.exists()) {
			return ret;
		}
		for (File file : dir.listFiles()) {
			if (file.isDirectory()) {
				ret.addAll(getFiles(file));
			} else if (file.isFile()) {
				ret.add(file);
			}
		}
		return ret;
	}
	
}
