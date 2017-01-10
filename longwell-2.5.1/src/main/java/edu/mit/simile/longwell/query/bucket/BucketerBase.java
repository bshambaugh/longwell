package edu.mit.simile.longwell.query.bucket;

import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.repository.RepositoryException;

import edu.mit.simile.longwell.Cache;
import edu.mit.simile.longwell.CacheFactory;
import edu.mit.simile.longwell.Profile;
import edu.mit.simile.longwell.ProfileListenerBase;
import edu.mit.simile.longwell.Utilities;
import edu.mit.simile.longwell.query.project.IProjection;

public abstract class BucketerBase extends ProfileListenerBase implements IBucketer {

    final protected Profile m_profile;

    final protected Cache m_projectionToParameterToBucket;

    final protected Cache m_projectionToNarrowingSuggestion;

    final protected Cache m_projectionToParameterToBroadeningSuggestion;

    protected BucketerBase(Profile profile) {
        m_profile = profile;
        CacheFactory f = profile.getCacheFactory();
        m_projectionToParameterToBucket = f.getCache("projection", "parameter", false);
        m_projectionToNarrowingSuggestion = f.getCache("projection-to-narrowing-suggestion", false);
        m_projectionToParameterToBroadeningSuggestion = f.getCache("projection", "parameter", false);
    }

    public Set getBucket(IProjection projection, String parameter) throws QueryEvaluationException, RepositoryException {
        Set bucket = (Set) m_projectionToParameterToBucket.get(projection, parameter);
        if (bucket == null) {
            bucket = internalGetBucket(projection, parameter);
            m_projectionToParameterToBucket.put(projection, parameter, bucket);
        }
        return bucket;
    }

    public String parameterToDescription(String parameter, String locale) throws QueryEvaluationException, RepositoryException {
        String[] params = splitParameter(parameter);
        String[] descriptions = new String[params.length];

        for (int i = 0; i < params.length; i++) {
            descriptions[i] = individualParameterToDescription(params[i], locale);
        }

        return concatenateListItems(descriptions);
    }

    public boolean matchesOneValue(String parameter) {
        return splitParameter(parameter).length == 1;
    }

    public List suggestNarrowingBuckets(IProjection projection, float desirable)  throws QueryEvaluationException, RepositoryException {
        List suggestion = (List) m_projectionToNarrowingSuggestion.get(projection);
        if (suggestion == null) {
            suggestion = internalSuggestNarrowingBuckets(projection, desirable);
            m_projectionToNarrowingSuggestion.put(projection, suggestion);
        }
        return suggestion;
    }

    public BroadeningResult suggestBroadeningBuckets(IProjection projection, String parameter) throws QueryEvaluationException, RepositoryException {
        BroadeningResult result = (BroadeningResult) m_projectionToParameterToBroadeningSuggestion.get(projection,
                parameter);
        if (result == null) {
            result = internalSuggestBroadeningBuckets(projection, parameter);
            m_projectionToParameterToBroadeningSuggestion.put(projection, parameter, result);
        }

        return result;
    }

    abstract protected Set internalGetBucket(IProjection projection, String parameter) throws QueryEvaluationException, RepositoryException;

    abstract protected List internalSuggestNarrowingBuckets(IProjection projection, float desirable)  throws QueryEvaluationException, RepositoryException;

    abstract protected BroadeningResult internalSuggestBroadeningBuckets(IProjection projection, String parameter) throws QueryEvaluationException, RepositoryException;

    abstract protected String individualParameterToDescription(String parameter, String locale) throws QueryEvaluationException, RepositoryException;

    protected ResourceBundle getResources(String locale) {
        return ResourceBundle.getBundle(this.getClass().getName(), locale == null ? Locale.getDefault() : new Locale(
                locale));
    }

    static protected String[] splitParameter(String s) {
        String[] params = StringUtils.splitPreserveAllTokens(s, ',');

        for (int i = 0; i < params.length; i++) {
            params[i] = decodeParameter(params[i]);
        }

        return params;
    }

    static protected String concatenateListItems(String[] items) {
        ResourceBundle resources = ResourceBundle.getBundle(BucketerBase.class.getName());

        if (items.length == 0) {
            return resources.getString("EmptyList");
        } else if (items.length == 1) {
            return items[0];
        } else {
            String s = resources.getString("ListLeftDelimiter");

            String separator = resources.getString("ListSeparator") + " ";

            for (int i = 0; i < items.length; i++) {
                if (i > 0) {
                    s += separator;
                }
                s += items[i];
            }
            s += resources.getString("ListRightDelimiter");

            return s;
        }
    }

    static public String decodeParameter(String s) {
        return Utilities.unescape(s, ',', '~', 'c');
    }

    static public String encodeParameter(String s) {
        return Utilities.escape(s, ',', '~', 'c');
    }
}
