package jp.mzw.vtr.validate;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import jp.mzw.vtr.core.Project;
import jp.mzw.vtr.maven.TestCase;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CloseResources extends SimpleValidatorBase {
	protected Logger LOGGER = LoggerFactory.getLogger(CloseResources.class);

	public CloseResources(Project project) {
		super(project);
	}

	@Override
	protected List<ASTNode> detect(final TestCase tc) throws IOException, MalformedTreeException, BadLocationException {
		final List<ASTNode> ret = new ArrayList<>();
		// Closable
		final List<SimpleName> closables = new ArrayList<>();
		tc.getMethodDeclaration().accept(new ASTVisitor() {
			@Override
			public boolean visit(VariableDeclarationStatement node) {
				for (Object object : node.fragments()) {
					VariableDeclarationFragment fragment = (VariableDeclarationFragment) object;
					if (ValidatorUtils.isClosable(fragment.getInitializer())) {
						closables.add(fragment.getName());
					}
				}
				return super.visit(node);
			}
		});
		// Closed?
		final List<Expression> closes = new ArrayList<>();
		tc.getMethodDeclaration().accept(new ASTVisitor() {
			@Override
			public boolean visit(TryStatement node) {
				for (Object resource : node.resources()) {
					if (resource instanceof Expression) {
						closes.add((Expression) resource);
					} else {
						// TODO will implement or nothing
						System.out.println("Unknow class at resource of try-with-resource: " + resource.getClass());
					}
				}
				return super.visit(node);
			}
			@Override
			public boolean visit(MethodInvocation node) {
				if (!"close".equals(node.getName().toString())) {
					return super.visit(node);
				}
				closes.add(node.getExpression());
				return super.visit(node);
			}
		});
		// Not closed
		for (SimpleName closable : closables) {
			boolean closed = false;
			for (Expression close : closes) {
				if (closable.toString().equals(close.toString())) {
					closed = true;
					break;
				}
			}
			if (!closed) {
				ret.add(closable);
			}
		}
		return ret;
	}

	@Override
	protected String getModified(String origin, TestCase tc) throws IOException, MalformedTreeException, BadLocationException {
		List<ASTNode> detects = detect(tc);
		if (detects.isEmpty()) {
			return origin;
		}
		AST ast = detects.get(0).getAST();
		ASTRewrite rewrite = ASTRewrite.create(ast);
		for (ASTNode detect : detects) {
			Block block = ValidatorUtils.getNearestParentBlock(detect);
			ListRewrite listRewrite = rewrite.getListRewrite(block, Block.STATEMENTS_PROPERTY);
			listRewrite.insertLast(rewrite.createStringPlaceholder(detect.toString() + ".close();", ASTNode.METHOD_INVOCATION), null);
		}
		// modify
		Document document = new Document(origin);
		TextEdit edit = rewrite.rewriteAST(document, null);
		edit.apply(document);
		return document.get();
	}
}
