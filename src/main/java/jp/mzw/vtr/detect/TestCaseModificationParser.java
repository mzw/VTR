package jp.mzw.vtr.detect;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;

import jp.mzw.vtr.core.Project;
import jp.mzw.vtr.git.Commit;
import jp.mzw.vtr.maven.TestCase;

public class TestCaseModificationParser {

	protected String projectId;

	protected File outputDir;

	public TestCaseModificationParser(Project project) {
		this.projectId = project.getProjectId();
		this.outputDir = project.getOutputDir();
	}

	/**
	 * Parse test case modifications detected
	 * 
	 * @param commit
	 * @return
	 * @throws IOException
	 */
	public List<TestCaseModification> parse(Commit commit) throws IOException {
		// Instantiate for return
		List<TestCaseModification> ret = new ArrayList<>();
		// Output file
		File outputSubjectDir = new File(this.outputDir, this.projectId);
		File outputDetectDir = new File(outputSubjectDir, Detector.DETECT_DIR);
		File file = new File(outputDetectDir, commit.getId() + ".xml");
		// Output content
		String content = FileUtils.readFileToString(file);
		Document document = Jsoup.parse(content, "", Parser.xmlParser());
		// Parse
		for (Element eTCM : document.select("TestCaseModification")) {
			// Test cases
			for (Element eTC : eTCM.select("TestCase")) {
				String clazz = eTC.attr("class");
				String method = eTC.attr("method");
				TestCase tc = new TestCase(method, clazz, null, null, null);
				TestCaseModification tcm = new TestCaseModification(commit, tc);
				// Covered lines
				Map<String, List<Integer>> covered = new HashMap<>();
				for (Element eCovered : eTCM.select("covered")) {
					for (Element eSrc : eCovered.select("source")) {
						String path = eSrc.attr("path");
						List<Integer> lines = new ArrayList<>();
						for (Element eLine : eSrc.select("line")) {
							String lineno = eLine.attr("number");
							lines.add(Integer.parseInt(lineno));
						}
						covered.put(path, lines);
					}
				}
				tcm.setCoveredLines(covered);
				ret.add(tcm);
			}
		}
		// Return
		return ret;
	}
}
