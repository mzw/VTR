package jp.mzw.vtr.cluster;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.eclipse.jdt.core.dom.ASTNode;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DetectionResult {
	protected static Logger log = LoggerFactory.getLogger(DetectionResult.class);

	protected File subject_root_dir;
	protected File log_dir;
	protected File file;
	public DetectionResult(File subject_root_dir, File log_dir, File file) {
		this.subject_root_dir = subject_root_dir;
		this.log_dir = log_dir;
		this.file = file;
	}
	
	protected String subjectName;
	protected String commitId;
	protected String testClass;
	protected String testMethod;
	public DetectionResult read() {
		String relative_path = file.getAbsolutePath().replace(log_dir.getAbsolutePath() + "/", "");
		
		String[] elements = relative_path.replace(".html", "").split("/|#");
		if(elements.length != 5) {
			log.error("Invalid file?: " + file.getAbsolutePath());
			return null;
		}
		
		subjectName = elements[0];
//		String visual = elements[1];
		commitId = elements[2];
		testClass = elements[3];
		testMethod = elements[4];
		
		return this;
	}
	
	public String getSubjectName() {
		return this.subjectName;
	}
	
	public String getCommitId() {
		return this.commitId;
	}
	
	public String getTestClass() {
		return this.testClass;
	}
	
	public String getTestMethod() {
		return this.testMethod;
	}

	private ArrayList<Integer> test_method_lines;
	public DetectionResult parseTestMethodLines() throws IOException {
		test_method_lines = new ArrayList<>();

		ArrayList<String> lines = new ArrayList<>();
		
		String html_content = FileUtils.readFileToString(file);
		Document doc = Jsoup.parse(html_content);
		Element test_table = doc.getElementsByTag("table").get(0);
		for(Element target_tr : test_table.select("tbody tr.target")) {
			this.test_method_lines.add(new Integer(target_tr.child(3).text()));
			
			lines.add(target_tr.child(4).text());
		}
		
		return this;
	}
	
	public List<Integer> getTestMethodLines() {
		return this.test_method_lines;
	}
	
	public void store(String result) throws IOException {
		String path_to_dir = file.getParentFile().getAbsolutePath().replace("visual", "sequence");
		File dir = new File(path_to_dir);
		dir.mkdirs();
		
		String filename = file.getName().replace(".html", ".txt");
		File file = new File(dir, filename);
		
		FileUtils.write(file, result);
	}

	public void store(List<ASTNode> nodes, String ext) throws IOException {
		String path_to_dir = file.getParentFile().getAbsolutePath().replace("visual", "sequence");
		File dir = new File(path_to_dir);
		dir.mkdirs();
		
		String filename = file.getName().replace(".html", ext);
		File file = new File(dir, filename);
		
		StringBuilder builder = new StringBuilder();
		for(ASTNode node : nodes) {
			builder.append(node.getClass().getSimpleName()).append("\n");
		}
		
		FileUtils.write(file, builder.toString());
	}
	
}
