package semantic;

import syntaxtree.Node;

/**
 * Raised by any analysis pass when the program violates a MiniJava static rule.
 * Analysis stops at the first error, so this carries a single message and location.
 */
public class SemanticException extends RuntimeException {

    private final SourcePosition position;

    public SemanticException(SourcePosition position, String message) {
        super(message);
        this.position = position == null ? SourcePosition.UNKNOWN : position;
    }

    public SemanticException(Node at, String message) {
        this(SourcePosition.of(at), message);
    }

    public SourcePosition position() {
        return position;
    }
}
