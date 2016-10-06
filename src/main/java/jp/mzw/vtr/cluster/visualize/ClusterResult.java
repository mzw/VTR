package jp.mzw.vtr.cluster.visualize;

import java.util.ArrayList;
import java.util.List;

import jp.mzw.vtr.cluster.similarity.DistAnalyzer;

import com.apporiented.algorithm.clustering.LinkageStrategy;

public class ClusterResult {
	
	DistAnalyzer distAnalyzer;
	LinkageStrategy linkageStrategy;
	double threshold;
	List<ClusterLeaf> clusterLeaves;
	List<List<ClusterLeaf>> clusters;

	public ClusterResult(DistAnalyzer analyzer, LinkageStrategy linkageStrategy, double threshold) {
		this.distAnalyzer = analyzer;
		this.linkageStrategy = linkageStrategy;
		this.threshold = threshold;
		clusters = new ArrayList<>();
	}

	public DistAnalyzer getDistAnalyzer() {
		return this.distAnalyzer;
	}

	public LinkageStrategy getLinkageStrategy() {
		return this.linkageStrategy;
	}

	public double getThreshold() {
		return this.threshold;
	}

	public void setClusterLeaves(List<ClusterLeaf> clusterLeaves) {
		this.clusterLeaves = clusterLeaves;
	}

	public List<ClusterLeaf> getClusterLeaves() {
		return this.clusterLeaves;
	}

	public void addCluster(List<Integer> hashcodes) {
		List<ClusterLeaf> cluster = new ArrayList<>();
		for (Integer hashcode : hashcodes) {
			for (ClusterLeaf leaf : this.clusterLeaves) {
				if (leaf.getHashcode() == hashcode) {
					cluster.add(leaf);
					break;
				}
			}
		}
		this.clusters.add(cluster);
	}

	public List<List<ClusterLeaf>> getClusters() {
		return this.clusters;
	}
}
