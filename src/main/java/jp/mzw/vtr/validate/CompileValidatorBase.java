package jp.mzw.vtr.validate;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.maven.shared.invoker.MavenInvocationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jp.mzw.vtr.core.Project;
import jp.mzw.vtr.git.Commit;
import jp.mzw.vtr.maven.MavenUtils;

abstract public class CompileValidatorBase extends SimpleValidatorBase {
	protected Logger LOGGER = LoggerFactory.getLogger(CompileValidatorBase.class);
	
	protected static List<String> compileOutputResults = null;
	protected static List<String> compileErrorResults = null;

	public CompileValidatorBase(Project project) {
		super(project);
		compileOutputResults = null;
		compileErrorResults = null;
	}

	@Override
	public void beforeCheckout(Commit commit) {
		try {
			if (compileOutputResults == null || compileErrorResults == null) {
				List<String> goals = Arrays.asList("clean", "test-compile", "-Dmaven.compiler.showDeprecation=true", "-Dmaven.compiler.showWarnings=true");
				Pair<List<String>, List<String>> results = MavenUtils.maven(projectDir, goals, mavenHome);
				compileOutputResults = results.getRight();
				compileErrorResults = results.getLeft();
			}
		} catch (MavenInvocationException e) {
			LOGGER.warn("Failed to compile: {}", e.getMessage());
		}
	}

	@Override
	public void afterCheckout(Commit commit) {
		compileOutputResults = null;
		compileErrorResults = null;
	}

	protected static List<String> getCompileOutputResults() {
		return compileOutputResults;
	}
	
	protected static List<String> getCompileErrorResults() {
		return compileErrorResults;
	}
}
