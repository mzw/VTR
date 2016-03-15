package jp.mzw.vtr.cluster;

import java.io.File;
import java.util.List;

public class LCSResult {

	File src;
	File dst;
	public LCSResult(File src, File dst) {
		this.src = src;
		this.dst = dst;
	}
	
	public File getSrcFile() {
		return this.src;
	}
	public File getDstFile() {
		return this.dst;
	}


	List<String> src_curr_lines;
	List<String> dst_curr_lines;
	List<String> curr_lcs;
	public LCSResult setCurrData(List<String> src, List<String> dst, List<String> lcs) {
		this.src_curr_lines = src;
		this.dst_curr_lines = dst;
		this.curr_lcs = lcs;
		return this;
	}

	List<String> src_prev_lines;
	List<String> dst_prev_lines;
	List<String> prev_lcs;
	public LCSResult setPrevData(List<String> src, List<String> dst, List<String> lcs) {
		this.src_prev_lines = src;
		this.dst_prev_lines = dst;
		this.prev_lcs = lcs;
		return this;
	}
	
	public double getCurrSimilarity() {
		if(this.src_curr_lines.size() == 0 && this.dst_curr_lines.size() == 0) {
			return -1;
		}
		return (double) this.curr_lcs.size() / (double) (this.src_curr_lines.size() + this.dst_curr_lines.size() - this.curr_lcs.size());
	}
	
	public double getPrevSimilarity() {
		if(this.src_prev_lines.size() == 0 && this.dst_prev_lines.size() == 0) {
			return -1;
		}
		return (double) this.prev_lcs.size() / (double) (this.src_prev_lines.size() + this.dst_prev_lines.size() - this.prev_lcs.size());
	}
}
