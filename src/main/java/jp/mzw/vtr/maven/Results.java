package jp.mzw.vtr.maven;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.jsoup.nodes.Document;

import jp.mzw.vtr.core.VtrUtils;
import jp.mzw.vtr.git.Commit;
import jp.mzw.vtr.maven.JavadocUtils.JavadocErrorMessage;
import jp.mzw.vtr.validate.ValidatorBase;

public class Results {
	
	protected List<String> compileOutputs;
	protected List<String> compileErrors;
	protected Map<String, List<JavadocErrorMessage>> javadocErrorMessages;

	protected Results(List<String> outputs, List<String> errors) {
		this.compileOutputs = outputs;
		this.compileErrors = errors;
	}
	
	public static Results of(List<String> outputs, List<String> errors) {
		return new Results(outputs, errors);
	}

	public List<String> getCompileOutputs() {
		return compileOutputs;
	}

	public List<String> getCompileErrors() {
		return compileErrors;
	}
	
	public void setJavadocErrorMessages(Map<String, List<JavadocErrorMessage>> javadocErrorMessages) {
		this.javadocErrorMessages = javadocErrorMessages;
	}
	
	public List<JavadocErrorMessage> getJavadocErrorMessages(File projectDir, File file) {
		String filepath = VtrUtils.getFilePath(projectDir, file);
		List<JavadocErrorMessage> messages = javadocErrorMessages.get(filepath);
		if (messages == null) {
			messages = new ArrayList<JavadocErrorMessage>();
			javadocErrorMessages.put(filepath, messages);
		}
		return messages;
	}

	public static final String VALIDATE_RESULTS_DIRNAME = "results";
	public static final String COMPILE_OUTPUTS_FILENAME = "compile_outputs.txt";
	public static final String COMPILE_ERRORS_FILENAME = "compile_errors.txt";
	public static final String JAVADOC_ERRORS_FILENAME = "javadoc_error_messages.xml";
	
	public void output(File outputDir, String projectId, Commit commit) throws IOException {
		File projectDir = new File(outputDir, projectId);
		File validateDir = new File(projectDir, ValidatorBase.VALIDATOR_DIRNAME);
		File resultsDir = new File(validateDir, VALIDATE_RESULTS_DIRNAME);
		File commitDir = new File(resultsDir, commit.getId());

		File compileOutputsFile = new File(commitDir, COMPILE_OUTPUTS_FILENAME);
		FileUtils.writeLines(compileOutputsFile, compileOutputs);
		
		File compileErrorsFile = new File(commitDir, COMPILE_ERRORS_FILENAME);
		FileUtils.writeLines(compileErrorsFile, compileErrors);
		
		File javadocErrorMessagesFile = new File(commitDir, JAVADOC_ERRORS_FILENAME);
		Document javadocErrorMessagesDocument = JavadocUtils.getXMLDocument(javadocErrorMessages);
		FileUtils.write(javadocErrorMessagesFile, javadocErrorMessagesDocument.toString());
	}

}
