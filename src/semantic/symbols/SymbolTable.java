package semantic.symbols;

import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import syntaxtree.Node;

/**
 * Program-wide symbol table: every class in declaration order, plus the subtyping and
 * method-resolution queries the checker needs.
 */
public final class SymbolTable {

    private final Map<String, ClassSymbol> classes = new LinkedHashMap<>();
    private final Map<Node, MethodSymbol> methodsByDeclaration = new IdentityHashMap<>();
    private ClassSymbol mainClass;

    // ------------------------------------------------------------------ population

    public void addClass(ClassSymbol cls) {
        classes.put(cls.name(), cls);
        if (cls.isMainClass()) {
            mainClass = cls;
        }
    }

    /** Remembers which symbol a method declaration node produced (methods can be overloaded). */
    public void bind(Node methodDeclaration, MethodSymbol method) {
        methodsByDeclaration.put(methodDeclaration, method);
    }

    // ------------------------------------------------------------------ lookup

    public ClassSymbol lookupClass(String name) {
        return classes.get(name);
    }

    public boolean isDeclaredClass(String name) {
        return classes.containsKey(name);
    }

    public ClassSymbol mainClass() {
        return mainClass;
    }

    /** All classes in source order, the main class first. */
    public Collection<ClassSymbol> classes() {
        return Collections.unmodifiableCollection(classes.values());
    }

    public MethodSymbol methodFor(Node methodDeclaration) {
        return methodsByDeclaration.get(methodDeclaration);
    }

    // ------------------------------------------------------------------ type relations

    /**
     * {@code sub <: sup}: identical types, or class types where {@code sub} extends
     * {@code sup} (directly or transitively). Primitive types are only subtypes of themselves.
     */
    public boolean isSubtype(Type sub, Type sup) {
        if (sub.equals(sup)) {
            return true;
        }
        if (!sub.isClass() || !sup.isClass()) {
            return false;
        }
        ClassSymbol subClass = classes.get(sub.name());
        ClassSymbol supClass = classes.get(sup.name());
        return subClass != null && supClass != null && subClass.isSubclassOf(supClass);
    }

    /** True when one type is a subtype of the other (in either direction). */
    public boolean areRelated(Type a, Type b) {
        return isSubtype(a, b) || isSubtype(b, a);
    }

    /**
     * Resolves a call {@code receiver.name(args)}: searches the receiver class, then its
     * ancestors, for a method with that name whose parameters accept the argument types.
     *
     * <p>Because MiniJava has single inheritance and overloads must differ in at least one
     * parameter position by unrelated types, at most one signature can ever be applicable;
     * searching most-derived first therefore also selects the correct override.
     *
     * @return the method, or null if no applicable method exists
     */
    public MethodSymbol resolveMethod(ClassSymbol receiver, String name, List<Type> argumentTypes) {
        for (ClassSymbol c = receiver; c != null; c = c.superclass()) {
            for (MethodSymbol candidate : c.methods()) {
                if (candidate.name().equals(name) && accepts(candidate, argumentTypes)) {
                    return candidate;
                }
            }
        }
        return null;
    }

    private boolean accepts(MethodSymbol method, List<Type> argumentTypes) {
        if (method.arity() != argumentTypes.size()) {
            return false;
        }
        for (int i = 0; i < argumentTypes.size(); i++) {
            if (!isSubtype(argumentTypes.get(i), method.parameterType(i))) {
                return false;
            }
        }
        return true;
    }
}
