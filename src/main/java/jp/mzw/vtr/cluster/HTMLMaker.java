package jp.mzw.vtr.cluster;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

import jp.mzw.vtr.core.Utils;

import org.apache.commons.io.FileUtils;

import com.hp.gagawa.java.Document;
import com.hp.gagawa.java.DocumentType;
import com.hp.gagawa.java.elements.A;
import com.hp.gagawa.java.elements.H1;
import com.hp.gagawa.java.elements.H2;
import com.hp.gagawa.java.elements.Li;
import com.hp.gagawa.java.elements.P;
import com.hp.gagawa.java.elements.Span;
import com.hp.gagawa.java.elements.Text;
import com.hp.gagawa.java.elements.Ul;

public class HTMLMaker extends ClusterBase {
	public static void main(String[] args) throws IOException {
		InputStream is = HTMLMaker.class.getClassLoader().getResourceAsStream("vtr.properties");
		Properties config = new Properties();
		config.load(is);
		new HTMLMaker(config).make();
	}

	public HTMLMaker(Properties config) throws IOException {
		super(config);
	}
	
	public HTMLMaker make() throws IOException {
		ArrayList<File> files = new ArrayList<>();
		for(File file : Utils.getFiles(new File(log_dir))) {
			if(isHtmlReportFile(file)) {
				List<String> prev_lines = read(file, ".prev");
				if(prev_lines.size() == 0) {
					continue;
				}
				files.add(file);
			}
		}
		
		for(int i = 1; i <= files.size(); i++) {
			writeInHtml(i);
		}
		
		return this;
	}
	
	public void writeInHtml(int num_of_cluster) throws IOException {
		HashMap<Integer, ArrayList<String>> clustered = analyzeClusteredResults(num_of_cluster);
		Document document = generateHtml(num_of_cluster, clustered);
		FileUtils.write(new File(getHtmlClusterResultDir(), getHtmlFilename(num_of_cluster)), document.write(), Charset.defaultCharset());
	}
	
	public HashMap<Integer, ArrayList<String>> analyzeClusteredResults(int num_of_cluster) throws IOException {
		HashMap<String, Integer> hashIdCluster = new HashMap<>();
		File cluster_result_file = new File(getClusterResultDir(), Integer.toString(num_of_cluster));
		String cluster_results = FileUtils.readFileToString(cluster_result_file, Charset.defaultCharset());
		for(String result : cluster_results.split("\n")) {
			String[] split = result.split(" ");
			hashIdCluster.put(split[0], Integer.parseInt(split[1]));
		}
		

		HashMap<String, String> hashIdTestfile = new HashMap<>();
		String relate_test_id = FileUtils.readFileToString(getTestIdRelateFile(), Charset.defaultCharset());
		for(String result : relate_test_id.split("\n")) {
			String[] split = result.split(",");
			hashIdTestfile.put(split[0], split[1]);
		}
		
		HashMap<Integer, ArrayList<String>> clustered = new HashMap<>();
		for(String id : hashIdTestfile.keySet()) {
			Integer cluster = hashIdCluster.get(id);
			String path_to_file = hashIdTestfile.get(id);
			
			ArrayList<String> tests = clustered.get(cluster);
			if(tests == null) tests = new ArrayList<>();
			tests.add(path_to_file);
			
			clustered.put(cluster, tests);
		}
		return clustered;
	}
	
	
	public Document generateHtml(int num_of_cluster, HashMap<Integer, ArrayList<String>> clustered) throws IOException {
		Document document = new Document(DocumentType.XHTMLTransitional);
		document.body.appendChild(new H1().appendChild(new Text("Clustered Results")));
		document.body.appendChild(new P().appendChild(new Text("Given the number of clustering: ")).appendChild(new Span().appendChild(new Text(num_of_cluster))));
		for(Integer cluster : clustered.keySet()) {
			document.body.appendChild(new H2().appendChild(new Text(cluster)));
			Ul ul = new Ul();
			for(String path_to_file : clustered.get(cluster)) {
				ul.appendChild(new Li().appendChild(new A().setHref(genUrl(path_to_file)).appendChild(new Text(genUrl(path_to_file)))));
			}
			document.body.appendChild(ul);
		}
		return document;
	}
}



