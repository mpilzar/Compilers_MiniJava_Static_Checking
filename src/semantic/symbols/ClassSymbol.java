package semantic.symbols;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import semantic.SourcePosition;

/** A class: its superclass, fields, methods, and computed object / v-table layout. */
public final class ClassSymbol {

    private final String name;
    private final ClassSymbol superclass;
    private final boolean isMainClass;
    private final SourcePosition namePosition;

    private final Map<String, VariableSymbol> fields = new LinkedHashMap<>();
    private final List<MethodSymbol> methods = new ArrayList<>();

    /** The static {@code main} method; only set on the main class, never part of the v-table. */
    private MethodSymbol mainMethod;

    private boolean laidOut;
    private int fieldsEnd;   // first free byte after this class's fields (including inherited)
    private int vtableEnd;   // first free byte after this class's v-table slots (including inherited)

    public ClassSymbol(String name, ClassSymbol superclass, boolean isMainClass, SourcePosition namePosition) {
        this.name = name;
        this.superclass = superclass;
        this.isMainClass = isMainClass;
        this.namePosition = namePosition;
    }

    public String name() {
        return name;
    }

    public ClassSymbol superclass() {
        return superclass;
    }

    public boolean isMainClass() {
        return isMainClass;
    }

    public SourcePosition namePosition() {
        return namePosition;
    }

    public Type asType() {
        return Type.classType(name);
    }

    // ------------------------------------------------------------------ members

    public boolean declaresField(String fieldName) {
        return fields.containsKey(fieldName);
    }

    public void addField(VariableSymbol field) {
        fields.put(field.name(), field);
    }

    public Collection<VariableSymbol> fields() {
        return Collections.unmodifiableCollection(fields.values());
    }

    public void addMethod(MethodSymbol method) {
        methods.add(method);
    }

    /** Methods declared directly in this class, in source order (excludes {@code main}). */
    public List<MethodSymbol> methods() {
        return Collections.unmodifiableList(methods);
    }

    public MethodSymbol mainMethod() {
        return mainMethod;
    }

    public void setMainMethod(MethodSymbol mainMethod) {
        this.mainMethod = mainMethod;
    }

    // ------------------------------------------------------------------ hierarchy queries

    /** Finds a field declared in this class or the nearest ancestor that declares it. */
    public VariableSymbol lookupField(String fieldName) {
        for (ClassSymbol c = this; c != null; c = c.superclass) {
            VariableSymbol field = c.fields.get(fieldName);
            if (field != null) {
                return field;
            }
        }
        return null;
    }

    /** True if this class is {@code other} or (transitively) extends it. */
    public boolean isSubclassOf(ClassSymbol other) {
        for (ClassSymbol c = this; c != null; c = c.superclass) {
            if (c == other) {
                return true;
            }
        }
        return false;
    }

    /**
     * Every method with this name visible on this class: declared here or inherited, most
     * derived first. Overridden ancestor versions are skipped.
     */
    public List<MethodSymbol> visibleMethodsNamed(String methodName) {
        List<MethodSymbol> result = new ArrayList<>();
        for (ClassSymbol c = this; c != null; c = c.superclass) {
            for (MethodSymbol m : c.methods) {
                if (m.name().equals(methodName) && !isOverriddenIn(result, m)) {
                    result.add(m);
                }
            }
        }
        return result;
    }

    private static boolean isOverriddenIn(List<MethodSymbol> moreDerived, MethodSymbol m) {
        for (MethodSymbol d : moreDerived) {
            for (MethodSymbol o = d.overridden(); o != null; o = o.overridden()) {
                if (o == m) {
                    return true;
                }
            }
        }
        return false;
    }

    // ------------------------------------------------------------------ layout

    /**
     * Assigns field offsets and v-table slots. Inherited members keep their parent offsets;
     * new fields are appended after the parent's fields and every method that does not
     * override an inherited one gets the next 8-byte v-table slot.
     * The superclass must already be laid out (guaranteed because a superclass must be
     * declared before its subclasses).
     */
    public void computeLayout() {
        if (laidOut) {
            return;
        }
        int fieldOffset = 0;
        int methodOffset = 0;
        if (superclass != null) {
            superclass.computeLayout();
            fieldOffset = superclass.fieldsEnd;
            methodOffset = superclass.vtableEnd;
        }
        for (VariableSymbol field : fields.values()) {
            field.setOffset(fieldOffset);
            fieldOffset += field.type().sizeInBytes();
        }
        for (MethodSymbol method : methods) {
            if (method.isOverride()) {
                method.setOffset(method.overridden().offset());
            } else {
                method.setOffset(methodOffset);
                methodOffset += Type.POINTER_SIZE;
            }
        }
        fieldsEnd = fieldOffset;
        vtableEnd = methodOffset;
        laidOut = true;
    }

    /** Total object size of the fields (inherited ones included), in bytes. */
    public int fieldsEnd() {
        return fieldsEnd;
    }

    /** Total v-table size (inherited slots included), in bytes. */
    public int vtableEnd() {
        return vtableEnd;
    }

    @Override
    public String toString() {
        return name;
    }
}
