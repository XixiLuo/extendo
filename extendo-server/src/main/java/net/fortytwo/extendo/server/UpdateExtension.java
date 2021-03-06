package net.fortytwo.extendo.server;

import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.KeyIndexableGraph;
import com.tinkerpop.rexster.RexsterResourceContext;
import com.tinkerpop.rexster.extension.ExtensionDefinition;
import com.tinkerpop.rexster.extension.ExtensionDescriptor;
import com.tinkerpop.rexster.extension.ExtensionNaming;
import com.tinkerpop.rexster.extension.ExtensionPoint;
import com.tinkerpop.rexster.extension.ExtensionRequestParameter;
import com.tinkerpop.rexster.extension.ExtensionResponse;
import com.tinkerpop.rexster.extension.HttpMethod;
import com.tinkerpop.rexster.extension.RexsterContext;
import net.fortytwo.extendo.Extendo;
import net.fortytwo.extendo.brain.Note;
import net.fortytwo.extendo.brain.NoteQueries;
import net.fortytwo.extendo.brain.Params;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.Principal;

/**
 * A service for updating an Extend-o-Brain graph
 *
 * @author Joshua Shinavier (http://fortytwo.net)
 */
@ExtensionNaming(namespace = "extendo", name = "update")
//@ExtensionDescriptor(description = "update an Extend-o-Brain graph")
public class UpdateExtension extends ExtendoExtension {

    @ExtensionDefinition(extensionPoint = ExtensionPoint.GRAPH, method = HttpMethod.POST)
    @ExtensionDescriptor(description = "update an Extend-o-Brain graph using the Extendo Wiki format")
    public ExtensionResponse handleRequest(@RexsterContext RexsterResourceContext context,
                                           @RexsterContext Graph graph,
                                           @ExtensionRequestParameter(name = Params.REQUEST,
                                                   description = "request description (JSON object)") String request) {

        RequestParams p = createParams(context, (KeyIndexableGraph) graph);
        UpdateRequest r;
        try {
            r = new UpdateRequest(new JSONObject(request), p.user);
        } catch (JSONException e) {
            return ExtensionResponse.error(e.getMessage());
        }
        p.height = r.getHeight();
        p.rootId = r.getRootId();
        p.styleName = r.getStyleName();
        p.jsonView = r.jsonView;
        p.wikiView = r.wikiView;
        p.filter = r.getFilter();

        Extendo.logInfo("extendo update " + r.getRootId());

        return handleRequestInternal(p);
    }

    protected ExtensionResponse performTransaction(final RequestParams p) throws Exception {
        Note rootNote;

        if (null != p.wikiView) {
            InputStream in = new ByteArrayInputStream(p.wikiView.getBytes());
            try {
                rootNote = p.parser.fromWikiText(in);
            } finally {
                in.close();
            }
        } else if (null != p.jsonView) {
            rootNote = p.parser.fromJSON(p.jsonView);
        } else {
            throw new IllegalStateException();
        }

        // Apply the update
        try {
            p.queries.update(p.root, rootNote, p.height, p.filter, p.style);
        } catch (NoteQueries.InvalidUpdateException e) {
            return ExtensionResponse.error("invalid update: " + e.getMessage());
        }

        Note n = p.queries.view(p.root, p.height, p.filter, p.style);
        addView(n, p);

        return ExtensionResponse.ok(p.map);
    }

    protected boolean doesRead() {
        return true;
    }

    protected boolean doesWrite() {
        return true;
    }

    private class UpdateRequest extends RootedViewRequest {
        public final String wikiView;
        public final JSONObject jsonView;

        public UpdateRequest(final JSONObject json,
                             final Principal user) throws JSONException {
            super(json, user);

            // default to wiki-formatted updates, but support updates in either the JSON or wiki formats
            String view = this.json.getString(Params.VIEW);
            String viewFormat = this.json.optString(Params.VIEW_FORMAT);
            if (null == viewFormat || 0 == viewFormat.length() || viewFormat.equals(Params.WIKI_FORMAT)) {
                wikiView = view;
                jsonView = null;
            } else if (viewFormat.equals(Params.JSON_FORMAT)) {
                wikiView = null;
                jsonView = new JSONObject(view);
            } else {
                throw new JSONException("unexpected view format: " + viewFormat);
            }
        }
    }
}
