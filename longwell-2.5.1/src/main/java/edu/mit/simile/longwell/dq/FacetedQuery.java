package edu.mit.simile.longwell.dq;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.openrdf.model.Value;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.repository.RepositoryException;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import edu.mit.simile.longwell.Profile;
import edu.mit.simile.longwell.query.Query;
import edu.mit.simile.longwell.query.QueryException;
import edu.mit.simile.longwell.schema.LearnedProperty;

public class FacetedQuery {
    
    final Profile   m_profile;
    final List<Facet>      m_currentFacets = new ArrayList<Facet>();
    final List<Facet>      m_rootFacets = new ArrayList<Facet>();
    
    public FacetedQuery(Profile profile, Element rootQuery, Element currentQuery) throws Exception {
        m_profile = profile;
        
        NodeList nodeList = rootQuery.getChildNodes();
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            if (node instanceof Element) {
                m_rootFacets.add(Facet.parse((Element) node));
            }
        }
        
        nodeList = currentQuery.getChildNodes();
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            if (node instanceof Element) {
                m_currentFacets.add(Facet.parse((Element) node));
            }
        }
    }
    
    public List getCurrentFacets() {
        return m_currentFacets;
    }
    
    public List getRootFacets() {
        return m_rootFacets;
    }
    
    public Query getRootLongwellQuery() {
        Query longwellQuery = new Query("");
        
        Iterator i = m_rootFacets.iterator();
        while (i.hasNext()) {
            Facet facet = (Facet) i.next();
            
            facet.addRestrictionToQuery(longwellQuery);
        }
        return longwellQuery;
    }
    
    public Query makeCurrentQuery(Facet exceptFacetInfo) {
        Query longwellQuery = getRootLongwellQuery();
        Iterator i = m_currentFacets.iterator();
        while (i.hasNext()) {
            Facet facet = (Facet) i.next();
            
            if (exceptFacetInfo == null ||
                !facet.getPropertyURI().equals(exceptFacetInfo.getPropertyURI()) ||
                facet.isForward() != exceptFacetInfo.isForward()) {
                facet.addRestrictionToQuery(longwellQuery);
            }
        }
        
        return longwellQuery;
    }
    
    public Set<Facet> updateSeveralFacets(
        Profile             profile, 
        DynamicQueryModel   dqModel, 
        Element             element, 
        String              locale
    ) throws QueryException {
        Set<Facet> facets = new HashSet<Facet>();
        try {
            NodeList nodeList = element.getChildNodes();
            for (int i = 0; i < nodeList.getLength(); i++) {
                Node node = nodeList.item(i);
                if (node instanceof Element) {
                    facets.add(updateOneFacet(profile, dqModel, (Element) node, locale));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return facets;
    }
    
    public Facet updateOneFacet(
        Profile             profile, 
        DynamicQueryModel   dqModel, 
        Element             element, 
        String              locale
    ) throws QueryEvaluationException, RepositoryException {
        NamedNodeMap attributes = element.getAttributes();
        
        String propertyURI = getStringAttribute(attributes, "propertyURI");
        boolean forward = "true".equals(getStringAttribute(attributes, "forward"));
        
        for (int j = 0; j < m_currentFacets.size(); j++) {
            Facet facet = (Facet) m_currentFacets.get(j);
            if (facet.getPropertyURI().equals(propertyURI) &&
                facet.isForward() == forward) {
                
                Query longwellQuery = makeCurrentQuery(facet);
                
                return facet.createReturnedFacet(
                    profile, dqModel, longwellQuery, element, locale);
            }
        }
        
        Class valueClass = Value.class;
        Facet facet = null;
        if (forward && propertyURI != null && !propertyURI.equals("null")) {
            LearnedProperty property = profile.getSchemaModel().getLearnedProperty(new URIImpl(propertyURI));
            
            if (property != null) {
                if (property.getTypeConfidence(LearnedProperty.s_type_integer) > 0.5) {
                    valueClass = Long.class;
                    facet = new LongRangeFacet(propertyURI, valueClass);
                } else if (property.getTypeConfidence(LearnedProperty.s_type_numeric) > 0.5) {
                    valueClass = Double.class;
                    facet = new DoubleRangeFacet(propertyURI, valueClass);
                } else if (property.getTypeConfidence(LearnedProperty.s_type_dateTime) > 0.5) {
                    valueClass = Date.class;
                    facet = new DateRangeFacet(propertyURI, valueClass);
                }
            }
        }
        
        if (facet == null) {
            facet = new ListFacet(propertyURI, forward, valueClass);
        }
        
        Query longwellQuery = makeCurrentQuery(null);
        
        return facet.createReturnedFacet(profile, dqModel, longwellQuery, element, locale);
    }
    
    static protected String getStringAttribute(NamedNodeMap attributes, String name) {
        Node node = attributes.getNamedItem(name);
        return node != null ? node.getNodeValue() : "";
    }
}
