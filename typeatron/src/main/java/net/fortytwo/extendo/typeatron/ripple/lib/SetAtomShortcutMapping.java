package net.fortytwo.extendo.typeatron.ripple.lib;

import net.fortytwo.extendo.Extendo;
import net.fortytwo.extendo.brain.Filter;
import net.fortytwo.extendo.brain.Note;
import net.fortytwo.extendo.typeatron.ripple.ExtendoBrainClient;
import net.fortytwo.flow.Sink;
import net.fortytwo.ripple.RippleException;
import net.fortytwo.ripple.model.ModelConnection;
import net.fortytwo.ripple.model.RippleList;

import java.util.logging.Logger;

/**
 * @author Joshua Shinavier (http://fortytwo.net)
 */
public class SetAtomShortcutMapping extends AtomMapping {

    private static final Logger logger = Logger.getLogger(SetAtomShortcutMapping.class.getName());

    public SetAtomShortcutMapping(final ExtendoBrainClient client,
                                  final Filter filter) {
        super(client, filter);
    }

    public String[] getIdentifiers() {
        return new String[]{
                ExtendoLibrary.NS_2014_12 + "set-atom-shortcut"
        };
    }

    public Parameter[] getParameters() {
        return new Parameter[]{
                new Parameter("atom", "the reference atom", true),
                new Parameter("shortcut", "the new shortcut", true)};
    }

    public String getComment() {
        return "sets the @shortcut property of an atom";
    }

    public void apply(RippleList stack,
                      final Sink<RippleList> solutions,
                      final ModelConnection mc) throws RippleException {

        String value = mc.toString(stack.getFirst());
        stack = stack.getRest();
        Object no = stack.getFirst();
        stack = stack.getRest();

        Note n = toNote(no, 0, false);

        if (null == n) {
            logger.warning("can't set @shortcut of non-atom: " + no);
        } else {
            setProperty(n, Extendo.SHORTCUT, value);

            // put the atom back on the stack
            solutions.put(stack.push(n));
        }
    }
}
