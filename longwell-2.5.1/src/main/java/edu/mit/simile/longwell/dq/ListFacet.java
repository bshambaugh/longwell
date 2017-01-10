package edu.mit.simile.longwell.dq;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.openrdf.model.Literal;
import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.repository.RepositoryException;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import edu.mit.simile.longwell.Profile;
import edu.mit.simile.longwell.query.Query;
import edu.mit.simile.longwell.query.bucket.BucketerBase;
import edu.mit.simile.longwell.query.bucket.DistinctValueBucketer;
import edu.mit.simile.longwell.query.engine.QueryEngine;
import edu.mit.simile.longwell.query.project.IProjection;
import edu.mit.simile.longwell.query.project.IProjector;
import edu.mit.simile.longwell.query.project.PropertyProjector;
import edu.mit.simile.longwell.schema.SchemaModel;

public class ListFacet extends Facet {

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
    
    public class ReturnedItem extends Item {
        final String m_label;
        final int    m_count;
        
        ReturnedItem(Object value, String label, int count) {
            super(value);
            m_label = label;
            m_count = count;
        }
        
        public String getLabel() {
            return m_label;
        }
        public int getCount() {
            return m_count;
        }
        public String getValueAsString() {
            if (m_value == null) {
                return "null";
            } else if (m_value instanceof URI) {
                return "r" + ((URI) m_value).toString();
            } else if (m_value instanceof Literal) {
                return "l" + ((Literal) m_value).getLabel();
            } else {
                return "l" + m_value.toString();
            }
        }
    }
    
    private Set<Item> m_items;

    public ListFacet(String propertyURI, boolean forward, Class valueClass) {
        super(propertyURI, forward, valueClass);
        m_items = new HashSet<Item>();
    }
    
    /* (non-Javadoc)
     * @see edu.mit.simile.longwell.dq.Facet#getFacetType()
     */
    final public int getFacetType() {
        return s_facetType_list;
    }
    
    public Set<Item> getItems() {
        return m_items;
    }
    
    protected void addValue(Object value) {
        m_items.add(new Item(value));
    }
    
    public void addRestrictionToQuery(Query query) {
        String projectorParameter = (m_forward ? "" : "!") + m_propertyURI;
        String projectorName = PropertyProjector.class.getName();
        String bucketerName = DistinctValueBucketer.class.getName();
        
        StringBuffer bucketerParameter = new StringBuffer();
        
        Iterator i = m_items.iterator();
        while (i.hasNext()) {
            if (bucketerParameter.length() > 0) {
                bucketerParameter.append(',');
            }
            
            ((Item) i.next()).appendToBucketerParameter(bucketerParameter);
        }

        query.addRestriction(
            projectorName, projectorParameter, 
            bucketerName, bucketerParameter.toString()
        );
    }
    
    public Facet createReturnedFacet(
            Profile profile, DynamicQueryModel dqModel, Query query, Element element, String locale) throws QueryEvaluationException, RepositoryException {
        
        SchemaModel schemaModel = profile.getSchemaModel();
        QueryEngine queryModel = (QueryEngine) profile.getStructuredModel(QueryEngine.class);

        Set objects = queryModel.queryObjects(query, false);
        IProjector projector = dqModel.getProjectorManager().getProjector(
            PropertyProjector.class.getName(), 
            (m_forward ? "" : "!") + m_propertyURI, 
            locale
        );
        IProjection projection = (objects != null) ? projector.project(objects) : projector.project();
        
        ListFacet facet = new ListFacet(m_propertyURI, m_forward, m_valueClass);
        
        /*
         *  Count objects with missing value
         */
        int nullCount = projection.countObjects(null);
        if (nullCount > 0) {
            facet.m_items.add(new ReturnedItem(null, "(missing)", nullCount));
        }
        
        /*
         *  Construct new sorted set of facet values
         */
        Iterator i = projection.getValues().iterator();
        while (i.hasNext()) {
            Value value = (Value) i.next();
            if (value != null) {
                int count = projection.countObjects(value);
                if (count > 0) {
                    if (value instanceof URI) {
                        facet.m_items.add(new ReturnedItem(
                            value, 
                            schemaModel.getLabel((URI) value, locale), 
                            count
                        ));
                    } else if (value instanceof Literal) {
                        facet.m_items.add(new ReturnedItem(
                            value, 
                            ((Literal) value).getLabel(), 
                            count
                        ));
                    }
                }
            }
        }
        
        return facet;
    }
    
    static public ListFacet parse(
        String propertyURI, boolean forward, Class valueClass, Element element) {
        
        ListFacet facet = new ListFacet(propertyURI, forward, valueClass);
        
        NodeList nodeList = element.getChildNodes();
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            if (node instanceof Element) {
                NamedNodeMap attributes = node.getAttributes();
                
                String valueString = getStringAttribute(attributes, "value");
                Object value = stringToValue(valueString, valueClass);
                
                facet.addValue(value);
            }
        }
        
        return facet;
    }
}
