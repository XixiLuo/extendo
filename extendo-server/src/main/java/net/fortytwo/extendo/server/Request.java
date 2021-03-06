package net.fortytwo.extendo.server;

import org.json.JSONException;
import org.json.JSONObject;

import java.security.Principal;

/**
 * @author Joshua Shinavier (http://fortytwo.net)
 */
public class Request {

    protected final JSONObject json;
    protected final Principal user;

    public Request(final JSONObject json,
                   final Principal user) throws JSONException {
        this.json = json;
        this.user = user;
    }
}
