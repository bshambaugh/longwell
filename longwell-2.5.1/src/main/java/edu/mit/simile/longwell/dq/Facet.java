package edu.mit.simile.longwell.dq;

import java.util.Date;

import org.apache.log4j.Logger;
import org.openrdf.model.Literal;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.impl.LiteralImpl;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.repository.RepositoryException;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import edu.mit.simile.longwell.Profile;
import edu.mit.simile.longwell.query.Query;

public abstract class Facet {

    final static private Logger s_logger = Logger.getLogger(Facet.class);
    
    final static public int     s_facetType_list = 0;
    final static public int     s_facetType_range = 1;
    final static public int     s_facetType_text = 2;
    
    final String        m_propertyURI;
    final boolean       m_forward;
    final Class         m_valueClass;
    
    Facet(String propertyURI, boolean forward, Class valueClass) {
        m_propertyURI = propertyURI;
        m_forward = forward;
        m_valueClass = valueClass;
    }
    
    abstract public int getFacetType();
    
    public String getPropertyURI() {
        return m_propertyURI;
    }
    
    public boolean isForward() {
        return m_forward;
    }
    
    public Class getValueClass() {
        return m_valueClass;
    }
    
    abstract public void addRestrictionToQuery(
        Query query
    );
    
    abstract public Facet createReturnedFacet(
        Profile             profile, 
        DynamicQueryModel   dqModel, 
        Query               query, 
        Element             element, 
        String              locale
    ) throws QueryEvaluationException, RepositoryException ;
    
    static public Facet parse(Element element) throws ClassNotFoundException {
        NamedNodeMap attributes = element.getAttributes();
        
        String  propertyURI = getStringAttribute(attributes, "propertyURI");
        boolean forward = !"false".equals(getStringAttribute(attributes, "forward"));
        
        Class valueClass = Class.forName(getStringAttribute(attributes, "valueClass"));
        switch (Integer.parseInt(getStringAttribute(attributes, "type"))) {
        case s_facetType_list:
            return ListFacet.parse(propertyURI, forward, valueClass, element);
        case s_facetType_range:
            if (!forward) {
                s_logger.error("Range facets can only work on forward properties");
            }
            return RangeFacet.parse(propertyURI, valueClass, element);
        case s_facetType_text:
            return TextFacet.parse(propertyURI, forward, valueClass, element);
        default:
            return null;
        }
    }
    
    static protected String getStringAttribute(NamedNodeMap attributes, String name) {
        Node node = attributes.getNamedItem(name);
        return node != null ? node.getNodeValue() : "";
    }
    
    static public Object stringToValue(String valueString, Class valueClass) {
        Object value = null;
        if ("null".equals(valueString)) {
            // value = null;
        } else if (valueClass == Value.class) {
            if (valueString.charAt(0) == 'r') {
                value = new URIImpl(valueString.substring(1));
            } else {
                value = new LiteralImpl(valueString.substring(1));
            }
        } else if (valueClass == URI.class) {
            value = new URIImpl(valueString.substring(1));
        } else if (valueClass == Date.class) {
            value = new Date(Long.parseLong(valueString));
        } else if (valueClass == Double.class) {
            value = new Double(Double.parseDouble(valueString));
        } else if (valueClass == Long.class) {
            value = new Long(Long.parseLong(valueString));
        } /* else value = null; */
        
        return value;
    }
    
    static public String valueToString(Object value, Class valueClass) {
        if (value == null) {
            return "null";
        } else if (valueClass == Value.class) {
            if (value instanceof URI) {
                return "r" + ((URI) value).toString();
            } else {
                return "l" + ((Literal) value).getLabel();
            }
        } else if (valueClass == URI.class) {
            return ((URI) value).toString();
        } else if (valueClass == Literal.class) {
            return ((Literal) value).getLabel();
        } else if (valueClass == String.class) {
            return value.toString();
            
        } else if (valueClass == Date.class) {
            return Long.toString(((Date) value).getTime());
        } else if (valueClass == Double.class) {
            return value.toString();
        } else if (valueClass == Long.class) {
            return value.toString();
        } else {
            return "null";
        }
    }
    
}