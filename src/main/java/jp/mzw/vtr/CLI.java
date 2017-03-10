package jp.mzw.vtr;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.text.ParseException;
import java.util.*;

import jp.mzw.vtr.command.*;
import jp.mzw.vtr.dict.Dictionary;
import jp.mzw.vtr.git.*;
import jp.mzw.vtr.repair.Repair;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.maven.shared.invoker.MavenInvocationException;
import org.dom4j.DocumentException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.NoHeadException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.jsoup.parser.Parser;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import difflib.PatchFailedException;
import jp.mzw.vtr.core.Project;
import jp.mzw.vtr.maven.MavenUtils;
import jp.mzw.vtr.maven.TestSuite;

public class CLI {
	static Logger LOGGER = LoggerFactory.getLogger(CLI.class);

	public static final String CONFIG_FILENAME = "config.properties";

	public static void main(String[] args) throws IOException, NoHeadException, GitAPIException, ParseException, MavenInvocationException, DocumentException,
			PatchFailedException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException,
			SecurityException, ClassNotFoundException, InterruptedException {

		if (args.length < 1) { // Invalid usage
			LOGGER.info("$ java -cp=<class-path> jp.mzw.vtr.CLI dict      <subject-id>");
			LOGGER.info("$ java -cp=<class-path> jp.mzw.vtr.CLI cov       <subject-id>");
			LOGGER.info("$ java -cp=<class-path> jp.mzw.vtr.CLI cov       <subject-id> At    <commit-id>");
			LOGGER.info("$ java -cp=<class-path> jp.mzw.vtr.CLI cov       <subject-id> After <commit-id>");
			LOGGER.info("$ java -cp=<class-path> jp.mzw.vtr.CLI detect    <subject-id>");
			LOGGER.info("$ java -cp=<class-path> jp.mzw.vtr.CLI detect    <subject-id> At    <commit-id>");
			LOGGER.info("$ java -cp=<class-path> jp.mzw.vtr.CLI detect    <subject-id> After <commit-id>");
			LOGGER.info("$ java -cp=<class-path> jp.mzw.vtr.CLI cluster   <similarity> <cluster-method> <threshold>");
			LOGGER.info("$ java -cp=<class-path> jp.mzw.vtr.CLI visualize <method>");
			LOGGER.info("$ java -cp=<class-path> jp.mzw.vtr.CLI validate  <subject-id>");
			LOGGER.info("$ java -cp=<class-path> jp.mzw.vtr.CLI validate  <subject-id> At    <commit-id>");
			LOGGER.info("$ java -cp=<class-path> jp.mzw.vtr.CLI validate  <subject-id> After <commit-id>");
			LOGGER.info("$ java -cp=<class-path> jp.mzw.vtr.CLI gen       <subject-id>");
			LOGGER.info("$ java -cp=<class-path> jp.mzw.vtr.CLI repair    <subject-id>");
			return;
		}

		String command = args[0];
		if ("dict".equals(command)) {
			DictCommand.command(Arrays.copyOfRange(args, 1, args.length));
		} else if ("cov".equals(command)) {
			CovCommand.command(Arrays.copyOfRange(args, 1, args.length));
		} else if ("detect".equals(command)) {
			DetectCommand.command(Arrays.copyOfRange(args, 1, args.length));
		} else if ("cluster".equals(command)) {
			ClusterCommand.command(Arrays.copyOfRange(args, 1, args.length));
		} else if ("visualize".equals(command)) {
			VisualizeCommand.command(Arrays.copyOfRange(args, 1, args.length));
		} else if ("validate".equals(command)) {
			ValidateCommand.command(Arrays.copyOfRange(args, 1, args.length));
		} else if ("gen".equals(command)) {
			GenCommand.command(Arrays.copyOfRange(args, 1, args.length));
		} else if ("repair".equals(command)) {
			RepairCommand.command(Arrays.copyOfRange(args, 1, args.length));
		}
		// For us
		else if ("eval".equals(command)) {
			EvalCommand.command(Arrays.copyOfRange(args, 1, args.length));
		}
		LOGGER.info("Finished: {}", command);
	}
}
