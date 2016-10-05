package jp.mzw.vtr.core;

import static org.junit.Assert.*;

import java.io.IOException;

import org.junit.Test;

import jp.mzw.vtr.VtrTestBase;

public class ProjectTest extends VtrTestBase {

	@Test
	public void testConstructorWithNoConfig() {
		Project project = new Project("vtr-example");
		assertArrayEquals("vtr-example".toCharArray(), project.getProjectId().toCharArray());
		assertArrayEquals("subjects/vtr-example".toCharArray(), project.getProjectDir().getPath().toCharArray());
		assertArrayEquals("output".toCharArray(), project.getOutputDir().getPath().toCharArray());
		assertArrayEquals("/usr/local/apache-maven-3.3.9".toCharArray(), project.getMavenHome().getPath().toCharArray());
	}

	@Test
	public void testConstructorWithConfig() throws IOException {
		Project project = new Project("vtr-example");
		project.setConfig(TEST_CONFIG_FILENAME);
		assertArrayEquals("vtr-example".toCharArray(), project.getProjectId().toCharArray());
		assertArrayEquals("src/test/resources/vtr-subjects/vtr-example".toCharArray(), project.getProjectDir().getPath().toCharArray());
		assertArrayEquals("src/test/resources/vtr-output".toCharArray(), project.getOutputDir().getPath().toCharArray());
		assertArrayEquals("/usr/local/apache-maven-3.3.9".toCharArray(), project.getMavenHome().getPath().toCharArray());
	}
}
