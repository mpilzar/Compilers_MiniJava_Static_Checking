package semantic.passes;

import java.util.List;

import semantic.SemanticException;
import semantic.SourcePosition;
import semantic.symbols.ClassSymbol;
import semantic.symbols.MethodSymbol;
import semantic.symbols.SymbolTable;
import semantic.symbols.Type;
import semantic.symbols.VariableSymbol;

/**
 * Pass 2 - validates declarations against the complete symbol table, then computes the
 * memory layout of every class.
 *
 * <p>This runs after collection because both checks need whole-program knowledge: a field
 * or parameter may name a class declared further down the file, and deciding whether two
 * overloads are distinguishable requires the full class hierarchy.
 *
 * <ul>
 *   <li>Every type used in a declaration must be {@code int}, {@code boolean},
 *       {@code int[]} or a declared class.</li>
 *   <li>Overloading / overriding (checked against the same class and all ancestors):
 *     <ul>
 *       <li>different parameter counts: always allowed;</li>
 *       <li>same count, and some position has unrelated types: allowed (distinct overload);</li>
 *       <li>identical parameter types in an ancestor: an override, whose return type must
 *           be identical to the overridden method's;</li>
 *       <li>anything else (identical in the same class, or all positions related by
 *           subtyping): error.</li>
 *     </ul>
 *   </li>
 * </ul>
 */
public final class DeclarationValidator {

    private final SymbolTable table;

    private DeclarationValidator(SymbolTable table) {
        this.table = table;
    }

    public static void validate(SymbolTable table) {
        new DeclarationValidator(table).run();
    }

    private void run() {
        // Classes are visited in source order, so every superclass is validated and laid out
        // before any of its subclasses.
        for (ClassSymbol cls : table.classes()) {
            checkDeclaredTypes(cls);
            checkOverloadsAndOverrides(cls);
            cls.computeLayout();
        }
    }

    // ------------------------------------------------------------------ declared types

    private void checkDeclaredTypes(ClassSymbol cls) {
        for (VariableSymbol field : cls.fields()) {
            requireKnownType(field.type(), field.typePosition());
        }
        if (cls.mainMethod() != null) {
            checkVariables(cls.mainMethod().locals());
        }
        for (MethodSymbol method : cls.methods()) {
            requireKnownType(method.returnType(), method.returnTypePosition());
            checkVariables(method.parameters());
            checkVariables(method.locals());
        }
    }

    private void checkVariables(Iterable<VariableSymbol> variables) {
        for (VariableSymbol v : variables) {
            requireKnownType(v.type(), v.typePosition());
        }
    }

    private void requireKnownType(Type type, SourcePosition where) {
        if (type.isClass() && !table.isDeclaredClass(type.name())) {
            throw new SemanticException(where, "cannot find class '" + type.name() + "'");
        }
    }

    // ------------------------------------------------------------------ overloading rules

    private void checkOverloadsAndOverrides(ClassSymbol cls) {
        List<MethodSymbol> methods = cls.methods();
        for (int i = 0; i < methods.size(); i++) {
            MethodSymbol method = methods.get(i);

            // Against earlier methods of the same class.
            for (int j = 0; j < i; j++) {
                checkPair(method, methods.get(j));
            }
            // Against every method inherited from ancestors, nearest ancestor first, so the
            // first identical signature found is the one this method directly overrides.
            for (ClassSymbol ancestor = cls.superclass(); ancestor != null; ancestor = ancestor.superclass()) {
                for (MethodSymbol inherited : ancestor.methods()) {
                    checkPair(method, inherited);
                }
            }
        }
    }

    /** Checks {@code method} (the later declaration) against an earlier/inherited one. */
    private void checkPair(MethodSymbol method, MethodSymbol other) {
        if (!method.name().equals(other.name()) || method.arity() != other.arity()) {
            return;
        }

        boolean identical = true;
        boolean everyPositionRelated = true;
        for (int i = 0; i < method.arity(); i++) {
            Type a = method.parameterType(i);
            Type b = other.parameterType(i);
            if (!a.equals(b)) {
                identical = false;
            }
            if (!table.areRelated(a, b)) {
                everyPositionRelated = false;
            }
        }

        if (!everyPositionRelated) {
            return; // some argument position tells the two apart: a legal overload
        }

        boolean sameClass = method.owner() == other.owner();
        if (identical && !sameClass) {
            // Override of an inherited method.
            if (!method.returnType().equals(other.returnType())) {
                throw new SemanticException(method.returnTypePosition(),
                        "'" + method.signature() + "' in class '" + method.owner().name()
                        + "' cannot override '" + other.signature() + "' in class '" + other.owner().name()
                        + "': return type " + method.returnType() + " differs from " + other.returnType());
            }
            if (!method.isOverride()) {
                method.setOverridden(other);
            }
            return;
        }

        if (identical) {
            throw new SemanticException(method.namePosition(),
                    "method '" + method.signature() + "' is already defined in class '"
                    + method.owner().name() + "' (line " + other.namePosition().line() + ")");
        }

        String where = sameClass ? "" : " inherited from class '" + other.owner().name() + "'";
        throw new SemanticException(method.namePosition(),
                "ambiguous overload: '" + method.signature() + "' in class '" + method.owner().name()
                + "' cannot be distinguished from '" + other.signature() + "'" + where
                + " (line " + other.namePosition().line() + "); every parameter type is related by subtyping");
    }
}
