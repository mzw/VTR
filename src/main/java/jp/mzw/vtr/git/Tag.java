package jp.mzw.vtr.git;

import java.util.Date;

public class Tag {

	/** Tag ID */
	String id;

	/** Date when this tag was created */
	Date date;

	/**
	 * Constructor
	 * 
	 * @param id
	 * @param date
	 */
	public Tag(String id, Date date) {
		this.id = id;
		this.date = date;
	}

	/**
	 * Get ID representing this tag
	 * 
	 * @return
	 */
	public String getId() {
		return this.id;
	}

	/**
	 * Get date when this tag was created
	 * 
	 * @return
	 */
	public Date getDate() {
		return this.date;
	}

}
