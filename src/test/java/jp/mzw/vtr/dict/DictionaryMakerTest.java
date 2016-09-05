package jp.mzw.vtr.dict;

import java.io.File;
import java.io.IOException;

import jp.mzw.vtr.git.GitUtils;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class DictionaryMakerTest {
	
	static String pathToRepo = "src/test/resources/vtr-example";
	
	protected Git git;
	
	@Before
	public void setup() throws IOException {
		FileRepositoryBuilder builder = new FileRepositoryBuilder();
		Repository repository = builder.setGitDir(new File(pathToRepo, GitUtils.DOT_GIT)).readEnvironment().findGitDir().build();
		this.git = new Git(repository);
	}
	
	@Test
	public void testConstructor() {
		DictionaryMaker dm = new DictionaryMaker(this.git);
		Assert.assertNotNull(dm);
	}
	
}
