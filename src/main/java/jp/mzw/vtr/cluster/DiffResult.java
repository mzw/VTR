package jp.mzw.vtr.cluster;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;

public class DiffResult {

	protected File file;
	protected List<String> revisedNodeClasses;
	protected List<String> originalNodeClasses;

	public DiffResult(File file) {
		this.file = file;
		this.revisedNodeClasses = new ArrayList<>();
		this.originalNodeClasses = new ArrayList<>();
	}

	protected void parse() throws IOException {
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
	}

	public List<String> getRevisedNodeClasses() {
		return this.revisedNodeClasses;
	}

	public List<String> getOriginalNodeClasses() {
		return this.originalNodeClasses;
	}
}
