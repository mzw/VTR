package jp.mzw.vtr.cluster.similarity;

import java.util.List;

import jp.mzw.vtr.detect.TestCaseModification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DistMap {
	protected static Logger LOGGER = LoggerFactory.getLogger(DistMap.class);

	private List<TestCaseModification> tcmList;
	private int size;
	private int[] hashcodes;

	private double[][] map;

	public DistMap(List<TestCaseModification> tcmList) {
		this.tcmList = tcmList;
		this.size = tcmList.size();
		this.hashcodes = new int[size];
		for (int i = 0; i < size; i++) {
			this.hashcodes[i] = tcmList.get(i).hashCode();
		}
		this.map = new double[size][size];
		for (int i = 0; i < size; i++) {
			for (int j = 0; j < size; j++) {
				this.map[i][j] = -1.0;
			}
		}
	}
	
	public DistMap(int[] hashcodes) {
		this.size = hashcodes.length;
		this.hashcodes = hashcodes;
		this.map = new double[size][size];
	}
	
	public int[] getHashcodes() {
		return this.hashcodes;
	}
	
	public String[] getHashcodesAsNames() {
		String[] names = new String[this.size];
		for (int i = 0; i < this.size; i++) {
			names[i] = new Integer(this.hashcodes[i]).toString();
		}
		return names;
	}

	public void add(double value, int i, int j) {
		if (!isRange(i) || !isRange(j)) {
			LOGGER.warn("Invalid LCS map addition: size={}, i={}, j={}", this.size, i, j);
			return;
		}
		this.map[i][j] = value;
	}

	private boolean isRange(int i) {
		if (i < 0 || size <= i) {
			return false;
		}
		return true;
	}

	public double[][] getMap() {
		return this.map;
	}

	public String getCsv() {
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < size; i++) {
			builder.append(",");
			builder.append(this.hashcodes[i]);
		}
		builder.append("\n");
		for (int i = 0; i < size; i++) {
			builder.append(this.hashcodes[i]);
			for (int j = 0; j < size; j++) {
				if (i == j) {
					builder.append(",").append("1");
				} else {
					String dist = 0 <= this.map[i][j] ? new Double(this.map[i][j]).toString() : new Double(this.map[j][i]).toString();
					builder.append(",").append(dist);
				}
			}
			builder.append("\n");
		}
		return builder.toString();
	}

	public String getHashcodeCsv() {
		StringBuilder builder = new StringBuilder();
		for (TestCaseModification tcm : this.tcmList) {
			builder.append(tcm.hashCode()).append(",");
			builder.append(tcm.getProjectId()).append(",");
			builder.append(tcm.getCommitId()).append(",");
			builder.append(tcm.getClassName()).append(",");
			builder.append(tcm.getMethodName()).append("\n");
		}
		return builder.toString();
	}

}
