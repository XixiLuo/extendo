package net.fortytwo.extendo.server;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.KeyIndexableGraph;
import com.tinkerpop.blueprints.TransactionalGraph;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.tg.TinkerGraph;
import com.tinkerpop.blueprints.oupls.jung.GraphJung;
import com.tinkerpop.blueprints.util.io.graphml.GraphMLWriter;
import com.tinkerpop.rexster.RexsterResourceContext;
import com.tinkerpop.rexster.extension.ExtensionDefinition;
import com.tinkerpop.rexster.extension.ExtensionDescriptor;
import com.tinkerpop.rexster.extension.ExtensionNaming;
import com.tinkerpop.rexster.extension.ExtensionPoint;
import com.tinkerpop.rexster.extension.ExtensionRequestParameter;
import com.tinkerpop.rexster.extension.ExtensionResponse;
import com.tinkerpop.rexster.extension.RexsterContext;
import edu.uci.ics.jung.algorithms.scoring.PageRank;
import net.fortytwo.extendo.Extendo;
import net.fortytwo.extendo.brain.Atom;
import net.fortytwo.extendo.brain.AtomList;
import net.fortytwo.extendo.brain.BrainGraph;
import org.json.JSONException;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.security.Principal;

/**
 * A service for exporting an Extend-o-Brain graph to the file system
 *
 * @author Joshua Shinavier (http://fortytwo.net)
 */
@ExtensionNaming(namespace = "extendo", name = "export")
public class ExportExtension extends ExtendoExtension {

    private enum Format {
        Vertices, Edges, GraphML, PageRank
    }

    @ExtensionDefinition(extensionPoint = ExtensionPoint.GRAPH)
    @ExtensionDescriptor(description = "an extension for exporting an Extend-o-Brain graph to the file system")
    public ExtensionResponse handleRequest(@RexsterContext RexsterResourceContext context,
                                           @RexsterContext Graph graph,
                                           @ExtensionRequestParameter(name = "request", description = "request description (JSON object)") String request) {
        // TODO: any security restrictions here?

        Params p = createParams(context, (KeyIndexableGraph) graph);

        ExportRequest r;
        try {
            r = new ExportRequest(request, p.user);
        } catch (JSONException e) {
            return ExtensionResponse.error(e.getMessage());
        }

        p.filter = r.filter;
        p.file = r.file;
        p.format = r.format;

        logInfo("extendo export " + r.format + " to " + r.file);

        return handleRequestInternal(p);
    }

    private void exportVertices(final BrainGraph g,
                                final PrintStream p) throws IOException {

        p.println("created\tid\tweight\tsharability\tvalue\talias");

        for (Vertex v : g.getPropertyGraph().getVertices()) {
            Object c = v.getProperty(Extendo.CREATED);
            if (null != c) {
                p.print(c);
                p.print('\t');
                p.print(v.getId());
                p.print('\t');
                p.print(v.getProperty(Extendo.WEIGHT));
                p.print('\t');
                p.print(v.getProperty(Extendo.SHARABILITY));
                p.print('\t');

                String value = v.getProperty(Extendo.VALUE);
                if (null == value) {
                    LOGGER.warning("note has null @value: " + v.getId());
                } else {
                    p.print(escapeValue((String) v.getProperty(Extendo.VALUE)));
                }
                p.print('\t');

                String alias = v.getProperty(Extendo.ALIAS);
                if (null != alias) {
                    p.print(escapeValue(alias));
                }

                p.print('\n');
            }
        }
    }

    private void exportEdges(final BrainGraph g,
                             final PrintStream p) throws IOException {
        p.println("from\tto");

        for (Vertex v : g.getPropertyGraph().getVertices()) {
            Atom a = g.getAtom(v);
            if (null != a) {
                AtomList l = a.getNotes();
                while (null != l) {
                    p.print(v.getId());
                    p.print('\t');
                    p.print(l.getFirst().asVertex().getId());
                    p.print('\n');
                    l = l.getRest();
                }
            }
        }
    }

    private void exportPageRank(final BrainGraph g,
                                final PrintStream p) {
        TinkerGraph g2 = new TinkerGraph();
        for (Vertex v : g.getPropertyGraph().getVertices()) {
            g2.addVertex(v.getId());
        }
        for (Edge e : g.getPropertyGraph().getEdges()) {
            g2.addEdge(null,
                    g2.getVertex(e.getVertex(Direction.OUT).getId()),
                    g2.getVertex(e.getVertex(Direction.IN).getId()),
                    "link");
        }

        PageRank<Vertex, Edge> pr = new PageRank<Vertex, Edge>(new GraphJung(g2), 0.15d);
        pr.evaluate();

        p.println("id\tscore");
        for (Vertex v : g2.getVertices()) {
            p.println(v.getId() + "\t" + pr.getVertexScore(v));
        }

        g2.shutdown();
    }

    private void exportGraphML(final BrainGraph g,
                               final OutputStream out) throws IOException {
        ((TransactionalGraph) g.getPropertyGraph()).stopTransaction(TransactionalGraph.Conclusion.SUCCESS);
        GraphMLWriter w = new GraphMLWriter(g.getPropertyGraph());
        w.setNormalize(true);
        w.outputGraph(out);
    }

    // Note: quote characters (") need to be replaced, e.g. with underscores (_), if this data is imported into R.
    // Otherwise, R becomes confused and skips rows.
    private String escapeValue(final String value) {
        return Extendo.unicodeEscape(value);
    }

    protected ExtensionResponse performTransaction(final Params p) throws Exception {
        Format f = Format.valueOf(p.format);
        if (null == f) {
            return ExtensionResponse.error("no such format: " + p.format);
        }

        //OutputStream out = new FileOutputStream(new File("/tmp/extendo-vertices.txt"));
        //out = new FileOutputStream(new File("/tmp/extendo-edges.txt"));
        //out = new FileOutputStream(new File("/tmp/extendo-pagerank.txt"));

        OutputStream out = new FileOutputStream(p.file);
        try {
            switch (f) {
                case Vertices:
                    exportVertices(p.brain.getBrainGraph(), new PrintStream(out));
                    break;
                case Edges:
                    exportEdges(p.brain.getBrainGraph(), new PrintStream(out));
                    break;
                case GraphML:
                    exportGraphML(p.brain.getBrainGraph(), out);
                    break;
                case PageRank:
                    exportPageRank(p.brain.getBrainGraph(), new PrintStream(out));
                    break;
            }
        } finally {
            out.close();
        }

        return ExtensionResponse.ok(p.map);
    }

    protected boolean doesRead() {
        // doesn't read, in that no data is returned by the service (data is only written to the file system)
        return false;
    }

    protected boolean doesWrite() {
        return false;
    }

    private class ExportRequest extends FilteredResultsRequest {
        private final String format;
        private final String file;

        public ExportRequest(final String jsonStr,
                             final Principal user) throws JSONException {
            super(jsonStr, user);

            format = json.getString(FORMAT);
            file = json.getString(FILE);
        }
    }
}