package jp.mzw.vtr.validate;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import jp.mzw.vtr.core.Project;
import jp.mzw.vtr.core.VtrUtils;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseException;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.ModifierSet;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.SingleMemberAnnotationExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.stmt.CatchClause;
import com.github.javaparser.ast.stmt.TryStmt;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.type.VoidType;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

public class Suggester {
	protected static Logger log = LoggerFactory.getLogger(Suggester.class);

	public static void main(String[] args) throws Exception {
//		Properties config = Utils.getConfig("vtr.properties");
//		Suggester suggester = new Suggester(config);
//		suggester.suggest(Project.make(config));
		
	}
	
//	public void suggest(Project project, List<String> tags) throws IOException {
//		log.info("Subject: " + project.getProjectName());
//
//		ArrayList<Suggestion> suggestions_static = new ArrayList<>();
//		ArrayList<Suggestion> suggestions_compile = new ArrayList<>();
//		
//		List<String> commit_list = FileUtils.readLines(new File(this.log_dir, project.getProjectName() + ".commit-list"));
//		String init_commit = commit_list.get(0);
//		for(String tag : tags) {
//			log.info("Checkout: " + tag);
//			
//			List<String> checkout_results = null;
//			try {
//				checkout_results = Utils.exec(project.getBaseDir(), Arrays.asList("git", "checkout", tag), false);
//			} catch (InterruptedException e) {
//				log.error("Exception occured: " + e.getClass());
//				continue;
//			}
//			if(checkout_results == null) {
//				log.error("Cannot checkout: " + tag);
//				continue;
//			}
//
//			File pom = new File(project.getBaseDir(), "pom.xml");
//			if(!pom.exists()) continue;
//			
//			String junit_ver = getJuitVer(pom);
//			if(junit_ver == null) continue;
//			
//			long start = System.currentTimeMillis();
//			suggestions_static.addAll(detectPatternsStaticAnalysisResults(project, tag, junit_ver)); // detect patterns from static analysis results
//			suggestions_compile.addAll(detectPatternsFromCompilationResults(project, tag, junit_ver)); // detect patterns from compilation results
//			long end = System.currentTimeMillis();
//			
//			long elapsed_time = end - start;
//			System.out.println(tag + "\t" + elapsed_time);
//			
//			System.gc();
//		}
//		try {
//			Utils.exec(project.getBaseDir(), Arrays.asList("git", "checkout", init_commit), false);
//		} catch (InterruptedException e) {
//			log.error("Exception occured: " + e.getClass());
//		}
//
//		if(0< suggestions_static.size()) storeResults(project, suggestions_static, "static");		
//		if(0 < suggestions_compile.size()) storeResults(project, suggestions_compile, "compile");
//	}
//	
//	public void suggest(Project project) throws IOException {
//		log.info("Subject: " + project.getProjectName());
//
//		ArrayList<Suggestion> suggestions_static = new ArrayList<>();
//		ArrayList<Suggestion> suggestions_compile = new ArrayList<>();
//		
//		List<String> commit_list = FileUtils.readLines(new File(this.log_dir, project.getProjectName() + ".commit-list"));
//		String init_commit = commit_list.get(0);
//		for(int i = commit_list.size(); 0 < i; i--) { // older-first
//			String commit = commit_list.get(i-1);
//			log.info("Checkout: " + commit);
//			
//			List<String> checkout_results = null;
//			try {
//				checkout_results = Utils.exec(project.getBaseDir(), Arrays.asList("git", "checkout", commit), false);
//			} catch (InterruptedException e) {
//				log.error("Exception occured: " + e.getClass());
//				continue;
//			}
//			if(checkout_results == null) {
//				log.error("Cannot checkout: " + commit);
//				continue;
//			}
//
//			File pom = new File(project.getBaseDir(), "pom.xml");
//			if(!pom.exists()) continue;
//			
//			String junit_ver = getJuitVer(pom);
//			if(junit_ver == null) continue;
//			
//			if(suggest_static) suggestions_static.addAll(detectPatternsStaticAnalysisResults(project, commit, junit_ver)); // detect patterns from static analysis results
//			if(suggest_compile) suggestions_compile.addAll(detectPatternsFromCompilationResults(project, commit, junit_ver)); // detect patterns from compilation results
//
//			System.gc();
//		}
//		try {
//			Utils.exec(project.getBaseDir(), Arrays.asList("git", "checkout", init_commit), false);
//		} catch (InterruptedException e) {
//			log.error("Exception occured: " + e.getClass());
//		}
//
//		if(0< suggestions_static.size()) storeResults(project, suggestions_static, "static");		
//		if(0 < suggestions_compile.size()) storeResults(project, suggestions_compile, "compile");
//	}
//
//	public void storeResults(Project project, List<Suggestion> suggestions, String postfix) throws IOException {
//		StringBuilder builder = new StringBuilder();
//		builder.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>").append("\n");
//		builder.append("<Suggestions>").append("\n");
//		for(Suggestion suggest : suggestions) {
//			builder.append(suggest.toXml()).append("\n");
//		}
//		builder.append("</Suggestions>").append("\n");
//		
//		File output = new File(getLogDir(), project.getProjectName()+"."+postfix+".suggestions.xml");
//		FileUtils.write(output,  builder.toString());
//		log.info("Store suggestion results: " + output.getAbsolutePath());
//	}
//
//	// Pattern 2 and 5
//	protected List<SuppressWarnings> detectPatternsFromCompilationResults(Project project, String commit, String junit_ver) {
//		ArrayList<SuppressWarnings> ret = new ArrayList<>();
//
//		List<String> results = null;
//		try {
//			Utils.exec(project.getBaseDir(), Arrays.asList(Utils.getPathToMaven(config), "clean", "compile", "dependency:copy-dependencies"));
//			results = Utils.exec(project.getBaseDir(), Arrays.asList(Utils.getPathToMaven(config), "test-compile", "-DcompilerArgument=-Xlint:all"));
//		} catch (IOException | InterruptedException e) {
//			log.error("Exception occured: " + e.getClass());
//			return ret;
//		}
//		if(results == null) return ret;
//		
//		for(String result : results) {
//			if(result.startsWith("[WARNING] ") && result.contains("Recompile with") && result.endsWith("for details.")) {
//				String[] elements = result.split(" ");
//				if(elements.length != 7) continue;
//				
//				String path_to_src_file = elements[1].replace(".java:", ".java");
//				String warning_verbose_option = elements[4];
//				String warning = warning_verbose_option.split(":")[1];
//				
//				List<String> cmds = Arrays.asList("javac", warning_verbose_option, "-cp", "target/classes:target/dependency/*:target/test-classes", path_to_src_file);
//				List<String> _results = null;
//				try {
//					_results = Utils.exec(project.getBaseDir(), cmds, false);
//				} catch (IOException | InterruptedException e) {
//					log.error("Exception occured: " + e.getClass());
//					continue;
//				}
//				if(_results == null) continue;
//				for(String _result : _results) {
//					if(_result.startsWith(path_to_src_file)) {
//						String[] _elements = _result.split(":");
//						if(_elements.length < 2) continue;
//						int lineno = Integer.parseInt(_elements[1]);
//						
//						File file = new File(path_to_src_file);
//						try {
//							CompilationUnit cu = JavaParser.parse(new FileInputStream(file));
//							MethodVisitor visitor = new MethodVisitor();
//							visitor.visit(cu, null);
//							for(MethodDeclaration method : visitor.getMethods()) {
//								if(method.getBeginLine() <= lineno && lineno <= method.getEndLine()) {
//									SuppressWarnings suggestion = new SuppressWarnings(project, commit, file, lineno, method, warning);
//									ret.add(suggestion);
//									log.info(suggestion.toXml());
//									break;
//								}
//							}
//						} catch (FileNotFoundException | ParseException e) {
//							log.error("Exception occured: " + e.getClass());
//						}
//					}
//				}
//			}
//		}
//		
//		return ret;
//	}
//	
//	// Pattern 1, 3, and 4
//	protected List<Suggestion> detectPatternsStaticAnalysisResults(Project project, String commit, String junit_ver) {
//		ArrayList<Suggestion> ret = new ArrayList<>();
//		if(junit_ver.startsWith("4")) {
//			for(File file : project.getTestFileList(project.getDefaultTestDir())) {
//				for(MethodDeclaration method : analyzeTestMethods(file)) {
//					
//					// Pattern 1
//					if(ModifierSet.isPublic(method.getModifiers()) && method.getType() instanceof VoidType && method.getName().startsWith("test")) {
//						boolean hasTestAnnot = false;
//						for(AnnotationExpr annot : method.getAnnotations()) {
//							if("Test".equals(annot.getName().getName())) {
//								hasTestAnnot = true;
//							}
//						}
//						if(!hasTestAnnot) {
//							TestCaseIdentification suggestion = new TestCaseIdentification(project, commit, file, method);
//							ret.add(suggestion);
//							log.info(suggestion.toXml());
//						}
//					}
//					
//					// Pattern 3
//					for(Node child : getChildrenNodes(method)) {
//						/// Expected exception
//						if(child instanceof TryStmt) {
//							TryStmt try_stmt = (TryStmt) child;
//							for(Node _child : getChildrenNodes(try_stmt.getTryBlock())) {
//								if(_child instanceof MethodCallExpr) {
//									MethodCallExpr method_call_expr = (MethodCallExpr) _child;
//									if("fail".equals(method_call_expr.getName())) {
//										if(try_stmt.getCatchs() != null) { // try-finally
//											for(CatchClause catch_clause : try_stmt.getCatchs()) {
//												for(Type type : catch_clause.getExcept().getTypes()) {
//													ExpectedException suggestion = new ExpectedException(project, commit, file, method, type.toString());
//													ret.add(suggestion);
//													log.info(suggestion.toXml());
//												}
//											}
//										}
//									}
//								}
//							}
//						}
//						/// Timeout
//						if(child instanceof NameExpr) {
//							NameExpr name_expr = (NameExpr) child;
//							if(name_expr.getName().contains("TIMEOUT")) {
//								FieldDeclaration field = findFieldDeclarator(name_expr, name_expr.getName());
//								if(field == null) {
//									TimeoutCandidate suggestion = new TimeoutCandidate(project, commit, file, method);
//									ret.add(suggestion);
//									log.info(suggestion.toXml());
//								} else {
//									if(ModifierSet.isStatic(field.getModifiers())) {
//										Timeout suggestion = new Timeout(project, commit, file, method);
//										ret.add(suggestion);
//										log.info(suggestion.toXml());
//									}
//								}
//							}
//						}
//					}
//					
//					// Pattern 4
//					for(Node child : getChildrenNodes(method)) {
//						if(child instanceof SingleMemberAnnotationExpr) {
//							SingleMemberAnnotationExpr single_member_annotation_expr = (SingleMemberAnnotationExpr) child;
//							if("SuppressWarnings".equals(single_member_annotation_expr.getName().toString())) {
//								Expression member_value = single_member_annotation_expr.getMemberValue();
//								if(member_value instanceof StringLiteralExpr) {
//									int lineno = single_member_annotation_expr.getBeginLine();
//									String warning = ((StringLiteralExpr) member_value).getValue();
//									
//									MoveSuppressWarnings suggestion = new MoveSuppressWarnings(project, commit, file, lineno, method, warning);
//									ret.add(suggestion);
//									log.info(suggestion.toXml());
//									
//								} else {
//									log.warn("Unknow SuppressWarnings member value: " + member_value + ", " + member_value.getClass());
//								}
//							}
//						}
//					}
//					
//				}
//			}
//		}
//		return ret;
//	}
//	
//	public FieldDeclaration findFieldDeclarator(Node node, String name) {
//		Node parent = node.getParentNode();
//		if(parent instanceof com.github.javaparser.ast.CompilationUnit) {
//			return null;
//		}
//		for(Node child : getChildrenNodes(parent)) {
//			if(child instanceof FieldDeclaration) {
//				FieldDeclaration field = (FieldDeclaration) child;
//				for(VariableDeclarator var : field.getVariables()) {
//					if(name.equals(var.getId().toString())) {
//						return field;
//					}
//				}
//			}
//		}
//		return findFieldDeclarator(parent, name);
//	}
//	
//	public VariableDeclarator getVariableDeclarator(FieldDeclaration field, String name) {
//		for(VariableDeclarator var : field.getVariables()) {
//			if(name.equals(var.getId().toString())) {
//				return var;
//			}
//		}
//		return null;
//	}
//	
//	private List<Node> getChildrenNodes(Node node) {
//		ArrayList<Node> ret = new ArrayList<>();
//		ret.add(node);
//		for(Node child : node.getChildrenNodes()) {
//			ret.addAll(getChildrenNodes(child));
//		}
//		return ret;
//	}
//	
//
//	private String getJuitVer(File pom) {
//		try {
//			String pom_content = FileUtils.readFileToString(pom);
//			Document pom_xml = Jsoup.parse(pom_content, "", Parser.xmlParser());
//			for(Element dep : pom_xml.select("project dependencies dependency")) {
//				if("junit".equals(dep.select("artifactid").text())) {
//					return dep.select("version").text();
//				}
//			}
//		} catch (IOException e) {
//			log.error("Exception occured: " + e.getClass());
//		}
//		return null;
//	}
//
//	////////////////////////////////////////////////////////////////////////////////
////	protected HashMap<File, List<MethodDeclaration>> analyzeTestMethods(Project project) {
////		HashMap<File, List<MethodDeclaration>> ret = new HashMap<>();
////		for(File file : project.getTestFileList(project.getDefaultTestDir())) {
////			try {
////				CompilationUnit cu = JavaParser.parse(new FileInputStream(file));
////				MethodVisitor visitor = new MethodVisitor();
////				visitor.visit(cu, null);
////				ret.put(file, visitor.getMethods());
////			} catch (FileNotFoundException e) {
////				log.error("Exception occured: " + e.getClass());
////			} catch (ParseException e) {
////				log.error("Exception occured: " + e.getClass());
////			}
////		}
////		return ret;
////	}
//	
//	protected List<MethodDeclaration> analyzeTestMethods(File file) {
//		try {
//			CompilationUnit cu = JavaParser.parse(new FileInputStream(file));
//			MethodVisitor visitor = new MethodVisitor();
//			visitor.visit(cu, null);
//			return visitor.getMethods();
//		} catch (FileNotFoundException e) {
//			log.error("Exception occured: " + e.getClass());
//		} catch (ParseException e) {
//			log.error("Exception occured: " + e.getClass());
//		}
//		return null;
//	}
//
//    private static class MethodVisitor extends VoidVisitorAdapter<Object> {
//    	
//    	ArrayList<MethodDeclaration> methods;
//    	public MethodVisitor() {
//    		methods = new ArrayList<>();
//    	}
//
//        @Override
//        public void visit(MethodDeclaration method, Object arg) {
//            methods.add(method);
//            super.visit(method, arg);
//        }
//        
//        public List<MethodDeclaration> getMethods() {
//        	return methods;
//        }
//    }
//
//	////////////////////////////////////////////////////////////////////////////////
//	protected Properties config;
//	public Suggester(Properties config) throws IOException {
//		this.config = config;
//		parseConfig(config);
//		parseSubjectsInfo();
//	}
//	
//	private String log_dir;
//	protected String subjects_info_xml;
//	private boolean suggest_static;
//	private boolean suggest_compile;
//	private void parseConfig(Properties config) {
//		log_dir = config.getProperty("log_dir") != null ? config.getProperty("log_dir") : "log";
//		subjects_info_xml = config.getProperty("subjects_info") != null ? config.getProperty("subjects_info") : "subjects_info.xml";
//		suggest_static = config.getProperty("suggest_static") != null ? Boolean.parseBoolean(config.getProperty("suggest_static")) : false;
//		suggest_compile = config.getProperty("suggest_compile") != null ? Boolean.parseBoolean(config.getProperty("suggest_compile")) : false;
//	}
//	
//
//	protected Document subjects_info;
//	protected void parseSubjectsInfo() throws IOException {
//		InputStream is = Suggester.class.getClassLoader().getResourceAsStream(subjects_info_xml);
//		String content = IOUtils.toString(is, Charset.defaultCharset());
//		subjects_info = Jsoup.parse(content, "", Parser.xmlParser());
//	}
//	private Element getSubjectInfo(String subjectName) {
//		for(Element e: subjects_info.select("subject name")) {
//			if(e.text().equals(subjectName)) {
//				return e.parent();
//			}
//		}
//		return null;
//	}
//	private Properties getConfig(String subjectName) {
//		Element subject_element = getSubjectInfo(subjectName);
//		Properties config = new Properties();
//		config.setProperty("path_to_project", subject_element.select("path").text());
//		config.setProperty("ref_to_compare", subject_element.select("branch").text());
//		config.setProperty("github_username", subject_element.select("github user").text());
//		config.setProperty("github_projname", subject_element.select("github project").text());
//		return config;
//	}
//
//	protected File getLogDir() {
//		return new File(log_dir);
//	}
	
}
