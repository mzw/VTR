package jp.mzw.vtr.validate;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import jp.mzw.vtr.core.Project;
import jp.mzw.vtr.maven.TestCase;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.fix.ConvertIterableLoopOperation;
import org.eclipse.jdt.internal.corext.fix.ConvertLoopFix;
import org.eclipse.jdt.internal.corext.fix.ConvertLoopOperation;
import org.eclipse.jdt.internal.corext.fix.CompilationUnitRewriteOperationsFix.CompilationUnitRewriteOperation;
import org.eclipse.jdt.internal.ui.util.CoreUtility;
import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.jdt.ui.cleanup.ICleanUpFix;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConvertForLoopsToEnhanced extends EclipseCleanUpValidatorBase {
	protected static Logger LOGGER = LoggerFactory.getLogger(ConvertForLoopsToEnhanced.class);

	public ConvertForLoopsToEnhanced(Project project) {
		super(project);
	}

	@Override
	protected List<ASTNode> detect(TestCase tc, CompilationUnit cu) throws CoreException {
		List<ASTNode> ret = new ArrayList<>();
		if (cu == null) {
			return ret;
		}
		ICleanUpFix fix = ConvertLoopFix.createCleanUp(cu, true, true, true);
		if (fix == null) {
			return ret;
		}
		for (CompilationUnitRewriteOperation operation : ((ConvertLoopFix) fix).getOperations()) {
			ConvertLoopOperation change = (ConvertLoopOperation) operation;
			ForStatement node = change.getForStatement();
			int start = cu.getLineNumber(node.getStartPosition());
			int end = cu.getLineNumber(node.getStartPosition() + node.getLength());
			if (tc.getStartLineNumber() <= start && end <= tc.getEndLineNumber()) {
				ret.add(node);
			}
		}
		return ret;
	}

	public static IJavaProject getJavaProject(final String name) throws CoreException {

		org.eclipse.equinox.launcher.Main launcher = new org.eclipse.equinox.launcher.Main();
		launcher.run(new String[] { "-framework", "osgi.framework" });

		IJavaProject jproject = JavaProjectHelper.createJavaProject(name, "src");;
		System.out.println(jproject);
		return jproject;
	}

	public static IPackageFragmentRoot getPackageFragmentRoot(IJavaProject jproject, String containerName, IPath[] inclusionFilters, IPath[] exclusionFilters,
			String outputLocation) throws CoreException {
		IProject project = jproject.getProject();
		IContainer container = null;
		if (containerName == null || containerName.length() == 0) {
			container = project;
		} else {
			IFolder folder = project.getFolder(containerName);
			if (!folder.exists()) {
				CoreUtility.createFolder(folder, false, true, null);
			}
			container = folder;
		}
		IPackageFragmentRoot root = jproject.getPackageFragmentRoot(container);

		IPath outputPath = null;
		if (outputLocation != null) {
			IFolder folder = project.getFolder(outputLocation);
			if (!folder.exists()) {
				CoreUtility.createFolder(folder, false, true, null);
			}
			outputPath = folder.getFullPath();
		}
		IClasspathEntry cpe = JavaCore.newSourceEntry(root.getPath(), inclusionFilters, exclusionFilters, outputPath);
		addToClasspath(jproject, cpe);
		return root;
	}

	public static void addToClasspath(IJavaProject jproject, IClasspathEntry cpe) throws JavaModelException {
		IClasspathEntry[] oldEntries = jproject.getRawClasspath();
		for (int i = 0; i < oldEntries.length; i++) {
			if (oldEntries[i].equals(cpe)) {
				return;
			}
		}
		int nEntries = oldEntries.length;
		IClasspathEntry[] newEntries = new IClasspathEntry[nEntries + 1];
		System.arraycopy(oldEntries, 0, newEntries, 0, nEntries);
		newEntries[nEntries] = cpe;
		jproject.setRawClasspath(newEntries, null);
	}

	@Override
	protected String getModified(String origin, TestCase tc) throws IOException, CoreException, MalformedTreeException, BadLocationException {
		IJavaProject jproject = getJavaProject("foo");
		IPackageFragmentRoot fSourceFolder = getPackageFragmentRoot(jproject, "src", new Path[0], new Path[0], null);
		IPackageFragment pack = fSourceFolder.createPackageFragment("bar", false, null);
		ICompilationUnit cu = pack.createCompilationUnit(tc.getTestFile().getName(), origin, false, null);

		System.out.println(tc.getTestFile().getName());

		// CompilationUnit cu = getCompilationUnit(tc.getTestFile());
		// ASTRewrite rewriter = ASTRewrite.create(cu.getAST());
		// List<ConvertLoopOperation> operations = getConvertLoopOperations(tc,
		// cu);
		// for (ConvertLoopOperation operation : operations) {
		// System.out.println("foo");
		// ForStatement forStatement = operation.getForStatement();
		// if (operation instanceof ConvertIterableLoopOperation) {
		// ConvertIterableLoopOperation op = (ConvertIterableLoopOperation)
		// operation;
		// EnhancedForStatement statement = createEnhancedForStatement(cu, op);
		// System.out.println("bar");
		// rewriter.replace(forStatement, statement, null);
		// }
		// }
		// org.eclipse.jface.text.Document document = new
		// org.eclipse.jface.text.Document(origin);
		// TextEdit edit = rewriter.rewriteAST(document, null);
		// edit.apply(document);
		// return document.get();

		return "";
	}

	private List<ConvertLoopOperation> getConvertLoopOperations(TestCase tc, CompilationUnit cu) throws CoreException {
		System.out.println("soko");
		List<ConvertLoopOperation> ret = new ArrayList<>();
		ConvertLoopFix fix = (ConvertLoopFix) ConvertLoopFix.createCleanUp(cu, true, true, true);
		if (fix == null) {
			return ret;
		}
		CompilationUnitRewriteOperation[] operations = fix.getOperations();
		System.out.println("size: " + operations.length);
		for (CompilationUnitRewriteOperation operation : operations) {
			System.out.println("doko");
			ConvertLoopOperation change = (ConvertLoopOperation) operation;
			ForStatement node = change.getForStatement();
			int start = cu.getLineNumber(node.getStartPosition());
			int end = cu.getLineNumber(node.getStartPosition() + node.getLength());
			System.out.println(node);
			if (tc.getStartLineNumber() <= start && end <= tc.getEndLineNumber()) {
				System.out.println("bura");
				ret.add(change);
			}
		}
		return ret;
	}

	private EnhancedForStatement createEnhancedForStatement(CompilationUnit cu, ConvertIterableLoopOperation op) {
		System.out.println("koko");

		AST ast = cu.getAST();
		EnhancedForStatement ret = ast.newEnhancedForStatement();

		Expression iterableExpression = op.getIterableExpression();
		IBinding iterable = op.getIterable();

		// parameter
		SingleVariableDeclaration parameter = ast.newSingleVariableDeclaration();
		// type
		SimpleName variableTypeName = ast.newSimpleName(iterable.getName());
		SimpleType variableType = ast.newSimpleType(variableTypeName);
		parameter.setType(variableType);
		// name
		String name = op.getIntroducedVariableName();
		SimpleName variableName = ast.newSimpleName(name);
		parameter.setName(variableName);
		// set
		ret.setParameter(parameter);

		System.out.println(ret.toString());

		// // expression
		// ret.setExpression(ast.newSimpleName(iterableExpression.toString()));
		//
		// // body
		// ret.setBody(op.getBody(name));
		// // System.out.println(op.getBody(name));
		//
		// System.out.println(ret.toString());

		return ret;
	}

}
