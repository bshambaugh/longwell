package edu.mit.simile.longwell.query.engine;

import java.util.Comparator;
import java.util.Iterator;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.openrdf.query.QueryEvaluationException;
import org.openrdf.repository.RepositoryException;

import edu.mit.simile.longwell.Profile;
import edu.mit.simile.longwell.query.Facet;
import edu.mit.simile.longwell.query.BroadeningFacet;
import edu.mit.simile.longwell.query.bucket.IBucketer.BroadeningResult;
import edu.mit.simile.longwell.query.project.IProjection;
import edu.mit.simile.longwell.query.project.IProjector;

public class Broadener {
    final protected Profile m_profile;

    final protected QueryEngine m_engine;

    Broadener(Profile profile, QueryEngine engine) {
        m_profile = profile;
        m_engine = engine;
    }

    public Set broaden(Set objects, SortedSet restrictors, String locale, boolean fresh) throws QueryEvaluationException, RepositoryException {
        Set<Facet> facets = new TreeSet<Facet>(new Comparator() {
            public boolean equals(Object obj) {
                return false;
            }

            public int compare(Object o1, Object o2) {
                BroadeningFacet f1 = (BroadeningFacet) o1;
                BroadeningFacet f2 = (BroadeningFacet) o2;
                int i = 0;
                if (f1.m_bucketThemes.size() == 0 && f1.m_existingBuckets.size() > 0) {
                    i = 1;
                } else if (f1.m_bucketThemes.size() > 0 && f1.m_existingBuckets.size() == 0) {
                    i = -1;
                } else {
                    i = f1.m_label.compareToIgnoreCase(f2.m_label);
                    if (i == 0) {
                        i = f1.m_projectorName.compareTo(f2.m_projectorName);
                    }
                    if (i == 0) {
                        i = f1.m_projectorParameter.compareTo(f2.m_projectorParameter);
                    }
                    if (i == 0) {
                        i = f1.m_restrictionID - f2.m_restrictionID;
                    }
                }
                return i;
            }
        });

        Iterator i = restrictors.iterator();
        while (i.hasNext()) {
            Restrictor restrictor = (Restrictor) i.next();
            IProjector projector = restrictor.m_projector;
            IProjection projection = null;

            if (restrictors.size() == 1) {
                projection = projector.project();
            } else {
                TreeSet otherRestrictors = new TreeSet(restrictors.comparator());
                otherRestrictors.addAll(restrictors);
                otherRestrictors.remove(restrictor);

                Set objects2 = m_engine.m_refiner.refine(otherRestrictors, locale, fresh);

                projection = projector.project(objects2);
            }

            BroadeningResult result = restrictor.m_bucketer.suggestBroadeningBuckets(projection,
                    restrictor.m_bucketerParameter);

            facets.add(new BroadeningFacet(projector.getClass().getName(), projector.getParameter(), projector
                    .getLabel(locale), result.m_bucketThemes, result.m_existingBuckets, restrictor.m_restrictionID));
        }

        return facets;
    }

}
