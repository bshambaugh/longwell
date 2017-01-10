package edu.mit.simile.longwell.query.bucket;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.repository.RepositoryException;

import edu.mit.simile.longwell.Cache;
import edu.mit.simile.longwell.CacheFactory;
import edu.mit.simile.longwell.Profile;
import edu.mit.simile.longwell.query.Query;
import edu.mit.simile.longwell.query.Restriction;
import edu.mit.simile.longwell.query.engine.QueryEngine;
import edu.mit.simile.longwell.query.project.IProjection;
import edu.mit.simile.longwell.query.project.IProjector;
import edu.mit.simile.longwell.query.project.ProjectorManager;

public class NestedQueryBucketer extends BucketerBase {

    final static private Logger s_logger = Logger.getLogger(NestedQueryBucketer.class);

    protected CacheFactory m_cacheFactory;

    public NestedQueryBucketer(Profile profile) {
        super(profile);
        m_cacheFactory = profile.getCacheFactory();
    }

    public Set getBucket(IProjection projection, String parameter) throws QueryEvaluationException, RepositoryException {
        Cache resultsToBucket = (Cache) m_projectionToParameterToBucket.get(projection, parameter);

        if (resultsToBucket == null) {
            resultsToBucket = m_cacheFactory.getCache("results-to-bucket", false);
            m_projectionToParameterToBucket.put(projection, parameter, resultsToBucket);
        }

        Query query = new Query(parameter);
        QueryEngine queryEngine = (QueryEngine) m_profile.getStructuredModel(QueryEngine.class);

        Set results = queryEngine.queryObjects(query, false);

        Set bucket = (Set) resultsToBucket.get(results);
        if (bucket == null) {
            bucket = projection.getObjectsWithValues(results);
            resultsToBucket.put(results, bucket);
        }
        return bucket;
    }

    protected Set internalGetBucket(IProjection projection, String parameter) {
        throw new InternalError("We don't use this method.");
    }

    protected List internalSuggestNarrowingBuckets(IProjection projection, float desirable) {
        return new ArrayList();
    }

    protected BroadeningResult internalSuggestBroadeningBuckets(IProjection projection, String parameter) throws QueryEvaluationException, RepositoryException {

        List<Bucket> existingBuckets = new ArrayList<Bucket>();

        existingBuckets.add(new Bucket(this.getClass().getName(), parameter, parameterToDescription(parameter,
                projection.getLocale()), 0));

        return new BroadeningResult(existingBuckets, new ArrayList());
    }

    protected String individualParameterToDescription(String parameter, String locale) throws QueryEvaluationException, RepositoryException {

        Query query = new Query(parameter);
        List restrictions = query.getRestrictions();
        String[] restrictionDescriptions = new String[restrictions.size()];
        int i = 0;

        QueryEngine queryEngine = (QueryEngine) m_profile.getStructuredModel(QueryEngine.class);

        BucketerManager bucketerManager = queryEngine.getBucketerManager();
        ProjectorManager projectorManager = queryEngine.getProjectorManager();

        Iterator j = restrictions.iterator();
        while (j.hasNext()) {
            Restriction restriction = (Restriction) j.next();

            try {
                IProjector projector = projectorManager.getProjector(restriction.m_projectorName,
                        restriction.m_projectorParameter, locale);

                IBucketer bucketer = bucketerManager.getBucketer(restriction.m_bucketerName);

                restrictionDescriptions[i++] = projector.getLabel(locale) + ": "
                        + bucketer.parameterToDescription(restriction.m_bucketerParameter, locale);
            } catch (QueryEvaluationException e) {
                s_logger.error(e);
            }
        }

        return concatenateListItems(restrictionDescriptions);
    }
}
