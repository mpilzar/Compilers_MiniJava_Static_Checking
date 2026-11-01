package semantic.passes;

import java.util.HashSet;
import java.util.Set;

import semantic.SemanticException;
import semantic.SourcePosition;
import semantic.symbols.ClassSymbol;
import semantic.symbols.MethodSymbol;
import semantic.symbols.SymbolTable;
import semantic.symbols.Type;
import semantic.symbols.VariableSymbol;
import syntaxtree.ClassDeclaration;
import syntaxtree.ClassExtendsDeclaration;
import syntaxtree.FormalParameter;
import syntaxtree.Goal;
import syntaxtree.Identifier;
import syntaxtree.MainClass;
import syntaxtree.MethodDeclaration;
import syntaxtree.Node;
import syntaxtree.TypeDeclaration;
import syntaxtree.VarDeclaration;
import visitor.DepthFirstVisitor;

/**
 * Pass 1 - walks the syntax tree once and records every declaration (classes, fields,
 * methods, parameters, locals) in a fresh {@link SymbolTable}.
 *
 * <p>Errors detected here are the ones that only need local information:
 * <ul>
 *   <li>duplicate class names;</li>
 *   <li>a superclass that is not declared earlier in the file;</li>
 *   <li>duplicate field names within one class;</li>
 *   <li>duplicate parameter / local variable names within one method.</li>
 * </ul>
 * Statements and expressions are skipped entirely.
 */
public final class DeclarationCollector extends DepthFirstVisitor {

    private final SymbolTable table = new SymbolTable();
    private final Set<String> allClassNames = new HashSet<>();

    private ClassSymbol currentClass;
    private MethodSymbol currentMethod;

    private DeclarationCollector() {
    }

    public static SymbolTable collect(Goal program) {
        DeclarationCollector collector = new DeclarationCollector();
        program.accept(collector);
        return collector.table;
    }

    /**
     * f0 -> MainClass()
     * f1 -> ( TypeDeclaration() )*
     * f2 -> &lt;EOF&gt;
     */
    @Override
    public void visit(Goal n) {
        // Pre-scan class names only to give a precise message when a superclass is
        // declared *after* its subclass rather than not at all.
        allClassNames.add(n.f0.f1.f0.tokenImage);
        for (Node typeDeclaration : n.f1.nodes) {
            Node decl = ((TypeDeclaration) typeDeclaration).f0.choice;
            allClassNames.add(decl instanceof ClassDeclaration
                    ? ((ClassDeclaration) decl).f1.f0.tokenImage
                    : ((ClassExtendsDeclaration) decl).f1.f0.tokenImage);
        }
        n.f0.accept(this);
        n.f1.accept(this);
    }

    /**
     * f0 -> "class" f1 -> Identifier() f2 -> "{" f3 -> "public" f4 -> "static" f5 -> "void"
     * f6 -> "main" f7 -> "(" f8 -> "String" f9 -> "[" f10 -> "]" f11 -> Identifier() f12 -> ")"
     * f13 -> "{" f14 -> ( VarDeclaration() )* f15 -> ( Statement() )* f16 -> "}" f17 -> "}"
     */
    @Override
    public void visit(MainClass n) {
        ClassSymbol mainClass = declareClass(n.f1, null, true);

        // main is static: it is recorded so its locals can be type-checked, but it is not
        // a member of the class's v-table and takes no part in overloading.
        MethodSymbol main = new MethodSymbol("main", null, mainClass,
                SourcePosition.of(n.f6), SourcePosition.of(n.f5));
        main.addParameter(new VariableSymbol(n.f11.f0.tokenImage, Type.STRING_ARRAY,
                VariableSymbol.Kind.PARAMETER, SourcePosition.of(n.f11), SourcePosition.of(n.f8)));
        mainClass.setMainMethod(main);

        currentClass = mainClass;
        currentMethod = main;
        n.f14.accept(this);
        currentMethod = null;
        currentClass = null;
    }

    /**
     * f0 -> "class" f1 -> Identifier() f2 -> "{" f3 -> ( VarDeclaration() )*
     * f4 -> ( MethodDeclaration() )* f5 -> "}"
     */
    @Override
    public void visit(ClassDeclaration n) {
        currentClass = declareClass(n.f1, null, false);
        n.f3.accept(this);
        n.f4.accept(this);
        currentClass = null;
    }

    /**
     * f0 -> "class" f1 -> Identifier() f2 -> "extends" f3 -> Identifier() f4 -> "{"
     * f5 -> ( VarDeclaration() )* f6 -> ( MethodDeclaration() )* f7 -> "}"
     */
    @Override
    public void visit(ClassExtendsDeclaration n) {
        String name = n.f1.f0.tokenImage;
        String superName = n.f3.f0.tokenImage;
        ClassSymbol superclass = table.lookupClass(superName);
        if (superclass == null) {
            if (superName.equals(name)) {
                throw new SemanticException(n.f3, "cyclic inheritance: class '" + name + "' cannot extend itself");
            }
            if (allClassNames.contains(superName)) {
                throw new SemanticException(n.f3, "superclass '" + superName + "' must be declared before its subclass '"
                        + name + "'");
            }
            throw new SemanticException(n.f3, "cannot find superclass '" + superName + "' of class '" + name + "'");
        }
        currentClass = declareClass(n.f1, superclass, false);
        n.f5.accept(this);
        n.f6.accept(this);
        currentClass = null;
    }

    /**
     * f0 -> "public" f1 -> Type() f2 -> Identifier() f3 -> "(" f4 -> ( FormalParameterList() )?
     * f5 -> ")" f6 -> "{" f7 -> ( VarDeclaration() )* f8 -> ( Statement() )* f9 -> "return"
     * f10 -> Expression() f11 -> ";" f12 -> "}"
     */
    @Override
    public void visit(MethodDeclaration n) {
        Type returnType = TypeNodeResolver.resolve(n.f1);
        currentMethod = new MethodSymbol(n.f2.f0.tokenImage, returnType, currentClass,
                SourcePosition.of(n.f2), SourcePosition.of(n.f1));
        n.f4.accept(this);   // parameters
        n.f7.accept(this);   // local variables
        currentClass.addMethod(currentMethod);
        table.bind(n, currentMethod);
        currentMethod = null;
    }

    /** f0 -> Type() f1 -> Identifier() */
    @Override
    public void visit(FormalParameter n) {
        String name = n.f1.f0.tokenImage;
        if (currentMethod.declares(name)) {
            throw new SemanticException(n.f1, "parameter '" + name + "' is already defined in method '"
                    + currentMethod.name() + "'");
        }
        currentMethod.addParameter(new VariableSymbol(name, TypeNodeResolver.resolve(n.f0),
                VariableSymbol.Kind.PARAMETER, SourcePosition.of(n.f1), SourcePosition.of(n.f0)));
    }

    /** f0 -> Type() f1 -> Identifier() f2 -> ";" */
    @Override
    public void visit(VarDeclaration n) {
        String name = n.f1.f0.tokenImage;
        Type type = TypeNodeResolver.resolve(n.f0);
        SourcePosition namePos = SourcePosition.of(n.f1);
        SourcePosition typePos = SourcePosition.of(n.f0);

        if (currentMethod != null) {
            if (currentMethod.declares(name)) {
                throw new SemanticException(namePos, "variable '" + name + "' is already defined in method '"
                        + currentMethod.name() + "'");
            }
            currentMethod.addLocal(new VariableSymbol(name, type, VariableSymbol.Kind.LOCAL, namePos, typePos));
        } else {
            if (currentClass.declaresField(name)) {
                throw new SemanticException(namePos, "field '" + name + "' is already defined in class '"
                        + currentClass.name() + "'");
            }
            currentClass.addField(new VariableSymbol(name, type, VariableSymbol.Kind.FIELD, namePos, typePos));
        }
    }

    private ClassSymbol declareClass(Identifier id, ClassSymbol superclass, boolean isMain) {
        String name = id.f0.tokenImage;
        ClassSymbol existing = table.lookupClass(name);
        if (existing != null) {
            throw new SemanticException(id, "duplicate class: '" + name + "' is already defined at line "
                    + existing.namePosition().line());
        }
        ClassSymbol cls = new ClassSymbol(name, superclass, isMain, SourcePosition.of(id));
        table.addClass(cls);
        return cls;
    }
}
