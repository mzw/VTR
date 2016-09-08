package jp.mzw.vtr.detect;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import jp.mzw.vtr.Project;
import jp.mzw.vtr.VtrBase;
import jp.mzw.vtr.git.Commit;
import jp.mzw.vtr.git.GitUtils;
import jp.mzw.vtr.git.Tag;
import jp.mzw.vtr.maven.JacocoRunner;
import jp.mzw.vtr.maven.TestCase;

import org.jacoco.core.analysis.IClassCoverage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Finder extends VtrBase {
	Logger log = LoggerFactory.getLogger(Finder.class);

	List<Commit> commits;
	HashMap<Tag, List<Commit>> dict;
	public Finder(Project project, Properties config, List<Commit> commits, HashMap<Tag, List<Commit>> dict) {
		super(project, config);
		this.commits = commits;
		this.dict = dict;
	}

	public Tag getTagBy(Commit commit) {
		for(Tag tag : dict.keySet()) {
			for(Commit _commit : dict.get(tag)) {
				if(_commit.getId().equals(commit.getId())) {
					return tag;
				}
			}
		}
		return null;
	}

	public void find(Commit cur_commit, List<Commit> test_commits, TestCase test_case) throws IOException, InterruptedException {
		HashMap<IClassCoverage, ArrayList<Finding>> findings = new HashMap<IClassCoverage, ArrayList<Finding>>();

		for(IClassCoverage cc : test_case.getCoverageBuilder().getClasses()) {
			ArrayList<Finding> _findings = new ArrayList<Finding>();
			if(0 < cc.getLineCounter().getCoveredCount()) {
				File src_file = project.getDefaultSrcFile(cc);

				for(int lineno = cc.getFirstLine(); lineno <= cc.getLastLine(); lineno++) {
					if(JacocoRunner.isCoveredLine(cc.getLine(lineno).getStatus())) {
						
						// Find commit where this source code line was added
						Commit src_commit = GitUtils.blame(lineno, src_file, project, config, commits);
						Tag src_tag = getTagBy(src_commit);
						
						for(Commit test_commit : test_commits) {
							Tag test_tag = getTagBy(test_commit);
							Finding finding = new Finding(project, config, cur_commit,
									test_commit, test_case, test_tag,
									src_commit, src_file, lineno, src_tag);
							
							// Test commit is released after covered-line commit is released
							if(src_tag.getDate().before(test_tag.getDate())) {
								finding.found();
							}
							_findings.add(finding);
							
						}
						
						
					}
				}
			}
			if(!_findings.isEmpty()) findings.put(cc, _findings);
		}
		if(!findings.isEmpty()) log(cur_commit, findings, test_case);
	}
	
	
	
	private void log(Commit cur_commit, HashMap<IClassCoverage, ArrayList<Finding>> findings, TestCase test_case) throws IOException, InterruptedException {
		boolean found = false;
		/// Default logger
		StringBuilder builder = new StringBuilder();
		String delim = "";
		builder.append("@Commit(id=\""+cur_commit.getId()+"\")").append("\n");
		// For test
		builder.append("@Test(path=\""+test_case.getTestFile().getAbsolutePath()+"\")").append("\n");
		List<String> test_blame_results = GitUtils.blame(
				test_case.getStartLineNumber(), test_case.getEndLineNumber(), test_case.getTestFile(), project, config, true);
		delim = "";
		for(String test_blame_result : test_blame_results) {
			builder.append(delim).append(test_blame_result);
			delim = "\n";
		}
		builder.append("\n");
		// For source
		for(Iterator<IClassCoverage> it = findings.keySet().iterator(); it.hasNext();) {
			IClassCoverage _cc = it.next();
			ArrayList<Finding> _findings = findings.get(_cc);
			
			boolean _found = false;
			for(Finding finding : _findings) {
				if(finding.isFound()) {
					found = true;
					_found = true;
					break;
				}
			}
			if(!_found) continue;
			
			builder.append("@Source(\"path="+Finding.getSrcFile(project, _cc).getAbsolutePath()+"\")").append("\n");
			delim = "";
			for(Finding finding : _findings) {
				builder.append(delim).append(finding.toString());
				delim = "\n";
			}
			
		}
		if(found) log.info("Found:\n" + builder.toString());
		
		new HTMLVisualizer(project, config, commits, dict).write(cur_commit, findings, test_case);
	}
}
