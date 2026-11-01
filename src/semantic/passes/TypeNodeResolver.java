package semantic.passes;

import semantic.symbols.Type;
import syntaxtree.ArrayType;
import syntaxtree.BooleanType;
import syntaxtree.Identifier;
import syntaxtree.IntegerType;
import visitor.GJNoArguDepthFirst;

/**
 * Converts a {@code Type()} syntax node into a {@link Type}. Class names are not checked
 * here: a class may legally be used before it is declared, so existence is verified only
 * once every declaration has been collected.
 */
final class TypeNodeResolver extends GJNoArguDepthFirst<Type> {

    private static final TypeNodeResolver INSTANCE = new TypeNodeResolver();

    private TypeNodeResolver() {
    }

    static Type resolve(syntaxtree.Type node) {
        return node.accept(INSTANCE);
    }

    /** f0 -> ArrayType() | BooleanType() | IntegerType() | Identifier() */
    @Override
    public Type visit(syntaxtree.Type n) {
        return n.f0.accept(this);
    }

    @Override
    public Type visit(ArrayType n) {
        return Type.INT_ARRAY;
    }

    @Override
    public Type visit(BooleanType n) {
        return Type.BOOLEAN;
    }

    @Override
    public Type visit(IntegerType n) {
        return Type.INT;
    }

    @Override
    public Type visit(Identifier n) {
        return Type.classType(n.f0.tokenImage);
    }
}
