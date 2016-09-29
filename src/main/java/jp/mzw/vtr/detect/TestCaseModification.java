package jp.mzw.vtr.detect;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.eclipse.jdt.core.dom.ASTNode;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;

import jp.mzw.vtr.git.Commit;
import jp.mzw.vtr.maven.TestCase;

public class TestCaseModification {

	private Commit commit;
	private TestCase testCase;
	private List<ASTNode> newNodes;
	private List<ASTNode> oldNodes;

	public TestCaseModification(Commit commit, TestCase testCase, List<ASTNode> newNodes, List<ASTNode> oldNodes) {
		this.commit = commit;
		this.testCase = testCase;
		this.newNodes = newNodes;
		this.oldNodes = oldNodes;
	}
	
	public Commit getCommit() {
		return this.commit;
	}

	public TestCase getTestCase() {
		return this.testCase;
	}

	public List<ASTNode> getNewNodes() {
		return this.newNodes;
	}

	public List<ASTNode> getOldNodes() {
		return this.oldNodes;
	}

	protected File file;
	protected List<String> revisedNodeClasses;
	protected List<String> originalNodeClasses;

	public TestCaseModification(File file) {
		this.file = file;
		this.revisedNodeClasses = new ArrayList<>();
		this.originalNodeClasses = new ArrayList<>();
	}

	public TestCaseModification parse() throws IOException {
		String content = FileUtils.readFileToString(file);
		Document document = Jsoup.parse(content, "", Parser.xmlParser());
		for (Element element : document.select("RevisedNodes Node")) {
			String clazz = element.attr("class");
			revisedNodeClasses.add(clazz);
		}
		List<String> originalNodeClasses = new ArrayList<>();
		for (Element element : document.select("OriginalNodes Node")) {
			String clazz = element.attr("class");
			originalNodeClasses.add(clazz);
		}
		return this;
	}

	public List<String> getRevisedNodeClasses() {
		return this.revisedNodeClasses;
	}

	public List<String> getOriginalNodeClasses() {
		return this.originalNodeClasses;
	}
}
