package jp.mzw.vtr;

import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jp.mzw.vtr.command.ClassifyCommand;
import jp.mzw.vtr.command.ClusterCommand;
import jp.mzw.vtr.command.CovCommand;
import jp.mzw.vtr.command.DetectCommand;
import jp.mzw.vtr.command.DictCommand;
import jp.mzw.vtr.command.EvalCommand;
import jp.mzw.vtr.command.GenCommand;
import jp.mzw.vtr.command.MLCommand;
import jp.mzw.vtr.command.RepairCommand;
import jp.mzw.vtr.command.ValidateCommand;
import jp.mzw.vtr.command.VisualizeCommand;

public class CLI {
	static Logger LOGGER = LoggerFactory.getLogger(CLI.class);

	public static final String CONFIG_FILENAME = "config.properties";

	public static void main(String[] args) throws Exception {

		if (args.length < 1) { // Invalid usage
			help();
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
		} else if ("ml".equals(command)) {
			MLCommand.command(Arrays.copyOfRange(args, 1, args.length));
		} else if ("visualize".equals(command)) {
			VisualizeCommand.command(Arrays.copyOfRange(args, 1, args.length));
		} else if ("validate".equals(command)) {
			ValidateCommand.command(Arrays.copyOfRange(args, 1, args.length));
		} else if ("gen".equals(command)) {
			GenCommand.command(Arrays.copyOfRange(args, 1, args.length));
		} else if ("repair".equals(command)) {
			RepairCommand.command(Arrays.copyOfRange(args, 1, args.length));
		} else if ("classify".equals(command)) {
			ClassifyCommand.command(Arrays.copyOfRange(args, 1, args.length));
		}
		// For us
		else if ("eval".equals(command)) {
			EvalCommand.command(Arrays.copyOfRange(args, 1, args.length));
		} else {
			help();
			return;
		}
		LOGGER.info("Finished: {}", command);
	}

	private static void help() {
		System.out.println("$ java -cp=<class-path> jp.mzw.vtr.CLI dict      <subject-id>");
		System.out.println("$ java -cp=<class-path> jp.mzw.vtr.CLI cov       <subject-id>");
		System.out.println("$ java -cp=<class-path> jp.mzw.vtr.CLI cov       <subject-id> At    <commit-id>");
		System.out.println("$ java -cp=<class-path> jp.mzw.vtr.CLI cov       <subject-id> After <commit-id>");
		System.out.println("$ java -cp=<class-path> jp.mzw.vtr.CLI detect    <subject-id>");
		System.out.println("$ java -cp=<class-path> jp.mzw.vtr.CLI detect    <subject-id> At    <commit-id>");
		System.out.println("$ java -cp=<class-path> jp.mzw.vtr.CLI detect    <subject-id> After <commit-id>");
		System.out.println("$ java -cp=<class-path> jp.mzw.vtr.CLI cluster   <similarity> <cluster-method> <threshold>");
		System.out.println("$ java -cp=<class-path> jp.mzw.vtr.CLI visualize <method>");
		System.out.println("$ java -cp=<class-path> jp.mzw.vtr.CLI validate  <subject-id>");
		System.out.println("$ java -cp=<class-path> jp.mzw.vtr.CLI validate  <subject-id> At    <commit-id>");
		System.out.println("$ java -cp=<class-path> jp.mzw.vtr.CLI validate  <subject-id> After <commit-id>");
		System.out.println("$ java -cp=<class-path> jp.mzw.vtr.CLI gen       <subject-id>");
		System.out.println("$ java -cp=<class-path> jp.mzw.vtr.CLI repair    <subject-id>");
	}
}
