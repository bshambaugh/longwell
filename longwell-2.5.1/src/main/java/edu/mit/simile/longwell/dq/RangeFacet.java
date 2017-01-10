package edu.mit.simile.longwell.dq;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import edu.mit.simile.longwell.query.Query;
import edu.mit.simile.longwell.query.bucket.BucketerBase;
import edu.mit.simile.longwell.query.bucket.DateTimeBucketer;
import edu.mit.simile.longwell.query.bucket.DoubleBucketer;
import edu.mit.simile.longwell.query.bucket.IntegerBucketer;
import edu.mit.simile.longwell.query.project.DateTimeProjector;
import edu.mit.simile.longwell.query.project.DoubleProjector;
import edu.mit.simile.longwell.query.project.IntegerProjector;

public abstract class RangeFacet extends Facet {
    
    public abstract class Range {}

    public class NullRange extends Range {
        public long compareTo(Object o) {
            return (o instanceof NullRange) ? 0 : -1;
        }
    }
    
    public class LongRange extends Range {
        final long    m_min;
        final long    m_max;
        
        LongRange(long min, long max) {
            m_min = min;
            m_max = max;
        }

        public long compareTo(Object o) {
            if (o instanceof NullRange) {
                return 1;
            } else {
                LongRange r = (LongRange) o; 
                return ((m_min == r.m_min) ? (r.m_max - m_max) : (m_min - r.m_min));
            }
        }
        
        public long getMin() {
            return m_min;
        }
        public long getMax() {
            return m_max;
        }
    }
    
    public class DoubleRange extends Range {
        final double  m_min;
        final double  m_max;
        
        DoubleRange(double min, double max) {
            m_min = min;
            m_max = max;
        }
        
        public long compareTo(Object o) {
            if (o instanceof NullRange) {
                return 1;
            } else {
                DoubleRange r = (DoubleRange) o;
                return (long) ((m_min == r.m_min) ? (r.m_max - m_max) : (m_min - r.m_min));
            }
        }
        public double getMin() {
            return m_min;
        }
        public double getMax() {
            return m_max;
        }
    }

    public class DateRange extends Range {
        final long    m_min;
        final long    m_max;
        
        DateRange(long min, long max) {
            m_min = min;
            m_max = max;
        }

        public long compareTo(Object o) {
            if (o instanceof NullRange) {
                return 1;
            } else {
                DateRange r = (DateRange) o;
                return ((m_min == r.m_min) ? (r.m_max - m_max) : (m_min - r.m_min));
            }
        }
        
        public String toString() {
            DateFormat df = DateFormat.getDateInstance(DateFormat.MEDIUM);
            return "[" + df.format(new Date(m_min)) + "," + df.format(new Date(m_max)) + "]";
        }
        
        public long getMin() {
            return m_min;
        }
        public long getMax() {
            return m_max;
        }
    }
    
    protected List<Range> m_ranges = new ArrayList<Range>();
    
    RangeFacet(String propertyURI, boolean forward, Class valueClass) {
        super(propertyURI, forward, valueClass);
    }
    
    public List<Range> getRanges() {
        return m_ranges;
    }

    public int getFacetType() {
        return s_facetType_range;
    }
    
    public void addRestrictionToQuery(Query query) {
        String projectorParameter = (m_forward ? "" : "!") + m_propertyURI;
        String projectorName = null;
        String bucketerName = null;
        
        if (m_valueClass == Double.class) {
            projectorName = DoubleProjector.class.getName();
            bucketerName = DoubleBucketer.class.getName();
        } else if (m_valueClass == Date.class) {
            projectorName = DateTimeProjector.class.getName();
            bucketerName = DateTimeBucketer.class.getName();
        } else {
            projectorName = IntegerProjector.class.getName();
            bucketerName = IntegerBucketer.class.getName();
        }
        
        StringBuffer bucketerParameter = new StringBuffer();
        
        Iterator i = m_ranges.iterator();
        while (i.hasNext()) {
            Range r = (Range) i.next();
            
            if (bucketerParameter.length() > 0) {
                bucketerParameter.append(',');
            }
            
            if (r instanceof NullRange) {
                bucketerParameter.append(BucketerBase.encodeParameter("==null"));
            } else if (m_valueClass == Double.class) {
                DoubleRange r2 = (DoubleRange) r;
                bucketerParameter.append(BucketerBase.encodeParameter("<>"));
                bucketerParameter.append(BucketerBase.encodeParameter(Double.toString(r2.m_min)));
                bucketerParameter.append(BucketerBase.encodeParameter(","));
                bucketerParameter.append(BucketerBase.encodeParameter(Double.toString(r2.m_max)));
            } else if (m_valueClass == Date.class){
                DateRange r2 = (DateRange) r;
                bucketerParameter.append(BucketerBase.encodeParameter("<>"));
                bucketerParameter.append(BucketerBase.encodeParameter(Long.toString(r2.m_min)));
                bucketerParameter.append(BucketerBase.encodeParameter(","));
                bucketerParameter.append(BucketerBase.encodeParameter(Long.toString(r2.m_max)));
            } else {
                LongRange r2 = (LongRange) r;
                bucketerParameter.append(BucketerBase.encodeParameter("<>"));
                bucketerParameter.append(BucketerBase.encodeParameter(Long.toString(r2.m_min)));
                bucketerParameter.append(BucketerBase.encodeParameter(","));
                bucketerParameter.append(BucketerBase.encodeParameter(Long.toString(r2.m_max)));
            }
        }
        
        String bucketerParameterString = bucketerParameter.toString();
        query.addRestriction(
            projectorName, projectorParameter, 
            bucketerName, bucketerParameterString
        );
    }
    
    protected void addRange(long min, long max) {
        m_ranges.add(new LongRange(min, max));
    }
    
    protected void addRange(double min, double max) {
        m_ranges.add(new DoubleRange(min, max));
    }
    
    protected void addNullRange() {
        m_ranges.add(new NullRange());
    }
    
    static public RangeFacet parse(
        String propertyURI, Class valueClass, Element element) {
        
        RangeFacet facet = null;
        if (valueClass == Date.class) {
            facet = new DateRangeFacet(propertyURI, valueClass);
        } else if (valueClass == Double.class) {
            facet = new DoubleRangeFacet(propertyURI, valueClass);
        } else {
            facet = new LongRangeFacet(propertyURI, valueClass);
        }
        
        facet.parseRanges(element, facet.m_ranges);
        
        return facet;
    }
    
    protected void parseRanges(Element element, List<Range> ranges) {
        NodeList nodeList = element.getChildNodes();
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            if (node instanceof Element) {
                NamedNodeMap attributes = node.getAttributes();
                
                String minString = getStringAttribute(attributes, "min");
                String maxString = getStringAttribute(attributes, "max");
                
                if (minString.length() == 0) {
                    ranges.add(new NullRange());
                } else if (m_valueClass == Double.class) {
                    double min = Double.parseDouble(minString);
                    double max = Double.parseDouble(maxString);
                    ranges.add(new DoubleRange(min, max));
                } else if (m_valueClass == Date.class) {
                    long min = Long.parseLong(minString);
                    long max = Long.parseLong(maxString);
                    ranges.add(new DateRange(min, max));
                } else {
                    long min = Long.parseLong(minString);
                    long max = Long.parseLong(maxString);
                    ranges.add(new LongRange(min, max));
                }
            }
        }
    }
}
