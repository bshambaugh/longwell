package edu.mit.simile.longwell.query.project;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.openrdf.model.URI;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.repository.RepositoryException;

import edu.mit.simile.longwell.FixedSetBuilder;
import edu.mit.simile.longwell.Profile;
import edu.mit.simile.longwell.query.project.ProjectorUtilities.Pair;

public abstract class LongValueProjection extends PropertyProjection {

    protected Pair[] m_pairs;

    public LongValueProjection(Profile profile, URI property, boolean forward, String locale, Set objects) {
        super(profile, property, forward, locale, objects);
    }

    protected void buildPairs() throws QueryEvaluationException, RepositoryException {
        if (m_pairs != null) {
            return;
        }

        Map m = getObjectToValueMap();
        Set objects = getObjects();
        Set keys = m.keySet();

        m_pairs = new Pair[keys.size()];
        int index = 0;

        Iterator i = keys.iterator();
        while (i.hasNext()) {
            URI object = (URI) i.next();
            if (objects.contains(object)) {
                Object value = m.get(object);

                if (value != null) {
                    try {
                        long l = valueToLong(value);

                        Pair p = new Pair();
                        p.m_object = object;
                        p.m_value = l;

                        m_pairs[index++] = p;
                    } catch (Exception e) {
                        // Ignore
                    }
                }
            }
        }

        if (index < m_pairs.length) {
            Pair[] pairs = m_pairs;
            m_pairs = new Pair[index];

            while (--index >= 0) {
                m_pairs[index] = pairs[index];
            }
        }

        m_pairs = ProjectorUtilities.mergeSort(m_pairs, 0, m_pairs.length);
    }

    protected long internalGetMin() throws QueryEvaluationException, RepositoryException {
        buildPairs();
        return m_pairs.length > 0 ? (long) m_pairs[0].m_value : Long.MAX_VALUE;
    }

    protected long internalGetMax() throws QueryEvaluationException, RepositoryException {
        buildPairs();
        return m_pairs.length > 0 ? (long) m_pairs[m_pairs.length - 1].m_value : Long.MIN_VALUE;
    }

    protected int internalCountObjects(long fromInclusive, long toExclusive) throws QueryEvaluationException, RepositoryException {
        buildPairs();
        if (fromInclusive < toExclusive && m_pairs.length > 0) {
            int startIndex = ProjectorUtilities.lookupIndex(m_pairs, fromInclusive);
            int endIndex = ProjectorUtilities.lookupIndex(m_pairs, toExclusive);

            return startIndex < 0 ? (endIndex < 0 ? 0 : endIndex)
                    : ((endIndex < 0 ? m_pairs.length : endIndex) - startIndex);
        }
        return 0;
    }

    protected Set internalGetObjects(long fromInclusive, long toExclusive) throws QueryEvaluationException, RepositoryException {
        buildPairs();

        FixedSetBuilder builder = new FixedSetBuilder();
        if (fromInclusive < toExclusive && m_pairs.length > 0) {
            int startIndex = ProjectorUtilities.lookupIndex(m_pairs, fromInclusive);
            int endIndex = ProjectorUtilities.lookupIndex(m_pairs, toExclusive);

            if (startIndex >= 0) {
                if (endIndex < 0) {
                    endIndex = m_pairs.length;
                }

                for (int i = startIndex; i < endIndex; i++) {
                    builder.add(m_pairs[i].m_object);
                }
            }
        }
        return builder.buildFixedSet();
    }

    abstract protected long valueToLong(Object v) throws Exception;
}
