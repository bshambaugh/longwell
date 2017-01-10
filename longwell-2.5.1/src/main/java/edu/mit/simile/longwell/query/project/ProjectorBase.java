package edu.mit.simile.longwell.query.project;

import java.util.Set;

import org.openrdf.repository.Repository;

import edu.mit.simile.longwell.Cache;
import edu.mit.simile.longwell.Profile;
import edu.mit.simile.longwell.ProfileListenerBase;
import edu.mit.simile.longwell.QueryManager;

/**
 * Base class for Projectors. This provides a caching service for
 * projection results.
 *
 * Subclasses must implement internalProject(), which this class wraps
 * with caching code. Subclasses may also wish to implement an
 * onAfterChange() method which clears the cache in appropriate
 * circumstances.
 */
public abstract class ProjectorBase extends ProfileListenerBase implements IProjector {

    final protected Profile m_profile;
    final protected QueryManager m_queryManager;
    final protected Cache m_resultCache;
    final protected Cache m_rootProjections;

    protected ProjectorBase(Profile profile) {
        m_profile = profile;
        m_queryManager = profile.getQueryManager();
        m_rootProjections = profile.getCacheFactory().getCache("root-projections", false);
        m_resultCache = profile.getCacheFactory().getCache("results", true);
    }

    public IProjection project() {
        IProjection projection = (IProjection) m_rootProjections.get(this);
        if (projection == null) {
            projection = internalProject();

            m_rootProjections.put(this, projection);
        }
        return projection;
    }

    public IProjection project(Set objects) {
        IProjection projection = (IProjection) m_resultCache.get(objects);
        if (projection == null) {
            projection = internalProject(objects);

            m_resultCache.put(objects, projection);
        }
        return projection;
    }

    protected void onAfterChange(Repository r) {
        // Very crude refreshing strategy
        m_resultCache.clear();
        m_rootProjections.remove(this);
    }

    abstract protected IProjection internalProject();

    abstract protected IProjection internalProject(Set objects);
}
