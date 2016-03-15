package jp.mzw.vtr.maven;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.MethodDeclaration;

public class AllMethodFindVisitor extends ASTVisitor {

	private List<MethodDeclaration> methods;

	public AllMethodFindVisitor() {
		this.methods = new ArrayList<MethodDeclaration>();
	}

	@Override
	public boolean visit(MethodDeclaration node) {
		if(!this.methods.contains(node)) {
			this.methods.add(node);
		}
		return false;
	}

	public List<MethodDeclaration> getFoundMethods() {
		return this.methods;
	}
}