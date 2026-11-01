package semantic.symbols;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import semantic.SourcePosition;

/** A method: its signature, parameters, local variables and v-table slot. */
public final class MethodSymbol {

    private final String name;
    private final Type returnType;
    private final ClassSymbol owner;
    private final SourcePosition namePosition;
    private final SourcePosition returnTypePosition;

    private final List<VariableSymbol> parameters = new ArrayList<>();
    private final Map<String, VariableSymbol> parametersByName = new LinkedHashMap<>();
    private final Map<String, VariableSymbol> locals = new LinkedHashMap<>();

    /** The inherited method this one overrides, or null if it introduces a new v-table slot. */
    private MethodSymbol overridden;

    /** Byte offset of this method's slot in the class v-table; -1 until layout runs. */
    private int offset = -1;

    public MethodSymbol(String name, Type returnType, ClassSymbol owner,
                        SourcePosition namePosition, SourcePosition returnTypePosition) {
        this.name = name;
        this.returnType = returnType;
        this.owner = owner;
        this.namePosition = namePosition;
        this.returnTypePosition = returnTypePosition;
    }

    public String name() {
        return name;
    }

    public Type returnType() {
        return returnType;
    }

    public ClassSymbol owner() {
        return owner;
    }

    public SourcePosition namePosition() {
        return namePosition;
    }

    public SourcePosition returnTypePosition() {
        return returnTypePosition;
    }

    /** Returns true if a parameter or local with this name already exists. */
    public boolean declares(String variableName) {
        return parametersByName.containsKey(variableName) || locals.containsKey(variableName);
    }

    public void addParameter(VariableSymbol parameter) {
        parameters.add(parameter);
        parametersByName.put(parameter.name(), parameter);
    }

    public void addLocal(VariableSymbol local) {
        locals.put(local.name(), local);
    }

    public List<VariableSymbol> parameters() {
        return Collections.unmodifiableList(parameters);
    }

    public Collection<VariableSymbol> locals() {
        return Collections.unmodifiableCollection(locals.values());
    }

    public int arity() {
        return parameters.size();
    }

    public Type parameterType(int index) {
        return parameters.get(index).type();
    }

    /** Looks a name up in this method's own scope: locals first, then parameters. */
    public VariableSymbol lookupLocalOrParameter(String variableName) {
        VariableSymbol local = locals.get(variableName);
        return local != null ? local : parametersByName.get(variableName);
    }

    public MethodSymbol overridden() {
        return overridden;
    }

    public boolean isOverride() {
        return overridden != null;
    }

    public void setOverridden(MethodSymbol overridden) {
        this.overridden = overridden;
    }

    public int offset() {
        return offset;
    }

    void setOffset(int offset) {
        this.offset = offset;
    }

    /** Human-readable signature, e.g. {@code foo(int, A)}. */
    public String signature() {
        StringBuilder sb = new StringBuilder(name).append('(');
        for (int i = 0; i < parameters.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(parameters.get(i).type());
        }
        return sb.append(')').toString();
    }

    @Override
    public String toString() {
        return owner.name() + "." + signature();
    }
}
