package jp.mzw.vtr.validate;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;

import com.puppycrawl.tools.checkstyle.Checker;
import com.puppycrawl.tools.checkstyle.ConfigurationLoader;
import com.puppycrawl.tools.checkstyle.PropertiesExpander;
import com.puppycrawl.tools.checkstyle.api.AuditEvent;
import com.puppycrawl.tools.checkstyle.api.CheckstyleException;
import com.puppycrawl.tools.checkstyle.api.Configuration;

import jp.mzw.vtr.core.Project;
import jp.mzw.vtr.git.Commit;
import jp.mzw.vtr.maven.MavenUtils;
import jp.mzw.vtr.maven.TestCase;
import jp.mzw.vtr.maven.TestSuite;

public class FormatCode extends ValidatorBase {
	protected static Logger LOGGER = LoggerFactory.getLogger(FormatCode.class);

	public FormatCode(Project project) {
		super(project);
	}

	@Override
	public void onCheckout(Commit commit) {
		try {
			for (TestSuite ts : MavenUtils.getTestSuites(this.projectDir)) {
				for (TestCase tc : ts.getTestCases()) {
					if (this.dupulicates.contains(tc.getFullName())) {
						continue;
					}
					try {
						// Instantiate checker
						final Checker checker = new Checker();
						checker.setModuleClassLoader(Checker.class.getClassLoader());
						// Config for checker
						Properties properties = new Properties();
						properties.put("checkstyle.cache.file", this.getPathToCheckStyleCacheFile().getAbsolutePath());
						// Set config to checker
						InputSource configSource = new InputSource(this.getClass().getClassLoader().getResourceAsStream(FormatCode.CHECKSTYPE));
						PropertiesExpander overridePropsResolver = new PropertiesExpander(properties);
						Configuration config = ConfigurationLoader.loadConfiguration(configSource, overridePropsResolver, true);
						checker.configure(config);
						// Set stream to get results
						CheckstyleListener listener = new CheckstyleListener();
						checker.addListener(listener);
						// Invoke checker
						List<File> files = new ArrayList<>();
						files.add(tc.getTestFile());
						checker.process(files);
						// Get results
						for (AuditEvent error : listener.getErrors()) {
							if (tc.getStartLineNumber() <= error.getLine() && error.getLine() <= tc.getEndLineNumber()) {
								this.dupulicates.add(tc.getFullName());
								ValidationResult vr = new ValidationResult(this.projectId, commit, tc, error.getLine(), error.getLine(), this);
								this.validationResultList.add(vr);
							}
						}
						// Finalize
						checker.destroy();
					} catch (CheckstyleException e) {
						LOGGER.warn("Failed to invoke Checkstyle: {}", e.getMessage());
					}
				}
			}
		} catch (IOException e) {
			LOGGER.warn("Failed to checkout: {}", commit.getId());
		}
	}

	public static final String CHECKSTYPE = "jp/mzw/vtr/validate/FormatCode.xml";

	protected File getPathToCheckStyleCacheFile() {
		File targetDir = new File(this.projectDir, "target");
		File classesDir = new File(targetDir, "classes");
		return new File(classesDir, "checkstyle.cache");
	}

	@Override
	public void generate(ValidationResult result) {
		// TODO implement
	}

}
