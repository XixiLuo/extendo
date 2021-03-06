package net.fortytwo.extendo.brain.rdf;

import net.fortytwo.extendo.brain.Atom;

import java.util.Collection;
import java.util.LinkedList;

/**
 * @author Joshua Shinavier (http://fortytwo.net)
 */
public class AtomCollectionMemory {

    private final String atomId;
    private final Collection<AtomCollectionMemory> collections = new LinkedList<AtomCollectionMemory>();
    private final Collection<Atom> atoms = new LinkedList<Atom>();

    public AtomCollectionMemory(final String atomId) {
        this.atomId = atomId;
    }

    public String getAtomId() {
        return atomId;
    }

    public Collection<AtomCollectionMemory> getMemberCollections() {
        return collections;
    }

    public Collection<Atom> getMemberAtoms() {
        return atoms;
    }
}
