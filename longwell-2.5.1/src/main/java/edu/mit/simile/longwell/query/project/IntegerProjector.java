package edu.mit.simile.longwell.query.project;

import java.util.Iterator;
import java.util.Set;

import org.openrdf.model.Literal;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.repository.RepositoryException;

import edu.mit.simile.longwell.FixedSetBuilder;
import edu.mit.simile.longwell.Profile;

public class IntegerProjector extends PropertyProjector {
    
    public class IntegerProjection extends LongValueProjection {

        public IntegerProjection(Set objects) {
            super(IntegerProjector.this.m_profile, IntegerProjector.this.m_property, IntegerProjector.this.m_forward,
                    IntegerProjector.this.m_locale, objects);
        }

        protected Set internalGetObjects(Object value) throws QueryEvaluationException, RepositoryException {
            Set allObjects = getObjects();
            FixedSetBuilder builder = new FixedSetBuilder();

            int v = value != null ? ((Integer) value).intValue() : Integer.MIN_VALUE;

            Iterator i = allObjects.iterator();
            while (i.hasNext()) {
                URI r = (URI) i.next();
                if (v == getInteger(r)) {
                    builder.add(r);
                }
            }
            return builder.buildFixedSet();
        }

        public Object nodeToValue(Value v) {
            return nodeToLong(v);
        }

        public long getInteger(URI object) throws QueryEvaluationException, RepositoryException {
            Long l = (Long) getValue(object);
            return l == null ? Long.MIN_VALUE : l.longValue();
        }

        public long getMin() throws QueryEvaluationException, RepositoryException {
            long l = internalGetMin();
            return l;
        }

        public long getMax() throws QueryEvaluationException, RepositoryException {
            long l = internalGetMax();
            return l;
        }

        public int countObjects(long fromInclusive, long toExclusive) throws QueryEvaluationException, RepositoryException {
            return internalCountObjects(fromInclusive, toExclusive);
        }

        public Set getObjects(long fromInclusive, long toExclusive) throws QueryEvaluationException, RepositoryException {
            return internalGetObjects(fromInclusive, toExclusive);
        }

        protected long valueToLong(Object v) {
            long l = Long.MIN_VALUE;
            if (v != null) {
                if (v instanceof Integer) {
                    l = ((Integer) v).longValue();
                } else {
                    l = ((Long) v).longValue();
                }
            }
            return l; 
        }
    }

    public IntegerProjector(Profile profile, String parameter, String locale) {
        super(profile, parameter, locale);
    }

    public boolean isEfficientForRootProjection() {
        return false;
    }

    protected IProjection internalProject() {
        return new IntegerProjection(null);
    }

    protected IProjection internalProject(Set objects) {
        return new IntegerProjection(objects);
    }

    final protected Long nodeToLong(Value v) {
        if (v instanceof Literal) {
            try {
                return new Long(((Literal) v).getLabel());
            } catch (NumberFormatException e) {
                // Ignore
            }
        }
        return null;
    }

}
