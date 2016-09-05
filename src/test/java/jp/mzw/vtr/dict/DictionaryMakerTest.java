package jp.mzw.vtr.dict;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;

import jp.mzw.vtr.git.GitUtils;

import org.apache.commons.io.FileUtils;
import org.dom4j.Document;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.NoHeadException;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class DictionaryMakerTest {

	public static final String PATH_TO_GIT_REPO = "src/test/resources/vtr-example";
	public static final String REF_TO_COMPARE = "refs/heads/master";
	
	protected Git git;

	@Before
	public void setup() throws IOException {
		FileRepositoryBuilder builder = new FileRepositoryBuilder();
		Repository repository = builder.setGitDir(new File(PATH_TO_GIT_REPO, GitUtils.DOT_GIT)).readEnvironment().findGitDir().build();
		this.git = new Git(repository);
	}

	@Test
	public void testConstructor() {
		DictionaryMaker dm = new DictionaryMaker(this.git);
		Assert.assertNotNull(dm);
	}

	@Test
	public void testGetTagList() throws GitAPIException, MissingObjectException, IncorrectObjectTypeException {
		DictionaryMaker dm = new DictionaryMaker(this.git);
		Map<Ref, Collection<RevCommit>> tagCommitMap = dm.getTagCommitsMap(REF_TO_COMPARE);

		for (Ref tag : tagCommitMap.keySet()) {
			Collection<RevCommit> commits = tagCommitMap.get(tag);
			if ("refs/tags/v0.1".equals(tag.getName())) {
				Assert.assertEquals(2, commits.size());
			} else if ("refs/tags/v0.2".equals(tag.getName())) {
				Assert.assertEquals(1, commits.size());
			} else if ("refs/heads/master".equals(tag.getName())) {
				Assert.assertEquals(1, commits.size());
			}
		}
	}
	
	@Test
	public void testGetTags() throws GitAPIException {
		DictionaryMaker dm = new DictionaryMaker(this.git);
		Collection<Ref> tags = dm.getTags(REF_TO_COMPARE);
		Assert.assertEquals(3, tags.size());
		
		Ref[] tagsArray = tags.toArray(new Ref[]{});
		Assert.assertArrayEquals("refs/tags/v0.1".toCharArray(), tagsArray[0].getName().toCharArray());
		Assert.assertArrayEquals("refs/tags/v0.2".toCharArray(), tagsArray[1].getName().toCharArray());
		Assert.assertArrayEquals("refs/heads/master".toCharArray(), tagsArray[2].getName().toCharArray());
	}

	@Test
	public void testGetDictXML() throws NoHeadException, GitAPIException, IOException {
		DictionaryMaker dm = new DictionaryMaker(this.git);
		Map<Ref, Collection<RevCommit>> tagCommitsMap = dm.getTagCommitsMap(REF_TO_COMPARE);
		
		Document document = dm.getDict(tagCommitsMap, REF_TO_COMPARE);
		Assert.assertNotNull(document);
	}
	
	@Test
	public void testWriteDictXML() throws NoHeadException, GitAPIException, IOException {
		DictionaryMaker dm = new DictionaryMaker(this.git);
		Map<Ref, Collection<RevCommit>> tagCommitsMap = dm.getTagCommitsMap(REF_TO_COMPARE);
		Document document = dm.getDict(tagCommitsMap, REF_TO_COMPARE);
		
		File dir = new File("tmp4test");
		File file = new File(dir, "dict.xml");
		
		dm.writeDictInXML(tagCommitsMap, REF_TO_COMPARE, file);
		String content = FileUtils.readFileToString(file);
		Assert.assertArrayEquals(document.asXML().toCharArray(), content.toCharArray());
		
		FileUtils.deleteQuietly(file);
		FileUtils.deleteDirectory(dir);
	}
	
	@Test
	public void testgGetCommitsXML() throws NoHeadException, GitAPIException {
		DictionaryMaker dm = new DictionaryMaker(this.git);
		Document ducument = dm.getCommits();
		Assert.assertNotNull(ducument);
	}

	@Test
	public void testWriteCommitsXML() throws NoHeadException, GitAPIException, IOException {
		DictionaryMaker dm = new DictionaryMaker(this.git);
		Document document = dm.getCommits();
		
		File dir = new File("tmp4test");
		File file = new File(dir, "commits.xml");
		
		dm.writeCommitListInXML(file);
		String content = FileUtils.readFileToString(file);
		Assert.assertArrayEquals(document.asXML().toCharArray(), content.toCharArray());
		
		FileUtils.deleteQuietly(file);
		FileUtils.deleteDirectory(dir);
	}
	
	
}
