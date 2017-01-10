package edu.mit.simile.longwell.query.project;

import java.util.Date;
import java.util.Iterator;
import java.util.Set;

import org.openrdf.model.Literal;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.repository.RepositoryException;

import edu.mit.simile.longwell.FixedSetBuilder;
import edu.mit.simile.longwell.Profile;
import edu.mit.simile.longwell.Utilities;

public class DateTimeProjector extends PropertyProjector {

    public class DateTimeProjection extends LongValueProjection {

        public DateTimeProjection(Set objects) {
            super(DateTimeProjector.this.m_profile, DateTimeProjector.this.m_property,
                    DateTimeProjector.this.m_forward, DateTimeProjector.this.m_locale, objects);
        }

        public Date getDate(URI object) throws QueryEvaluationException, RepositoryException {
            return (Date) getValue(object);
        }

        protected Set internalGetObjects(Object value) throws QueryEvaluationException, RepositoryException {
            Set allObjects = getObjects();
            FixedSetBuilder builder = new FixedSetBuilder();
            Date date = (Date) value;

            Iterator i = allObjects.iterator();
            while (i.hasNext()) {
                URI r = (URI) i.next();
                if (datesEqual(date, (Date) getValue(r))) {
                    builder.add(r);
                }
            }
            return builder.buildFixedSet();
        }

        public Object nodeToValue(Value v) {
            return nodeToDate(v);
        }

        public Date getEarliest() throws QueryEvaluationException, RepositoryException {
            return new Date(internalGetMin());
        }

        public Date getLatest() throws QueryEvaluationException, RepositoryException{
            return new Date(internalGetMax());
        }

        public Set getObjects(Date fromInclusive, Date toExclusive) throws QueryEvaluationException, RepositoryException {
            return internalGetObjects(fromInclusive != null ? fromInclusive.getTime() : Long.MIN_VALUE,
                    toExclusive != null ? toExclusive.getTime() : Long.MAX_VALUE);
        }

        public int countObjects(Date fromInclusive, Date toExclusive) throws QueryEvaluationException, RepositoryException {
            return internalCountObjects(fromInclusive != null ? fromInclusive.getTime() : Long.MIN_VALUE,
                    toExclusive != null ? toExclusive.getTime() : Long.MAX_VALUE);
        }

        protected long valueToLong(Object v) throws Exception {
            return ((Date) v).getTime();
        }

        public int countObjects(long fromInclusive, long toExclusive) throws QueryEvaluationException, RepositoryException {
            return internalCountObjects(fromInclusive, toExclusive);
        }

        public long getInteger(URI object) throws QueryEvaluationException, RepositoryException {
            return getDate(object).getTime();
        }

        public long getMax() throws QueryEvaluationException, RepositoryException {
            return internalGetMin();
        }

        public long getMin() throws QueryEvaluationException, RepositoryException {
            return internalGetMax();
        }

        public Set getObjects(long fromInclusive, long toExclusive) throws QueryEvaluationException, RepositoryException {
            return internalGetObjects(fromInclusive, toExclusive);
        }
    }

    public DateTimeProjector(Profile profile, String parameter, String locale) {
        super(profile, parameter, locale);
    }

    public boolean isEfficientForRootProjection() {
        return false;
    }

    protected IProjection internalProject() {
        return new DateTimeProjection(null);
    }

    protected IProjection internalProject(Set objects) {
        return new DateTimeProjection(objects);
    }

    final protected boolean datesEqual(Date date1, Date date2) {
        if (date1 == null) {
            return date2 == null;
        }
        return date1.equals(date2);
    }

    final protected Date nodeToDate(Value v) {
        if (v instanceof Literal) {
            return Utilities.parseDate(((Literal) v).getLabel());
        }
        return null;
    }
}
