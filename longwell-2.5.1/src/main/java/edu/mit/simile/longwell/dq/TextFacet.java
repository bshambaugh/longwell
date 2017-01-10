package edu.mit.simile.longwell.dq;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.openrdf.model.Literal;
import org.openrdf.model.Resource;
import org.openrdf.model.Value;
import org.openrdf.query.QueryEvaluationException;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import edu.mit.simile.longwell.Profile;
import edu.mit.simile.longwell.query.Query;
import edu.mit.simile.longwell.query.bucket.BucketerBase;
import edu.mit.simile.longwell.query.bucket.TextIndexBucketer;
import edu.mit.simile.longwell.query.project.TextIndexProjector;

public class TextFacet extends Facet {

    public class Item {
        final Object m_value;
        
        Item(Object value) {
            m_value = value;
        }
        
        public void appendToBucketerParameter(StringBuffer sb) {
            if (m_valueClass == Value.class) {
                if (m_value instanceof Resource) {
                    sb.append('r');
                    sb.append(BucketerBase.encodeParameter(((Resource) m_value).toString()));
                } else if (m_value instanceof Literal){
                    sb.append('l');
                    sb.append(BucketerBase.encodeParameter(((Literal) m_value).getLabel()));
                } else if (m_value != null) {
                    sb.append('l');
                    sb.append(BucketerBase.encodeParameter(m_value.toString()));
                } else {
                    sb.append("null");
                }
            } else if (m_value != null) {
                sb.append('l');
                sb.append(BucketerBase.encodeParameter(m_value.toString()));
            } else {
                sb.append("null");
            }
        }
    }
    
    private List<String> m_strings;

    public TextFacet(String propertyURI, boolean forward, Class valueClass) {
        super(propertyURI, forward, valueClass);
        m_strings = new ArrayList<String>();
    }
    
    /* (non-Javadoc)
     * @see edu.mit.simile.longwell.dq.Facet#getFacetType()
     */
    final public int getFacetType() {
        return s_facetType_text;
    }
    
    public List getStrings() {
        return m_strings;
    }
    
    public void addRestrictionToQuery(Query query) {
        String projectorParameter = "";
        String projectorName = TextIndexProjector.class.getName();
        String bucketerName = TextIndexBucketer.class.getName();
        
        Iterator i = m_strings.iterator();
        while (i.hasNext()) {
            query.addRestriction(
                projectorName, projectorParameter, 
                bucketerName, ((String) i.next())
            );
        }
    }
    
    public Facet createReturnedFacet(
            Profile profile, DynamicQueryModel dqModel, Query query, Element element, String locale) throws QueryEvaluationException {
        
        return null;
    }
    
    static public TextFacet parse(
        String propertyURI, boolean forward, Class valueClass, Element element) {
        
        TextFacet facet = new TextFacet(propertyURI, forward, valueClass);
        
        NodeList nodeList = element.getChildNodes();
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            if (node instanceof Element) {
                NamedNodeMap attributes = node.getAttributes();
                
                String valueString = getStringAttribute(attributes, "value");
                
                facet.m_strings.add(valueString);
            }
        }
        
        return facet;
    }
}
