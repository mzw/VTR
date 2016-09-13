package jp.mzw.vtr.cluster;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Properties;

import javax.xml.bind.DatatypeConverter;

import jp.mzw.vtr.core.Utils;

import org.apache.commons.io.IOUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.parser.Parser;

public class ClusterBase {

	protected String subject_root_dir;
	protected String subjects_info_xml;
	protected String log_dir;
	protected String clustering_method;
	protected String result_url_base;
	
	public ClusterBase(Properties config) throws IOException {
		parseConfig(config);
		parseSubjectsInfo();
	}
	
	protected void parseConfig(Properties config) {
		subject_root_dir = config.getProperty("subject_root_dir") != null ? config.getProperty("subject_root_dir") : "subjects";
		subjects_info_xml = config.getProperty("subjects_info") != null ? config.getProperty("subjects_info") : "subjects_info.xml";
		log_dir = config.getProperty("log_dir") != null ? config.getProperty("log_dir") : "log";
		clustering_method = config.getProperty("clustering_method") != null ? config.getProperty("clustering_method") : "complete";
		result_url_base = config.getProperty("result_url_base") != null ? config.getProperty("result_url_base") : "http://localhost";
	}

	protected Document subjects_info;
	protected void parseSubjectsInfo() throws IOException {
		InputStream is = LCSGenerator.class.getClassLoader().getResourceAsStream(subjects_info_xml);
		String content = IOUtils.toString(is, Charset.defaultCharset());
		subjects_info = Jsoup.parse(content, "", Parser.xmlParser());
	}

	public static boolean isHtmlReportFile(File file) {
		if(!file.getAbsolutePath().contains("visual")) {
			return false;
		}
		if(!file.getName().endsWith(".html")) {
			return false;
		}
		return true;
	}
	
	public File getTestIdRelateFile() {
		return new File(new File(log_dir), "relate_test_id.csv");
	}
	
	public File getDistFile() {
		return new File(new File(log_dir), "dist.csv");
	}
	
	public File getClusterResultDir() {
		File dir = new File(new File(log_dir), "cluster_results");
		if(!dir.exists()) dir.mkdirs();
		return dir;
	}

	public File getHtmlClusterResultDir() {
		File dir = new File(new File(log_dir), "clustered");
		if(!dir.exists()) dir.mkdirs();
		return dir;
	}
	
	public String getResultUrlBase() {
		return this.result_url_base;
	}
	
	public String getClusteringMethod() {
		return this.clustering_method;
	}
	
	public static String genShortId(File file) throws NoSuchAlgorithmException, UnsupportedEncodingException {
		String path = file.getAbsolutePath();
        byte[] bytes = MessageDigest.getInstance("SHA").digest(path.getBytes("UTF-8"));
        String id = "X" + DatatypeConverter.printHexBinary(bytes).substring(0, 6);
        return id;
	}

	public static List<String> read(File file, String ext) throws IOException {
		String _path = file.getAbsolutePath().replace("visual", "sequence").replace(".html", ext);
		Path path = FileSystems.getDefault().getPath(_path);
		return Files.readAllLines(path, Charset.defaultCharset());
	}
	
	public int getClusterNum() throws IOException {
		int num = 0;
		for(File file : Utils.getFiles(new File(log_dir))) {
			if(isHtmlReportFile(file)) {
				List<String> prev_lines = read(file, ".prev");
				if(prev_lines.size() == 0) {
					continue;
				}
				num++;
			}
		}
		return num;
	}

	public String genUrl(String path_to_file) {
		return path_to_file.replace(new File(log_dir).getAbsolutePath(), getResultUrlBase()).replace("visual/", "").replace("#", "%23");
	}
	
	public static String getHtmlFilename(int num_of_cluster) {
		return String.format("%1$03d", num_of_cluster) + ".html";
	}
}
