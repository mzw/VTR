package jp.mzw.vtr.core;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;

public class Utils {

	public static <E> Collection<E> makeCollection(Iterable<E> iter) {
		Collection<E> list = new ArrayList<>();
		for (E item : iter) {
			list.add(item);
		}
		return list;
	}

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
	
	public static Properties getConfig(String filename) throws IOException {
		InputStream is = Utils.class.getClassLoader().getResourceAsStream(filename);
		Properties config = new Properties();
		config.load(is);
		return config;
	}
}
