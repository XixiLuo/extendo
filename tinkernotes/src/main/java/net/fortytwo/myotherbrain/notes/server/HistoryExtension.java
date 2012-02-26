package net.fortytwo.myotherbrain.notes.server;

import com.tinkerpop.blueprints.pgm.Graph;
import com.tinkerpop.rexster.RexsterResourceContext;
import com.tinkerpop.rexster.extension.ExtensionDefinition;
import com.tinkerpop.rexster.extension.ExtensionDescriptor;
import com.tinkerpop.rexster.extension.ExtensionNaming;
import com.tinkerpop.rexster.extension.ExtensionPoint;
import com.tinkerpop.rexster.extension.ExtensionRequestParameter;
import com.tinkerpop.rexster.extension.ExtensionResponse;
import com.tinkerpop.rexster.extension.RexsterContext;
import net.fortytwo.myotherbrain.notes.Filter;

import javax.ws.rs.core.SecurityContext;
import java.security.Principal;
import java.util.List;

/**
 * @author Joshua Shinavier (http://fortytwo.net)
 */
@ExtensionNaming(namespace = "tinkernotes", name = "history")
public class HistoryExtension extends TinkerNotesExtension {

    @ExtensionDefinition(extensionPoint = ExtensionPoint.GRAPH)
    @ExtensionDescriptor(description = "an extension for viewing TinkerNotes browsing history")
    public ExtensionResponse handleRequest(@RexsterContext RexsterResourceContext context,
                                           @RexsterContext Graph graph,
                                           @ExtensionRequestParameter(name = "minWeight", description = "minimum-weight criterion for atoms in the view") Float minWeight,
                                           @ExtensionRequestParameter(name = "maxWeight", description = "maximum-weight criterion for atoms in the view") Float maxWeight,
                                           @ExtensionRequestParameter(name = "minSharability", description = "minimum-sharability criterion for atoms in the view") Float minSharability,
                                           @ExtensionRequestParameter(name = "maxSharability", description = "maximum-sharability criterion for atoms in the view") Float maxSharability) {
        LOGGER.info("tinkernotes history");
        System.err.println("tinkernotes history");

        SecurityContext security = context.getSecurityContext();
        Principal user = null == security ? null : security.getUserPrincipal();

        Filter filter;

        try {
            float m = findMinAuthorizedSharability(user, minSharability);
            filter = new Filter(m, maxSharability, -1, minWeight, maxWeight, -1);
        } catch (IllegalArgumentException e) {
            return ExtensionResponse.error(e.getMessage());
        }

        Params p = new Params();
        p.baseGraph = graph;
        p.filter = filter;
        p.context = context;

        return this.handleRequestInternal(p);
    }

    @Override
    protected ExtensionResponse performTransaction(final Params p) throws Exception {
        List<String> ids = getHistory(p.context, p.graph, p.filter);

        addView(p.semantics.customView(ids, p.filter), p);

        return ExtensionResponse.ok(p.map);
    }

    @Override
    protected boolean isReadOnly() {
        return true;
    }
}