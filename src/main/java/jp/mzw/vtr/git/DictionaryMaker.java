package jp.mzw.vtr.git;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

import jp.mzw.vtr.Project;
import jp.mzw.vtr.Utils;

import org.apache.commons.io.FileUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DictionaryMaker {
	protected static Logger log = LoggerFactory.getLogger(DictionaryMaker.class);
	
	public static void main(String[] args) throws IOException, InterruptedException {
		DictionaryMaker maker = new DictionaryMaker();
		maker.traverse();
	}

	Properties config;
	Project project;
	
	public DictionaryMaker() throws IOException {
		config = Utils.getConfig("vtr.properties");
		project = Project.make(config);
	}

	public static Tag getTagBy(Commit commit, HashMap<Tag, ArrayList<Commit>> dict) {
		for(Tag tag : dict.keySet()) {
			for(Commit _commit : dict.get(tag)) {
				if(_commit.getId().equals(commit.getId())) {
					return tag;
				}
			}
		}
		return null;
	}
	
	public void traverse() throws IOException, InterruptedException {
		String SEP = "!SEP!";
		HashMap<String, ArrayList<String>> hashTagCommits = new HashMap<>();
		String git = Utils.getPathToGit(config);
		File dir = project.getBaseDir();
		String compare = Utils.getRefToCompare(config);
		
		/// Analyze
		List<String> commits = Utils.exec(dir, Arrays.asList(git, "log", "--pretty=format:%H"+SEP+"%ad", "--date=iso"));
		List<String> added_commits = new ArrayList<>();
		List<String> tagList = Utils.exec(dir, Arrays.asList(git, "tag", "-l"));
		for(String tag : tagList) {
			ArrayList<String> related_commits = new ArrayList<>();
			List<String> compared_commits = Utils.exec(dir, Arrays.asList(git, "log", tag+".."+compare, "--pretty=format:%H"+SEP+"%ad", "--date=iso"));
			for(String commit : commits) {
				boolean compared = false;
				for(String commit_compared : compared_commits) {
					if(commit.equals(commit_compared)) {
						compared = true;
						break;
					}
				}
				if(!compared && !added_commits.contains(commit)) {
					added_commits.add(commit);
					related_commits.add(commit);
				}
			}
			hashTagCommits.put(tag, related_commits);
		}
		ArrayList<String> latest_commits = new ArrayList<>();
		for(String commit : commits) {
			boolean added = false;
			for(String added_commit : added_commits) {
				if(commit.equals(added_commit)) {
					added = true;
					break;
				}
			}
			if(!added) {
				latest_commits.add(commit);
			}
		}
		hashTagCommits.put("latest", latest_commits);

		/// Write
		/// Commits
		StringBuilder commits_builder = new StringBuilder();
		commits_builder.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>").append("\n");
		commits_builder.append("<Commits>").append("\n");
		for(String commit : commits) {
			commits_builder.append("\t").append("<Commit id=\""+commit.replace(SEP, "\" date=\"")+"\" />").append("\n");
		}
		commits_builder.append("</Commits>").append("\n");
		FileUtils.write(getCommits(config, project), commits_builder.toString());
		/// Tag-Commits
		StringBuilder dict_builder = new StringBuilder();
		dict_builder.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>").append("\n");
		dict_builder.append("<Dictionary>").append("\n");
		for(String tag : hashTagCommits.keySet()) {

			String tag_date = null;
			if("latest".equals(tag)) {
				tag_date = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z").format(new Date()).toString();
			} else {
				List<String> tag_show_results = Utils.exec(dir, Arrays.asList(git, "show", tag, "--date=iso", "-s"));
				for(String tag_show_result : tag_show_results) {
					if(tag_show_result.startsWith("Date:")) {
						tag_date = tag_show_result.replace("Date:", "").trim();
						break;
					}
				}
			}
			
			dict_builder.append("\t").append("<Tag id=\""+tag+"\" "+(tag_date != null ? "date=\""+tag_date+"\"" : "")+">").append("\n");
			for(String commit : hashTagCommits.get(tag)) {
				dict_builder.append("\t").append("\t").append("<Commit id=\""+commit.replace(SEP, "\" date=\"")+"\" />").append("\n");
			}
			dict_builder.append("\t").append("</Tag>").append("\n");
		}
		dict_builder.append("</Dictionary>").append("\n");
		FileUtils.write(getDictionary(config, project), dict_builder.toString());
	}
	
	
	public static File getCommits(Properties config, Project project) {
		File dir = new File(Utils.getPathToLogDir(config));
		String filename = project.getProjectName() + ".commits.xml";
		return new File(dir, filename);
	}
	
	public static File getDictionary(Properties config, Project project) {
		File dir = new File(Utils.getPathToLogDir(config));
		String filename = project.getProjectName() + ".dict.xml";
		return new File(dir, filename);
	}
	
	public static HashMap<Tag, ArrayList<Commit>> parse(Properties config, Project project) throws IOException, ParseException {
		HashMap<Tag, ArrayList<Commit>> ret = new HashMap<>();
		
		File file = DictionaryMaker.getDictionary(config, project);
		String content = FileUtils.readFileToString(file);
		Document doc = Jsoup.parse(content, "", Parser.xmlParser());
		for(Element _tag : doc.getElementsByTag("Tag")) {
			
			String tag_id = _tag.attr("id");
			Date tag_date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss ZZ").parse(_tag.attr("date"));
			Tag tag = new Tag(tag_id, tag_date);
			
			ArrayList<Commit> commits = new ArrayList<>();
			for(Element _commit : _tag.getElementsByTag("Commit")) {
				
				String id = _commit.attr("id");
				Date date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss ZZ").parse(_commit.attr("date"));
				Commit commit = new Commit(id, date);
				commits.add(commit);
				
			}
			ret.put(tag, commits);
		}
		
		return ret;
	}
	
}
