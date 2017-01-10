package edu.mit.simile.longwell;

import java.util.Iterator;

import org.apache.log4j.Logger;
import org.openrdf.model.Literal;
import org.openrdf.model.URI;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.repository.RepositoryConnection;

import edu.mit.simile.longwell.query.Query;
import edu.mit.simile.longwell.query.bucket.DistinctValueBucketer;
import edu.mit.simile.longwell.query.engine.EngineUtilities;
import edu.mit.simile.longwell.query.engine.QueryEngine;

public class QueryBasedFacade extends Facade {

    final static private Logger s_logger = Logger.getLogger(QueryBasedFacade.class);

    final static public String s_flair_restriction = LongwellServlet.NAMESPACE + "restriction";
    final static public String s_flair_subject = LongwellServlet.NAMESPACE + "subject";
    final static public String s_flair_predicate = LongwellServlet.NAMESPACE + "predicate";
    final static public String s_flair_object = LongwellServlet.NAMESPACE + "object";
    final static public String s_flair_bucketerParameter = LongwellServlet.NAMESPACE + "bucketerParameter";

    final protected Query m_query;
    final protected Profile m_profile;
    final protected QueryManager m_queryManager;

    public QueryBasedFacade(String uri, String shortLabel, String contentDescription, Query query, Profile profile) {

        super(uri, shortLabel, contentDescription, query.toURLQueryString());
        m_query = query;
        m_profile = profile;
        m_queryManager = profile.getQueryManager();
    }

    public int getCount() {
        QueryEngine queryModel = (QueryEngine) m_profile.getStructuredModel(QueryEngine.class);

        try {
            return queryModel.queryCount(m_query, false);
        } catch (Exception e) {
            s_logger.error(e);
            return 0;
        }
    }

    public static QueryBasedFacade constructFacade(QueryManager queryManager, URI object, String shortLabel, String contentDescription,
            RepositoryConnection c, Profile profile) throws Exception {

        Query query = new Query("");

        Iterator i = queryManager.listObjectsOfProperty(c, object, new URIImpl(s_flair_restriction)).iterator();

        while (i.hasNext()) {
            try {
                URI restrictionResource = (URI) i.next();

                try {
                    URI predicate = (URI) queryManager.getObjectOfProperty(c, restrictionResource, new URIImpl(
                            s_flair_predicate));

                    String bucketerParameter = "";
                    String separator = "";
                    boolean forward = true;

                    Iterator i2 = queryManager.listObjectsOfProperty(c, restrictionResource,
                            new URIImpl(s_flair_subject)).iterator();

                    if (i2.hasNext()) {
                        forward = false;
                    } else {
                        i2 = queryManager.listObjectsOfProperty(c, restrictionResource, new URIImpl(s_flair_object))
                                .iterator();
                    }

                    while (i2.hasNext()) {
                        Object o = i2.next();
                        String value = null;

                        if (o instanceof URI) {
                            value = "r" + ((URI) o).toString();
                        } else {
                            value = "l" + ((Literal) o).getLabel();
                        }
                        bucketerParameter += separator + DistinctValueBucketer.encodeParameter(value);

                        separator = ",";
                    }

                    i2 = queryManager.listObjectsOfProperty(c, restrictionResource,
                            new URIImpl(s_flair_bucketerParameter)).iterator();

                    while (i2.hasNext()) {
                        Literal l = (Literal) i2.next();

                        bucketerParameter += separator + DistinctValueBucketer.encodeParameter(l.getLabel());

                        separator = ",";
                    }

                    EngineUtilities.FacetInfo info = EngineUtilities.getFacetInfo(predicate.toString(), forward, "",
                            profile);

                    query.addRestriction(info.m_projectorName, info.m_projectorParameter, info.m_bucketerName,
                            bucketerParameter, null);

                    continue;
                } catch (Exception e) {
                    s_logger.error(e);
                }
            } catch (Exception e) {
                s_logger.error(e);
            }
        }

        return new QueryBasedFacade(object.toString(), shortLabel, contentDescription, query, profile);
    }
}
