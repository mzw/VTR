package jp.mzw.vtr.cluster.difflib;

public class ChunkTagRest {

	protected String tag;
	protected String rest;

	public ChunkTagRest(String tag, String rest) {
		this.tag = tag;
		this.rest = rest;
	}

	public String getTag() {
		return this.tag;
	}

	public String getRest() {
		return this.rest;
	}

}
