package jp.mzw.vtr.dict;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;

import jp.mzw.vtr.git.Commit;
import jp.mzw.vtr.git.Tag;

public class DictionaryParser extends DictionaryBase {
	
	/**
	 * 
	 * @param dir
	 * @return
	 * @throws IOException
	 * @throws ParseException
	 */
	public static List<Commit> parseCommits(File dir) throws IOException, ParseException {
		File file = new File(dir, FILENAME_COMMITS_XML);
		// Read
		List<Commit> commits = new ArrayList<>();
		String content = FileUtils.readFileToString(file);
		Document doc = Jsoup.parse(content, "", Parser.xmlParser());
		for(Element commit : doc.getElementsByTag("Commit")) {
			String id = commit.attr("id");
			Date date = SDF.parse(commit.attr("date"));
			commits.add(new Commit(id, date));
		}
		// Sort
		Collections.sort(commits, new Comparator<Commit>() {
			@Override
			public int compare(Commit c1, Commit c2) {
				if(c1.getDate().before(c2.getDate())) return -1;
				else if(c1.getDate().after(c2.getDate())) return 1;
				return 0;
			}
		});
		// Return
		return commits;
	}
	
	/**
	 * 
	 * @param dir
	 * @return
	 * @throws IOException
	 * @throws ParseException
	 */
	public static Map<Tag, List<Commit>> parseDictionary(File dir) throws IOException, ParseException {
		File file = new File(dir, FILENAME_DICT_XML);
		// Read
		HashMap<Tag, List<Commit>> dict = new HashMap<>();
		String content = FileUtils.readFileToString(file);
		Document doc = Jsoup.parse(content, "", Parser.xmlParser());
		for(Element _tag : doc.getElementsByTag("Tag")) {
			// Tag
			String tag_id = _tag.attr("id");
			Date tag_date = SDF.parse(_tag.attr("date"));
			Tag tag = new Tag(tag_id, tag_date);
			// Relevant commits
			List<Commit> commits = new ArrayList<>();
			for(Element _commit : _tag.getElementsByTag("Commit")) {
				String id = _commit.attr("id");
				Date date = SDF.parse(_commit.attr("date"));
				Commit commit = new Commit(id, date);
				commits.add(commit);
			}
			dict.put(tag, commits);
		}
		// Return
		return dict;
	}
}
