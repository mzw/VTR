package jp.mzw.vtr.example;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

public class FileUtils {

	public static ArrayList<String> grep(File file, String str) throws IOException {
		ArrayList<String> ret = new ArrayList<String>();
		BufferedReader br = new BufferedReader(new FileReader(file));
		String line = null;
		while ((line = br.readLine()) != null) {
			if (line.contains(str)) {
				ret.add(line);
			}
		}
		br.close();
		return ret;
	}

	public static ArrayList<File> getFiles(File dir) {
		ArrayList<File> ret = new ArrayList<File>();
		for(File file : dir.listFiles()) {
			if(file.isFile()) ret.add(file);
			else if(file.isDirectory()) ret.addAll(getFiles(file));
		}
		return ret;
	}
	
}
