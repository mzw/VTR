package jp.mzw.vtr.git;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;

public class Commit {

	String id;
	Date date;
	public Commit(String id, Date date) {
		this.id = id;
		this.date = date;
	}
	
	public String getId() {
		return this.id;
	}
	
	public Date getDate() {
		return this.date;
	}
	
	public static List<Commit> parse(File file) throws IOException, ParseException {
		ArrayList<Commit> ret = new ArrayList<>();
		
		String content = FileUtils.readFileToString(file);
		Document doc = Jsoup.parse(content, "", Parser.xmlParser());
		for(Element commit : doc.getElementsByTag("Commit")) {
			String id = commit.attr("id");
			Date date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss ZZ").parse(commit.attr("date"));
			ret.add(new Commit(id, date));
		}
		
		return ret;
	}
	
}
