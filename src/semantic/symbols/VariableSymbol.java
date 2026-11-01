package semantic.symbols;

import semantic.SourcePosition;

/** A field, method parameter or local variable. */
public final class VariableSymbol {

    public enum Kind { FIELD, PARAMETER, LOCAL }

    private final String name;
    private final Type type;
    private final Kind kind;
    private final SourcePosition namePosition;
    private final SourcePosition typePosition;

    /** Byte offset inside the object; only meaningful for fields, -1 until layout runs. */
    private int offset = -1;

    public VariableSymbol(String name, Type type, Kind kind,
                          SourcePosition namePosition, SourcePosition typePosition) {
        this.name = name;
        this.type = type;
        this.kind = kind;
        this.namePosition = namePosition;
        this.typePosition = typePosition;
    }

    public String name() {
        return name;
    }

    public Type type() {
        return type;
    }

    public Kind kind() {
        return kind;
    }

    public SourcePosition namePosition() {
        return namePosition;
    }

    public SourcePosition typePosition() {
        return typePosition;
    }

    public int offset() {
        return offset;
    }

    void setOffset(int offset) {
        this.offset = offset;
    }
}
