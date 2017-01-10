package edu.mit.simile.longwell.dq;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.openrdf.query.QueryEvaluationException;
import org.openrdf.repository.RepositoryException;
import org.w3c.dom.Element;

import edu.mit.simile.longwell.Profile;
import edu.mit.simile.longwell.query.Query;
import edu.mit.simile.longwell.query.engine.QueryEngine;
import edu.mit.simile.longwell.query.project.IProjector;
import edu.mit.simile.longwell.query.project.IntegerProjector;
import edu.mit.simile.longwell.query.project.IntegerProjector.IntegerProjection;

public class LongRangeFacet extends RangeFacet {
    
    public class ReturnedClosedLongRange extends LongRange {
        final String m_label;
        final int    m_count;
        final int    m_level;
        
        ReturnedClosedLongRange(long min, long max, String label, int count, int level) {
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
    
    LongRangeFacet(String propertyURI, Class valueClass) {
        super(propertyURI, true, valueClass);
    }

    public Facet createReturnedFacet(
        Profile             profile, 
        DynamicQueryModel   dqModel, 
        Query               query, 
        Element             element, 
        String              locale
    ) throws QueryEvaluationException, RepositoryException {
        QueryEngine queryModel = (QueryEngine) profile.getStructuredModel(QueryEngine.class);
        IProjector projector = dqModel.getProjectorManager().getProjector(
            IntegerProjector.class.getName(), 
            m_propertyURI, 
            locale
        );
        
        Set objects = queryModel.queryObjects(query, false);
        IntegerProjection projection = (IntegerProjection) 
            ((objects != null) ? projector.project(objects) : projector.project());
        
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
        
        LongRangeFacet facet = new LongRangeFacet(m_propertyURI, m_valueClass);
        
        List<Range> openedRanges = new ArrayList<Range>();
        parseRanges(element, openedRanges);
        
        Iterator openedRangeIterator = openedRanges.iterator();
        LongRange openedRange = openedRangeIterator.hasNext() ?
                (LongRange) openedRangeIterator.next() : null;
        
        recursiveUpdate(
            objects, 
            projection, 
            facet.m_ranges,
            openedRanges.iterator(),
            openedRange,
            min, 
            max, 
            interval,
            0
        );
        
        return facet;
    }
    
    protected LongRange recursiveUpdate(
        Set                 objects, 
        IntegerProjection   projection,
        List<Range>         newRanges,
        
        Iterator            openedRangeIterator,
        LongRange           openedRange,
        
        long                min, 
        long                max, 
        long                interval,
        int                 level
    ) throws QueryEvaluationException, RepositoryException {
        for (long i = min; i < max; i += interval) {
            long from = i; long to = i + interval;
            int count = projection.countObjects(from, to);
            if (count > 0) {
                ReturnedClosedLongRange newRange = new ReturnedClosedLongRange(
                    from, 
                    to, 
                    from + " - " + to, 
                    count,
                    level
                );
                
                newRanges.add(newRange);
                
                while (openedRange != null && openedRange.compareTo(newRange) < 0) {
                    openedRange = openedRangeIterator.hasNext() ?
                        (LongRange) openedRangeIterator.next() : null;
                }
                
                if (openedRange != null && openedRange.compareTo(newRange) == 0 && count > 0) {
                    openedRange = recursiveUpdate(
                        objects,
                        projection,
                        newRanges,
                        openedRangeIterator,
                        openedRangeIterator.hasNext() ? (LongRange) openedRangeIterator.next() : null,
                        from,
                        from + interval,
                        interval / 10,
                        level + 1
                    );
                }
            }
        }
        return openedRange;
    }
}
