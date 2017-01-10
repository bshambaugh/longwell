package edu.mit.simile.longwell.schema;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.log4j.Logger;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.RepositoryResult;

import edu.mit.simile.longwell.FixedSetBuilder;

public class LearnedClass {

    final static private Logger s_logger = Logger.getLogger(LearnedClass.class);

    final static String s_learned_forwardProperty = SchemaModel.s_learnedNamespace + "forwardProperty";
    final static String s_learned_backwardProperty = SchemaModel.s_learnedNamespace + "backwardProperty";

    final URI m_uri;

    final SchemaModel m_schemaModel;

    Set<URI> m_forwardProperties = new HashSet<URI>();
    Set<URI> m_backwardProperties = new HashSet<URI>();
    Set<URI> m_properties = new HashSet<URI>();

    protected Set<Value> m_allItems;

    LearnedClass(SchemaModel schemaModel, URI uri) {
        m_uri = uri;
        m_schemaModel = schemaModel;
    }

    public URI getURI() {
        return m_uri;
    }

    public String getLabel(String locale) {
        String label = "";
        try {
            label = m_schemaModel.getLabel(getURI(), locale);
        } catch (RepositoryException e) {
            s_logger.error("Error retrieving label for " + getURI(), e);
        }
        return label;
    }

    public Set<URI> getAllProperties() {
        return m_properties;
    }

    public Set<URI> getProperties(boolean forward) {
        return forward ? m_forwardProperties : m_backwardProperties;
    }

    public Set<Value> getItems() {
        if (m_allItems == null) {
            m_allItems = internalGetItems();
        }
        return m_allItems;
    }

    public int countItems() {
        return getItems().size();
    }

    public boolean containsItem(URI item) {
        boolean contains = false;
        try {
            RepositoryConnection c = null;
            try {
                c = m_schemaModel.getProfile().getRepository().getConnection();
                contains = m_schemaModel.m_queryManager.containsStatement(c, item, RDF.TYPE, m_uri);
            } finally {
                if (c != null) c.close();
            }
        } catch (Exception e) {
            s_logger.error(e);
        }
        return contains;
    }

    public void load(RepositoryConnection c) {
        if (s_logger.isDebugEnabled()) s_logger.debug("> load()");
        
        try {
            URI forwardProperty = new URIImpl(s_learned_forwardProperty);
            URI backwardProperty = new URIImpl(s_learned_backwardProperty);

            RepositoryResult<Statement> results = c.getStatements(m_uri, forwardProperty, null, true);
            while (results.hasNext()) {
                Value v = ((Statement) results.next()).getObject();
                if (v instanceof URI) {
                    URI u = (URI) v;
                    m_forwardProperties.add(u);
                    m_properties.add(u);
                }
            }
            results.close();

            results = c.getStatements(m_uri, backwardProperty, null, true);
            while (results.hasNext()) {
                Value v = ((Statement) results.next()).getObject();
                if (v instanceof URI) {
                    URI u = (URI) v;
                    m_backwardProperties.add(u);
                    m_properties.add(u);
                }
            }
            results.close();
        } catch (Exception e) {
            s_logger.error(e);
        }
        if (s_logger.isDebugEnabled()) s_logger.debug("< load()");
    }

    public void learnAddition(RepositoryConnection repoConnection, RepositoryConnection cacheConnection) {
        if (s_logger.isDebugEnabled()) s_logger.debug("> learnAddition('" + m_uri + "')");

        try {
            try {
                cacheConnection.add(m_uri, RDF.TYPE, new URIImpl(SchemaModel.s_learned_Class));
            } catch (Exception e) {
                s_logger.error(e);
            }

            try {
                if (m_allItems != null) {
                    FixedSetBuilder builder = new FixedSetBuilder();
                    builder.addAll(m_allItems);
                    builder.addAll(m_schemaModel.m_queryManager.listSubjectsOfProperty(repoConnection, RDF.TYPE, m_uri));
                    m_allItems = builder.buildFixedSet();
                }

                Set forwardProperties = m_schemaModel.m_queryManager.listForwardPropertiesOfClass(repoConnection, getURI());
                forwardProperties.removeAll(m_forwardProperties);

                if (forwardProperties.size() > 0) {
                    URI forwardProperty = new URIImpl(s_learned_forwardProperty);

                    m_forwardProperties = new HashSet<URI>(m_forwardProperties);
                    m_forwardProperties.addAll(forwardProperties);

                    Iterator i = forwardProperties.iterator();
                    while (i.hasNext()) {
                        URI property = (URI) i.next();
                        m_forwardProperties.add(property);
                        cacheConnection.add(m_uri, forwardProperty, property);
                    }
                }

                Set backwardProperties = m_schemaModel.m_queryManager.listBackwardPropertiesOfClass(repoConnection, getURI());
                backwardProperties.removeAll(m_backwardProperties);

                if (backwardProperties.size() > 0) {
                    URI backwardProperty = new URIImpl(s_learned_backwardProperty);

                    m_backwardProperties = new HashSet<URI>(m_backwardProperties);
                    m_backwardProperties.addAll(backwardProperties);

                    Iterator i = backwardProperties.iterator();
                    while (i.hasNext()) {
                        URI property = (URI) i.next();
                        m_backwardProperties.add(property);
                        cacheConnection.add(m_uri, backwardProperty, property);
                    }
                }

                if (forwardProperties.size() > 0 || backwardProperties.size() > 0) {
                    m_properties = new HashSet<URI>(m_properties);
                    m_properties.addAll(m_forwardProperties);
                    m_properties.addAll(m_backwardProperties);
                }
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

    public void learnRemoval(RepositoryConnection c) {
        if (s_logger.isDebugEnabled()) s_logger.debug("> learnRemoval('" + m_uri + "')");
        try {
            if (m_allItems != null) {
                FixedSetBuilder builder = new FixedSetBuilder();
                builder.addAll(m_allItems);
                builder.removeAll(m_schemaModel.m_queryManager.listSubjectsOfProperty(c, RDF.TYPE, m_uri));
                m_allItems = builder.buildFixedSet();
            }
        } catch (Exception e) {
            s_logger.error(e);
        }
        if (s_logger.isDebugEnabled()) s_logger.debug("< learnRemoval('" + m_uri + "')");
    }

    protected Set<Value> internalGetItems() {
        if (s_logger.isDebugEnabled()) s_logger.debug("> internalGetItems()");
        Set values = null;
        try {
            RepositoryConnection c = null;
            try {
                c = m_schemaModel.getProfile().getRepository().getConnection();
                values = m_schemaModel.m_queryManager.listSubjectsOfProperty(c, RDF.TYPE, m_uri);
            } finally {
                if (c != null) c.close();
            }
        } catch (Exception e) {
            s_logger.error(e);
        }
        if (s_logger.isDebugEnabled()) s_logger.debug("< internalGetItems()");
        return (values != null) ? values : new HashSet();
    }

}
