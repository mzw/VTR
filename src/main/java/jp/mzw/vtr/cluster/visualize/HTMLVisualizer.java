package jp.mzw.vtr.cluster.visualize;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import jp.mzw.vtr.CLI;
import jp.mzw.vtr.core.Project;
import jp.mzw.vtr.core.VtrUtils;
import jp.mzw.vtr.dict.Dictionary;
import jp.mzw.vtr.git.CheckoutConductor;
import jp.mzw.vtr.git.Commit;
import jp.mzw.vtr.git.GitUtils;
import jp.mzw.vtr.git.Tag;
import jp.mzw.vtr.maven.JacocoInstrumenter;
import jp.mzw.vtr.maven.MavenUtils;
import jp.mzw.vtr.maven.TestCase;
import jp.mzw.vtr.maven.TestSuite;
import jp.mzw.vtr.validate.ValidatorBase;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.eclipse.jgit.api.BlameCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.blame.BlameResult;
import org.eclipse.jgit.revwalk.RevCommit;
import org.jsoup.Jsoup;
import org.jsoup.parser.Parser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.gagawa.java.Document;
import com.hp.gagawa.java.DocumentType;
import com.hp.gagawa.java.elements.A;
import com.hp.gagawa.java.elements.Caption;
import com.hp.gagawa.java.elements.Code;
import com.hp.gagawa.java.elements.H1;
import com.hp.gagawa.java.elements.P;
import com.hp.gagawa.java.elements.Pre;
import com.hp.gagawa.java.elements.Style;
import com.hp.gagawa.java.elements.Table;
import com.hp.gagawa.java.elements.Tbody;
import com.hp.gagawa.java.elements.Td;
import com.hp.gagawa.java.elements.Text;
import com.hp.gagawa.java.elements.Tfoot;
import com.hp.gagawa.java.elements.Th;
import com.hp.gagawa.java.elements.Thead;
import com.hp.gagawa.java.elements.Tr;

public class HTMLVisualizer extends VisualizerBase {
	static Logger LOGGER = LoggerFactory.getLogger(HTMLVisualizer.class);

	public static final String HTML_VISUAL_DIR = "html";

	protected File htmlVisualDir;

	public HTMLVisualizer(File outputDir) throws IOException, ParseException {
		super(outputDir);
		this.htmlVisualDir = new File(this.visualDir, HTML_VISUAL_DIR);
		if (!this.htmlVisualDir.exists()) {
			this.htmlVisualDir.mkdirs();
		}
	}

	@Override
	public String getMethodName() {
		return HTML_VISUAL_DIR;
	}

	@Override
	public String getContent(ClusterLeaf leaf) {
		Document document = new Document(DocumentType.XHTMLTransitional);
		try {
			String projectId = leaf.getProjectId();
			// Instantiate project
			Project project = new Project(projectId).setConfig(CLI.CONFIG_FILENAME);
			File projectDir = project.getProjectDir();
			// Get dictionary, commit, and coverage
			Dictionary dict = this.getDict(project.getProjectId());
			Commit curCommit = dict.getCommitBy(leaf.getCommitId());
			Commit prvCommit = dict.getPrevCommitBy(curCommit.getId());
			// Checkout
			CheckoutConductor cc = new CheckoutConductor(project);
			// Get test cases
			cc.checkout(prvCommit);
			List<TestSuite> prvTestSuites = MavenUtils.getTestSuites(projectDir);
			TestCase prvTestCase = TestSuite.getTestCaseWithClassMethodName(prvTestSuites, leaf.getClassName(), leaf.getMethodName());
			List<String> prvTestCaseContent = prvTestCase != null ? FileUtils.readLines(prvTestCase.getTestFile()) : null;
			cc.checkout(curCommit);
			List<TestSuite> curTestSuites = MavenUtils.getTestSuites(projectDir);
			TestCase curTestCase = TestSuite.getTestCaseWithClassMethodName(curTestSuites, leaf.getClassName(), leaf.getMethodName());
			List<String> curTestCaseContent = FileUtils.readLines(curTestCase.getTestFile());
			// Get patch
			List<String> patch = null;
			if (prvTestCase == null) {
				patch = new ArrayList<>();
				patch.add("Addition");
			} else {
				String delim = "";
				// Previous
				StringBuilder prv = new StringBuilder();
				delim = "";
				for (int line = prvTestCase.getStartLineNumber(); line <= prvTestCase.getEndLineNumber(); line++) {
					prv.append(delim).append(prvTestCaseContent.get(line - 1));
					delim = "\n";
				}
				// Current
				StringBuilder cur = new StringBuilder();
				delim = "";
				// }
				for (int line = curTestCase.getStartLineNumber(); line <= curTestCase.getEndLineNumber(); line++) {
					cur.append(delim).append(curTestCaseContent.get(line - 1));
					delim = "\n";
				}
				patch = ValidatorBase.genPatch(prv.toString(), cur.toString(), prvTestCase.getTestFile(), curTestCase.getTestFile());
			}
			// Get URL
			Git git = GitUtils.getGit(project.getProjectDir());
			String url = GitUtils.getRemoteOriginUrl(git);
			File jacocoCommitDir = JacocoInstrumenter.getJacocoCommitDir(outputDir, projectId, curCommit);
			// Instantiate git-blame
			BlameCommand blame = new BlameCommand(git.getRepository());

			// Header
			document.head.appendChild(new Style("text/css").appendText(".target {background-color: pink;}"));
			document.head.appendChild(new Style("text/css").appendText(".same-tag {background-color: #FFD5EC;}"));
			document.head.appendChild(new Style("text/css").appendText(".after-tag {background-color: #FFDBC9;}"));
			document.head.appendChild(new Style("text/css").appendText("pre {margin: 0px;}"));
			document.head.appendChild(new Style("text/css").appendText("table caption {text-align: left;}"));
			document.head.appendChild(new Style("text/css").appendText("table thead th, table tbody tr {text-align: center;}"));
			document.head.appendChild(new Style("text/css").appendText("table {margin: 15px;}"));

			// Commit message
			document.body.appendChild(new H1().appendChild(new Text("Commit Message")));
			RevCommit revCommit = GitUtils.getCommit(git.getRepository(), curCommit);
			document.body.appendChild(new P().appendChild(new Text(revCommit.getFullMessage())));

			// Test case
			document.body.appendChild(new H1().appendChild(new Text("Test Blame")));
			Table testTable = getModifiedTestCaseTable(blame, curCommit, dict, projectDir, curTestCase, url);
			document.body.appendChild(testTable);

			// Patch
			document.body.appendChild(new H1().appendChild(new Text("Patch")));
			document.body.appendChild(new P().appendChild(new Text("* Note that line numbers below are not correct")));
			{
				Table table = new Table().setRules("groups");
				table.appendChild(new Thead().appendChild(new Tr().appendChild(new Th().appendText("&nbsp;"))));
				Tbody tbody = new Tbody();
				for (String line : patch) {
					Tr tr = new Tr();
					tr.appendChild(new Td().setAlign("left").appendChild(new Pre().appendChild(new Code().appendText(StringEscapeUtils.escapeHtml4(line)))));
					tbody.appendChild(tr);
				}
				table.appendChild(tbody);
				table.appendChild(new Tfoot().appendChild(new Tr().appendChild(new Td().appendText("&nbsp;"))));
				document.body.appendChild(table);
			}

			// Source code
			document.body.appendChild(new H1().appendChild(new Text("Source Blame")));
			List<Table> srcTables = getCoveredSourceTables(blame, curCommit, dict, jacocoCommitDir, projectDir, curTestCase, url);
			for (Table table : srcTables) {
				document.body.appendChild(table);
			}
		} catch (IOException | ParseException | GitAPIException e) {
			e.printStackTrace();
		}
		return document.write();
	}

	/**
	 * 
	 * @param blame
	 * @param commit
	 * @param dict
	 * @param projectDir
	 * @param tc
	 * @param url
	 * @return
	 * @throws GitAPIException
	 * @throws IOException
	 */
	private Table getModifiedTestCaseTable(BlameCommand blame, Commit commit, Dictionary dict, File projectDir, TestCase tc, String url)
			throws GitAPIException, IOException {
		String relative = VtrUtils.getFilePath(projectDir, tc.getTestFile());
		BlameResult result = blame.setFilePath(relative).call();
		Tag cur = dict.getTagBy(commit);
		List<String> lines = FileUtils.readLines(tc.getTestFile());
		// Table
		Table table = new Table().setRules("groups");
		// table-header
		table.appendChild(new Caption().appendChild(getBlameAnchor(url, commit, relative)));
		table.appendChild(new Thead().appendChild(new Tr().appendChild(new Th().appendText("Tag")).appendChild(new Th().appendText("Date"))
				.appendChild(new Th().appendText("Blame")).appendChild(new Th().appendText("Line")).appendChild(new Th().appendText("Source"))));
		// table-body
		Tbody tbody = new Tbody();
		for (int lineno = tc.getStartLineNumber(); lineno <= tc.getEndLineNumber(); lineno++) {
			Tr tr = new Tr();
			// determine whether this test-line was modified at this commit
			Commit blameCommit = new Commit(result.getSourceCommit(lineno - 1));
			Tag tag = dict.getTagBy(blameCommit);
			if (commit.getId().equals(blameCommit.getId())) {
				tr.setCSSClass("target");
			} else if (cur.getDate().equals(tag.getDate())) {
				tr.setCSSClass("same-tag");
			} else if (cur.getDate().before(tag.getDate())) {
				tr.setCSSClass("after-tag");
			}
			// create test-line
			tr.appendChild(new Td().appendText("&nbsp;&nbsp;").appendChild(getTagAnchor(url, tag)).appendText("&nbsp;&nbsp;"));
			tr.appendChild(new Td().appendText("&nbsp;&nbsp;").appendText(blameCommit.getDate().toString()).appendText("&nbsp;&nbsp;"));
			tr.appendChild(new Td().appendText("&nbsp;&nbsp;").appendChild(getBlameAnchor(url, blameCommit, relative, lineno)).appendText("&nbsp;&nbsp;"));
			tr.appendChild(new Td().setAlign("right").appendText(new Integer(lineno).toString()));
			tr.appendChild(new Td().setAlign("left").appendChild(
					new Pre().appendChild(new Code().appendText(StringEscapeUtils.escapeHtml4(lines.get(lineno - 1))))));
			// append
			tbody.appendChild(tr);
		}
		table.appendChild(tbody);
		table.appendChild(new Tfoot().appendChild(new Tr().appendChild(new Td().appendText("&nbsp;"))));
		return table;
	}

	/**
	 * 
	 * @param blame
	 * @param commit
	 * @param dict
	 * @param jacocoCommitDir
	 * @param projectDir
	 * @param tc
	 * @param url
	 * @return
	 * @throws GitAPIException
	 * @throws IOException
	 */
	private List<Table> getCoveredSourceTables(BlameCommand blame, Commit commit, Dictionary dict, File jacocoCommitDir, File projectDir, TestCase tc,
			String url) throws GitAPIException, IOException {
		List<Table> ret = new ArrayList<>();
		// parse
		File site = new File(jacocoCommitDir, tc.getFullName());
		File xml = new File(site, "jacoco.xml");
		String content = FileUtils.readFileToString(xml);
		org.jsoup.nodes.Document document = Jsoup.parse(content, "", Parser.xmlParser());
		for (org.jsoup.nodes.Element pkg : document.select("package")) {
			String pkgName = pkg.attr("name");
			// Sources
			for (org.jsoup.nodes.Element src : pkg.select("sourcefile")) {
				String srcName = src.attr("name");
				String filePath = "src/main/java/" + pkgName + "/" + srcName;
				File file = new File(projectDir, filePath);
				// Create table
				Table table = new Table().setRules("groups");
				table.appendChild(new Caption().appendChild(getBlobAnchor(url, projectDir, pkgName, srcName, commit)));
				// Special cases for JavaCC code generation
				if (!file.exists()) {
					LOGGER.info("Generated code? {}", filePath);
					if (pkgName.contains("configuration")) { // Commons-Configuration
						filePath = "src/main/javacc/PropertyListParser.jj";
					} else if (pkgName.contains("jexl")) { // Commons-JEXL
						filePath = "src/main/java/" + pkgName + "/" + "Parser.jjt";
					}
					Tbody tbody = new Tbody();
					Tr tr = new Tr();
					tr.appendChild(new Td().setAlign("left").appendText("&nbsp;&nbsp;").appendText("Generated from").appendText("&nbsp;&nbsp;")
							.appendChild(getBlobAnchor(url, projectDir, filePath, commit)));
					// append
					tbody.appendChild(tr);
					table.appendChild(tbody);
					ret.add(table);
				} else {
					table.appendChild(new Thead().appendChild(new Tr().appendChild(new Th().appendText("Tag")).appendChild(new Th().appendText("Date"))
							.appendChild(new Th().appendText("Blame")).appendChild(new Th().appendText("Line")).appendChild(new Th().appendText("Source"))));
					boolean covered = false;
					List<String> lines = FileUtils.readLines(file);
					BlameResult result = blame.setFilePath(filePath).call();
					Tbody tbody = new Tbody();
					for (org.jsoup.nodes.Element line : src.select("line")) {
						int nr = Integer.parseInt(line.attr("nr"));
						int ci = Integer.parseInt(line.attr("ci"));
						int cb = Integer.parseInt(line.attr("cb"));
						Commit blameCommit = new Commit(result.getSourceCommit(nr));
						Tag tag = dict.getTagBy(blameCommit);
						// Create line
						Tr tr = new Tr();
						if (0 < ci || 0 < cb) { // Covered
							covered = true;
							tr.setCSSClass("target");
						}
						tr.appendChild(new Td().appendText("&nbsp;&nbsp;").appendChild(getTagAnchor(url, tag)).appendText("&nbsp;&nbsp;"));
						tr.appendChild(new Td().appendText("&nbsp;&nbsp;").appendText(blameCommit.getDate().toString()).appendText("&nbsp;&nbsp;"));
						tr.appendChild(new Td().appendText("&nbsp;&nbsp;").appendChild(getBlameAnchor(url, blameCommit, filePath, nr))
								.appendText("&nbsp;&nbsp;"));
						tr.appendChild(new Td().setAlign("right").appendText(new Integer(nr).toString()));
						tr.appendChild(new Td().setAlign("left").appendChild(
								new Pre().appendChild(new Code().appendText(StringEscapeUtils.escapeHtml4(lines.get(nr - 1))))));
						// append
						tbody.appendChild(tr);
					}
					if (covered) {
						table.appendChild(tbody);
						table.appendChild(new Tfoot().appendChild(new Tr().appendChild(new Td().appendText("&nbsp;"))));
						ret.add(table);
					}
				}
			}
		}
		return ret;
	}

	/**
	 * 
	 * @param url
	 * @param commit
	 * @param relative
	 * @return
	 */
	private A getBlameAnchor(String url, Commit commit, String relative) {
		return new A().setHref(url + "/blame/" + commit.getId() + "/" + relative).appendText(relative);
	}

	/**
	 * 
	 * @param url
	 * @param commit
	 * @param relative
	 * @param lineno
	 * @return
	 */
	private A getBlameAnchor(String url, Commit commit, String relative, int lineno) {
		return new A().setHref(url + "/blame/" + commit.getId() + "/" + relative + "#L" + lineno).appendText(commit.getIdSha());
	}

	/**
	 * 
	 * @param url
	 * @param tag
	 * @return
	 */
	private A getTagAnchor(String url, Tag tag) {
		String shortTagName = tag.getId().replace("refs/tags/", "").replace("refs/heads/", "");
		if ("latest".equals(shortTagName)) {
			return new A().setHref("#").appendText(shortTagName);
		}
		return new A().setHref(url + "/releases/tag/" + shortTagName).appendText(shortTagName);
	}

	/**
	 * 
	 * @param url
	 * @param projectDir
	 * @param pkg
	 * @param filename
	 * @param commit
	 * @return
	 */
	private A getBlobAnchor(String url, File projectDir, String pkg, String filename, Commit commit) {
		File srcDir = new File(projectDir, "src/main/java");
		File pkgDir = new File(srcDir, pkg);
		File src = new File(pkgDir, filename);
		String relative = VtrUtils.getFilePath(projectDir, src);
		return new A().setHref(url + "/blob/" + commit.getId() + "/" + relative).appendText(relative);
	}

	/**
	 * 
	 * @param url
	 * @param projectDir
	 * @param filePath
	 * @param commit
	 * @return
	 */
	private A getBlobAnchor(String url, File projectDir, String filePath, Commit commit) {
		return new A().setHref(url + "/blob/" + commit.getId() + "/" + filePath).appendText(filePath);
	}
}
