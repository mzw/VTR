package jp.mzw.vtr.validate.coding_style;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import jp.mzw.vtr.core.Project;
import jp.mzw.vtr.git.Commit;
import jp.mzw.vtr.maven.Results;
import jp.mzw.vtr.maven.TestCase;
import jp.mzw.vtr.validate.SimpleValidatorBase;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.ArrayType;
import org.eclipse.jdt.core.dom.CastExpression;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.Type;
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
	protected List<ASTNode> detect(Commit commit, TestCase tc, Results results) throws IOException, MalformedTreeException, BadLocationException {
		final List<ASTNode> ret = new ArrayList<>();
		tc.getMethodDeclaration().accept(new ASTVisitor() {
			@Override
			public boolean visit(MethodInvocation node) {
				if (isTargetMethod(node)) {
					ret.add(node);
				}
				return super.visit(node);
			}
		});
		return ret;
	}

	public static boolean isNull(Object object) {
		if (object == null) {
			return false;
		}
		if (!(object instanceof ASTNode)) {
			return false;
		}
		ASTNode node = (ASTNode) object;
		if (node.getNodeType() == ASTNode.NULL_LITERAL) {
			return true;
		}
		return false;
	}

	public static boolean hasNullArgument(MethodInvocation method) {
		boolean hasNull = false;
		for (Object argument : method.arguments()) {
			if (isNull(argument)) {
				hasNull = true;
				break;
			}
		}
		return hasNull;
	}

	public static boolean checkNullExpression(MethodInvocation method) {
		if (method.getExpression() == null) {
			return true;
		}
		ITypeBinding binding = method.getExpression().resolveTypeBinding();
		if (binding == null) {
			return true;
		}
		return false;
	}

	public static List<IMethodBinding> getMethodHavingSameNameAndArguments(MethodInvocation method) {
		List<IMethodBinding> ret = new ArrayList<>();
		ITypeBinding binding = method.getExpression().resolveTypeBinding();
		for (IMethodBinding methodBinding : binding.getDeclaredMethods()) {
			// Not same method name
			if (!methodBinding.getName().toString().equals(method.getName().toString())) {
				continue;
			}
			// Not same method argument length
			if (method.arguments().size() != methodBinding.getParameterTypes().length) {
				continue;
			}
			// FIXME: Need to check argument 'types'
			ret.add(methodBinding);
		}
		return ret;
	}

	private static boolean isTargetMethod(MethodInvocation method) {
		if (!hasNullArgument(method)) {
			return false;
		}
		if (checkNullExpression(method)) {
			return false;
		}
		List<IMethodBinding> methodBindings = getMethodHavingSameNameAndArguments(method);
		for (IMethodBinding methodBinding : methodBindings) {
			for (ITypeBinding argument : methodBinding.getParameterTypes()) {
				if (!argument.isPrimitive()) {
					return true;
				}
			}
		}
		return false;
	}

	@SuppressWarnings("unchecked")
	@Override
	protected String getModified(String origin, Commit commit, TestCase tc, Results results) throws IOException, MalformedTreeException, BadLocationException {
		// prepare
		CompilationUnit cu = tc.getCompilationUnit();
		AST ast = cu.getAST();
		ASTRewrite rewrite = ASTRewrite.create(ast);
		// detect
		for (ASTNode node : detect(commit, tc, results)) {
			MethodInvocation target = (MethodInvocation) node;
			MethodInvocation replace = (MethodInvocation) ASTNode.copySubtree(ast, target);
			// Clear arguments
			replace.arguments().clear();
			// Set arguments
			List<Object> targetArguments = target.arguments();
			for (IMethodBinding methodBinding : getMethodHavingSameNameAndArguments(target)) {
				ITypeBinding[] declaredArguments = methodBinding.getParameterTypes();
				for (int i = 0; i < targetArguments.size(); i++) {
					Object targetArgument = targetArguments.get(i);
					ITypeBinding declaredArgument = declaredArguments[i];
					// Determine whether this argument should be casted
					if ((isNull(targetArgument)) && (!declaredArgument.isPrimitive())) {
						Type type = null;
						if (declaredArgument.isArray()) {
							ITypeBinding elementType = declaredArgument.getElementType();
							Type newType = null;
							if (elementType.isPrimitive()) {
								newType = ast.newPrimitiveType(PrimitiveType.toCode(elementType.getName()));
							} else if (elementType.isGenericType()) {
								System.out.println("soko");
								LOGGER.info("Generic type is limited: {}", elementType);
								return origin;
							} else if (elementType.isParameterizedType()) {
								Type simpleType = ast.newSimpleType(ast.newName(elementType.getErasure().getName()));
								ParameterizedType parameterizedType = ast.newParameterizedType(simpleType);
								for (ITypeBinding typeArgument : elementType.getTypeArguments()) {
									Type typeParameter = ast.newSimpleType(ast.newSimpleName(typeArgument.getErasure().getName()));
									parameterizedType.typeArguments().add(typeParameter);
								}
								newType = parameterizedType;
							} else {
								try {
									newType = ast.newSimpleType(ast.newName(elementType.getName()));
								} catch (IllegalArgumentException e) {
									// TODO for debugging
									System.out.println(e.getMessage());
									System.out.println(elementType);
									System.out.println("\t" + elementType.isAnnotation());
									System.out.println("\t" + elementType.isAnonymous());
									System.out.println("\t" + elementType.isArray());
									System.out.println("\t" + elementType.isCapture());
									System.out.println("\t" + elementType.isClass());
									System.out.println("\t" + elementType.isDeprecated());
									System.out.println("\t" + elementType.isEnum());
									System.out.println("\t" + elementType.isFromSource());
									System.out.println("\t" + elementType.isGenericType());
									System.out.println("\t" + elementType.isInterface());
									System.out.println("\t" + elementType.isIntersectionType());
									System.out.println("\t" + elementType.isLocal());
									System.out.println("\t" + elementType.isMember());
									System.out.println("\t" + elementType.isNested());
									System.out.println("\t" + elementType.isNullType());
									System.out.println("\t" + elementType.isParameterizedType());
									System.out.println("\t" + elementType.isRawType());
									System.out.println("\t" + elementType.isRecovered());
									System.out.println("\t" + elementType.isSynthetic());
									System.out.println("\t" + elementType.isTopLevel());
									System.out.println("\t" + elementType.isTypeVariable());
									System.out.println("\t" + elementType.isUpperbound());
									System.out.println("\t" + elementType.isWildcardType());
								}
							}
							ArrayType arrayType = ast.newArrayType(newType);
							for (int d = 1; d < declaredArgument.getDimensions(); d++) {
								arrayType.dimensions().add(ast.newDimension());
							}
							type = arrayType;
						} else if(declaredArgument.isParameterizedType()) {
							Type simpleType = ast.newSimpleType(ast.newName(declaredArgument.getErasure().getName()));
							ParameterizedType parameterizedType = ast.newParameterizedType(simpleType);
							for (ITypeBinding typeArgument : declaredArgument.getTypeArguments()) {
								Type typeParameter = ast.newSimpleType(ast.newSimpleName(typeArgument.getErasure().getName()));
								parameterizedType.typeArguments().add(typeParameter);
							}
							type = parameterizedType;
						} else if(declaredArgument.isTypeVariable()) {
							LOGGER.info("Type variable is limited: {}", declaredArgument);
							return origin;
						} else {
							type = ast.newSimpleType(ast.newSimpleName(declaredArgument.getName()));
						}
						if (type != null) {
							CastExpression cast = ast.newCastExpression();
							cast.setType(type);
							cast.setExpression(ast.newNullLiteral());
							replace.arguments().add(cast);
						} else {
							System.out.println("Unknown cast type: " + declaredArgument.getName());
							if(declaredArgument.isGenericType()) {
								System.out.println("\t" + "generic");
							} else if(declaredArgument.isWildcardType()) {
								System.out.println("\t" + "wildcard");
							} else if(declaredArgument.isAnonymous()) {
								System.out.println("\t" + "anonymous");
							} else if(declaredArgument.isRawType()) {
								System.out.println("\t" + "raw");
							} else {
								System.out.println("\t" + "unknown");
							}
						}
					} else {
						replace.arguments().add(ASTNode.copySubtree(ast, (ASTNode) targetArguments.get(i)));
					}
				}
			}
			rewrite.replace(target, replace, null);
		}
		// modify
		Document document = new Document(origin);
		TextEdit edit = rewrite.rewriteAST(document, null);
		edit.apply(document);
		return document.get();
	}
}
