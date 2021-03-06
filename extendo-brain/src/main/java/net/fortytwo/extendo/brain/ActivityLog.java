package net.fortytwo.extendo.brain;

import net.fortytwo.extendo.brain.Atom;

import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;

/**
 * @author Joshua Shinavier (http://fortytwo.net)
 */
public class ActivityLog {
    private final OutputStreamWriter out;

    public ActivityLog(FileWriter out) {
        this.out = out;
    }

    public void logCreate(final Atom a) {
        log("create", a);
    }

    public void logView(final Atom a) {
        log("view", a);
    }

    public void logUpdate(final Atom a) {
        log("change-value", a);
    }

    public void logSetProperties(final Atom a) {
        log("set-props", a);
    }

    public void logLink(final Atom tail,
                        final Atom head) {
        log("link", tail, head);
    }

    public void logUnlink(final Atom tail,
                          final Atom head) {
        log("unlink", tail, head);
    }

    public void flush() {
        try {
            out.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void shutDown() throws IOException {
        out.close();
    }

    private void log(final String action, final Atom... a) {
        try {
            out.append("")
                    .append("" + System.currentTimeMillis())
                    .append("\t").append(action);

            for (Atom atom : a) {
                out.append("\t").append(atom.asVertex().getId().toString());
            }

            out.append("\n");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
