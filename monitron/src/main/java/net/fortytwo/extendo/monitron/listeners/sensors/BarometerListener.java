package net.fortytwo.extendo.monitron.listeners.sensors;

import net.fortytwo.extendo.monitron.Context;
import net.fortytwo.extendo.monitron.data.GaussianData;
import net.fortytwo.extendo.monitron.events.AtmosphericPressureObservation;
import net.fortytwo.extendo.monitron.events.MonitronEvent;
import org.openrdf.model.URI;

/**
 * @author Joshua Shinavier (http://fortytwo.net)
 */
public class BarometerListener extends GaussianSensorListener {
    public BarometerListener(final Context context, final URI sensor) {
        super(context, sensor);
    }

    protected MonitronEvent handleSample(final GaussianData data) {
        return new AtmosphericPressureObservation(context, sensor, data);
    }
}
