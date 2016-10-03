package jp.mzw.vtr.detect;

import java.io.IOException;
import java.text.ParseException;

import jp.mzw.vtr.VtrTestBase;
import jp.mzw.vtr.dict.DictionaryBase;
import jp.mzw.vtr.git.Commit;

import org.apache.maven.shared.invoker.MavenInvocationException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class DetectorTest extends VtrTestBase {

	public static final String COMMIT_ID = "fcf9382884874b7ceecc16cd2155ab73b1346931";
	public static final String COMMIT_DATE = "2016-03-05 15:47:44 +0900";

	protected Commit commit;
	protected Detector detector;

	@Before
	public void setup() throws IOException, MavenInvocationException, ParseException {
		this.commit = new Commit(COMMIT_ID, DictionaryBase.SDF.parse(COMMIT_DATE));
		this.detector = new Detector(this.project);
	}

	@Test
	public void testConstructor() {
		Assert.assertNotNull(this.detector);
	}

}
