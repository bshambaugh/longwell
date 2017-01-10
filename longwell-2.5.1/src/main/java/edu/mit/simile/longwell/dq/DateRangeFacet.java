package edu.mit.simile.longwell.dq;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.openrdf.query.QueryEvaluationException;
import org.openrdf.repository.RepositoryException;
import org.w3c.dom.Element;

import edu.mit.simile.longwell.Profile;
import edu.mit.simile.longwell.query.Query;
import edu.mit.simile.longwell.query.engine.QueryEngine;
import edu.mit.simile.longwell.query.project.DateTimeProjector;
import edu.mit.simile.longwell.query.project.IProjector;
import edu.mit.simile.longwell.query.project.DateTimeProjector.DateTimeProjection;

public class DateRangeFacet extends RangeFacet {

    final static private int    s_level_hour = 0;
    final static private int    s_level_day = 1;
    final static private int    s_level_month= 2;
    final static private int    s_level_year = 3;
    final static private int    s_level_decade = 4;
    final static private int    s_level_century = 5;
    final static private int    s_level_millennium = 6;
    
    final static private long[] s_lengths;
    static {
        s_lengths = new long[7];
        s_lengths[s_level_hour]         = 1000 * 60 * 60;
        s_lengths[s_level_day]          = s_lengths[s_level_hour] * 24;
        s_lengths[s_level_month]        = s_lengths[s_level_day] * 30;
        s_lengths[s_level_year]         = s_lengths[s_level_day] * 365;
        s_lengths[s_level_decade]       = s_lengths[s_level_year] * 10;
        s_lengths[s_level_century]      = s_lengths[s_level_year] * 100;
        s_lengths[s_level_millennium]   = s_lengths[s_level_year] * 1000;
    }
    
    public class ReturnedDateRange extends DateRange {
        final String m_label;
        final int    m_count;
        final int    m_level;
        
        ReturnedDateRange(long min, long max, String label, int count, int level) {
            super(min, max);
            m_label = label;
            m_count = count;
            m_level = level;
        }
        
        public int getCount() {
            return m_count;
        }
        public String getLabel() {
            return m_label;
        }
        public int getLevel() {
            return m_level;
        }
    }

    public DateRangeFacet(String propertyURI, Class valueClass) {
        super(propertyURI, true, valueClass);
    }

    public Facet createReturnedFacet(Profile profile,
            DynamicQueryModel dqModel, Query query, Element element,
            String locale) throws QueryEvaluationException, RepositoryException {
        
        QueryEngine queryModel = (QueryEngine) profile.getStructuredModel(QueryEngine.class);
        IProjector projector = dqModel.getProjectorManager().getProjector(
            DateTimeProjector.class.getName(), 
            m_propertyURI, 
            locale
        );
        
        Set objects = queryModel.queryObjects(query, false);
        DateTimeProjection projection = (DateTimeProjection) 
            ((objects != null) ? projector.project(objects) : projector.project());
        
        Calendar earliest = Calendar.getInstance();
        Calendar latest = Calendar.getInstance();

        earliest.setTime(projection.getEarliest());
        latest.setTime(projection.getLatest());
        
        long diff = latest.getTimeInMillis() - earliest.getTimeInMillis();
        int level = s_lengths.length - 1;
        while (level > 0) {
            if (diff > s_lengths[level]) {
                break;
            }
            level--;
        }

        switch (level) {
            case s_level_millennium:
                earliest.set(Calendar.YEAR, (earliest.get(Calendar.YEAR) / 1000) * 1000);
                // fall through
            case s_level_century:
                earliest.set(Calendar.YEAR, (earliest.get(Calendar.YEAR) / 100) * 100);
                // fall through
            case s_level_decade:
                earliest.set(Calendar.YEAR, (earliest.get(Calendar.YEAR) / 10) * 10);
                // fall through
            case s_level_year:
                earliest.set(Calendar.MONTH, 0);
                // fall through
            case s_level_month:
                earliest.set(Calendar.DAY_OF_MONTH, 1);
                // fall through
            case s_level_day:
                earliest.set(Calendar.HOUR_OF_DAY, 0);
                // fall through
            case s_level_hour:
                earliest.set(Calendar.MINUTE, 0);
                earliest.set(Calendar.MILLISECOND, 0);
                earliest.set(Calendar.SECOND, 0);
        }
        
        DateRangeFacet facet = new DateRangeFacet(m_propertyURI, m_valueClass);
        
        List<Range> openedRanges = new ArrayList<Range>();
        parseRanges(element, openedRanges);
        
        Iterator openedRangeIterator = openedRanges.iterator();
        DateRange openedRange = openedRangeIterator.hasNext() ?
                (DateRange) openedRangeIterator.next() : null;
        
        recursiveUpdate(
            objects, 
            projection, 
            facet.m_ranges,
            openedRanges.iterator(),
            openedRange,
            earliest, 
            latest, 
            level,
            level
        );
        
        return facet;
    }

    protected DateRange recursiveUpdate(
        Set                 objects, 
        DateTimeProjection projection,
        List<Range>         newRanges,
        
        Iterator            openedRangeIterator,
        DateRange           openedRange,
        
        Calendar            earliest,
        Calendar            latest,
        int                 level,
        int                 topLevel
    ) throws QueryEvaluationException, RepositoryException {
        DateFormat df = null;

        df = DateFormat.getDateInstance(DateFormat.MEDIUM);
        
        switch (level) {
            case s_level_hour:
                df = new SimpleDateFormat("h:mm a"); break;
            case s_level_day:
                df = DateFormat.getDateInstance(DateFormat.MEDIUM); break;
            case s_level_month:
                df = new SimpleDateFormat("MMMMM yyyy"); break;
            case s_level_year:
            case s_level_decade:
            case s_level_century:
            case s_level_millennium:
                df = new SimpleDateFormat("yyyy"); break;
            default:
                df = DateFormat.getDateInstance(DateFormat.MEDIUM); break;
        }
        
        while (earliest.getTimeInMillis() < latest.getTimeInMillis()) {
            Date start = new Date(earliest.getTimeInMillis());
            
            increment(earliest, level);
            
            Date end = new Date(earliest.getTimeInMillis());
            
            int count = projection.countObjects(start, end);
            if (count > 0) {
                ReturnedDateRange newRange = new ReturnedDateRange(
                    start.getTime(), 
                    end.getTime(), 
                    df.format(start),
                    count,
                    topLevel - level
                );
                
                newRanges.add(newRange);

                while (openedRange != null && openedRange.compareTo(newRange) < 0) {
                    openedRange = openedRangeIterator.hasNext() ?
                        (DateRange) openedRangeIterator.next() : null;
                }

                if (openedRange != null && openedRange.compareTo(newRange) == 0 && level > 0) {
                    Calendar earliest2 = Calendar.getInstance();
                    Calendar latest2 = Calendar.getInstance();
                    earliest2.setTime(start);
                    latest2.setTime(end);
                    
                    openedRange = recursiveUpdate(
                        objects,
                        projection,
                        newRanges,
                        openedRangeIterator,
                        openedRangeIterator.hasNext() ? (DateRange) openedRangeIterator.next() : null,
                        earliest2,
                        latest2,
                        level - 1,
                        topLevel
                    );
                }
            }
        }
        
        return openedRange;
    }
    
    protected void increment(Calendar c, int level) {
        switch (level) {
        case s_level_hour:
            c.add(Calendar.HOUR, 1);
            return;
        case s_level_day:
            c.add(Calendar.DATE, 1);
            return;
        case s_level_month:
            c.add(Calendar.MONTH, 1);
            return;
        case s_level_year:
            c.add(Calendar.YEAR, 1);
            return;
        case s_level_decade:
            c.add(Calendar.YEAR, 10);
            return;
        case s_level_century:
            c.add(Calendar.YEAR, 100);
            return;
        case s_level_millennium:
            c.add(Calendar.YEAR, 1000);
            return;
        default:
            throw new InternalError("Cannot get here");
        }
    }
}
