package jp.mzw.vtr.git;

import static org.junit.Assert.*;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import jp.mzw.vtr.dict.DictionaryBase;

import org.junit.Before;
import org.junit.Test;

public class CommitTest {

	public static final String COMMIT_ID = "fcf9382884874b7ceecc16cd2155ab73b1346931";
	public static final String COMMIT_DATE = "2016-03-05 15:47:44 +0900";
	
	public static final String PREV_COMMIT_ID = "7fcfdfa99bf9f220b9643f372c36609ca35c60b3";
	public static final String PREV_COMMIT_DATE = "2016-03-05 15:45:09 +0900";
	
	Commit commit;
	
	@Before
	public void setup() throws ParseException {
		this.commit = new Commit(COMMIT_ID, DictionaryBase.SDF.parse(COMMIT_DATE));
	}
	
	@Test
	public void testGetId() throws ParseException {
		assertArrayEquals(COMMIT_ID.toCharArray(), this.commit.getId().toCharArray());
	}
	
	@Test
	public void testGetDate() throws ParseException {
		assertTrue(this.commit.getDate().equals(DictionaryBase.SDF.parse(COMMIT_DATE)));
	}
	
	@Test
	public void testGetCommitBy() {
		List<Commit> commits = new ArrayList<>();
		commits.add(this.commit);
		assertNotNull(Commit.getCommitBy(COMMIT_ID, commits));
		assertNull(Commit.getCommitBy(PREV_COMMIT_ID, commits));
	}
}
