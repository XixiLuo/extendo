package net.fortytwo.extendo.typeatron.ripple.lib;

import net.fortytwo.extendo.brain.Filter;
import net.fortytwo.extendo.brain.Note;
import net.fortytwo.extendo.typeatron.TypeatronControl;
import net.fortytwo.extendo.typeatron.ripple.ExtendoBrainClient;
import net.fortytwo.flow.Sink;
import net.fortytwo.ripple.RippleException;
import net.fortytwo.ripple.model.ModelConnection;
import net.fortytwo.ripple.model.RippleList;
import org.openrdf.model.URI;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Joshua Shinavier (http://fortytwo.net)
 */
public class LaserPointerMapping extends AtomMapping {

    private static final Logger logger = Logger.getLogger(LaserPointerMapping.class.getName());

    private final TypeatronControl typeatron;

    public LaserPointerMapping(final ExtendoBrainClient client,
                               final Filter filter,
                               final TypeatronControl typeatron) {
        super(client, filter);
        this.typeatron = typeatron;
    }

    public String[] getIdentifiers() {
        return new String[]{
                ExtendoLibrary.NS_2014_12 + "point"
        };
    }

    public Parameter[] getParameters() {
        return new Parameter[]{new Parameter("thingIndicated", "the thing pointed to or referenced", true)};
    }

    public String getComment() {
        return "points to or indicates an item";
    }

    public void apply(final RippleList stack,
                      final Sink<RippleList> solutions,
                      final ModelConnection mc) throws RippleException {
        Object first = stack.getFirst();
        Note n = toNote(first, 0, true);

        if (null == n) {
            logger.warning("can't point to non-atom: " + first);

            // soft fail; propagate the stack even if we couldn't point
        } else {
            URI uri = uriOf(n);

            // value is informational; it is used only for development/debugging purposes
            String value = n.getValue();

            logger.log(Level.INFO, "pointing to " + uri + " (" + value + ")");

            try {
                typeatron.pointTo(uri);
            } catch (Throwable t) {
                throw new RippleException(t);
            }
        }

        // keep the stack unchanged
        solutions.put(stack);
    }
}
