package net.fortytwo.extendo.monitron.events;

import net.fortytwo.extendo.monitron.Context;
import net.fortytwo.extendo.monitron.data.Data;
import net.fortytwo.extendo.monitron.ontologies.OMOntology;
import net.fortytwo.extendo.monitron.ontologies.OWLTime;
import net.fortytwo.extendo.monitron.ontologies.Universe;
import net.fortytwo.rdfagents.model.Dataset;
import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.model.vocabulary.RDF;

import java.util.Date;

/**
 * @author Joshua Shinavier (http://fortytwo.net)
 */
public abstract class Observation extends MonitronEvent {

    protected final Data data;
    protected final URI sensor;
    protected final URI event;
    protected final Resource result;

    public Observation(final Context context,
                       final URI sensor,
                       final Data data) {
        super(context);
        this.sensor = sensor;
        event = coinEventURI();
        this.data = data;
        result = valueFactory.createBNode();
    }

    public URI getSensor() {
        return sensor;
    }

    public Data getData() {
        return data;
    }

    @Override
    public Dataset toRDF() {
        Dataset dataset = super.toRDF();

        // add event type and observed property in subclasses

        // TODO: don't hard-code location
        addStatement(dataset, event, OMOntology.OBSERVATION_LOCATION, Universe.ROOM_1);
        //addStatement(dataset, event, EventOntology.PLACE, Universe.ROOM_1);
        addStatement(dataset, event, OMOntology.FEATURE_OF_INTEREST, Universe.ROOM_1);

        addStatement(dataset, event, OMOntology.PROCEDURE, sensor);

        Resource samplingTime = valueFactory.createBNode();
        addStatement(dataset, samplingTime, RDF.TYPE, OWLTime.INTERVAL);
        addStatement(dataset, event, OMOntology.SAMPLING_TIME, samplingTime);
        Resource beginning = valueFactory.createBNode();
        addStatement(dataset, beginning, RDF.TYPE, OWLTime.INSTANT);
        Resource end = valueFactory.createBNode();
        addStatement(dataset, end, RDF.TYPE, OWLTime.INSTANT);
        addStatement(dataset, beginning, OWLTime.IN_XSD_DATE_TIME,
                toLiteral(new Date(data.getSampleIntervalBeginning())));
        addStatement(dataset, end, OWLTime.IN_XSD_DATE_TIME,
                toLiteral(new Date(data.getSampleIntervalEnd())));
        addStatement(dataset, samplingTime, OWLTime.HAS_BEGINNING, beginning);
        addStatement(dataset, samplingTime, OWLTime.HAS_END, end);

        addStatement(dataset, result, RDF.TYPE, OMOntology.RESULT_DATA);
        addStatement(dataset, event, OMOntology.RESULT, result);
        // add details of result in subclasses

        return dataset;
    }
}
