package report;

import java.util.List;

/**
 * A compiler diagnostic rendered in the familiar {@code file:line:col: error: message}
 * form, followed by the offending source line and a caret under the reported column.
 *
 * <p>JavaCC reports columns with tabs expanded to 8-column stops. The caret line is drawn
 * against the tab-expanded source, while the column printed in the header is converted
 * back to a character index so editors jump to the right spot.
 */
public final class Diagnostic {

    private static final int TAB_SIZE = 8;

    private final String file;
    private final int line;           // 1-based, 0 if unknown
    private final int expandedColumn; // 1-based, tab-expanded as reported by JavaCC
    private final String message;

    public Diagnostic(String file, int line, int expandedColumn, String message) {
        this.file = file;
        this.line = line;
        this.expandedColumn = expandedColumn;
        this.message = message;
    }

    public String render(List<String> sourceLines) {
        StringBuilder sb = new StringBuilder(file);
        String sourceLine = (line >= 1 && sourceLines != null && line <= sourceLines.size())
                ? sourceLines.get(line - 1) : null;

        if (line >= 1) {
            sb.append(':').append(line);
            if (expandedColumn >= 1) {
                sb.append(':').append(sourceLine == null ? expandedColumn : charColumn(sourceLine));
            }
        }
        sb.append(": error: ").append(message);

        if (sourceLine != null) {
            String expanded = expandTabs(sourceLine);
            sb.append(System.lineSeparator()).append("    ").append(expanded);
            if (expandedColumn >= 1) {
                sb.append(System.lineSeparator()).append("    ");
                for (int i = 1; i < expandedColumn; i++) {
                    sb.append(' ');
                }
                sb.append('^');
            }
        }
        return sb.toString();
    }

    /** Maps the tab-expanded column back to a 1-based character index in the raw line. */
    private int charColumn(String raw) {
        int visual = 0;
        for (int i = 0; i < raw.length(); i++) {
            if (visual >= expandedColumn - 1) {
                return i + 1;
            }
            visual = raw.charAt(i) == '\t' ? (visual / TAB_SIZE + 1) * TAB_SIZE : visual + 1;
        }
        return raw.length() + 1;
    }

    private static String expandTabs(String raw) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (c == '\t') {
                do {
                    sb.append(' ');
                } while (sb.length() % TAB_SIZE != 0);
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
