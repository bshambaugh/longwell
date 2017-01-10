package edu.mit.simile.longwell.query.project;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.repository.RepositoryException;

import edu.mit.simile.longwell.Cache;
import edu.mit.simile.longwell.CacheFactory;
import edu.mit.simile.longwell.FixedSetBuilder;

public abstract class ProjectionBase implements IProjection {

    final protected String m_locale;
    final protected Cache m_objectToValueCache;
    final protected Cache m_valueToObjectsCache;
    final protected Cache m_valuesToObjectsCache;
    final protected Object s_null = ProjectionBase.class; // could be anything

    protected Set m_values;
    protected Map m_objectToValueMap;
    protected Map m_valueToObjectsMap;

    protected ProjectionBase(String locale, CacheFactory f) {
        m_locale = locale != null ? locale : "";
        m_objectToValueCache = f.getCache("object-to-value", false);
        m_valueToObjectsCache = f.getCache("value-to-objects", false);
        m_valuesToObjectsCache = f.getCache("values-to-objects", false);
    }

    public Set getValues() throws QueryEvaluationException, RepositoryException {
        if (m_values == null) {
            Map m = getObjectToValueMap();

            FixedSetBuilder builder = new FixedSetBuilder();

            Set objects = getObjects();

            if (objects.size() < m.size()) {
                Iterator i = objects.iterator();
                while (i.hasNext()) {
                    builder.add(m.get(i.next()));
                }
            } else {
                Iterator i = m.keySet().iterator();
                while (i.hasNext()) {
                    if (!objects.contains(i.next())) {
                        i.remove();
                    }
                }
                builder.addAll(m.values());
            }

            m_values = builder.buildFixedSet();
        }
        return m_values;
    }

    public Object getValue(URI object) throws QueryEvaluationException, RepositoryException {
        Object value = m_objectToValueCache.get(object);
        if (value == null) {
            value = internalGetValue(object);
            m_objectToValueCache.put(object, value);
        }
        return value;
    }

    public int countObjects() throws QueryEvaluationException, RepositoryException {
        return getObjects().size();
    }

    public Set getObjects(Object value) throws QueryEvaluationException, RepositoryException {
        Object key = (value != null) ? value : s_null;
        Set objects = (Set) m_valueToObjectsCache.get(key);
        if (objects == null) {
            objects = internalGetObjects(value);
            m_valueToObjectsCache.put(key, objects);
        }
        return objects;
    }

    public int countObjects(Object value) throws QueryEvaluationException, RepositoryException {
        return getObjects(value).size();
    }

    public Set getObjectsWithValues(Set values) throws QueryEvaluationException, RepositoryException {
        Set objects = (Set) m_valuesToObjectsCache.get(values);
        if (objects == null) {
            objects = internalGetObjectsWithValues(values);

            m_valuesToObjectsCache.put(values, objects);
        }
        return objects;
    }

    public int countObjectsWithValues(Set values) throws QueryEvaluationException, RepositoryException {
        return getObjectsWithValues(values).size();
    }

    public Map getObjectToValueMap() throws QueryEvaluationException, RepositoryException {
        if (m_objectToValueMap == null) {
            m_objectToValueMap = internalGetObjectToValueMap();
        }
        return m_objectToValueMap;
    }

    public Map getValueToObjectsMap() throws QueryEvaluationException, RepositoryException {
        if (m_valueToObjectsMap == null) {
            m_valueToObjectsMap = internalGetValueToObjectsMap();
        }
        return m_valueToObjectsMap;
    }

    public String getLocale() {
        return m_locale;
    }

    public Object nodeToValue(Value v) {
        return v;
    }
    
    abstract protected Object internalGetValue(URI object) throws QueryEvaluationException, RepositoryException;

    abstract protected Set internalGetObjects(Object value) throws QueryEvaluationException, RepositoryException;

    abstract protected Map internalGetObjectToValueMap() throws QueryEvaluationException, RepositoryException;

    abstract protected Map internalGetValueToObjectsMap() throws QueryEvaluationException, RepositoryException;

    protected Set internalGetObjectsWithValues(Set values) throws QueryEvaluationException, RepositoryException {
        FixedSetBuilder builder = new FixedSetBuilder();

        Iterator i = values.iterator();
        while (i.hasNext()) {
            builder.addAll(getObjects(i.next()));
        }

        return builder.buildFixedSet();
    }
}
