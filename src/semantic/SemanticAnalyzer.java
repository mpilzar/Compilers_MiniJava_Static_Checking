package semantic;

import semantic.passes.DeclarationCollector;
import semantic.passes.DeclarationValidator;
import semantic.passes.TypeChecker;
import semantic.symbols.SymbolTable;
import syntaxtree.Goal;

/**
 * Runs the full static-checking pipeline on one parsed MiniJava program.
 *
 * <ol>
 *   <li>{@link DeclarationCollector} (visitor) - builds the symbol table;</li>
 *   <li>{@link DeclarationValidator} - checks declared types and overloading/overriding,
 *       then computes field offsets and v-table slots;</li>
 *   <li>{@link TypeChecker} (visitor) - checks every statement and expression.</li>
 * </ol>
 *
 * The first violation aborts analysis with a {@link SemanticException}.
 */
public final class SemanticAnalyzer {

    private SemanticAnalyzer() {
    }

    public static SymbolTable analyze(Goal program) {
        SymbolTable table = DeclarationCollector.collect(program);
        DeclarationValidator.validate(table);
        TypeChecker.check(program, table);
        return table;
    }
}
