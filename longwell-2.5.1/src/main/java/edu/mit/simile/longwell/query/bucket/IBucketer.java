package edu.mit.simile.longwell.query.bucket;

import java.util.List;
import java.util.Set;

import org.openrdf.query.QueryEvaluationException;
import org.openrdf.repository.RepositoryException;

import edu.mit.simile.longwell.IProfileListener;
import edu.mit.simile.longwell.query.project.IProjection;

/**
 * Integer for an object which can take a group of objects (which have
 * been projected to a known type) and extract out a subset of them,
 * according to some criteria.
 */
public interface IBucketer extends IProfileListener {

    /**
     * Answer the set of objects from the given projection fitting whatever
     * criteria specified by the given parameter.
     * 
     * @param projection
     * @param parameter
     * @return
     */
    public Set getBucket(IProjection projection, String parameter) throws QueryEvaluationException, RepositoryException;

    /**
     * Returns a human-understandable description of the parameter.
     * 
     * @param parameter
     * @param locale
     * @return
     */
    public String parameterToDescription(String parameter, String locale) throws QueryEvaluationException, RepositoryException;

    /**
     * Answer whether the given parameter specifies matching against exactly one
     * value.
     * 
     * @param parameter
     * @return
     */
    public boolean matchesOneValue(String parameter);

    /**
     * Answer a list of BucketTheme's from the objects in the given projection.
     * The parameter desirable varies from 0 to 1 to indicate how much this
     * bucketer is desired to give some suggestions. 1 means it should
     * definitely give some suggestions.
     * 
     * @param projection
     * @param desirable
     * @return
     */
    public List suggestNarrowingBuckets(IProjection projection, float desirable) throws QueryEvaluationException, RepositoryException;

    /**
     * Answer a broadening facet that allows broadening from the restriction
     * specified by "parameter".
     * 
     * @param projection
     * @param parameter
     * @return
     */
    public BroadeningResult suggestBroadeningBuckets(IProjection projection, String parameter) throws QueryEvaluationException, RepositoryException;

    static public class BroadeningResult {
        final public List m_existingBuckets;

        final public List m_bucketThemes;

        public BroadeningResult(List existingBuckets, List bucketThemes) {
            m_existingBuckets = existingBuckets;
            m_bucketThemes = bucketThemes;
        }

        public List getExistingBuckets() {
            return m_existingBuckets;
        }

        public List getBucketThemes() {
            return m_bucketThemes;
        }
    }
}
