package jp.mzw.vtr.dict;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jp.mzw.vtr.git.Commit;
import jp.mzw.vtr.git.Tag;

public class Dictionary extends DictionaryBase {
	protected static Logger LOGGER = LoggerFactory.getLogger(Dictionary.class);

	protected File outputDir;
	protected String projectId;

	private Map<Tag, List<Commit>> contents;
	private List<Commit> commits;
	private Map<String, Commit> prevCommitByCommitId;
	private Map<String, Commit> postCommitByCommitId;

	public Dictionary(File outputDir, String projectId) {
		this.outputDir = outputDir;
		this.projectId = projectId;
	}

	public Dictionary parse() throws IOException, ParseException {
		parseTagCommits();
		parseCommits();
		return this;
	}

	/**
	 * Parse relationships among tags and commits
	 * 
	 * @throws IOException
	 * @throws ParseException
	 */
	private void parseTagCommits() throws IOException, ParseException {
		File dir = new File(this.outputDir, this.projectId);
		File file = new File(dir, FILENAME_DICT_XML);
		this.contents = new HashMap<>();
		String content = FileUtils.readFileToString(file);
		Document doc = Jsoup.parse(content, "", Parser.xmlParser());
		for (Element _tag : doc.getElementsByTag("Tag")) {
			// Tag
			String tag_id = _tag.attr("id");
			Date tag_date = SDF.parse(_tag.attr("date"));
			Tag tag = new Tag(tag_id, tag_date);
			// Relevant commits
			List<Commit> commits = new ArrayList<>();
			for (Element _commit : _tag.getElementsByTag("Commit")) {
				String id = _commit.attr("id");
				Date date = SDF.parse(_commit.attr("date"));
				Commit commit = new Commit(id, date);
				commits.add(commit);
			}
			this.contents.put(tag, commits);
		}
	}

	/**
	 * Parse all commits
	 * 
	 * @return
	 * @throws IOException
	 * @throws ParseException
	 */
	private void parseCommits() throws IOException, ParseException {
		File dir = new File(this.outputDir, this.projectId);
		File file = new File(dir, FILENAME_COMMITS_XML);
		// Read
		this.commits = new ArrayList<>();
		String content = FileUtils.readFileToString(file);
		Document doc = Jsoup.parse(content, "", Parser.xmlParser());
		for (Element commit : doc.getElementsByTag("Commit")) {
			String id = commit.attr("id");
			Date date = SDF.parse(commit.attr("date"));
			this.commits.add(new Commit(id, date));
		}
		// Sort
		Collections.sort(this.commits, new Comparator<Commit>() {
			@Override
			public int compare(Commit c1, Commit c2) {
				if (c1.getDate().before(c2.getDate()))
					return -1;
				else if (c1.getDate().after(c2.getDate()))
					return 1;
				return 0;
			}
		});
	}

	/**
	 * Get all commits
	 * 
	 * @return List of all commits
	 */
	public List<Commit> getCommits() {
		return this.commits;
	}

	/**
	 * 
	 * @param commitId
	 * @return
	 */
	public Commit getCommitBy(String commitId) {
		for (Commit commit : this.commits) {
			if (commit.getId().equals(commitId)) {
				return commit;
			}
		}
		return null;
	}

	/**
	 * Get all tags parsed
	 * 
	 * @return
	 */
	public Set<Tag> getTags() {
		return this.contents.keySet();
	}

	/**
	 * Get tag relevant to given commit
	 * 
	 * @param commit
	 * @return
	 */
	public Tag getTagBy(Commit commit) {
		for (Tag tag : this.contents.keySet()) {
			for (Commit _commit : this.contents.get(tag)) {
				if (_commit.getId().equals(commit.getId())) {
					return tag;
				}
			}
		}
		return null;
	}

	public Tag getTagBy(String tagId) {
		for (Tag tag : this.contents.keySet()) {
			if (tag.getId().equals("refs/tags/" + tagId)) {
				return tag;
			}
		}
		return null;
	}
	/**
	 * Create previous commit by given commit ID
	 * 
	 * @return
	 * @throws IOException
	 * @throws ParseException
	 */
	public Dictionary createPrevCommitByCommitIdMap() throws IOException, ParseException {
		this.prevCommitByCommitId = new HashMap<>();
		if (this.commits.size() < 3) {
			return null;
		}
		Commit prv = this.commits.get(0);
		for (int i = 1; i < this.commits.size(); i++) {
			Commit cur = this.commits.get(i);
			this.prevCommitByCommitId.put(cur.getId(), prv);
			prv = cur;
		}
		return this;
	}

	/**
	 * Create previous commit by given commit ID
	 * 
	 * @return
	 * @throws IOException
	 * @throws ParseException
	 */
	public Dictionary createPostCommitByCommitIdMap() throws IOException, ParseException {
		this.postCommitByCommitId = new HashMap<>();
		if (this.commits.size() < 3) {
			return null;
		}
		Commit prv = this.commits.get(0);
		for (int i = 1; i < this.commits.size(); i++) {
			Commit cur = this.commits.get(i);
			this.postCommitByCommitId.put(prv.getId(), cur);
			prv = cur;
		}
		return this;
	}

	/**
	 * Get previous commit by given commit
	 * 
	 * @param commitId
	 * @return
	 * @throws ParseException
	 * @throws IOException
	 */
	public Commit getPrevCommitBy(String commitId) throws IOException, ParseException {
		if (this.prevCommitByCommitId == null) {
			this.parse().createPrevCommitByCommitIdMap();
		}
		return this.prevCommitByCommitId.get(commitId);
	}

	/**
	 * Get previous commit by given commit
	 * 
	 * @param commitId
	 * @return
	 * @throws ParseException
	 * @throws IOException
	 */
	public Commit getPostCommitBy(String commitId) throws IOException, ParseException {
		if (this.postCommitByCommitId == null) {
			this.parse().createPostCommitByCommitIdMap();
		}
		return this.postCommitByCommitId.get(commitId);
	}

}
