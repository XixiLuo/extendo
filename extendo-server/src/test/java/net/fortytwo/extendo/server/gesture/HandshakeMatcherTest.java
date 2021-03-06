package net.fortytwo.extendo.server.gesture;

import org.junit.Before;
import org.junit.Test;
import org.openrdf.model.URI;
import org.openrdf.model.impl.URIImpl;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class HandshakeMatcherTest {

    private final HandshakeMatcher server;

    private final Set<String> matches = new HashSet<String>();
    private int totalMatches;

    // a typical handshake
    private long[] series1 = new long[]{0, 226, 417, 526, 612, 709};

    // another typical, but distinct handshake
    private long[] series2 = new long[]{0, 120, 191, 343, 405};

    private static final String BASE_URI = "http://example.org/";

    private URI
            actor1 = new URIImpl(BASE_URI + "a1"),
            actor2 = new URIImpl(BASE_URI + "a2");

    public HandshakeMatcherTest() {
        HandshakeMatcher.HandshakeHandler handler = new HandshakeMatcher.HandshakeHandler() {
            @Override
            public void handle(HandshakeMatcher.HandshakeSequence left, HandshakeMatcher.HandshakeSequence right, long timestamp) {
                totalMatches++;
                matches.add("" + left.actor + "," + right.actor);
                //System.out.println("handshake: " + left.actor + " and " + right.actor + " at " + timestamp);
            }
        };

        server = new HandshakeMatcher(handler);
    }

    @Before
    public void setUp() {
        totalMatches = 0;
        matches.clear();
        server.reset();
    }

    private void submitEvents(final URI actor1,
                              final long[] series1,
                              final long offset1,
                              final URI actor2,
                              final long[] series2,
                              final long offset2) {
        List<Event> events = new LinkedList<Event>();

        for (long l : series1) {
            Event e = new Event();
            e.actor = actor1;
            e.timestamp = l + offset1;
            events.add(e);
        }

        for (long l : series2) {
            Event e = new Event();
            e.actor = actor2;
            e.timestamp = l + offset2;
            events.add(e);
        }

        Collections.sort(events);

        /*System.out.println("events:");
        for (Event e : events) {
            System.out.println("\t" + e);
        }*/

        for (Event e : events) {
            server.receiveEvent(e.actor, e.timestamp);
        }
    }

    @Test
    public void testCompleteOverlap() throws Exception {
        submitEvents(actor1, series1, 0, actor2, series2, 0);
        // the handshakes match just once, even though there are several more events after they first match
        assertEquals(1, totalMatches);
        assertTrue(matches.contains(actor1.stringValue()+ "," + actor2.stringValue())
                || matches.contains(actor2.stringValue()+ "," + actor1.stringValue()));
    }

    @Test
    public void testNoOverlap() throws Exception {
        submitEvents(actor1, series1, 0, actor2, series2, 1000);
        assertEquals(0, totalMatches);
    }

    @Test
    public void testPartialOverlap() throws Exception {
        submitEvents(actor1, series1, 0, actor2, series2, 300);
        assertEquals(1, totalMatches);
    }

    private class Event implements Comparable<Event> {
        public URI actor;
        public long timestamp;

        @Override
        public int compareTo(Event other) {
            return ((Long) timestamp).compareTo(other.timestamp);
        }

        @Override
        public String toString() {
            return "(" + actor + "," + timestamp + ")";
        }
    }
}
