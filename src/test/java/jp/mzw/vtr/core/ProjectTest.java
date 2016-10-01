package jp.mzw.vtr.core;

import static org.junit.Assert.*;

import org.junit.Test;

import jp.mzw.vtr.VtrTestBase;

public class ProjectTest extends VtrTestBase {

	@Test
	public void testConstructor() {
		Project project = new Project("vtr-example", "src/test/resources/vtr-example");
		assertArrayEquals("vtr-example".toCharArray(), project.getProjectId().toCharArray());
		assertArrayEquals("src/test/resources/vtr-example".toCharArray(), project.getProjectDir().getPath().toCharArray());
		assertArrayEquals("output".toCharArray(), project.getOutputDir().getPath().toCharArray());
		assertArrayEquals("/usr/local/apache-maven-3.3.9".toCharArray(), project.getMavenHome().getPath().toCharArray());
	}
}
