package jp.mzw.vtr.detect;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import jp.mzw.vtr.Project;
import jp.mzw.vtr.Utils;
import jp.mzw.vtr.dict.DictionaryBase;
import jp.mzw.vtr.git.Commit;
import jp.mzw.vtr.git.GitUtils;
import jp.mzw.vtr.git.Tag;
import jp.mzw.vtr.maven.TestCase;

import org.jacoco.core.analysis.IClassCoverage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.gagawa.java.Document;
import com.hp.gagawa.java.DocumentType;
import com.hp.gagawa.java.elements.A;
import com.hp.gagawa.java.elements.Caption;
import com.hp.gagawa.java.elements.H1;
import com.hp.gagawa.java.elements.Pre;
import com.hp.gagawa.java.elements.Style;
import com.hp.gagawa.java.elements.Table;
import com.hp.gagawa.java.elements.Tbody;
import com.hp.gagawa.java.elements.Td;
import com.hp.gagawa.java.elements.Text;
import com.hp.gagawa.java.elements.Th;
import com.hp.gagawa.java.elements.Thead;
import com.hp.gagawa.java.elements.Tr;

public class HTMLVisualizer {
	static Logger log = LoggerFactory.getLogger(HTMLVisualizer.class);
	
	Project project;
	Properties config;
	List<Commit> commits;
	HashMap<Tag, List<Commit>> dict;
	public HTMLVisualizer(Project project, Properties config, List<Commit> commits, HashMap<Tag, List<Commit>> dict) {
		this.project = project;
		this.config = config;
		this.dict = dict;
		this.commits = commits;
	}

	public void write(Commit cur_commit, HashMap<IClassCoverage, ArrayList<Finding>> findings, TestCase test_case) throws IOException, InterruptedException {
		boolean found = false;
		
		Document document = new Document(DocumentType.XHTMLTransitional);
		document.head.appendChild(new Style("text/css").appendText(".target{background-color: pink;}"));
		document.head.appendChild(new Style("text/css").appendText("pre {margin: 0px;}"));
		document.head.appendChild(new Style("text/css").appendText("table caption {text-align: left;}"));
		document.head.appendChild(new Style("text/css").appendText("table thead th, table tbody tr {text-align: center;}"));
		document.head.appendChild(new Style("text/css").appendText("table {margin: 15px;}"));
		
		/// Test
		document.body.appendChild(new H1().appendChild(new Text("Test Blame")));
		{
			String relative = test_case.getRelativeFilePath("src/test/java");
			String url = project.getGithubUrlBlame(cur_commit, relative);
			A test_case_anchor = new A().setHref(url).appendText(relative);
			
			Table table = new Table().setRules("groups");
			table.appendChild(new Caption().appendChild(test_case_anchor));
			table.appendChild(new Thead().appendChild(new Tr()
				.appendChild(new Th().appendText("Tag"))
				.appendChild(new Th().appendText("Date"))
				.appendChild(new Th().appendText("Blame"))
				.appendChild(new Th().appendText("Line"))
				.appendChild(new Th().appendText("Source"))
				));

			Tbody tbody = new Tbody();
			for(int lineno = test_case.getStartLineNumber(); lineno <= test_case.getEndLineNumber(); lineno++) {
				Tr tr = new Tr();
				
				Commit test_commit = GitUtils.blame(lineno, test_case.getTestFile(), project, config, commits);
				Tag test_tag = DictionaryBase.getTagBy(test_commit, dict);

				if(!cur_commit.getDate().after(test_commit.getDate())) tr.setCSSClass("target");
				
				tr.appendChild(new Td().appendText("&nbsp;&nbsp;").appendChild(Finding.getTagAnchor(project, test_tag)).appendText("&nbsp;&nbsp;"));
				tr.appendChild(new Td().appendText("&nbsp;&nbsp;").appendText(test_commit.getDate().toString()).appendText("&nbsp;&nbsp;"));
				tr.appendChild(new Td().appendText("&nbsp;&nbsp;").appendChild(Finding.getBlameAnchor(project, test_commit, test_case.getTestFile(), lineno)).appendText("&nbsp;&nbsp;"));
				tr.appendChild(new Td().setAlign("right").appendText(new Integer(lineno).toString()));
				tr.appendChild(new Td().setAlign("left").appendChild(new Pre().appendText(Finding.getSrcLine(test_case.getTestFile(), lineno))));
				
				tbody.appendChild(tr);
			}
			table.appendChild(tbody);
			document.body.appendChild(table);
		}
		
		/// Source
		document.body.appendChild(new H1().appendChild(new Text("Found Source Blame")));
		for(Iterator<IClassCoverage> it = findings.keySet().iterator(); it.hasNext();) {
			IClassCoverage cc = it.next();
			
			Table table = new Table().setRules("groups");
			table.appendChild(new Caption().appendChild(Finding.getSrcFileAnchor(project, cc, cur_commit)));
			table.appendChild(new Thead().appendChild(new Tr()
				.appendChild(new Th().appendText("Tag"))
				.appendChild(new Th().appendText("Date"))
				.appendChild(new Th().appendText("Blame"))
				.appendChild(new Th().appendText("Line"))
				.appendChild(new Th().appendText("Source"))
				));
			
			Tbody tbody = new Tbody();
			for(Finding finding : findings.get(cc)) {
				Tr tr = new Tr();
				
				if(finding.isFound()) tr.setCSSClass("target");
				if(finding.isFound()) found = true;
				
				tr.appendChild(new Td().appendText("&nbsp;&nbsp;").appendChild(finding.getSrcTagAnchor()).appendText("&nbsp;&nbsp;"));
				tr.appendChild(new Td().appendText("&nbsp;&nbsp;").appendText(finding.getSrcDate() != null ? finding.getSrcDate().toString() : "latest").appendText("&nbsp;&nbsp;"));
				tr.appendChild(new Td().appendText("&nbsp;&nbsp;").appendChild(finding.getSrcBlameAnchor() != null ? finding.getSrcBlameAnchor() : new Text("Unknown")).appendText("&nbsp;&nbsp;"));
				tr.appendChild(new Td().setAlign("right").appendText(new Integer(finding.getSrcLineno()).toString()));
				tr.appendChild(new Td().setAlign("left").appendChild(new Pre().appendText(finding.getSrcLine() != null ? finding.getSrcLine() : "Unknown")));
				
				tbody.appendChild(tr);
			}
			table.appendChild(tbody);
			document.body.appendChild(table);
		}
		
		if(found) {
			String filename = test_case.getClassName() + "#" + test_case.getName() + ".html";
			File file = new File(getLogDir(cur_commit), filename);
			FileWriter writer = new FileWriter(file);
			writer.write(document.write());
			writer.close();
		}
	}
	
	private File getLogDir(Commit commit) {
		File logBaseDir = getVisualLogBaseDir();
		return this.getLogDir(logBaseDir, commit);
	}

	public File getVisualLogBaseDir() {
		File projectLogDir = new File(Utils.getPathToLogDir(config), project.getProjectName());
		File visualLogDir = new File(projectLogDir, "visual");
		if(!visualLogDir.exists() && !visualLogDir.mkdirs()) {
			log.error("Cannot create directory: " + visualLogDir.getAbsolutePath());
		}
		return visualLogDir;
	}

	public File getLogDir(File logBaseDir, Commit commit) {
		File logDir = new File(logBaseDir, commit.getId());
		if(!logDir.exists() && !logDir.mkdirs()) {
			log.error("Cannot make directory: " + logDir);
		}
		return logDir;
	}
	
}
