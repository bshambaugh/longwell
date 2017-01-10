package edu.mit.simile.longwell.dq;

//import org.apache.log4j.Logger;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.query.QueryEvaluationException;
import org.w3c.dom.Element;

import edu.mit.simile.longwell.Profile;
import edu.mit.simile.longwell.StructuredModelBase;
import edu.mit.simile.longwell.query.Query;
import edu.mit.simile.longwell.query.QueryException;
import edu.mit.simile.longwell.query.bucket.BucketerManager;
import edu.mit.simile.longwell.query.compare.ComparatorManager;
import edu.mit.simile.longwell.query.compare.DateComparator;
import edu.mit.simile.longwell.query.compare.DoubleComparator;
import edu.mit.simile.longwell.query.compare.IntegerComparator;
import edu.mit.simile.longwell.query.compare.StringComparator;
import edu.mit.simile.longwell.query.engine.QueryEngine;
import edu.mit.simile.longwell.query.engine.QueryEngine.Answer;
import edu.mit.simile.longwell.query.project.DateTimeProjector;
import edu.mit.simile.longwell.query.project.DoubleProjector;
import edu.mit.simile.longwell.query.project.IntegerProjector;
import edu.mit.simile.longwell.query.project.ProjectorManager;
import edu.mit.simile.longwell.query.project.PropertyProjector;
import edu.mit.simile.longwell.query.project.SelfURIProjector;
import edu.mit.simile.longwell.schema.LearnedProperty;

public class DynamicQueryModel extends StructuredModelBase {

    //final static private Logger s_logger = Logger.getLogger(DynamicQueryModel.class);
    
    final protected ProjectorManager    m_projectorManager;
    final protected BucketerManager     m_bucketerManager;
    final protected ComparatorManager   m_comparatorManager;
    
    public DynamicQueryModel(Profile profile) {
        super(profile);
        
        m_projectorManager = new ProjectorManager(m_profile);
        m_bucketerManager = new BucketerManager(m_profile);
        m_comparatorManager = new ComparatorManager(m_profile);
    }
    
    public ProjectorManager getProjectorManager() {
        return m_projectorManager;
    }
    
    public BucketerManager getBucketerManager() {
        return m_bucketerManager;
    }
    
    public ComparatorManager getComparatorManager() {
        return m_comparatorManager;
    }
    
    public FacetedQuery createFacetedQuery(Element element) throws Exception {
        return new FacetedQuery(m_profile, 
            (Element) element.getElementsByTagName("root").item(0),
            (Element) element.getElementsByTagName("current").item(0)
        );
    }

    public Answer getAnswer(FacetedQuery query) throws QueryEvaluationException {
        QueryEngine queryModel = (QueryEngine) m_profile.getStructuredModel(QueryEngine.class);
        return queryModel.query(query.makeCurrentQuery(null), false);
    }
    
    public Answer getAnswer(
        FacetedQuery    query, 
        String          sortPropertyURI, 
        boolean         ascending
    ) throws QueryException {
        try {
            Query longwellQuery = query.makeCurrentQuery(null);
            
            String projectorName = PropertyProjector.class.getName();
            String projectorParameter = sortPropertyURI;
            String comparatorName = StringComparator.class.getName();
            String comparatorParameter = "";
            
            //if (s_logger.isInfoEnabled()) s_logger.info("sortPropertyURI: " + sortPropertyURI);
            
            if (sortPropertyURI == null || sortPropertyURI.equals("null") || sortPropertyURI.length() == 0) {
                projectorName = SelfURIProjector.class.getName();
            } else {
                LearnedProperty property = m_profile.getSchemaModel().getLearnedProperty(new URIImpl(sortPropertyURI));
                if (property.getTypeConfidence(LearnedProperty.s_type_integer) > 0.5) {
                    projectorName = IntegerProjector.class.getName();
                    comparatorName = IntegerComparator.class.getName();
                } else if (property.getTypeConfidence(LearnedProperty.s_type_numeric) > 0.5) {
                    projectorName = DoubleProjector.class.getName();
                    comparatorName = DoubleComparator.class.getName();
                } else if (property.getTypeConfidence(LearnedProperty.s_type_dateTime) > 0.5) {
                    projectorName = DateTimeProjector.class.getName();
                    comparatorName = DateComparator.class.getName();
                }
            }
            
            longwellQuery.addOrder(projectorName, projectorParameter, comparatorName, comparatorParameter, ascending);
            QueryEngine queryModel = (QueryEngine) m_profile.getStructuredModel(QueryEngine.class);

            return queryModel.query(longwellQuery, false);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
