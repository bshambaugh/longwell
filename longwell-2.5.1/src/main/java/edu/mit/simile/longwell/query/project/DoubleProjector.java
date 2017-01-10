package edu.mit.simile.longwell.query.project;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.openrdf.model.Literal;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.repository.RepositoryException;

import edu.mit.simile.longwell.FixedSetBuilder;
import edu.mit.simile.longwell.Profile;
import edu.mit.simile.longwell.query.project.ProjectorUtilities.DoublePair;

public class DoubleProjector extends PropertyProjector {
    
    public class DoubleProjection extends PropertyProjection {

        protected DoublePair[] m_pairs;
        
        public DoubleProjection(Set objects) {
            super(DoubleProjector.this.m_profile, DoubleProjector.this.m_property, DoubleProjector.this.m_forward,
                    DoubleProjector.this.m_locale, objects);
        }

        protected void buildPairs() throws QueryEvaluationException, RepositoryException {
            if (m_pairs != null) {
                return;
            }

            Map m = getObjectToValueMap();
            Set objects = getObjects();
            Set keys = m.keySet();

            m_pairs = new DoublePair[keys.size()];
            int index = 0;

            Iterator i = keys.iterator();
            while (i.hasNext()) {
                URI object = (URI) i.next();
                if (objects.contains(object)) {
                    Object value = m.get(object);

                    if (value != null) {
                        try {
                            double d = valueToDouble(value);

                            DoublePair p = new DoublePair();
                            p.m_object = object;
                            p.m_value = d;

                            m_pairs[index++] = p;
                        } catch (Exception e) {
                            // Ignore
                        }
                    }
                }
            }

            if (index < m_pairs.length) {
                DoublePair[] pairs = m_pairs;
                m_pairs = new DoublePair[index];

                while (--index >= 0) {
                    m_pairs[index] = pairs[index];
                }
            }

            m_pairs = ProjectorUtilities.mergeSort(m_pairs, 0, m_pairs.length);
        }

        protected double internalGetMin() throws QueryEvaluationException, RepositoryException {
            buildPairs();
            return m_pairs.length > 0 ? (double) m_pairs[0].m_value : Double.MAX_VALUE;
        }

        protected double internalGetMax() throws QueryEvaluationException, RepositoryException {
            buildPairs();
            return m_pairs.length > 0 ? (double) m_pairs[m_pairs.length - 1].m_value : Double.MIN_VALUE;
        }

        protected int internalCountObjects(double fromInclusive, double toExclusive) throws QueryEvaluationException, RepositoryException {
            buildPairs();
            if (fromInclusive < toExclusive && m_pairs.length > 0) {
                int startIndex = ProjectorUtilities.lookupIndex(m_pairs, fromInclusive);
                int endIndex = ProjectorUtilities.lookupIndex(m_pairs, toExclusive);

                return startIndex < 0 ? (endIndex < 0 ? 0 : endIndex)
                        : ((endIndex < 0 ? m_pairs.length : endIndex) - startIndex);
            }
            return 0;
        }

        protected Set internalGetObjects(double fromInclusive, double toExclusive) throws QueryEvaluationException, RepositoryException {
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
        
        protected Set internalGetObjects(Object value) throws QueryEvaluationException, RepositoryException {
            Set allObjects = getObjects();
            FixedSetBuilder builder = new FixedSetBuilder();

            double v = value != null ? ((Double) value).doubleValue() : Double.MIN_VALUE;

            Iterator i = allObjects.iterator();
            while (i.hasNext()) {
                URI r = (URI) i.next();
                if (v == getDouble(r)) {
                    builder.add(r);
                }
            }
            return builder.buildFixedSet();
        }

        public Object nodeToValue(Value v) {
            return nodeToDouble(v);
        }

        public double getDouble(URI object) throws QueryEvaluationException, RepositoryException {
            Double d = (Double) getValue(object);
            return d == null ? Double.MIN_VALUE : d.doubleValue();
        }

        public double getMin() throws QueryEvaluationException, RepositoryException {
            return internalGetMin();
        }

        public double getMax() throws QueryEvaluationException, RepositoryException {
            return internalGetMax();
        }

        public int countObjects(double fromInclusive, double toExclusive) throws QueryEvaluationException, RepositoryException {
            return internalCountObjects(fromInclusive, toExclusive);
        }

        public Set getObjects(double fromInclusive, double toExclusive) throws QueryEvaluationException, RepositoryException {
            return internalGetObjects(fromInclusive, toExclusive);
        }

        protected double valueToDouble(Object v) {
            return v != null ? ((Double) v).doubleValue() : Double.MIN_VALUE;
        }
    }

    public DoubleProjector(Profile profile, String parameter, String locale) {
        super(profile, parameter, locale);
    }

    public boolean isEfficientForRootProjection() {
        return false;
    }

    protected IProjection internalProject() {
        return new DoubleProjection(null);
    }

    protected IProjection internalProject(Set objects) {
        return new DoubleProjection(objects);
    }

    final protected Double nodeToDouble(Value v) {
        if (v instanceof Literal) {
            try {
                return new Double(((Literal) v).getLabel());
            } catch (NumberFormatException e) {
                // Ignore
            }
        }
        return null;
    }
}
