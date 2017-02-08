package jp.mzw.vtr.maven;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by TK on 2017/02/08.
 */
public class CompilerPlugin extends JacocoInstrumenter {
    protected static Logger LOGGER = LoggerFactory.getLogger(CompilerPlugin.class);

    public CompilerPlugin(File dir) throws IOException {
        super(dir);
    }

    @Override
    public boolean instrument() throws IOException, org.dom4j.DocumentException {
        String modified = String.copyValueOf(this.originalPomContent.toCharArray());
        modified = this.modifyCompilerArguments(modified);

        if (this.originalPomContent.compareTo(modified) != 0) { // modified
            FileUtils.writeStringToFile(this.pom, modified);
            return true;
        }
        return false;
    }

    protected String modifyCompilerArguments(String content) throws IOException {
        // Read compiler-plugins snippet
        InputStream is = this.getClass().getClassLoader().getResourceAsStream("compiler-plugin.pom.txt");
        String compilerPlugin = IOUtils.toString(is);
        // modify
        org.jsoup.nodes.Document document = org.jsoup.Jsoup.parse(content, "", org.jsoup.parser.Parser.xmlParser());
        org.jsoup.select.Elements build = document.getElementsByTag("build");
        if (build.size() == 0) {
            LOGGER.info("Add Compiler plugin");
            return content.replace("</project>", "<build><plugins>" + compilerPlugin + "</plugins></build></project>");
        }
        boolean found = false;
        for (org.jsoup.nodes.Element plugins : document.select("build plugins plugin")) {
            String artifactId = null;
            for (org.jsoup.nodes.Element plugin : plugins.children()) {
                if ("artifactId".equalsIgnoreCase(plugin.tagName())) {
                    artifactId = plugin.text();
                    break;
                }
            }
            if ("maven-compiler-plugin".equalsIgnoreCase(artifactId)) {
                found = true;
            }
        }
        if (!found) {
            LOGGER.info("Add Compiler plugin");
            String _build = content.substring(content.indexOf("<build>"));
            String _plugins = _build.substring(0, _build.indexOf("</plugins>") + "</plugins>".length());
            String _modified = _plugins.replace("</plugins>", compilerPlugin + "</plugins>");
            content = content.replace(_plugins, _modified);
        }
        return content;
    }
}
