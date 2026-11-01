import java.io.IOException;
import java.io.PrintStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import report.Diagnostic;
import report.OffsetPrinter;
import semantic.SemanticAnalyzer;
import semantic.SemanticException;
import semantic.symbols.SymbolTable;
import syntaxtree.Goal;

/**
 * Command-line driver: parses and statically checks every MiniJava file given as an argument.
 *
 * <pre>
 *   java Main [file1] [file2] ... [fileN]
 *   java -jar build/minijava-checker.jar [file1] ... [fileN]
 * </pre>
 *
 * For each well-typed file the class layout (field and method offsets) is printed on
 * standard output. The first syntax or semantic error in a file is reported on standard
 * error, and checking moves on to the next file. When several files are given, each
 * file's output is preceded by a {@code ==> file <==} header.
 *
 * Exit status: 0 if every file is valid, 1 if any file has an error, 2 on bad usage.
 */
public final class Main {

    private static final Pattern LEXICAL_POSITION = Pattern.compile("line (\\d+), column (\\d+)");
    private static final Pattern LEXICAL_CHARACTER = Pattern.compile("Encountered: (<EOF>|\".*?\")(?: \\(\\d+\\))?");

    private Main() {
    }

    public static void main(String[] args) {
        if (args.length == 0 || Arrays.asList(args).contains("-h") || Arrays.asList(args).contains("--help")) {
            PrintStream stream = args.length == 0 ? System.err : System.out;
            stream.println("Usage: java Main <file1.java> [file2.java ...]");
            stream.println("Parses and type-checks each MiniJava file and prints its class field/method offsets.");
            System.exit(args.length == 0 ? 2 : 0);
        }

        boolean multipleFiles = args.length > 1;
        int failures = 0;
        for (String path : args) {
            if (multipleFiles) {
                System.out.println("==> " + path + " <==");
            }
            if (!checkFile(path)) {
                failures++;
            }
        }
        System.out.flush();
        System.exit(failures == 0 ? 0 : 1);
    }

    /** Parses and analyzes one file. Returns true if it is a valid MiniJava program. */
    private static boolean checkFile(String path) {
        String source;
        try {
            source = new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
        } catch (NoSuchFileException e) {
            return fail(path, null, new Diagnostic(path, 0, 0, "file not found"));
        } catch (IOException e) {
            return fail(path, null, new Diagnostic(path, 0, 0, "cannot read file: " + e.getMessage()));
        }
        List<String> lines = splitLines(source);

        try {
            Goal program = new MiniJavaParser(new StringReader(source)).Goal();
            SymbolTable table = SemanticAnalyzer.analyze(program);
            OffsetPrinter.print(table, System.out);
            return true;
        } catch (ParseException e) {
            return fail(path, lines, syntaxError(path, e));
        } catch (TokenMgrError e) {
            return fail(path, lines, lexicalError(path, e));
        } catch (SemanticException e) {
            return fail(path, lines, new Diagnostic(path, e.position().line(), e.position().column(), e.getMessage()));
        }
    }

    private static boolean fail(String path, List<String> lines, Diagnostic diagnostic) {
        System.out.flush(); // keep stdout/stderr ordering sane on a terminal
        System.err.println(diagnostic.render(lines));
        System.err.flush();
        return false;
    }

    // ------------------------------------------------------------------ parse errors

    private static Diagnostic syntaxError(String path, ParseException e) {
        Token offending = e.currentToken != null && e.currentToken.next != null ? e.currentToken.next : null;
        if (offending == null) {
            return new Diagnostic(path, 0, 0, "syntax error: " + firstLine(e.getMessage()));
        }
        String found = offending.kind == MiniJavaParserConstants.EOF
                ? "end of file" : "'" + offending.image + "'";
        StringBuilder message = new StringBuilder("syntax error: unexpected ").append(found);

        List<String> expected = expectedTokens(e);
        if (!expected.isEmpty() && expected.size() <= 6) {
            message.append(", expected ").append(String.join(" or ", expected));
        }
        return new Diagnostic(path, offending.beginLine, offending.beginColumn, message.toString());
    }

    private static List<String> expectedTokens(ParseException e) {
        List<String> expected = new ArrayList<>();
        if (e.expectedTokenSequences == null || e.tokenImage == null) {
            return expected;
        }
        for (int[] sequence : e.expectedTokenSequences) {
            if (sequence.length > 0) {
                String image = e.tokenImage[sequence[0]];
                if (!expected.contains(image)) {
                    expected.add(image);
                }
            }
        }
        return expected;
    }

    private static Diagnostic lexicalError(String path, TokenMgrError e) {
        // JavaCC format: Lexical error at line L, column C.  Encountered: "%" (37), after : ""
        String raw = firstLine(e.getMessage());
        Matcher found = LEXICAL_CHARACTER.matcher(raw);
        String message = found.find()
                ? (found.group(1).equals("<EOF>") ? "unexpected end of file (unterminated comment?)"
                                                  : "unexpected character " + found.group(1))
                : raw;
        Matcher at = LEXICAL_POSITION.matcher(raw);
        if (at.find()) {
            return new Diagnostic(path, Integer.parseInt(at.group(1)), Integer.parseInt(at.group(2)),
                    "lexical error: " + message);
        }
        return new Diagnostic(path, 0, 0, "lexical error: " + message);
    }

    // ------------------------------------------------------------------ utilities

    private static String firstLine(String s) {
        if (s == null) {
            return "";
        }
        int newline = s.indexOf('\n');
        return (newline >= 0 ? s.substring(0, newline) : s).trim();
    }

    private static List<String> splitLines(String source) {
        return Arrays.asList(source.split("\r\n|\r|\n", -1));
    }
}
