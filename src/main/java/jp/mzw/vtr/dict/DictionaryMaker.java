package jp.mzw.vtr.dict;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import jp.mzw.vtr.core.VtrUtils;
import jp.mzw.vtr.git.GitUtils;

import org.apache.commons.io.FileUtils;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.NoHeadException;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTag;
import org.eclipse.jgit.revwalk.RevWalk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DictionaryMaker extends DictionaryBase {
	static Logger LOGGER = LoggerFactory.getLogger(DictionaryMaker.class);

	protected Git git;

	public DictionaryMaker(Git git) {
		this.git = git;
	}

	/**
	 * Get Map whose keys are tags (i.e., versions) otherwise ref-to-compare
	 * (e.g., refs/heads/master) and values are lists of relevant commits
	 * 
	 * @return The map
	 * @throws NoHeadException
	 * @throws GitAPIException
	 * @throws MissingObjectException
	 * @throws IncorrectObjectTypeException
	 */
	public Map<Ref, Collection<RevCommit>> getTagCommitsMap(String refToCompare) throws NoHeadException, GitAPIException, MissingObjectException,
			IncorrectObjectTypeException {
		Map<Ref, Collection<RevCommit>> tagCommitMap = new HashMap<>();

		Collection<RevCommit> commits = VtrUtils.makeCollection(this.git.log().call());
		Collection<RevCommit> registered = new ArrayList<>();
		Collection<RevCommit> unregistered = new ArrayList<>();

		Ref compare = GitUtils.getBranch(this.git, refToCompare);
		Collection<Ref> tags = this.git.tagList().call();
		for (Ref tag : tags) {
			Collection<RevCommit> relevantCommits = new ArrayList<>();
			// Get commits which are necessary to be included for achieving
			// ref-to-compare from this tag
			ObjectId tagRef = tag.getPeeledObjectId();
			if (tagRef == null) {
				tagRef = tag.getObjectId();
			}
			ObjectId compRef = compare.getObjectId();
			Iterable<RevCommit> commitsInRange = this.git.log().addRange(tagRef, compRef).call();
			Collection<RevCommit> commitsToCompare = VtrUtils.makeCollection(commitsInRange);
			for (RevCommit commit : commits) {
				boolean relevant = true;
				for (RevCommit commitToCompare : commitsToCompare) {
					if (commit.equals(commitToCompare)) {
						relevant = false;
						break;
					}
				}
				if (relevant && !registered.contains(commit)) {
					relevantCommits.add(commit);
					registered.add(commit);
				}
			}
			tagCommitMap.put(tag, relevantCommits);
		}
		for (RevCommit commit : commits) {
			if (!registered.contains(commit)) {
				unregistered.add(commit);
			}
		}
		tagCommitMap.put(compare, unregistered);

		return tagCommitMap;
	}

	/**
	 * Get tags (i.e., versions) otherwise ref-to-compare (e.g.,
	 * refs/heads/master) indicating the "latest version"
	 * 
	 * @return The tags
	 * @throws GitAPIException
	 */
	public Collection<Ref> getTags(String refToCompare) throws GitAPIException {
		Ref compare = GitUtils.getBranch(this.git, refToCompare);
		Collection<Ref> tags = VtrUtils.makeCollection(this.git.tagList().call());
		tags.add(compare);
		return tags;
	}

	/**
	 * Translate Map(tag, commits) into XML format
	 * 
	 * @param tagCommitsMap
	 *            map whose keys are tags (i.e., versions) otherwise
	 *            ref-to-compare (e.g., refs/heads/master) representing "latest"
	 *            and values are relevant commits
	 * @return XML document representing given map
	 * @throws MissingObjectException
	 * @throws IncorrectObjectTypeException
	 * @throws IOException
	 */
	protected Document getDict(Map<Ref, Collection<RevCommit>> tagCommitsMap, String refToCompare) throws MissingObjectException, IncorrectObjectTypeException,
			IOException {
		Document document = DocumentHelper.createDocument();
		Element root = document.addElement("Dictionary");
		for (Ref tag : tagCommitsMap.keySet()) {
			Date date = new Date();
			if (!refToCompare.equals(tag.getName())) { // Not latest
				RevWalk walk = new RevWalk(this.git.getRepository());
				try {
					RevTag _tag = walk.parseTag(tag.getObjectId());
					_tag = walk.parseTag(tag.getObjectId());
					date = _tag.getTaggerIdent().getWhen();
				} catch (IncorrectObjectTypeException e) {
					RevCommit commit = walk.parseCommit(tag.getObjectId());
					date = new Date((long) commit.getCommitTime() * 1000L);
				}
			}
			Element tagElement = root.addElement("Tag").addAttribute("id", tag.getName()).addAttribute("date", SDF.format(date));
			for (RevCommit commit : tagCommitsMap.get(tag)) {
				tagElement.addElement("Commit").addAttribute("id", commit.name()).addAttribute("date", SDF.format(commit.getAuthorIdent().getWhen()));
			}
		}
		return document;
	}

	/**
	 * 
	 * @param tagCommitsMap
	 * @param file
	 * @throws MissingObjectException
	 * @throws IncorrectObjectTypeException
	 * @throws IOException
	 */
	public void writeDictInXML(Map<Ref, Collection<RevCommit>> tagCommitsMap, String refToCompare, File dir) throws MissingObjectException,
			IncorrectObjectTypeException, IOException {
		Document document = this.getDict(tagCommitsMap, refToCompare);
		FileUtils.write(new File(dir, FILENAME_DICT_XML), document.asXML());
	}

	/**
	 * Get commit information into XML format
	 * 
	 * @return XML document representing commits
	 * @throws NoHeadException
	 * @throws GitAPIException
	 */
	protected Document getCommits() throws NoHeadException, GitAPIException {
		Document document = DocumentHelper.createDocument();
		Element root = document.addElement("Commits");
		Iterable<RevCommit> commits = this.git.log().call();
		for (RevCommit commit : commits) {
			root.addElement("Commit").addAttribute("id", commit.name()).addAttribute("date", SDF.format(commit.getAuthorIdent().getWhen()));
		}
		return document;
	}

	/**
	 * 
	 * @param commits
	 * @param file
	 * @throws NoHeadException
	 * @throws GitAPIException
	 * @throws IOException
	 */
	public void writeCommitListInXML(File dir) throws NoHeadException, GitAPIException, IOException {
		Document document = this.getCommits();
		FileUtils.write(new File(dir, FILENAME_COMMITS_XML), document.asXML());
	}
}
