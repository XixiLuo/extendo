package net.fortytwo.extendo.typeatron.ripple.lib.music;

import net.fortytwo.extendo.typeatron.ripple.lib.ExtendoLibrary;
import net.fortytwo.flow.Sink;
import net.fortytwo.ripple.RippleException;
import net.fortytwo.ripple.model.ModelConnection;
import net.fortytwo.ripple.model.PrimitiveStackMapping;
import net.fortytwo.ripple.model.RippleList;

/**
* @author Joshua Shinavier (http://fortytwo.net)
*/
public class DisableMusicMapping extends PrimitiveStackMapping {

    private final TypeatronMusicControl music;

    public DisableMusicMapping(final TypeatronMusicControl music) {
        this.music = music;
    }

    public String[] getIdentifiers() {
        return new String[]{
                ExtendoLibrary.NS_2014_12 + "disable-music"
        };
    }

    public Parameter[] getParameters() {
        return new Parameter[]{};
    }

    public String getComment() {
        return "disables musical output from the Typeatron";
    }

    public void apply(final RippleList stack,
                      final Sink<RippleList> solutions,
                      final ModelConnection context) throws RippleException {

        music.disable();

        solutions.put(stack);
    }
}
