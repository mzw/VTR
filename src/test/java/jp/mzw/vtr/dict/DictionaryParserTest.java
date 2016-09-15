package jp.mzw.vtr.dict;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.List;
import java.util.Map;

import jp.mzw.vtr.VtrTestBase;
import jp.mzw.vtr.git.Commit;
import jp.mzw.vtr.git.Tag;

import org.junit.Assert;
import org.junit.Test;

public class DictionaryParserTest extends VtrTestBase {

	@Test
	public void testGetCommits() throws IOException, ParseException {
		File dir = new File("src/test/resources/output/vtr-example");
		List<Commit> commits = DictionaryParser.parseCommits(dir);
		Assert.assertEquals(4, commits.size());
	}
	
	@Test
	public void testGetDictionary() throws IOException, ParseException {
		File dir = new File("src/test/resources/output/vtr-example");
		Map<Tag, List<Commit>> dict = DictionaryParser.parseDictionary(dir);
		Assert.assertEquals(3, dict.keySet().size());
	}
	
}
