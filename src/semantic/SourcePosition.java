package semantic;

import syntaxtree.Node;
import syntaxtree.NodeToken;
import visitor.DepthFirstVisitor;

/** A 1-based line/column location in a MiniJava source file. */
public final class SourcePosition {

    public static final SourcePosition UNKNOWN = new SourcePosition(0, 0);

    private final int line;
    private final int column;

    public SourcePosition(int line, int column) {
        this.line = line;
        this.column = column;
    }

    public int line() {
        return line;
    }

    public int column() {
        return column;
    }

    public boolean isKnown() {
        return line > 0;
    }

    /** Position of a single token. */
    public static SourcePosition of(NodeToken token) {
        return new SourcePosition(token.beginLine, token.beginColumn);
    }

    /** Position of the first token of an arbitrary syntax-tree node. */
    public static SourcePosition of(Node node) {
        if (node instanceof NodeToken) {
            return of((NodeToken) node);
        }
        FirstTokenFinder finder = new FirstTokenFinder();
        node.accept(finder);
        return finder.first == null ? UNKNOWN : of(finder.first);
    }

    @Override
    public String toString() {
        return line + ":" + column;
    }

    /** Walks a subtree and remembers the first real token it meets. */
    private static final class FirstTokenFinder extends DepthFirstVisitor {
        private NodeToken first;

        @Override
        public void visit(NodeToken token) {
            // JTB represents EOF and other synthetic tokens with beginLine <= 0.
            if (first == null && token.beginLine > 0) {
                first = token;
            }
        }
    }
}
