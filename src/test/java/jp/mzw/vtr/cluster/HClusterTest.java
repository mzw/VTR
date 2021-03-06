package jp.mzw.vtr.cluster;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.apporiented.algorithm.clustering.AverageLinkageStrategy;
import com.apporiented.algorithm.clustering.Cluster;
import com.apporiented.algorithm.clustering.CompleteLinkageStrategy;
import com.apporiented.algorithm.clustering.LinkageStrategy;
import com.apporiented.algorithm.clustering.SingleLinkageStrategy;
import com.apporiented.algorithm.clustering.WeightedLinkageStrategy;

import jp.mzw.vtr.VtrTestBase;
import jp.mzw.vtr.cluster.similarity.DistAnalyzer;
import jp.mzw.vtr.cluster.similarity.DistMap;

public class HClusterTest extends VtrTestBase {

	private DistAnalyzer analyzer;

	@Before
	public void setup() {
		this.analyzer = DistAnalyzer.analyzerFactory(this.project.getOutputDir(), "lcs");
	}

	@Test
	public void testParseHashcodes() throws IOException {
		HCluster hCluster = new HCluster(this.project.getOutputDir(), this.analyzer.getMethodName());
		int[] hashcodes = hCluster.parseHashcodes();
		assertEquals(37, hashcodes.length);
	}

	@Test
	public void testParseDist() throws IOException {
		HCluster hCluster = new HCluster(this.project.getOutputDir(), this.analyzer.getMethodName());
		int[] hashcodes = hCluster.parseHashcodes();
		DistMap map = hCluster.parseDist(hashcodes);
		assertEquals(1.0, map.getMap()[0][0], 0.01);
		assertNotEquals(0.0, map.getMap()[0][1], 0.01);
		assertNotEquals(0.0, map.getMap()[1][0], 0.01);
		assertEquals(1.0, map.getMap()[1][1], 0.01);
	}

	@Test
	public void testCluster() throws IOException {
		HCluster hCluster = new HCluster(this.project.getOutputDir(), this.analyzer.getMethodName()).parse();
		LinkageStrategy strategy = HCluster.getStrategy("complete");
		List<Cluster> clusters = hCluster.cluster(strategy, 0.5);
		assertEquals(new File("src/test/resources/vtr-output/similarity/lcs/latest/complete/0.5").listFiles().length, clusters.size());
	}

	@Test
	public void testGetStrategy() {
		assertTrue(HCluster.getStrategy("average") instanceof AverageLinkageStrategy);
		assertTrue(HCluster.getStrategy("complete") instanceof CompleteLinkageStrategy);
		assertTrue(HCluster.getStrategy("single") instanceof SingleLinkageStrategy);
		assertTrue(HCluster.getStrategy("weighted") instanceof WeightedLinkageStrategy);
		assertNull(HCluster.getStrategy("median"));
		assertNull(HCluster.getStrategy("ward"));
	}
}
