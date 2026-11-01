package semantic.passes;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import semantic.SemanticException;
import semantic.symbols.ClassSymbol;
import semantic.symbols.MethodSymbol;
import semantic.symbols.SymbolTable;
import semantic.symbols.Type;
import semantic.symbols.VariableSymbol;
import syntaxtree.AllocationExpression;
import syntaxtree.AndExpression;
import syntaxtree.ArrayAllocationExpression;
import syntaxtree.ArrayAssignmentStatement;
import syntaxtree.ArrayLength;
import syntaxtree.ArrayLookup;
import syntaxtree.AssignmentStatement;
import syntaxtree.Block;
import syntaxtree.BracketExpression;
import syntaxtree.ClassDeclaration;
import syntaxtree.ClassExtendsDeclaration;
import syntaxtree.Clause;
import syntaxtree.CompareExpression;
import syntaxtree.Expression;
import syntaxtree.ExpressionList;
import syntaxtree.ExpressionTerm;
import syntaxtree.FalseLiteral;
import syntaxtree.Goal;
import syntaxtree.Identifier;
import syntaxtree.IfStatement;
import syntaxtree.IntegerLiteral;
import syntaxtree.MainClass;
import syntaxtree.MessageSend;
import syntaxtree.MethodDeclaration;
import syntaxtree.MinusExpression;
import syntaxtree.Node;
import syntaxtree.NodeOptional;
import syntaxtree.NotExpression;
import syntaxtree.PlusExpression;
import syntaxtree.PrimaryExpression;
import syntaxtree.PrintStatement;
import syntaxtree.Statement;
import syntaxtree.ThisExpression;
import syntaxtree.TimesExpression;
import syntaxtree.TrueLiteral;
import syntaxtree.WhileStatement;
import visitor.GJNoArguDepthFirst;

/**
 * Pass 3 - type-checks every statement and expression.
 *
 * <p>Each expression visit returns the static {@link Type} of that expression; statement
 * visits return {@code null}. An {@link Identifier} reached through an expression is a
 * variable use and is resolved in scope order: locals, parameters, then fields of the
 * enclosing class and its ancestors (a local therefore shadows a field). Identifiers that
 * name classes or methods are read directly from their parent node and never visited.
 */
public final class TypeChecker extends GJNoArguDepthFirst<Type> {

    private static final BigInteger MAX_INT_LITERAL = BigInteger.valueOf(Integer.MAX_VALUE);

    private final SymbolTable table;
    private ClassSymbol currentClass;
    private MethodSymbol currentMethod;
    private boolean inStaticMain;

    private TypeChecker(SymbolTable table) {
        this.table = table;
    }

    public static void check(Goal program, SymbolTable table) {
        program.accept(new TypeChecker(table));
    }

    // =================================================================== declarations

    /** f0 -> MainClass() f1 -> ( TypeDeclaration() )* f2 -> &lt;EOF&gt; */
    @Override
    public Type visit(Goal n) {
        n.f0.accept(this);
        n.f1.accept(this);
        return null;
    }

    /** Only the statements of main need checking; its locals were validated in pass 2. */
    @Override
    public Type visit(MainClass n) {
        currentClass = table.mainClass();
        currentMethod = currentClass.mainMethod();
        inStaticMain = true;
        n.f15.accept(this);
        inStaticMain = false;
        currentMethod = null;
        currentClass = null;
        return null;
    }

    @Override
    public Type visit(ClassDeclaration n) {
        currentClass = table.lookupClass(n.f1.f0.tokenImage);
        n.f4.accept(this);
        currentClass = null;
        return null;
    }

    @Override
    public Type visit(ClassExtendsDeclaration n) {
        currentClass = table.lookupClass(n.f1.f0.tokenImage);
        n.f6.accept(this);
        currentClass = null;
        return null;
    }

    /**
     * f0 -> "public" f1 -> Type() f2 -> Identifier() f3 -> "(" f4 -> ( FormalParameterList() )?
     * f5 -> ")" f6 -> "{" f7 -> ( VarDeclaration() )* f8 -> ( Statement() )* f9 -> "return"
     * f10 -> Expression() f11 -> ";" f12 -> "}"
     */
    @Override
    public Type visit(MethodDeclaration n) {
        currentMethod = table.methodFor(n);
        n.f8.accept(this);
        Type returned = n.f10.accept(this);
        if (!table.isSubtype(returned, currentMethod.returnType())) {
            throw new SemanticException(n.f10, "incompatible return type in '" + currentMethod.signature()
                    + "': " + returned + " cannot be converted to " + currentMethod.returnType());
        }
        currentMethod = null;
        return null;
    }

    // =================================================================== statements

    @Override
    public Type visit(Statement n) {
        n.f0.accept(this);
        return null;
    }

    /** f0 -> "{" f1 -> ( Statement() )* f2 -> "}" */
    @Override
    public Type visit(Block n) {
        n.f1.accept(this);
        return null;
    }

    /** f0 -> Identifier() f1 -> "=" f2 -> Expression() f3 -> ";" */
    @Override
    public Type visit(AssignmentStatement n) {
        VariableSymbol target = resolveVariable(n.f0);
        Type value = n.f2.accept(this);
        if (!table.isSubtype(value, target.type())) {
            throw new SemanticException(n.f2, "incompatible types: " + value + " cannot be assigned to '"
                    + target.name() + "' of type " + target.type());
        }
        return null;
    }

    /** f0 -> Identifier() f1 -> "[" f2 -> Expression() f3 -> "]" f4 -> "=" f5 -> Expression() f6 -> ";" */
    @Override
    public Type visit(ArrayAssignmentStatement n) {
        VariableSymbol array = resolveVariable(n.f0);
        if (!array.type().equals(Type.INT_ARRAY)) {
            throw new SemanticException(n.f0, "array required, but '" + array.name() + "' has type " + array.type());
        }
        expect(n.f2, Type.INT, "array index must be int, found %s");
        expect(n.f5, Type.INT, "incompatible types: %s cannot be stored in an int[]");
        return null;
    }

    /** f0 -> "if" f1 -> "(" f2 -> Expression() f3 -> ")" f4 -> Statement() f5 -> "else" f6 -> Statement() */
    @Override
    public Type visit(IfStatement n) {
        expect(n.f2, Type.BOOLEAN, "if condition must be boolean, found %s");
        n.f4.accept(this);
        n.f6.accept(this);
        return null;
    }

    /** f0 -> "while" f1 -> "(" f2 -> Expression() f3 -> ")" f4 -> Statement() */
    @Override
    public Type visit(WhileStatement n) {
        expect(n.f2, Type.BOOLEAN, "while condition must be boolean, found %s");
        n.f4.accept(this);
        return null;
    }

    /** f0 -> "System.out.println" f1 -> "(" f2 -> Expression() f3 -> ")" f4 -> ";" */
    @Override
    public Type visit(PrintStatement n) {
        expect(n.f2, Type.INT, "System.out.println only accepts int, found %s");
        return null;
    }

    // =================================================================== expressions

    @Override
    public Type visit(Expression n) {
        return n.f0.accept(this);
    }

    @Override
    public Type visit(Clause n) {
        return n.f0.accept(this);
    }

    @Override
    public Type visit(PrimaryExpression n) {
        return n.f0.accept(this);
    }

    /** f0 -> Clause() f1 -> "&&" f2 -> Clause() */
    @Override
    public Type visit(AndExpression n) {
        expect(n.f0, Type.BOOLEAN, "bad operand type %s for operator '&&' (boolean required)");
        expect(n.f2, Type.BOOLEAN, "bad operand type %s for operator '&&' (boolean required)");
        return Type.BOOLEAN;
    }

    /** f0 -> PrimaryExpression() f1 -> "&lt;" f2 -> PrimaryExpression() */
    @Override
    public Type visit(CompareExpression n) {
        expect(n.f0, Type.INT, "bad operand type %s for operator '<' (int required)");
        expect(n.f2, Type.INT, "bad operand type %s for operator '<' (int required)");
        return Type.BOOLEAN;
    }

    @Override
    public Type visit(PlusExpression n) {
        return arithmetic(n.f0, n.f2, "+");
    }

    @Override
    public Type visit(MinusExpression n) {
        return arithmetic(n.f0, n.f2, "-");
    }

    @Override
    public Type visit(TimesExpression n) {
        return arithmetic(n.f0, n.f2, "*");
    }

    /** f0 -> PrimaryExpression() f1 -> "[" f2 -> PrimaryExpression() f3 -> "]" */
    @Override
    public Type visit(ArrayLookup n) {
        expect(n.f0, Type.INT_ARRAY, "array required, but %s found");
        expect(n.f2, Type.INT, "array index must be int, found %s");
        return Type.INT;
    }

    /** f0 -> PrimaryExpression() f1 -> "." f2 -> "length" */
    @Override
    public Type visit(ArrayLength n) {
        expect(n.f0, Type.INT_ARRAY, "'.length' requires an int[], found %s");
        return Type.INT;
    }

    /**
     * f0 -> PrimaryExpression() f1 -> "." f2 -> Identifier() f3 -> "(" f4 -> ( ExpressionList() )? f5 -> ")"
     */
    @Override
    public Type visit(MessageSend n) {
        Type receiverType = n.f0.accept(this);
        String methodName = n.f2.f0.tokenImage;
        if (!receiverType.isClass()) {
            throw new SemanticException(n.f2, "cannot invoke method '" + methodName + "' on a value of type "
                    + receiverType);
        }
        ClassSymbol receiver = table.lookupClass(receiverType.name());
        List<Type> arguments = argumentTypes(n.f4);

        MethodSymbol target = table.resolveMethod(receiver, methodName, arguments);
        if (target == null) {
            List<MethodSymbol> candidates = receiver.visibleMethodsNamed(methodName);
            if (candidates.isEmpty()) {
                throw new SemanticException(n.f2, "cannot find method '" + methodName + "' in class '"
                        + receiver.name() + "'");
            }
            StringBuilder signatures = new StringBuilder();
            for (MethodSymbol candidate : candidates) {
                signatures.append(signatures.length() == 0 ? "" : ", ").append(candidate.signature());
            }
            throw new SemanticException(n.f2, "no method '" + methodName + "' in class '" + receiver.name()
                    + "' is applicable to arguments " + describe(arguments) + "; candidates: " + signatures);
        }
        return target.returnType();
    }

    /** f0 -> &lt;INTEGER_LITERAL&gt; */
    @Override
    public Type visit(IntegerLiteral n) {
        if (new BigInteger(n.f0.tokenImage).compareTo(MAX_INT_LITERAL) > 0) {
            throw new SemanticException(n.f0, "integer number too large: " + n.f0.tokenImage);
        }
        return Type.INT;
    }

    @Override
    public Type visit(TrueLiteral n) {
        return Type.BOOLEAN;
    }

    @Override
    public Type visit(FalseLiteral n) {
        return Type.BOOLEAN;
    }

    /** An identifier in expression position is always a variable use. */
    @Override
    public Type visit(Identifier n) {
        return resolveVariable(n).type();
    }

    @Override
    public Type visit(ThisExpression n) {
        if (inStaticMain) {
            throw new SemanticException(n.f0, "'this' cannot be referenced from the static method main");
        }
        return currentClass.asType();
    }

    /** f0 -> "new" f1 -> "int" f2 -> "[" f3 -> Expression() f4 -> "]" */
    @Override
    public Type visit(ArrayAllocationExpression n) {
        expect(n.f3, Type.INT, "array size must be int, found %s");
        return Type.INT_ARRAY;
    }

    /** f0 -> "new" f1 -> Identifier() f2 -> "(" f3 -> ")" */
    @Override
    public Type visit(AllocationExpression n) {
        String className = n.f1.f0.tokenImage;
        if (!table.isDeclaredClass(className)) {
            throw new SemanticException(n.f1, "cannot find class '" + className + "'");
        }
        return Type.classType(className);
    }

    /** f0 -> "!" f1 -> Clause() */
    @Override
    public Type visit(NotExpression n) {
        expect(n.f1, Type.BOOLEAN, "bad operand type %s for operator '!' (boolean required)");
        return Type.BOOLEAN;
    }

    /** f0 -> "(" f1 -> Expression() f2 -> ")" */
    @Override
    public Type visit(BracketExpression n) {
        return n.f1.accept(this);
    }

    // =================================================================== helpers

    private Type arithmetic(Node left, Node right, String operator) {
        String message = "bad operand type %s for operator '" + operator + "' (int required)";
        expect(left, Type.INT, message);
        expect(right, Type.INT, message);
        return Type.INT;
    }

    /** Type-checks {@code expression} and requires its type to be exactly {@code expected}. */
    private void expect(Node expression, Type expected, String messageFormat) {
        Type actual = expression.accept(this);
        if (!actual.equals(expected)) {
            throw new SemanticException(expression, String.format(messageFormat, actual));
        }
    }

    /** Resolves a variable use: locals and parameters, then (outside main) inherited fields. */
    private VariableSymbol resolveVariable(Identifier id) {
        String name = id.f0.tokenImage;
        VariableSymbol variable = currentMethod.lookupLocalOrParameter(name);
        if (variable == null && !inStaticMain) {
            variable = currentClass.lookupField(name);
        }
        if (variable == null) {
            throw new SemanticException(id, "cannot find symbol: variable '" + name + "'");
        }
        return variable;
    }

    private List<Type> argumentTypes(NodeOptional optionalList) {
        List<Type> types = new ArrayList<>();
        if (optionalList.present()) {
            ExpressionList list = (ExpressionList) optionalList.node;
            types.add(list.f0.accept(this));
            for (Node term : list.f1.f0.nodes) {
                types.add(((ExpressionTerm) term).f1.accept(this));
            }
        }
        return types;
    }

    private static String describe(List<Type> types) {
        StringBuilder sb = new StringBuilder("(");
        for (int i = 0; i < types.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(types.get(i));
        }
        return sb.append(')').toString();
    }
}
