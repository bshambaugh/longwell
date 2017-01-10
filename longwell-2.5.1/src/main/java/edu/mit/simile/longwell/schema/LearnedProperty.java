package edu.mit.simile.longwell.schema;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.openrdf.model.Literal;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.impl.LiteralImpl;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.RepositoryResult;

import edu.mit.simile.longwell.Utilities;

public class LearnedProperty {
    
    final static private Logger s_logger = Logger.getLogger(LearnedProperty.class);

    final static String s_learned_occurrences = SchemaModel.s_learnedNamespace + "occurrences";
    final static String s_learned_uniqueOccurrences = SchemaModel.s_learnedNamespace + "uniqueOccurrences";
    final static String s_learned_subjects = SchemaModel.s_learnedNamespace + "subjects";
    final static String s_learned_objects = SchemaModel.s_learnedNamespace + "objects";
    final static String s_learned_typeCounts = SchemaModel.s_learnedNamespace + "typeCounts";

    final static public int s_type_literal = 0;
    final static public int s_type_numeric = 1;
    final static public int s_type_integer = 2;
    final static public int s_type_boolean = 3;
    final static public int s_type_dateTime = 4;
    final static public int s_type_uri = 5;
    final static public int s_type_latlong = 6;
    final static public int s_type_max = 7;
    
    final URI m_uri;

    final SchemaModel m_schemaModel;

    Set<URI> m_domainClasses = new HashSet<URI>();
    Set<URI> m_rangeClasses = new HashSet<URI>();

    long m_occurrences;
    long m_uniqueOccurrences;
    long m_subjects;
    long m_objects;

    long[] m_typeCounts = new long[LearnedProperty.s_type_max];

    LearnedProperty(SchemaModel schemaModel, URI uri) {
        m_uri = uri;
        m_schemaModel = schemaModel;
    }

    public URI getURI() {
        return m_uri;
    }

    public Set getDomainClasses() {
        return m_domainClasses;
    }

    public Set getRangeClasses() {
        return m_rangeClasses;
    }

    public String getLabel(String locale) {
        String label = "";
        try {
            label = m_schemaModel.getLabel(m_uri, locale);
        } catch (RepositoryException e) {
            s_logger.error("Error retrieving label for " + getURI(), e);
        }
        return label;
    }
    
    public long countOccurrences() {
        return m_occurrences;
    }

    public int getFanOut() {
        return (int) (m_occurrences / m_subjects);
    }

    public int getFanIn() {
        return (int) (m_occurrences / m_objects);
    }

    public float getUniqueness() {
        return ((float) m_uniqueOccurrences) / m_occurrences;
    }

    public float getTypeConfidence(int type) {
        return ((float) m_typeCounts[type]) / m_uniqueOccurrences;
    }

    public void load(RepositoryConnection c) throws RepositoryException {
        if (s_logger.isDebugEnabled()) s_logger.debug("> load()");
        try {
            URI forwardProperty = new URIImpl(LearnedClass.s_learned_forwardProperty);
            URI backwardProperty = new URIImpl(LearnedClass.s_learned_backwardProperty);

            RepositoryResult<Statement> results = c.getStatements(null, forwardProperty, m_uri, true);
            while (results.hasNext()) {
                Value v = ((Statement) results.next()).getObject();
                if (v instanceof URI) {
                    m_domainClasses.add(Utilities.dupURI((URI) v));
                }
            }
            results.close();

            results = c.getStatements(null, backwardProperty, m_uri, true);
            while (results.hasNext()) {
                Value v = ((Statement) results.next()).getObject();
                if (v instanceof URI) {
                    m_rangeClasses.add(Utilities.dupURI((URI) v));
                }
            }
            results.close();

            URI occurrencesProperty = new URIImpl(s_learned_occurrences);
            URI uniqueOccurrencesProperty = new URIImpl(s_learned_uniqueOccurrences);
            URI subjectsProperty = new URIImpl(s_learned_subjects);
            URI objectsProperty = new URIImpl(s_learned_objects);
            URI typeCountsProperty = new URIImpl(s_learned_typeCounts);

            m_occurrences = tryGetLong(c, m_uri, occurrencesProperty);
            m_uniqueOccurrences = tryGetLong(c, m_uri, uniqueOccurrencesProperty);
            m_subjects = tryGetLong(c, m_uri, subjectsProperty);
            m_objects = tryGetLong(c, m_uri, objectsProperty);

            try {
                String[] ss = StringUtils.splitPreserveAllTokens(tryGetString(c, m_uri, typeCountsProperty), ',');
                for (int i = 0; i < ss.length; i++) {
                    m_typeCounts[i] = Long.parseLong(ss[i]);
                }
            } catch (Exception e) {
                s_logger.warn(e);
            }
            
        } catch (Exception e) {
            s_logger.error(e);
        }
        if (s_logger.isDebugEnabled()) s_logger.debug("< load()");
    }

    public void learnAddition(RepositoryConnection repoConnection, RepositoryConnection cacheConnection) throws RepositoryException {
        if (s_logger.isDebugEnabled()) s_logger.debug("> learnAddition('" + m_uri + "')");
        
        try {
            Set<URI> domainClasses = m_schemaModel.m_queryManager.listDomainClasses(repoConnection, m_uri);
            domainClasses.removeAll(m_domainClasses);

            if (domainClasses.size() > 0) {
                m_domainClasses = new HashSet<URI>(m_domainClasses);
                m_domainClasses.addAll(domainClasses);
            }
        } catch (Exception e) {
            s_logger.error(e);
        }

        try {
            Set<URI> rangeClasses = m_schemaModel.m_queryManager.listRangeClasses(repoConnection, m_uri);
            rangeClasses.removeAll(m_rangeClasses);

            if (rangeClasses.size() > 0) {
                m_rangeClasses = new HashSet<URI>(m_rangeClasses);
                m_rangeClasses.addAll(rangeClasses);
            }
        } catch (Exception e) {
            s_logger.error(e);
        }

        try {
            Set pairs = m_schemaModel.m_queryManager.listSubjectObjectPairsOfProperty(repoConnection, m_uri);

            Set<Value> subjects = new HashSet<Value>();
            Set<Value> objects = new HashSet<Value>();

            Iterator i = pairs.iterator();
            while (i.hasNext()) {
                Value[] pair = (Value[]) i.next();

                subjects.add(pair[0]);
                objects.add(pair[1]);
            }

            m_occurrences += pairs.size();
            m_uniqueOccurrences += objects.size();

            m_subjects += subjects.size();
            m_objects += objects.size();

            i = objects.iterator();
            while (i.hasNext()) {
                analyzeValue((Value) i.next());
            }
        } catch (Exception e) {
            s_logger.error(e);
        }

        try {
            try {
                URI occurrencesProperty = new URIImpl(s_learned_occurrences);
                URI uniqueOccurrencesProperty = new URIImpl(s_learned_uniqueOccurrences);
                URI subjectsProperty = new URIImpl(s_learned_subjects);
                URI objectsProperty = new URIImpl(s_learned_objects);
                URI typeCountsProperty = new URIImpl(s_learned_typeCounts);

                cacheConnection.remove(m_uri, null, null); 
                cacheConnection.add(m_uri, RDF.TYPE, new URIImpl(SchemaModel.s_learned_Property));

                writeLong(cacheConnection, m_uri, occurrencesProperty, m_occurrences);
                writeLong(cacheConnection, m_uri, uniqueOccurrencesProperty, m_uniqueOccurrences);
                writeLong(cacheConnection, m_uri, subjectsProperty, m_subjects);
                writeLong(cacheConnection, m_uri, objectsProperty, m_objects);

                StringBuffer b = new StringBuffer();
                String separator = "";
                for (int i = 0; i < m_typeCounts.length; i++) {
                    b.append(separator);
                    b.append(Long.toString(m_typeCounts[i]));
                    separator = ",";
                }
                cacheConnection.add(m_uri, typeCountsProperty, new LiteralImpl(b.toString()));
            } catch (Exception e) {
                s_logger.error(e);
            }
            cacheConnection.commit();
        } catch (RepositoryException e) {
            s_logger.error(e);
            try {
                cacheConnection.rollback();
            } catch (RepositoryException se) {
                s_logger.error("Could not rollback failed transaction: " + se);
            }
        }
        if (s_logger.isDebugEnabled()) s_logger.debug("< learnAddition('" + m_uri + "')");
    }

    public void learnRemoval(RepositoryConnection r) {
        // Nothing to do
    }

    protected void analyzeValue(Value v) {
        final String[] protocolPrefixes = new String[] {
                // TODO where can we get the real list of protocol prefixes?
                "http:", "https:", "ftp:", "news:", "file:", "mailto:" 
        };

        if (v instanceof Literal) {
            String s = ((Literal) v).getLabel();

            parse: while (s.length() > 0) {
                try {
                    Integer.parseInt(s);
                    m_typeCounts[s_type_integer]++;
                    m_typeCounts[s_type_numeric]++;
                    break parse;
                } catch (NumberFormatException e) {
                    // Ignore s_logger.error(e);
                }

                try {
                    Double.parseDouble(s);
                    m_typeCounts[s_type_numeric]++;
                    break parse;
                } catch (NumberFormatException e) {
                    // Ignore s_logger.error(e);
                }

                if ("true".equals(s) || "false".equals(s)) {
                    m_typeCounts[s_type_boolean]++;
                    break parse;
                }

                for (int i = 0; i < protocolPrefixes.length; i++) {
                    if (s.startsWith(protocolPrefixes[i])) {
                        m_typeCounts[s_type_uri]++;
                        break parse;
                    }
                }

                if (Utilities.parseDate(s) != null) {
                    m_typeCounts[s_type_dateTime]++;
                    break parse;
                }

                parseLatLong: {
                    int comma = s.indexOf(',');
                    if (comma > 0 && comma < s.length() - 1) {
                        try {
                            Double.parseDouble(s.substring(0, comma));
                            Double.parseDouble(s.substring(comma + 1));
                            m_typeCounts[s_type_latlong]++;
                            break parse;
                        } catch (NumberFormatException e) {
                            break parseLatLong;
                        }
                    }
                }

                break;
            }
        }
    }

    protected long tryGetLong(RepositoryConnection c, URI subject, URI predicate) {
        if (s_logger.isDebugEnabled()) s_logger.debug("> tryGetLong()");
        long result = 0;
        try {
            result = Long.parseLong(tryGetString(c, subject, predicate));
        } catch (Exception e) {
            // Ignore s_logger.error(e);
        }
        if (s_logger.isDebugEnabled()) s_logger.debug("< tryGetLong()");
        return result;
    }

    protected void writeLong(RepositoryConnection c, URI subject, URI predicate, long l) throws RepositoryException {
        c.add(subject, predicate, new LiteralImpl(Long.toString(l)));
    }

    protected String tryGetString(RepositoryConnection c, URI subject, URI predicate) throws RepositoryException {
        if (s_logger.isDebugEnabled()) s_logger.debug("> tryGetString()");
        RepositoryResult<Statement> results = c.getStatements(subject, predicate, null, true);
        Statement st = (Statement) results.next();
        results.close();
        if (s_logger.isDebugEnabled()) s_logger.debug("< tryGetString()");
        return ((Literal) st.getObject()).getLabel();
    }

}
