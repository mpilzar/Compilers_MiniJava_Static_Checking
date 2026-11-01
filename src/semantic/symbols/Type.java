package semantic.symbols;

import java.util.Objects;

/**
 * A MiniJava static type.
 *
 * <p>MiniJava has three built-in types ({@code int}, {@code boolean}, {@code int[]}) plus
 * user-defined class types. {@code String[]} only exists as the type of the ignored
 * parameter of {@code main}; it is not a legal type anywhere else, so it is incompatible
 * with every operation and every other type.
 */
public final class Type {

    public enum Kind { INT, BOOLEAN, INT_ARRAY, STRING_ARRAY, CLASS }

    /** Object layout sizes, in bytes, used for field offset computation. */
    public static final int INT_SIZE = 4;
    public static final int BOOLEAN_SIZE = 1;
    public static final int POINTER_SIZE = 8;

    public static final Type INT = new Type(Kind.INT, "int");
    public static final Type BOOLEAN = new Type(Kind.BOOLEAN, "boolean");
    public static final Type INT_ARRAY = new Type(Kind.INT_ARRAY, "int[]");
    public static final Type STRING_ARRAY = new Type(Kind.STRING_ARRAY, "String[]");

    private final Kind kind;
    private final String name;

    private Type(Kind kind, String name) {
        this.kind = kind;
        this.name = name;
    }

    /** Returns the type that refers to the class called {@code className}. */
    public static Type classType(String className) {
        return new Type(Kind.CLASS, Objects.requireNonNull(className));
    }

    public Kind kind() {
        return kind;
    }

    /** The type's source spelling; for class types, the class name. */
    public String name() {
        return name;
    }

    public boolean isClass() {
        return kind == Kind.CLASS;
    }

    /** Size of a field of this type inside an object: ints 4, booleans 1, everything else is a pointer. */
    public int sizeInBytes() {
        switch (kind) {
            case INT:
                return INT_SIZE;
            case BOOLEAN:
                return BOOLEAN_SIZE;
            default:
                return POINTER_SIZE;
        }
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Type)) {
            return false;
        }
        Type that = (Type) other;
        return kind == that.kind && name.equals(that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(kind, name);
    }

    @Override
    public String toString() {
        return name;
    }
}
