package net.fortytwo.extendo.brain.rdf.classes;

import net.fortytwo.extendo.brain.Atom;
import net.fortytwo.extendo.brain.rdf.AtomClass;
import net.fortytwo.extendo.brain.rdf.AtomRegex;
import net.fortytwo.extendo.brain.rdf.RDFizationContext;
import net.fortytwo.extendo.brain.rdf.classes.collections.DocumentAboutTopicCollection;
import net.fortytwo.extendo.brain.rdf.classes.collections.GenericCollection;
import net.fortytwo.extendo.brain.rdf.classes.collections.PersonCollection;
import net.fortytwo.extendo.brain.rdf.classes.collections.TopicCollection;
import net.fortytwo.extendo.brain.wiki.NoteParser;
import net.fortytwo.extendo.rdf.vocab.Bibo;
import net.fortytwo.extendo.rdf.vocab.FOAF;
import org.apache.commons.validator.routines.ISBNValidator;
import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.vocabulary.DCTERMS;
import org.openrdf.rio.RDFHandler;
import org.openrdf.rio.RDFHandlerException;

import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * @author Joshua Shinavier (http://fortytwo.net)
 */
public class Document extends AtomClass {
    private static final Logger logger = Logger.getLogger(Document.class.getName());
    private static final ISBNValidator isbnValidator = new ISBNValidator();

    public Document() {
        super(
                "document",
                Pattern.compile("[A-Z].+"),
                null,
                new AtomRegex(Arrays.asList(
                        new AtomRegex.El(new NickHandler(),
                                AtomRegex.Modifier.ZeroOrOne, AKAReference.class),
                        new AtomRegex.El(new PageHandler(),
                                AtomRegex.Modifier.ZeroOrMore, WebPage.class),

                        new AtomRegex.El(new DocumentsAboutTopicHandler(),
                                AtomRegex.Modifier.ZeroOrOne, DocumentAboutTopicCollection.class),

                        // multiple RFID tags on an object are possible, though they may be uncommon
                        new AtomRegex.El(2, new RFIDHandler(),
                                AtomRegex.Modifier.ZeroOrMore, RFIDReference.class),

                        new AtomRegex.El(2, new BibtexHandler(),
                                AtomRegex.Modifier.ZeroOrOne, BibtexReference.class),
                        new AtomRegex.El(2, new ISBNHandler(),
                                AtomRegex.Modifier.ZeroOrOne, ISBNReference.class),

                        // note: without a collection, only the first author is recognized.
                        // Otherwise, we run the risk of incorrectly classifying publishers (which often
                        // follow authors) as people.
                        new AtomRegex.El(new MakerHandler(),
                                AtomRegex.Modifier.ZeroOrOne, AuthorCollection.class, Person.class),

                        new AtomRegex.El(new TopicHandler(),
                                AtomRegex.Modifier.ZeroOrOne, TopicCollection.class),
                        new AtomRegex.El(new NoteHandler(),
                                AtomRegex.Modifier.ZeroOrOne, NoteCollection.class),
                        new AtomRegex.El(null,
                                AtomRegex.Modifier.ZeroOrMore)
                )));
    }

    @Override
    protected boolean isCollectionClass() {
        return false;
    }

    @Override
    public URI toRDF(Atom a, RDFizationContext context) throws RDFHandlerException {
        ValueFactory vf = context.getValueFactory();
        RDFHandler handler = context.getHandler();

        // TODO: a more specific type than foaf:Document may be appropriate (WebPage also uses foaf:Document)
        URI self = handleTypeAndAlias(a, vf, handler, FOAF.DOCUMENT);

        handler.handleStatement(vf.createStatement(self, DCTERMS.TITLE, vf.createLiteral(a.getValue())));

        return self;
    }

    private static class ISBNHandler implements FieldHandler {
        @Override
        public void handle(Atom object, RDFizationContext context) throws RDFHandlerException {
            String value = object.getValue();
            URI predicate;

            int i = value.indexOf(':');
            String isbn = value.substring(i + 1).trim();

            if (value.startsWith("ISBN-10:")) {
                predicate = Bibo.ISBN_10;
                if (!isbnValidator.isValidISBN10(isbn)) {
                    logger.warning("not a valid ISBN-10 value: " + isbn);
                    return;
                }
            } else if (value.startsWith("ISBN-13:")) {
                predicate = Bibo.ISBN_13;
                if (!isbnValidator.isValidISBN13(isbn)) {
                    logger.warning("not a valid ISBN-13 value: " + isbn);
                    return;
                }
            } else if (value.startsWith("ISBN:")) {
                predicate = Bibo.ISBN;
                if (!isbnValidator.isValid(isbn)) {
                    logger.warning("not a valid ISBN value: " + isbn);
                    return;
                }
            } else {
                logger.log(Level.WARNING, "invalid ISBN value: " + value);
                return;
            }

            ValueFactory vf = context.getValueFactory();
            context.getHandler().handleStatement(vf.createStatement(
                    context.getSubjectUri(), predicate, vf.createLiteral(isbn)));
        }
    }

    private static class BibtexHandler implements FieldHandler {
        @Override
        public void handle(Atom object, RDFizationContext context) throws RDFHandlerException {
            String entry = object.getValue().trim();

            ValueFactory vf = context.getValueFactory();
            context.getHandler().handleStatement(vf.createStatement(
                    context.getSubjectUri(), DCTERMS.BIBLIOGRAPHIC_CITATION, vf.createLiteral(entry)));
        }
    }

    private static class MakerHandler implements FieldHandler {
        @Override
        public void handle(Atom object, RDFizationContext context) throws RDFHandlerException {
            ValueFactory vf = context.getValueFactory();
            URI objectURI = context.uriOf(object);
            context.getHandler().handleStatement(vf.createStatement(
                    // note: dc:creator is recommended only for simple textual names, hence foaf:maker
                    context.getSubjectUri(), FOAF.MAKER, objectURI));
        }
    }

    private static class TopicHandler implements FieldHandler {
        @Override
        public void handle(Atom object, RDFizationContext context) throws RDFHandlerException {
            ValueFactory vf = context.getValueFactory();
            URI objectURI = context.uriOf(object);
            context.getHandler().handleStatement(vf.createStatement(
                    context.getSubjectUri(), FOAF.TOPIC, objectURI));
            // The skos:note on dc:subject reads "This term is intended to be used with non-literal values
            // as defined in the DCMI Abstract Model (http://dublincore.org/documents/abstract-model/) [...]"
            //context.getSubjectUri(), DCTerms.SUBJECT, objectURI));
        }
    }

    private static class NoteHandler implements FieldHandler {
        @Override
        public void handle(Atom object, RDFizationContext context) throws RDFHandlerException {
            // TODO
        }
    }

    public static class AuthorCollection extends PersonCollection {
        public AuthorCollection() {
            super();
            name = "author-collection";
            valueRegex = Pattern.compile("the authors of .+");
        }
    }

    public static class NoteCollection extends GenericCollection {
        public NoteCollection() {
            super();
            name = "notes-from";
            valueRegex = Pattern.compile("some notes from .+");
        }
    }
}
