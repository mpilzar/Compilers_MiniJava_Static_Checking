package report;

import java.io.PrintStream;

import semantic.symbols.ClassSymbol;
import semantic.symbols.MethodSymbol;
import semantic.symbols.SymbolTable;
import semantic.symbols.VariableSymbol;

/**
 * Prints the field and method offsets of every class (except the main class) in the
 * format used by the course reference outputs:
 *
 * <pre>
 * -----------Class B-----------
 * --Variables---
 * B.type : 9
 * B.k : 17
 * ---Methods---
 * B.bla : 16
 * </pre>
 *
 * Only members introduced by a class are listed: inherited fields are omitted, and an
 * overriding method is omitted because it reuses the slot of the method it overrides.
 */
public final class OffsetPrinter {

    private OffsetPrinter() {
    }

    public static void print(SymbolTable table, PrintStream out) {
        for (ClassSymbol cls : table.classes()) {
            if (cls.isMainClass()) {
                continue;
            }
            out.println("-----------Class " + cls.name() + "-----------");
            out.println("--Variables---");
            for (VariableSymbol field : cls.fields()) {
                out.println(cls.name() + "." + field.name() + " : " + field.offset());
            }
            out.println("---Methods---");
            for (MethodSymbol method : cls.methods()) {
                if (!method.isOverride()) {
                    out.println(cls.name() + "." + method.name() + " : " + method.offset());
                }
            }
            out.println();
        }
    }
}
