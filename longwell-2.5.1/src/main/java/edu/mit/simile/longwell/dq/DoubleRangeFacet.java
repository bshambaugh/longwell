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
import edu.mit.simile.longwell.query.project.DoubleProjector;
import edu.mit.simile.longwell.query.project.IProjector;
import edu.mit.simile.longwell.query.project.DoubleProjector.DoubleProjection;

public class DoubleRangeFacet extends RangeFacet {
    
    public class ReturnedDoubleRange extends DoubleRange {
        final String m_label;
        final int    m_count;
        final int    m_level;
        
        ReturnedDoubleRange(double min, double max, String label, int count, int level) {
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
    
    DoubleRangeFacet(String propertyURI, Class valueClass) {
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
            DoubleProjector.class.getName(), 
            m_propertyURI, 
            locale
        );
        
        Set objects = queryModel.queryObjects(query, false);
        DoubleProjection projection = (DoubleProjection) 
            ((objects != null) ? projector.project(objects) : projector.project());
        
        double min = projection.getMin();
        double max = projection.getMax();
        
        double diff = max - min;
        double interval = 10;

        while (diff / interval > 10) {
            interval *= 10;
        }
        while (diff / interval < 2) {
            interval /= 10;
        }

        min = Math.floor(min / interval) * interval;
        max = Math.ceil(max / interval) * interval;

        DoubleRangeFacet facet = new DoubleRangeFacet(m_propertyURI, m_valueClass);
        
        List<Range> openedRanges = new ArrayList<Range>();
        parseRanges(element, openedRanges);
        
        Iterator openedRangeIterator = openedRanges.iterator();
        DoubleRange openedRange = openedRangeIterator.hasNext() ?
                (DoubleRange) openedRangeIterator.next() : null;
        
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
    
    protected DoubleRange recursiveUpdate(
        Set                 objects, 
        DoubleProjection    projection,
        List<Range>         newRanges,
        
        Iterator            openedRangeIterator,
        DoubleRange         openedRange,
        
        double              min, 
        double              max, 
        double              interval,
        int                 level
    ) throws QueryEvaluationException, RepositoryException {

        for (double i = min; i < max; i += interval) {
            double from = i; double to = i + interval;
            int count = projection.countObjects(from, to);
            if (count > 0) {
                ReturnedDoubleRange newRange = new ReturnedDoubleRange(
                    from, 
                    to, 
                    from + " - " + to, 
                    count,
                    level
                );
                
                newRanges.add(newRange);
                
                while (openedRange != null && openedRange.compareTo(newRange) < 0) {
                    openedRange = openedRangeIterator.hasNext() ?
                        (DoubleRange) openedRangeIterator.next() : null;
                }
                
                if (openedRange != null && openedRange.compareTo(newRange) == 0 && count > 0) {
                    openedRange = recursiveUpdate(
                        objects,
                        projection,
                        newRanges,
                        openedRangeIterator,
                        openedRangeIterator.hasNext() ? (DoubleRange) openedRangeIterator.next() : null,
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
