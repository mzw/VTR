package jp.mzw.vtr.maven;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import jp.mzw.vtr.Project;
import jp.mzw.vtr.git.GitUtils;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.NoHeadException;
import org.eclipse.jgit.api.errors.StashApplyFailureException;
import org.eclipse.jgit.api.errors.WrongRepositoryStateException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PomModifier {
	protected static Logger log = LoggerFactory.getLogger(PomModifier.class);
	
	protected boolean modified;
	protected String org_pom_str;
	
	Project project;
	Properties config;
	
	public PomModifier(Project project, Properties config) {
		this.project = project;
		this.config = config;
		
		this.modified = false;
		this.org_pom_str = null;
	}
	
	public void restore() throws IOException, InterruptedException, WrongRepositoryStateException, NoHeadException, StashApplyFailureException, GitAPIException {
		if(this.modified) {
			GitUtils.stash(project, config);
			GitUtils.stash_drop(project, config);
			MavenUtils.clean(project, config);
			log.info("Done git-stash and mvn-clean due to modified pom file");
		}
	}
	
	public PomModifier modify() throws IOException, InterruptedException {
		File pom = project.getDefaultPomFile();
		if(!pom.exists()) return this;
		this.org_pom_str = FileUtils.readFileToString(pom);
		
		this.modified = false;
		this.modified = makeJunitVer4() || modified ? true : false;
		this.modified = makeMavenSurefirePluginVer2_18_1() || modified ? true : false;
		this.modified = addJacoco() || modified ? true: false;
		
		if(this.modified) {
			MavenUtils.compile(project, config);
		}
		
		return this;
	}
	
	public boolean makeJunitVer4() throws IOException {
		File pom = project.getDefaultPomFile();
		String pom_str = FileUtils.readFileToString(pom);
		String modified_pom_str = pom_str;
		
		Document doc = Jsoup.parse(pom_str, "", Parser.xmlParser());
		for(Element dep : doc.select("dependencies dependency")) {
			
			String artifactId = null;
			String version = null;
			for(Element dep_info : dep.children()) {
				if("artifactId".equalsIgnoreCase(dep_info.tagName())) {
					artifactId = dep_info.text();
				} else if("version".equalsIgnoreCase(dep_info.tagName())) {
					version = dep_info.text();
				}
			}
			
			if("junit".equalsIgnoreCase(artifactId)) {
				if(version == null) {
					modified_pom_str = modified_pom_str.replace(
							"<artifactId>junit</artifactId>",
							"<artifactId>junit</artifactId><version>4.12</version>");
				} else if(version.startsWith("3")) {
					modified_pom_str = modified_pom_str.replace(
							"<version>" + version + "</version>",
							"<version>4.12</version>");
				}
			}
		}
		
		if(!pom_str.equals(modified_pom_str)) {
			FileUtils.write(pom, modified_pom_str);
			log.info("Make JUnit ver.4");
			return true;
		}
		
		return false;
	}
	
	public boolean makeMavenSurefirePluginVer2_18_1() throws IOException {
		File pom = project.getDefaultPomFile();
		String pom_str = FileUtils.readFileToString(pom);
		String modified_pom_str = pom_str;
		
		Document doc = Jsoup.parse(pom_str, "", Parser.xmlParser());
		for(Element plugin : doc.select("build plugins plugin")) {
			
			String artifactId = null;
			String version = null;
			for(Element plugin_info : plugin.children()) {
				if("artifactId".equalsIgnoreCase(plugin_info.tagName())) {
					artifactId = plugin_info.text();
				} else if("version".equalsIgnoreCase(plugin_info.tagName())) {
					version = plugin_info.text();
				}
			}
			
			if("maven-surefire-plugin".equalsIgnoreCase(artifactId)) {
				if(version == null) {
					modified_pom_str = modified_pom_str.replace(
							"<artifactId>maven-surefire-plugin</artifactId>",
							"<artifactId>maven-surefire-plugin</artifactId><version>2.18.1</version>");
				} else if(version.startsWith("3")) {
					modified_pom_str = modified_pom_str.replace(
							"<version>" + version + "</version>",
							"<version>2.18.1</version>");
				}
			}
		}

		if(!pom_str.equals(modified_pom_str)) {
			FileUtils.write(pom, modified_pom_str);
			log.info("Make Maven-Surefire-Plugin ver.2.18.1");
			return true;
		}
		
		return false;
	}

	public boolean addJacoco() throws IOException {
		File pom = project.getDefaultPomFile();
		String pom_str = FileUtils.readFileToString(pom);
		
		InputStream in = PomModifier.class.getClassLoader().getResourceAsStream("jacoco.pom.txt");
		String jacoco_str = IOUtils.toString(in);
		
		Document doc = Jsoup.parse(pom_str, "", Parser.xmlParser());

		Elements build = doc.getElementsByTag("build");
		if(build.size() == 0) {
			String _pom_str = pom_str.replace("</project>", "<build><plugins>"+jacoco_str+"</plugins></build></project>");
			FileUtils.write(pom, _pom_str);
			log.info("Add Jacoco plugin due to no <build>");
			return true;
		}

		boolean found = false;
		for(Element plugin : doc.select("build plugins plugin")) {
			String artifactId = null;
			for(Element plugin_info : plugin.children()) {
				if("artifactId".equalsIgnoreCase(plugin_info.tagName())) {
					artifactId = plugin_info.text();
				}
			}
			if("jacoco-maven-plugin".equalsIgnoreCase(artifactId)) {
				found = true;
			}
		}
		if(!found) {
			int index_build = pom_str.indexOf("<build>");
			String build_str = pom_str.substring(index_build);
			int index_end_plugins = build_str.indexOf("</plugins>");
			String end_plugins_str = build_str.substring(0, index_end_plugins+"</plugins>".length());
			String modified_end_plugins_str = end_plugins_str.replace("</plugins>", jacoco_str+"</plugins>");
			String modified_pom_str = pom_str.replace(end_plugins_str, modified_end_plugins_str);
			
			FileUtils.write(pom, modified_pom_str);
			log.info("Add Jacoco plugin");
			return true;
		}
		
		return false;
	}
	
}
