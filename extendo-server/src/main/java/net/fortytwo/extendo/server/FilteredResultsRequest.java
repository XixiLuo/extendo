package net.fortytwo.extendo.server;

import net.fortytwo.extendo.brain.Filter;
import net.fortytwo.extendo.brain.Params;
import org.json.JSONException;
import org.json.JSONObject;

import java.security.Principal;

/**
* @author Joshua Shinavier (http://fortytwo.net)
*/
public class FilteredResultsRequest extends Request {
    private final Filter filter;

    public FilteredResultsRequest(final JSONObject json,
                                  final Principal user) throws JSONException {
        super(json, user);

        filter = constructFilter();
    }

    public Filter getFilter() {
        return filter;
    }

    private Filter constructFilter() throws JSONException {
        JSONObject f = json.getJSONObject(Params.FILTER);

        float defaultWeight = (float) f.optDouble(Params.DEFAULT_WEIGHT, -1);
        float defaultSharability = (float) f.optDouble(Params.DEFAULT_SHARABILITY, -1);
        float minWeight = (float) f.getDouble(Params.MIN_WEIGHT);
        float maxWeight = (float) f.optDouble(Params.MAX_WEIGHT, 1.0);

        float ms = (float) f.getDouble(Params.MIN_SHARABILITY);
        float minSharability = ExtendoExtension.findMinAuthorizedSharability(user, ms);

        float maxSharability = (float) f.optDouble(Params.MAX_SHARABILITY, 1.0);

        return new Filter(minWeight, maxWeight, defaultWeight, minSharability, maxSharability, defaultSharability);
    }
}
