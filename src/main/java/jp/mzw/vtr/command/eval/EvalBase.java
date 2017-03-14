package jp.mzw.vtr.command.eval;

import java.util.ArrayList;
import java.util.List;

public class EvalBase {

	public static Subject[] subjects = {
			new Subject("commons-bcel", "BCEL"),
			new Subject("commons-beanutils", "BeanUtils"),
			new Subject("commons-bsf", "BSF"),
			new Subject("commons-chain", "Chain"),
			new Subject("commons-cli", "CLI"),
			new Subject("commons-codec", "Codec"),
//			new Subject("commons-collections", "Collections"),
			new Subject("commons-collections", "Collect."),
			new Subject("commons-compress", "Compress"),
//			new Subject("commons-configuration", "Configuration"),
			new Subject("commons-configuration", "Config."),
			new Subject("commons-crypto", "Crypto"),
			new Subject("commons-csv", "CSV"),
			new Subject("commons-dbcp", "DBCP"),
			new Subject("commons-dbutils", "DbUtils"),
			new Subject("commons-digester", "Digester"),
			new Subject("commons-discovery", "Discovery"),
			new Subject("commons-email", "Email"),
			new Subject("commons-exec", "Exec"),
			new Subject("commons-fileupload", "FileUpload"),
			new Subject("commons-functor", "Functor"),
			new Subject("commons-imaging", "Imaging"),
			new Subject("commons-io", "IO"),
			new Subject("commons-jcs", "JCS"),
			new Subject("commons-jexl", "Jexl"),
			new Subject("commons-jxpath", "JXPath"),
			new Subject("commons-lang", "Lang"),
			new Subject("commons-logging", "Logging"),
			new Subject("commons-math", "Math"),
			new Subject("commons-net", "Net"),
			new Subject("commons-pool", "Pool"),
			new Subject("commons-proxy", "Proxy"),
			new Subject("commons-rng", "RNG"),
			new Subject("commons-scxml", "SCXML"),
			new Subject("commons-validator", "Validator"),
	};
	
	public static Pattern[] patterns = {
			new Pattern("jp.mzw.vtr.validate.coding_style.AccessFilesProperly", 15),
			new Pattern("jp.mzw.vtr.validate.coding_style.AccessStaticFieldsAtDefinedSuperClass", 14),
			new Pattern("jp.mzw.vtr.validate.coding_style.AccessStaticMethodsAtDefinedSuperClass", 14),
			new Pattern("jp.mzw.vtr.validate.coding_style.AddCastToNull", 16),
			new Pattern("jp.mzw.vtr.validate.coding_style.AddExplicitBlocks", 13),
			new Pattern("jp.mzw.vtr.validate.coding_style.ConvertForLoopsToEnhanced", 13),
			new Pattern("jp.mzw.vtr.validate.coding_style.FormatCode", 13),
			new Pattern("jp.mzw.vtr.validate.coding_style.UseArithmeticAssignmentOperators", 13),
			new Pattern("jp.mzw.vtr.validate.coding_style.UseDiamondOperators", 13),
			new Pattern("jp.mzw.vtr.validate.coding_style.UseModifierFinalWherePossible", 13),
			new Pattern("jp.mzw.vtr.validate.coding_style.UseProcessWaitfor", 3),
			new Pattern("jp.mzw.vtr.validate.coding_style.UseThisIfNecessary", 13),
			new Pattern("jp.mzw.vtr.validate.exception_handling.AddFailStatementsForHandlingExpectedExceptions", 1),
			new Pattern("jp.mzw.vtr.validate.exception_handling.DoNotSwallowTestErrorsSilently", 1),
			new Pattern("jp.mzw.vtr.validate.exception_handling.HandleExpectedExecptionsProperly", 12),
			new Pattern("jp.mzw.vtr.validate.exception_handling.RemoveUnusedExceptions", 13),
			new Pattern("jp.mzw.vtr.validate.javadoc.FixJavadocErrors", 7),
			new Pattern("jp.mzw.vtr.validate.javadoc.ReplaceAtTodoWithTODO", 7),
			new Pattern("jp.mzw.vtr.validate.javadoc.UseCodeAnnotationsAtJavaDoc", 7),
			new Pattern("jp.mzw.vtr.validate.junit.AddTestAnnotations", 8),
			new Pattern("jp.mzw.vtr.validate.junit.AssertNotNullToInstances", 2),
			new Pattern("jp.mzw.vtr.validate.junit.ModifyAssertImports", 9),
			new Pattern("jp.mzw.vtr.validate.junit.SwapActualExpectedValues", 11),
			new Pattern("jp.mzw.vtr.validate.junit.UseAssertArrayEqualsProperly", 10),
			new Pattern("jp.mzw.vtr.validate.junit.UseAssertEqualsProperly", 10),
			new Pattern("jp.mzw.vtr.validate.junit.UseAssertFalseProperly", 10),
			new Pattern("jp.mzw.vtr.validate.junit.UseAssertNotSameProperly", 10),
			new Pattern("jp.mzw.vtr.validate.junit.UseAssertNullProperly", 10),
			new Pattern("jp.mzw.vtr.validate.junit.UseAssertTrueProperly", 10),
			new Pattern("jp.mzw.vtr.validate.junit.UseFailInsteadOfAssertTrueFalse", 10),
			new Pattern("jp.mzw.vtr.validate.junit.UseStringContains", 10),
			new Pattern("jp.mzw.vtr.validate.outputs.RemovePrintStatements", 6),
			new Pattern("jp.mzw.vtr.validate.outputs.suppress_warnings.AddSerialVersionUids", 13),
			new Pattern("jp.mzw.vtr.validate.outputs.suppress_warnings.DeleteUnnecessaryAssignmenedVariables", 5),
			new Pattern("jp.mzw.vtr.validate.outputs.suppress_warnings.IntroduceAutoBoxing", 5),
			new Pattern("jp.mzw.vtr.validate.outputs.suppress_warnings.RemoveUnnecessaryCasts", 5),
			new Pattern("jp.mzw.vtr.validate.outputs.suppress_warnings.add_override_annotation.AddOverrideAnnotationToMethodsInConstructors", 13),
			new Pattern("jp.mzw.vtr.validate.outputs.suppress_warnings.add_override_annotation.AddOverrideAnnotationToTestCase", 13),
			new Pattern("jp.mzw.vtr.validate.outputs.suppress_warnings.add_suppress_warnings_annotation.AddSuppressWarningsDeprecationAnnotation", 5),
			new Pattern("jp.mzw.vtr.validate.outputs.suppress_warnings.add_suppress_warnings_annotation.AddSuppressWarningsRawtypesAnnotation", 5),
			new Pattern("jp.mzw.vtr.validate.outputs.suppress_warnings.add_suppress_warnings_annotation.AddSuppressWarningsUncheckedAnnotation", 5),
			new Pattern("jp.mzw.vtr.validate.resources.CloseResources", 4),
			new Pattern("jp.mzw.vtr.validate.resources.UseTryWithResources", 4),
	};
	
	public static class Subject {
		
		String id;
		
		String name;
		
		public Subject(String id, String name) {
			this.id = id;
			this.name = name;
		}
		
		public String getId() {
			return id;
		}
		
		public String getName() {
			return name;
		}
	}
	
	public static class Pattern {
		
		int num;
		
		String name;
		
		public Pattern(String name, int num) {
			this.num = num;
			this.name = name;
		}
		
		public int getNum() {
			return num;
		}
		
		public String getName() {
			return name;
		}
		
	}
	
	public static List<Pattern> getPatternsBy(int num) {
		List<Pattern> ret = new ArrayList<>();
		for (Pattern pattern : patterns) {
			if (num == pattern.getNum()) {
				ret.add(pattern);
			}
		}
		return ret;
	}

	public static Pattern getPatternBy(String fullname) {
		for (Pattern pattern : patterns) {
			if (fullname.contains(pattern.getName())) {
				return pattern;
			}
		}
		return null;
	}
}
