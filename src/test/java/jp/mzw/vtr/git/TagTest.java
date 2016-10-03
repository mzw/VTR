package jp.mzw.vtr.git;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertTrue;

import java.text.ParseException;

import jp.mzw.vtr.dict.DictionaryBase;

import org.junit.Before;
import org.junit.Test;

public class TagTest {

	public static final String TAG_ID = "refs/tags/v0.2";
	public static final String TAG_DATE = "2016-03-05 15:46:56 +0900";

	Tag tag;

	@Before
	public void setup() throws ParseException {
		this.tag = new Tag(TAG_ID, DictionaryBase.SDF.parse(TAG_DATE));
	}

	@Test
	public void testGetId() throws ParseException {
		assertArrayEquals(TAG_ID.toCharArray(), this.tag.getId().toCharArray());
	}

	@Test
	public void testGetDate() throws ParseException {
		assertTrue(this.tag.getDate().equals(DictionaryBase.SDF.parse(TAG_DATE)));
	}

}
