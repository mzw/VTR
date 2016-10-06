package jp.mzw.vtr.cluster.visualize;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.List;

import jp.mzw.vtr.VtrTestBase;

import org.junit.Before;
import org.junit.Test;

public class HTMLVisualizerTest extends VtrTestBase {

	protected VisualizerBase visualizer;
	
	@Before
	public void setup() throws IOException, ParseException {
		this.visualizer = new HTMLVisualizer(this.project.getOutputDir());
	}
	
	@Test
	public void testInstance() {
		assertNotNull(this.visualizer);
		assertTrue(this.visualizer instanceof HTMLVisualizer);
	}

	@Test
	public void testGetDicts() {
		assertNotNull(this.visualizer.getDict("vtr-example"));
	}

	@Test
	public void testGetClusteringLeaves() throws IOException {
		List<ClusterLeaf> leaves = this.visualizer.parseClusterLeaves(new File(this.project.getOutputDir(), "similarity/lcs/latest/hashcode.csv"));
		assertFalse(leaves.isEmpty());
		ClusterLeaf leaf = leaves.get(0);
		assertEquals(747464370, leaf.getHashcode());
		assertArrayEquals("commons-exec".toCharArray(), leaf.getProjectId().toCharArray());
		assertArrayEquals("12b4a201cb887fccb7d396f6ed19566795a60d12".toCharArray(), leaf.getCommitId().toCharArray());
		assertArrayEquals("org.apache.commons.exec.CommandLineTest".toCharArray(), leaf.getClassName().toCharArray());
		assertArrayEquals("testCommandLineParsingWithExpansion2".toCharArray(), leaf.getMethodName().toCharArray());
	}

	@Test
	public void testGetClusterResults() {
		assertFalse(this.visualizer.getClusterResults().isEmpty());
		assertEquals(1, this.visualizer.getClusterResults().size());
		ClusterResult result = this.visualizer.getClusterResults().get(0);
		List<List<ClusterLeaf>> clusters = result.getClusters();
		assertFalse(clusters.isEmpty());
		assertEquals(15, clusters.size());
		List<ClusterLeaf> cluster = clusters.get(0);
		assertFalse(cluster.isEmpty());
	}
}
