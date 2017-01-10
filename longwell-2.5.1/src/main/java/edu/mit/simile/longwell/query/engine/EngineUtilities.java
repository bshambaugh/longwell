package edu.mit.simile.longwell.query.engine;

import java.util.Iterator;

import org.openrdf.model.URI;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;

import edu.mit.simile.longwell.Longwell;
import edu.mit.simile.longwell.Profile;
import edu.mit.simile.longwell.QueryManager;
import edu.mit.simile.longwell.query.bucket.DateTimeBucketer;
import edu.mit.simile.longwell.query.bucket.DistinctValueBucketer;
import edu.mit.simile.longwell.query.bucket.DoubleBucketer;
import edu.mit.simile.longwell.query.bucket.IntegerBucketer;
import edu.mit.simile.longwell.query.project.DateTimeProjector;
import edu.mit.simile.longwell.query.project.DoubleProjector;
import edu.mit.simile.longwell.query.project.IntegerProjector;
import edu.mit.simile.longwell.query.project.PropertyProjector;
import edu.mit.simile.longwell.schema.LearnedProperty;

public class EngineUtilities {
    
    public static class FacetInfo {
        public String m_projectorName;
        public String m_projectorParameter;
        public String m_label;
        public String m_bucketerName;
    }

    static public FacetInfo getFacetInfo(String propertyURI, boolean forward, String locale, Profile profile) throws QueryEvaluationException, RepositoryException {
    	LearnedProperty p = profile.getSchemaModel().getLearnedProperty(new URIImpl(propertyURI));
    	if (null == p)
        	throw new QueryEvaluationException("No info on property " + propertyURI);
    	else
    		return getFacetInfo(p, forward, locale, profile);
    }

    static public FacetInfo getFacetInfo(LearnedProperty learnedProperty, boolean forward, String locale, Profile profile) throws QueryEvaluationException, RepositoryException {
        FacetInfo info = new FacetInfo();

        String propertyURI = learnedProperty.getURI().toString();

        info.m_label = learnedProperty.getLabel(locale);
        info.m_projectorParameter = forward ? propertyURI : "!" + propertyURI;

        QueryManager manager = profile.getQueryManager();
        Repository r = profile.getRepository();
        Repository rDefault = profile.getRepository();

        URI property = new URIImpl(propertyURI);

        getFacetInfo(manager, property, locale, r, rDefault, info);
        if (r != rDefault && (info.m_projectorName == null || info.m_bucketerName == null)) {
            getFacetInfo(manager, property, locale, rDefault, rDefault, info);
        }

        if (info.m_projectorName == null || info.m_bucketerName == null) {
            if (learnedProperty.getTypeConfidence(LearnedProperty.s_type_integer) > 0.5) {
                info.m_projectorName = IntegerProjector.class.getName();
                info.m_bucketerName = IntegerBucketer.class.getName();
            } else if (learnedProperty.getTypeConfidence(LearnedProperty.s_type_numeric) > 0.5) {
                info.m_projectorName = DoubleProjector.class.getName();
                info.m_bucketerName = DoubleBucketer.class.getName();
            } else if (learnedProperty.getTypeConfidence(LearnedProperty.s_type_dateTime) > 0.5) {
                info.m_projectorName = DateTimeProjector.class.getName();
                info.m_bucketerName = DateTimeBucketer.class.getName();
            } else {
                info.m_projectorName = PropertyProjector.class.getName();
                info.m_bucketerName = DistinctValueBucketer.class.getName();
            }
        }

        return info;
    }

    static protected void getFacetInfo(QueryManager queryManager, URI property, String locale, Repository r, Repository rDefault, FacetInfo info) throws QueryEvaluationException, RepositoryException {
        URI typePredicate = RDF.TYPE;
        URI projectorPredicate = new URIImpl(Longwell.s_namespace + "projector");
        URI bucketerPredicate = new URIImpl(Longwell.s_namespace + "bucketer");

        RepositoryConnection c = null, cDefault = null;
        try {
            c = r.getConnection();
            cDefault = rDefault.getConnection();
            
            Iterator i = queryManager.listObjectsOfProperty(c, property, typePredicate).iterator();
            while (i.hasNext()) {
                try {
                    URI propertyType = (URI) i.next();
                    if (info.m_projectorName == null) {
                        try {
                            info.m_projectorName = queryManager.getStringOfProperty(c, propertyType, projectorPredicate);
                        } catch (Exception e) {
                            // Do nothing
                        }
                    }
                    if (info.m_bucketerName == null) {
                        try {
                            info.m_bucketerName = queryManager.getStringOfProperty(c, propertyType, bucketerPredicate);
                        } catch (Exception e) {
                            // Do nothing
                        }
                    }
    
                    if (r != rDefault) {
                        if (info.m_projectorName == null) {
                            try {
                                info.m_projectorName = queryManager.getStringOfProperty(cDefault, propertyType,
                                        projectorPredicate);
                            } catch (Exception e) {
                                // Do nothing
                            }
                        }
                        if (info.m_bucketerName == null) {
                            try {
                                info.m_bucketerName = queryManager.getStringOfProperty(cDefault, propertyType,
                                        bucketerPredicate);
                            } catch (Exception e) {
                                // Do nothing
                            }
                        }
                    }
    
                    if (info.m_projectorName != null && info.m_bucketerName != null) {
                        return;
                    }
                } catch (Exception e) {
                    // Do nothing
                }
            }
        } finally {
            if (cDefault != null) cDefault.close();
            if (c != null) c.close();
        }
    }
}
