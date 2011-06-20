package net.fortytwo.myotherbrain.notes;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * User: josh
 * Date: 5/18/11
 * Time: 6:00 PM
 */
public class NotesIO {
    private static final int MAX_TYPE_LENGTH = 5;

    // Regex of valid id prefixes, including parentheses, colon and trailing space
    private final Pattern ID = Pattern.compile("\\([a-zA-Z0-9+/]+:[a-zA-Z0-9+/]+\\) ");

    // Tabs count as four spaces each.
    private static final String TAB_REPLACEMENT = "    ";

    public void write(final List<NoteContext> notes,
                      final OutputStream out) {
        PrintStream p = new PrintStream(out);
        for (NoteContext c : notes) {
            printContext(c, p);
            p.println("");
        }
    }

    public void writeChildren(final Note note,
                              final OutputStream out) {
        PrintStream p = new PrintStream(out);
        for (Note n : note.getChildren()) {
            printNote(n, 0, p);
        }
    }

    public List<NoteContext> parse(final InputStream in) throws IOException, NoteParsingException {
        LinkedList<Note> hierarchy = new LinkedList<Note>();
        NoteContext context = null;

        List<NoteContext> contexts = new LinkedList<NoteContext>();

        InputStreamReader r = new InputStreamReader(in, "UTF-8");
        BufferedReader br = new BufferedReader(r);
        String line;
        int lineNumber = 0;
        while ((line = br.readLine()) != null) {
            String l = line;
            lineNumber++;
            //System.out.println("" + lineNumber + ") " + line);

            if (0 == l.trim().length()) {
                context = null;
                hierarchy.clear();
            } else if (l.startsWith("[")) {
                int m = l.lastIndexOf("]");
                if (m < 0) {
                    throw new NoteParsingException(lineNumber, "non-terminated note context");
                }
                String text = l.substring(1, m).trim();

                hierarchy.clear();
                context = new NoteContext(text);
                contexts.add(context);
            } else {
                String atomId = null;
                String associationId = null;

                // Extract id
                if (l.startsWith("(")) {
                    int k = l.indexOf(") ");
                    if (k < 0) {
                        throw new NoteParsingException(lineNumber, "line terminated within apparent note ID");
                    }

                    String s = l.substring(0, k + 2);
                    if (!ID.matcher(s).matches()) {
                        throw new NoteParsingException(lineNumber, "invalid note ID");
                    }

                    int i = s.indexOf(":");
                    int j = s.indexOf(")");
                    atomId = s.substring(i + 1, j);
                    associationId = s.substring(1, i);

                    l = l.substring(k + 2);
                }

                // Find indent level
                int indent = 0;
                if (l.startsWith(" ") || l.startsWith("\t")) {
                    // Tabs count as four spaces.
                    l = l.replaceAll("[\\t]", TAB_REPLACEMENT);
                    int i = 0;
                    while (l.charAt(i) == ' ') {
                        i++;
                    }

                    if (0 != i % 4) {
                        throw new NoteParsingException(lineNumber, "notes must be indented by multiples of 4 spaces");
                    }

                    indent = i / 4;

                    if (indent > hierarchy.size()) {
                        throw new NoteParsingException(lineNumber, "note is too deeply indented");
                    }

                    l = l.substring(i).trim();
                }

                while (hierarchy.size() > indent) {
                    hierarchy.removeLast();
                }

                if (0 == indent && null == context) {
                    context = new NoteContext("");
                    contexts.add(context);
                }

                int j = l.indexOf(" ");
                if (j < 0) {
                    j = l.length();
                }

                String type = l.substring(0, j);
                if (type.length() > MAX_TYPE_LENGTH) {
                    throw new NoteParsingException(lineNumber, "apparent note type is too long: " + type);
                }
                l = l.substring(j);

                String description = "";
                String qualifier = null;
                if (0 < l.length()) {
                    if (l.startsWith(" [")) {
                        int m = l.indexOf("]");
                        if (m < 0) {
                            throw new NoteParsingException(lineNumber, "non-terminated note qualifier");
                        }

                        qualifier = l.substring(2, m - 1);
                        l = l.substring(m + 1);
                    }

                    if (!l.startsWith("  ")) {
                        throw new NoteParsingException(lineNumber, "double space after note type is missing");
                    }
                    // Note: a gap of *more* than two spaces is tolerated, for now.
                    l = l.trim();

                    if (l.contains("{{{")) {
                        int start = lineNumber;
                        boolean inside = false;
                        int index = 0;
                        while (true) {
                            // Check for the closing symbol before the opening symbol
                            int b2 = l.indexOf("}}}", index);
                            if (b2 >= 0) {
                                if (!inside) {
                                    throw new NoteParsingException(start, "unmatched verbatim block terminator" +
                                            " (on line " + lineNumber + ")");
                                }

                                inside = false;
                                index = b2 + 3;
                                continue;
                            }

                            int b1 = l.indexOf("{{{", index);
                            if (b1 >= 0) {
                                if (inside) {
                                    throw new NoteParsingException(start, "nested verbatim blocks (detected on line " +
                                            lineNumber + ") are not allowed");
                                }
                                inside = true;
                                index = b1 + 3;
                                continue;
                            }

                            description += l;

                            if (inside) {
                                description += "\n";
                                l = br.readLine();
                                if (null == l) {
                                    throw new NoteParsingException(start, "non-terminated verbatim block");
                                }
                                lineNumber++;
                                index = 0;
                            } else {
                                break;
                            }
                        }
                    } else {
                        description = l;
                    }
                }

                Note n = new Note(type, description);

                if (null != qualifier) {
                    n.setQualifier(qualifier);
                }

                if (null != atomId) {
                    n.setAtomId(atomId);
                }

                if (null != associationId) {
                    n.setAssociationId(associationId);
                }

                if (0 < indent) {
                    hierarchy.get(hierarchy.size() - 1).addChild(n);
                } else {
                    context.addNote(n);
                }

                hierarchy.add(n);
            }
        }

        return contexts;
    }

    public class NoteParsingException extends Exception {
        public NoteParsingException(final int lineNumber,
                                    final String message) {
            super("line " + lineNumber + ": " + message);
        }
    }

    private static void printContext(final NoteContext c,
                                     final PrintStream p) {
        if (0 < c.getText().length()) {
            p.print("[");
            p.print(c.getText());
            p.print("]");
            p.print("\n");
        }

        for (Note n : c.getNotes()) {
            printNote(n, 0, p);
        }
    }

    private static void printNote(final Note n,
                                  final int indent,
                                  final PrintStream p) {
        if (null != n.getAtomId() || null != n.getAssociationId()) {
            p.print("(");
            if (null != n.getAssociationId()) {
                p.print(padId(n.getAssociationId()));
            }
            p.print(":");
            if (null != n.getAtomId()) {
                p.print(padId(n.getAtomId()));
            }
            p.print(") ");
        }

        for (int i = 0; i < indent; i++) {
            p.print("    ");
        }

        p.print(null == n.getType() || 0 == n.getType().length() ? "_" : n.getType());
        p.print("  ");

        p.print(n.getText());

        p.print("\n");

        for (Note child : n.getChildren()) {
            printNote(child, indent + 1, p);
        }
    }

    private static String padId( String id) {
        while (id.length() < 5) {
            id = "0" + id;
        }

        return id;
    }

    public static void main(final String[] args) throws Exception {
        NotesIO p = new NotesIO();
        List<NoteContext> contexts;

        InputStream in = new FileInputStream("/Users/josh/notes/notes.txt");
        try {
            contexts = p.parse(in);
        } finally {
            in.close();
        }

        p.write(contexts, System.out);
    }
}
