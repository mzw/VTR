package jp.mzw.vtr.git;

import java.util.Date;

public class Tag {

	String id;
	Date date;
	public Tag(String id, Date date) {
		this.id = id;
		this.date = date;
	}
	
	public String getId() {
		return this.id;
	}
	
	public Date getDate() {
		return this.date;
	}
	
}
