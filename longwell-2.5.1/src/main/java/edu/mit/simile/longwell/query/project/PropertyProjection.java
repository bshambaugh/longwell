package edu.mit.simile.longwell.query.project;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.impl.LiteralImpl;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;

import edu.mit.simile.longwell.FixedSetBuilder;
import edu.mit.simile.longwell.Profile;
import edu.mit.simile.longwell.QueryManager;

public class PropertyProjection extends ProjectionBase {

    final static private Logger s_logger = Logger.getLogger(PropertyProjection.class);

    final protected Profile m_profile;
    final protected QueryManager m_queryManager;
    final protected URI m_property;
    final protected boolean m_forward;
    final protected Set m_objects;

    public PropertyProjection(Profile profile, URI property, boolean forward, String locale, Set objects) {
        super(locale, profile.getCacheFactory());
        m_profile = profile;
        m_queryManager = profile.getQueryManager();
        m_property = property;
        m_forward = forward;
        m_objects = objects;
    }

    public float getUniqueness() {
        return m_profile.getSchemaModel().getLearnedProperty(m_property).getUniqueness();
    }

    public Set getObjects() {
        if (m_objects != null) {
            return m_objects;
        }
        return m_profile.getSchemaModel().getAllItems();
    }

    public Set getValues() throws QueryEvaluationException, RepositoryException {
        if (optimizeForType()) {
            if (m_values == null) {
                if (m_objects != null) {
                    m_values = m_profile.getSchemaModel().getLearnedClassURIsOfItems(m_objects);
                } else {
                    m_values = m_profile.getSchemaModel().getLearnedClassURIs();
                }
            }
        } else if (m_objects != null) {
            return super.getValues();
        }

        if (m_values == null) {
            try {
                RepositoryConnection c = null;
                try {
                    c = m_profile.getRepository().getConnection();
                    m_values = m_queryManager.listObjectsOfProperty(c, m_property);
                } finally {
                    if (c != null) c.close();
                }
            } catch (Exception e) {
                s_logger.error(e);
            }
        }

        return m_values;
    }
    
    protected Object internalGetValue(URI object) throws QueryEvaluationException, RepositoryException {
        s_logger.debug("> inernalGetValue(" + object + ")");
        Object o = null;
        RepositoryConnection c = null;
        try {
            c = m_profile.getRepository().getConnection();
            if (optimizeForType()) {
                o = m_profile.getSchemaModel().getLearnedClassURIOfItem(object);
            } else if (m_forward) {
                o = nodeToValue(m_queryManager.getObjectOfProperty(c, object, m_property));
            } else {
                o = nodeToValue(m_queryManager.getSubjectOfProperty(c, m_property, object));
            }
        } finally {
            if (c != null) c.close();
        }
        s_logger.debug("< inernalGetValue(" + object + ")");
        return o;
    }

    protected Set internalGetObjects(Object value) throws QueryEvaluationException, RepositoryException {
        if (optimizeForType() && value instanceof URI) {
            if (value == null) {
                return new HashSet();
            }

            Set objects = m_profile.getSchemaModel().getItemsOfClass((URI) value);
            if (m_objects != null) {
                FixedSetBuilder builder = new FixedSetBuilder();

                builder.addAll(objects);
                builder.retainAll(m_objects);

                objects = builder.buildFixedSet();
            }

            return objects;
        }

        if (value != null) {
            Set objects = new HashSet();

            try {
                objects = iterateObjectsWithValue(value instanceof Value ? (Value) value : new LiteralImpl(value
                        .toString()));

                if (m_objects != null) {
                    FixedSetBuilder builder = new FixedSetBuilder();

                    builder.addAll(objects);
                    builder.retainAll(m_objects);

                    objects = builder.buildFixedSet();
                }
            } catch (Exception e) {
                s_logger.error(e);
            }

            return objects;
        }

        FixedSetBuilder builder = new FixedSetBuilder();

        builder.addAll(getObjects());
        try {
            builder.removeAll(iterateObjectsWithAnyValue());
        } catch (Exception e) {
            s_logger.error(e);
        }

        return builder.buildFixedSet();
    }

    protected Set internalGetObjectsWithValues(Set values) throws QueryEvaluationException, RepositoryException {
        if (optimizeForType()) {
            return m_profile.getSchemaModel().getItemsOfClasses(values);
        }
        return super.internalGetObjectsWithValues(values);
    }

    protected Set iterateObjectsWithValue(Value value) throws Exception {
        Set set = null;
        RepositoryConnection c = null;
        try {
            c = m_profile.getRepository().getConnection();
            if (m_forward) {
                set = m_queryManager.listSubjectsOfProperty(c, m_property, value);
            } else {
                set = m_queryManager.listObjectsOfProperty(c, (URI) value, m_property);
            }
        } finally {
            if (c != null) c.close();
        }
        return set;
    }

    protected Set iterateObjectsWithAnyValue() throws Exception {
        Set set = null;
        RepositoryConnection c = null;
        try {
            c = m_profile.getRepository().getConnection();
            if (m_forward) {
                set = m_queryManager.listSubjectsOfProperty(c, m_property);
            } else {
                set = m_queryManager.listObjectsOfProperty(c, m_property);
            }
        } finally {
            if (c != null) c.close();
        }
        return set;
    }

    protected boolean optimizeForType() {
        return (m_property.equals(RDF.TYPE) && m_forward);
    }
    
    protected Map internalGetObjectToValueMap() {
        Map m = new HashMap();
        fillObjectToValueMap(m);
        return m;
    }

    protected Map internalGetValueToObjectsMap() {
        Map m = new HashMap();
        fillValueToObjectsMap(m);
        return m;
    }
    
    protected void fillObjectToValueMap(Map m) {
        try {
            RepositoryConnection c = null;
            try {
                c = m_profile.getRepository().getConnection();
                m_queryManager.fillObjectToValueMap(c, m_property, m_forward, m, this);
            } finally {
                if (c != null) c.close();
            }
        } catch (Exception e) {
            s_logger.error(e);
        }
    }

    protected void fillValueToObjectsMap(Map m) {
        try {
            RepositoryConnection c = null;
            try {
                c = m_profile.getRepository().getConnection();
                m_queryManager.fillValueToObjectMap(c, m_property, m_forward, m, m_objects);
            } finally {
                if (c != null) c.close();
            }
        } catch (Exception e) {
            s_logger.error(e);
        }
    }
}
