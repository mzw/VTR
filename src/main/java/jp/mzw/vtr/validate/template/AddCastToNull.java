package jp.mzw.vtr.validate.template;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import jp.mzw.vtr.core.Project;
import jp.mzw.vtr.maven.TestCase;
import jp.mzw.vtr.validate.SimpleValidatorBase;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CastExpression;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AddCastToNull extends SimpleValidatorBase {
	protected static Logger LOGGER = LoggerFactory.getLogger(AddCastToNull.class);
	
	public AddCastToNull(Project project) {
		super(project);
	}

	@Override
	protected List<ASTNode> detect(TestCase tc) throws IOException, MalformedTreeException, BadLocationException {
		final List<ASTNode> ret = new ArrayList<>();
		final List<MethodInvocation> targets = new ArrayList<>();
		
		tc.getMethodDeclaration().accept(new ASTVisitor() {
			@Override
			public boolean visit(MethodInvocation node) {
				targets.add(node);
				return super.visit(node);
			}
		});
		
		for (MethodInvocation target: targets) {
			if (isTargetMethod(target)) {
				ret.add(target);
			}
		}
		return ret;
	}
	
	private boolean isNullLiteral(Object obj) {
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof ASTNode)) {
			return false;
		}
		ASTNode node = (ASTNode) obj;
		return node.getNodeType() == ASTNode.NULL_LITERAL;
	}
	
	private boolean isTargetMethod (MethodInvocation method) {
		final List<ASTNode> targets = new ArrayList<>();
		// 引数がnullかチェック
		for (Object argument: method.arguments()) {
			if (!isNullLiteral(argument)) {
				return false;
			}
		}
		// 引数の方がprimitiveじゃないかチェック
		if (method.getExpression() == null) return false; // FIXME: 時々，ぬるぽが起きるのでnullチェック．
		ITypeBinding binding = method.getExpression().resolveTypeBinding();
		if (binding == null) return false; // FIXME: 時々，ぬるぽが起きるのでnullチェック．
		for (IMethodBinding methodBinding: binding.getDeclaredMethods()) {
			// メソッド名および引数の数が同じ
			// FIXME: 引数の型は見ていない
			if (methodBinding.getName().toString().equals(method.getName().toString())
					&& method.arguments().size() == methodBinding.getParameterTypes().length) {
				for (ITypeBinding argument: methodBinding.getParameterTypes()) {
					if (!argument.isPrimitive()) {
						return true;
					}
				}
			}
		}
		return false;
	}

	@Override
	protected String getModified(String origin, TestCase tc) throws IOException, MalformedTreeException, BadLocationException {
		// prepare
		CompilationUnit cu = tc.getCompilationUnit();
		AST ast = cu.getAST();
		ASTRewrite rewrite = ASTRewrite.create(ast);
		// detect
		for (ASTNode node: detect(tc)) {
			MethodInvocation target = (MethodInvocation) node;
			MethodInvocation replace = (MethodInvocation) ASTNode.copySubtree(ast, target);
			// 引数を一旦すべて削除
			replace.arguments().clear();
			ITypeBinding binding = target.getExpression().resolveTypeBinding();
			if (binding == null) break; // FIXME: 時々，ぬるぽが起きるのでnullチェック．
			for (IMethodBinding methodBinding: binding.getDeclaredMethods()) {
				// メソッド名および引数の数が同じ
				// FIXME: 引数の型は見ていない
				if (methodBinding.getName().toString().equals(target.getName().toString())
						&& target.arguments().size() == methodBinding.getParameterTypes().length) {
					List<Object> inputArguments = target.arguments();
					ITypeBinding[] declaredArguments = methodBinding.getParameterTypes();
					// 引数を設定していく
					for (int i = 0; i < inputArguments.size(); i++) {
						if ((isNullLiteral(inputArguments.get(i))) &&(!declaredArguments[i].isPrimitive())) {
							CastExpression cast = ast.newCastExpression();
							cast.setType(ast.newSimpleType(ast.newSimpleName(declaredArguments[i].getName())));
							cast.setExpression(ast.newNullLiteral());
							replace.arguments().add(cast);
						} else {
							replace.arguments().add(ASTNode.copySubtree(ast, (ASTNode) inputArguments.get(i)));
						}
					}
				}
			}
			rewrite.replace(target, replace, null);
		}
		//modify
		Document document = new Document(origin);
		TextEdit edit = rewrite.rewriteAST(document, null);
		edit.apply(document);
		return document.get();
	}

}
