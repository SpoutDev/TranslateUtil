package org.spout.translationutil;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.spout.api.lang.Translation;

import japa.parser.ast.CompilationUnit;
import japa.parser.ast.ImportDeclaration;
import japa.parser.ast.expr.Expression;
import japa.parser.ast.expr.MethodCallExpr;
import japa.parser.ast.expr.NameExpr;
import japa.parser.ast.expr.StringLiteralExpr;
import japa.parser.ast.visitor.VoidVisitorAdapter;

public class TranslationUsageVisitor extends VoidVisitorAdapter {
	private static Set<String> methods = new HashSet<String>();
	private static String TRANSLATION_SIMPLE_CLASS_NAME = Translation.class.getSimpleName();
	private static String TRANSLATION_CLASS_NAME = Translation.class.getCanonicalName();
	private CompilationUnit cu;
	private List<Occurence> occurences = new LinkedList<Occurence>();
	
	public TranslationUsageVisitor(CompilationUnit cu) {
		this.cu = cu;
	}
	
	
	static {
		for (Method m:Translation.class.getMethods()) {
			if (Modifier.isStatic(m.getModifiers()) && Modifier.isPublic(m.getModifiers()) && !Modifier.isNative(m.getModifiers())) {
				methods.add(m.getName());
			}
		}
	}
	
	@Override
	public void visit(MethodCallExpr method, Object arg1) {
		Expression scope = method.getScope();
		if (methods.contains(method.getName())) {
			if (scope instanceof NameExpr) { // Has most likely been called as Translation.<method>
				NameExpr nameexpr = (NameExpr) scope;
				if (nameexpr.getName().equals(TRANSLATION_SIMPLE_CLASS_NAME)) { // Match by Translation
					for (ImportDeclaration imp : cu.getImports()) {
						if (imp.getName().toString().equals(TRANSLATION_CLASS_NAME)) { // found import of by org.spout.api.lang.Translation
							// this is an occurence
							addOccurence(method);
							break;
						}
					}
				} else if (nameexpr.getName().equals(TRANSLATION_CLASS_NAME)) { // Match by org.spout.api.lang.Translation
					addOccurence(method);
				}
			} else if (scope == null) { // This method was most likely statically imported
				String staticImport = TRANSLATION_CLASS_NAME + "." + method.getName();
				for (ImportDeclaration imp : cu.getImports()) {
					if (imp.getName().toString().equals(staticImport)) { // found import of by org.spout.api.lang.Translation.<method>
						// this is an occurence
						addOccurence(method);
						break;
					}
				}
			}
		}
	}
	
	private void addOccurence(MethodCallExpr method) {
		List<Expression> arguments = method.getArgs();
		if (arguments.size() >= 1 && arguments.get(0) instanceof StringLiteralExpr) {
			StringLiteralExpr string = (StringLiteralExpr) arguments.get(0);
			String value = string.getValue();
			Occurence occurence = new Occurence(method.getBeginLine(), method.getBeginColumn(), value);
			occurences.add(occurence);
		}
	}

	public List<Occurence> getOccurences() {
		return occurences;
	}
}
