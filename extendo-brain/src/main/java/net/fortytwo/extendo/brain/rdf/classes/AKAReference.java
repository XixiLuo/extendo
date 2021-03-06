package net.fortytwo.extendo.brain.rdf.classes;

import net.fortytwo.extendo.brain.rdf.SimpleAtomClass;

import java.util.regex.Pattern;

/**
 * @author Joshua Shinavier (http://fortytwo.net)
 */
public class AKAReference extends SimpleAtomClass {

    public AKAReference() {
        super(
                "aka",
                // TODO: the foaf:nick mapping is not quite appropriate to the "brand/trade name" and "formerly" usage
                Pattern.compile("(aka|brand name|trade name|formerly) \\\"[^\\\"]+\\\"(, \\\"[^\\\"]+\\\")*"),
                null,
                null
                );
    }

    public static String extractAlias(final String value) {
        int i = value.indexOf('\"');
        int j = value.lastIndexOf('\"');
        return value.substring(i + 1, j);
    }
}
