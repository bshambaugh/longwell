package edu.mit.simile.longwell.query.engine;

import java.util.Iterator;
import java.util.Set;
import java.util.SortedSet;

import org.openrdf.query.QueryEvaluationException;
import org.openrdf.repository.RepositoryException;

import edu.mit.simile.longwell.Profile;
import edu.mit.simile.longwell.ProfileListenerBase;
import edu.mit.simile.longwell.query.project.IProjection;

public class Refiner extends ProfileListenerBase {
    
    final protected Profile m_profile;

    final protected QueryEngine m_engine;

    Refiner(Profile profile, QueryEngine engine) {
        m_profile = profile;
        m_engine = engine;
    }

    public Set refine(SortedSet restrictors, String locale, boolean fresh) throws QueryEvaluationException, RepositoryException {

        Set objects = null;

        Iterator i = restrictors.iterator();
        while (i.hasNext()) {
            Restrictor restrictor = (Restrictor) i.next();
            IProjection projection = null;

            if (objects == null) {
                projection = restrictor.m_projector.project();
            } else {
                projection = restrictor.m_projector.project(objects);
            }

            objects = restrictor.m_bucketer.getBucket(projection, restrictor.m_bucketerParameter);
        }

        return objects;
    }

}
