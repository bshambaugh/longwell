package edu.mit.simile.longwell.query.engine;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.openrdf.model.Literal;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.repository.RepositoryException;

import edu.mit.simile.longwell.Cache;
import edu.mit.simile.longwell.Profile;
import edu.mit.simile.longwell.query.Order;
import edu.mit.simile.longwell.query.project.IProjection;
import edu.mit.simile.longwell.query.project.IProjector;
import edu.mit.simile.longwell.query.project.DateTimeProjector.DateTimeProjection;
import edu.mit.simile.longwell.query.project.IntegerProjector.IntegerProjection;
import edu.mit.simile.longwell.schema.SchemaModel;

public class Indexer {

    final protected Profile m_profile;
    final protected QueryEngine m_engine;
    final protected Cache m_results;

    public class Entry {

        final public String m_label;
        final public int m_pageIndex;
        final public int m_count;

        public Entry(String label, int pageIndex, int count) {
            m_label = label;
            m_pageIndex = pageIndex;
            m_count = count;
        }

        public String getLabel() {
            return m_label;
        }

        public int getPageIndex() {
            return m_pageIndex;
        }

        public int getCount() {
            return m_count;
        }
    }
    
    Indexer(Profile profile, QueryEngine engine) {
        m_profile = profile;
        m_engine = engine;
        m_results = profile.getCacheFactory().getCache("results", false);
    }

    public List index(Set objects, List sortedObjects, List orders, int itemsPerPage, String locale, boolean fresh)
            throws QueryEvaluationException {

        List results = (List) m_results.get(sortedObjects);
        if (results == null || fresh) {
            results = internalIndex(objects, sortedObjects, orders, itemsPerPage, locale);
            m_results.put(sortedObjects, results);
        }
        return results;
    }

    protected List internalIndex(Set objects, List sortedObjects, List orders, int itemsPerPage, String locale) {

        List<Entry> entries = new ArrayList<Entry>();
        if (orders.size() > 0) {
            try {
                Order order = (Order) orders.get(0);

                IProjector projector = m_engine.getProjectorManager().getProjector(order.m_projectorName,
                        order.m_projectorParameter, locale);
                IProjection projection = projector.project(objects);

                if (projection instanceof IntegerProjection) {
                    indexIntegers(sortedObjects, (IntegerProjection) projection, itemsPerPage, order.m_ascending,
                            locale, entries);
                } else if (projection instanceof DateTimeProjection) {
                    indexDates(sortedObjects, (DateTimeProjection) projection, itemsPerPage, order.m_ascending,
                            locale, entries);
                } else {
                    indexStrings(sortedObjects, projection, itemsPerPage, locale, entries);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return entries;
    }

    protected void indexIntegers(List objects, IntegerProjection projection, int itemsPerPage, boolean ascending,
            String locale, List<Entry> entries) throws QueryEvaluationException, RepositoryException {

        long min = projection.getMin();
        long max = projection.getMax();
        long diff = max - min;

        long interval = 10;
        while (diff / interval > 10) {
            interval *= 10;
        }
        if (diff / interval < 2) {
            interval /= 10;
        }

        min = (min / interval) * interval;
        max = (1 + max / interval) * interval;

        long intervalCount = (max - min) / interval;
        int soFar = ascending ? objects.size() - projection.countObjects(min, Integer.MAX_VALUE) : projection
                .countObjects(max, Integer.MAX_VALUE);

        long start = ascending ? 0 : intervalCount - 1;
        long stop = ascending ? intervalCount : -1;
        long change = ascending ? 1 : -1;

        for (long j = start; j != stop; j += change) {
            long from = (min + j * interval);
            long to = (min + j * interval + (interval > 1 ? interval - 1 : interval));
            int count = projection.countObjects(from, to);

            if (count > 0) {
                entries.add(new Entry(Long.toString(from), soFar / itemsPerPage, count));

                soFar += count;
            }
        }
    }

    final static protected long s_day = 24 * 60 * 60 * 1000;
    final static protected long s_month = s_day * 30;
    final static protected long s_year = s_day * 365;
    final static protected long s_decade = s_year * 10;
    final static protected long s_century = s_year * 100;

    protected void indexDates(List objects, DateTimeProjection projection, int itemsPerPage, boolean ascending,
            String locale, List<Entry> entries) throws QueryEvaluationException, RepositoryException {

        Date earliestD = projection.getEarliest();
        Date latestD = projection.getLatest();
        long diff = latestD.getTime() - earliestD.getTime();

        Calendar earliest = Calendar.getInstance();
        earliest.setTime(earliestD);
        Calendar latest = Calendar.getInstance();
        latest.setTime(latestD);

        int earliestYear = earliest.get(Calendar.YEAR);
        int latestYear = latest.get(Calendar.YEAR);

        if (diff > s_century * 5) {
            earliest.set(earliestYear - (earliestYear % 100), Calendar.YEAR);
            latest.set(latestYear - (latestYear % 100) + 100, Calendar.YEAR);

            indexPeriods(objects, projection, itemsPerPage, earliest, latest, ascending, locale, entries,
                    Calendar.YEAR, 100, "yyyy");
        } else if (diff > s_decade * 5) {
            earliest.set(earliestYear - (earliestYear % 10), Calendar.YEAR);
            latest.set(latestYear - (latestYear % 10) + 10, Calendar.YEAR);

            indexPeriods(objects, projection, itemsPerPage, earliest, latest, ascending, locale, entries,
                    Calendar.YEAR, 10, "yyyy");
        } else if (diff > s_year * 5) {
            latest.set(latestYear + 1, Calendar.YEAR);

            indexPeriods(objects, projection, itemsPerPage, earliest, latest, ascending, locale, entries,
                    Calendar.YEAR, 1, "yyyy");
        } else if (diff > s_month * 5) {
            latest.set(latestYear + 1, Calendar.YEAR);

            indexPeriods(objects, projection, itemsPerPage, earliest, latest, ascending, locale, entries,
                    Calendar.MONTH, 1, "MMM yyyy");
        } else {
            earliest.clear(Calendar.HOUR_OF_DAY);
            earliest.clear(Calendar.MINUTE);
            earliest.clear(Calendar.SECOND);
            earliest.clear(Calendar.MILLISECOND);

            indexPeriods(objects, projection, itemsPerPage, earliest, latest, ascending, locale, entries,
                    Calendar.DATE, 1, null);
        }
    }

    protected void indexPeriods(List objects, DateTimeProjection projection, int itemsPerPage, Calendar earliest,
            Calendar latest, boolean ascending, String locale, List<Entry> entries, int field, int change, String format) throws QueryEvaluationException, RepositoryException {
        DateFormat sdf = format != null ? new SimpleDateFormat(format) : DateFormat.getDateInstance(DateFormat.SHORT);

        int total = objects.size();
        int soFar = ascending ? objects.size() - projection.countObjects(null, earliest.getTime()) : projection
                .countObjects(latest.getTime(), null);

        while (earliest.before(latest) && soFar < total) {
            Date start, end;
            if (ascending) {
                start = (Date) earliest.getTime().clone();
                earliest.add(field, change);
                end = earliest.getTime();
            } else {
                end = (Date) latest.getTime().clone();
                latest.add(field, -change);
                start = latest.getTime();
            }

            int count = projection.countObjects(start, end);
            if (count > 0) {
                entries.add(new Entry(sdf.format(start), soFar / itemsPerPage, count));

                soFar += count;
            }
        }
    }

    protected void indexStrings(List objects, IProjection projection, int itemsPerPage, String locale, List<Entry> entries) throws QueryEvaluationException, RepositoryException {

        int pageCount = objects.size() / itemsPerPage;

        indexStrings2(objects, projection, itemsPerPage, locale, entries, 1);
        if (entries.size() < pageCount / 3 && entries.size() < 30) {
            entries.clear();
            indexStrings2(objects, projection, itemsPerPage, locale, entries, 2);
        }

        if (entries.size() < 2 || entries.size() > 50) {
            entries.clear();
        }
    }

    protected void indexStrings2(List objects, IProjection projection, int itemsPerPage, String locale, List<Entry> entries,
            int prefixLength) throws QueryEvaluationException, RepositoryException {

        int start = 0;
        int current = 0;
        String lastPrefix = "";

        SchemaModel schemaModel = m_profile.getSchemaModel();

        Iterator i = objects.iterator();
        while (i.hasNext()) {
            URI object = (URI) i.next();
            Value value = (Value) projection.getValue(object);

            if (value != null) {
                String s = value instanceof Literal ? ((Literal) value).getLabel() : schemaModel.getLabel((URI) value, locale);

                String prefix = s.substring(0, Math.min(prefixLength, s.length())).toLowerCase();

                if (!prefix.equals(lastPrefix)) {
                    if (current > start && lastPrefix.length() > 0) {
                        entries.add(new Entry(lastPrefix, start / itemsPerPage, current - start));
                    }

                    lastPrefix = prefix;
                    start = current;
                }
            }
            current++;
        }

        if (current > start && lastPrefix.length() > 0) {
            entries.add(new Entry(lastPrefix, start / itemsPerPage, current - start));
        }
    }
}
