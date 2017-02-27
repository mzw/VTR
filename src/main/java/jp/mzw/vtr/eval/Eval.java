package jp.mzw.vtr.eval;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.FileUtils;

public class Eval {
	public static void main(String[] args) throws Exception {

		String path_to_file = "my/detection-results.csv";
		File file = new File(path_to_file);
		String content = FileUtils.readFileToString(file);
		CSVParser parser = CSVParser.parse(content, CSVFormat.DEFAULT);
		List<CSVRecord> records = parser.getRecords();
		// Traverse
		Map<String, Map<String, Integer>> patternsBySubject = new HashMap<>();
		Map<String, Map<String, Integer>> subjectsByPattern = new HashMap<>();
		for (int i = 1; i < records.size(); i++) { // skip header
			CSVRecord record = records.get(i);
			String subject = record.get(1);
			String pattern = record.get(13);
			// Read
			{
				Map<String, Integer> numberByPattern = patternsBySubject.get(subject);
				if (numberByPattern == null) {
					numberByPattern = new HashMap<>();
				}
				Integer number = numberByPattern.get(pattern);
				if (number == null) {
					number = new Integer(1);
				} else {
					number = new Integer(number + 1);
				}
				numberByPattern.put(pattern, number);
				patternsBySubject.put(subject, numberByPattern);
			}
			{
				Map<String, Integer> numberBySubject = subjectsByPattern.get(pattern);
				if (numberBySubject == null) {
					numberBySubject = new HashMap<>();
				}
				Integer number = numberBySubject.get(subject);
				if (number == null) {
					number = new Integer(1);
				} else {
					number = new Integer(number + 1);
				}
				numberBySubject.put(subject, number);
				subjectsByPattern.put(pattern, numberBySubject);
			}
		}
		// Print
		{
			StringBuilder builder = new StringBuilder();
			for (String subject : patternsBySubject.keySet()) {
				Map<String, Integer> patterns = patternsBySubject.get(subject);
				builder.append(subject).append("\n");
				for (String pattern : patterns.keySet()) {
					Integer number = patterns.get(pattern);
					builder.append("\t").append(pattern).append(": ").append(number).append("\n");
				}
			}
			FileUtils.write(new File(file.getParent(), "patterns_by_subject.txt"), builder.toString());
		}
		{
			StringBuilder builder = new StringBuilder();
			for (String pattern : subjectsByPattern.keySet()) {
				Map<String, Integer> subjects = subjectsByPattern.get(pattern);
				builder.append(pattern).append("\n");
				for (String subject : subjects.keySet()) {
					Integer number = subjects.get(subject);
					builder.append("\t").append(subject).append(": ").append(number).append("\n");
				}
			}
			FileUtils.write(new File(file.getParent(), "subjects_by_patterns.txt"), builder.toString());
		}
		
		

		// for paper
		{
			StringBuilder builder = new StringBuilder();
			StringBuilder tsv = new StringBuilder();
			for (int s = 0; s < subjects.length; s++) {
				String subject = subjects[s];
				String subjectName = subjectNames[s];
				builder.append(subjectName);
				tsv.append(subjectName);
				Map<String, Integer> patterns = patternsBySubject.get(subject);
				if (patterns == null) {
					for (int t = 0; t < types.length; t++) {
						builder.append(" & ").append("--");
						tsv.append("\t").append("0");
					}
					builder.append(" & -- \\\\\n");
					tsv.append("\t0\n");
					continue;
				}
				
				Map<String, Integer> m = new HashMap<>();
				for (int i = 0; i < mappedTypes.length; i++) {
					String mappedType = mappedTypes[i];
					m.put(mappedType, new Integer(0));
				}
				for (int i = 0; i < rawTypes.length; i++) {
					String rawType = rawTypes[i];
					String mappedType = mappedTypes[i];
					Integer number = patterns.get(rawType);
					if (number == null) {
						continue;
					}
					Integer n = m.get(mappedType);
					m.put(mappedType, new Integer(n + number));
				}
				

				int sum = 0;
				for (String pattern : types) {
					builder.append(" & ");
					tsv.append("\t");
					Integer number = m.get(pattern);
					if (number == null) {
						builder.append("--");
						tsv.append("0");
					} else if (number == 0) {
						builder.append("--");
						tsv.append("0");
					} else {
						sum += number;
						builder.append(number);
						tsv.append(number);
					}
				}
				if (sum == 0) {
					builder.append(" & --");
					tsv.append("\t0");
				} else {
					builder.append(" & " + sum);
					tsv.append("\t" + sum);
				}
				builder.append(" \\\\\n");
				tsv.append("\n");
			}

			FileUtils.write(new File(file.getParent(), "patterns_by_subject.tex"), builder.toString());
			FileUtils.write(new File(file.getParent(), "patterns_by_subject.tsv"), tsv.toString());
		}
	}
	
	

	static String[] subjects = {
			"commons-bcel",
			"commons-beanutils",
			"commons-bsf",
			"commons-chain",
			"commons-cli",
			"commons-codec",
			"commons-collections",
			"commons-compress",
			"commons-configuration",
			"commons-crypto",
			"commons-csv",
			"commons-dbcp",
			"commons-dbutils",
			"commons-digester",
			"commons-discovery",
			"commons-email",
			"commons-exec",
			"commons-fileupload",
			"commons-functor",
			"commons-imaging",
			"commons-io",
			"commons-jcs",
			"commons-jexl",
			"commons-jxpath",
			"commons-lang",
			"commons-logging",
			"commons-math",
			"commons-net",
			"commons-pool",
			"commons-proxy",
			"commons-rng",
			"commons-scxml",
			"commons-validator"	
	};
	static String[] subjectNames = {
			"BCEL",
			"BeanUtils",
			"BSF",
			"Chain",
			"CLI",
			"Codec",
			"Collections",
			"Compress",
			"Configuration",
			"Crypto",
			"CSV",
			"DBCP",
			"DbUtils",
			"Digester",
			"Discovery",
			"Email",
			"Exec",
			"FileUpload",
			"Functor",
			"Imaging",
			"IO",
			"JCS",
			"Jexl",
			"JXPath",
			"Lang",
			"Logging",
			"Math",
			"Net",
			"Pool",
			"Proxy",
			"RNG",
			"SCXML",
			"Validator"	
	};
	static String[] rawTypes = {
			"#1",
			"#2",
			"#3",
			"#4",
			"#5",
			"#6",
			"#7",
			"#8",
			"#9",
			"#10",
			"#11",
			"#12",
			"#13",
			"#14",
			"#15",
			"#16",
			"#17",
			"#18",
			"#19",
			"#20",
			"#21",
			"#22",
			"#23",
			"#24",
			"#25",
			"#26",
			"#27",
			"#28",
			"#29",
			"#30",
			"#31",
			"#32",
			"#33",
			"#34",
			"#35",
			"#36",
			"#37",
			"#38",
			"#39",
			"#40",
			"#L1",
			"#L2",
			"#L3",
			"#L4",
			"#L5",
			"#L6",
			"#L7",
			"#L8",
			"#L9",
			"#L10",
			"#L11",
			"#L12",
			"#L13",
			"#L14",
			"#FP1"
	};
	static String[] mappedTypes = {
			"#8",
			"#10",
			"#10",
			"#10",
			"#10",
			"#10",
			"#10",
			"#10",
			"#10",
			"#11",
			"#2",
			"#9",
			"#1",
			"#1",
			"#12",
			"#13",
			"#4",
			"#4",
			"#3",
			"#5",
			"#5",
			"#5",
			"#5",
			"#6",
			"#7",
			"#7",
			"#7",
			"#13",
			"#13",
			"#13",
			"#13",
			"#13",
			"#13",
			"#13",
			"#13",
			"#13",
			"#15",
			"#16",
			"#14",
			"#14",
			"#L1",
			"#L2",
			"#L3",
			"#L4",
			"#L5",
			"#L6",
			"#L7",
			"#L8",
			"#L9",
			"#L10",
			"#L11",
			"#L12",
			"#L13",
			"#L14",
			"#FP1"	
	};
	static String[] types = {
			"#1",
			"#2",
			"#3",
			"#4",
			"#5",
			"#6",
			"#7",
			"#8",
			"#9",
			"#10",
			"#11",
			"#12",
			"#13",
			"#14",
			"#15",
			"#16",
//			"#L1",
//			"#L2",
//			"#L3",
//			"#L4",
//			"#L5",
//			"#L6",
//			"#L7",
//			"#L8",
//			"#L9",
//			"#L10",
//			"#L11",
//			"#L12",
//			"#L13",
//			"#L14",
//			"#FP1"
	};
}
