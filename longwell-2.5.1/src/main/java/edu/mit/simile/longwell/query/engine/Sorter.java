package edu.mit.simile.longwell.query.engine;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.openrdf.model.URI;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.repository.RepositoryException;

import edu.mit.simile.longwell.Cache;
import edu.mit.simile.longwell.CacheFactory;
import edu.mit.simile.longwell.Profile;
import edu.mit.simile.longwell.query.Order;
import edu.mit.simile.longwell.query.Ranking;
import edu.mit.simile.longwell.query.compare.DateComparator;
import edu.mit.simile.longwell.query.compare.DoubleComparator;
import edu.mit.simile.longwell.query.compare.IComparator;
import edu.mit.simile.longwell.query.compare.IntegerComparator;
import edu.mit.simile.longwell.query.compare.StringComparator;
import edu.mit.simile.longwell.query.project.DateTimeProjector;
import edu.mit.simile.longwell.query.project.DoubleProjector;
import edu.mit.simile.longwell.query.project.IProjection;
import edu.mit.simile.longwell.query.project.IProjector;
import edu.mit.simile.longwell.query.project.IntegerProjector;
import edu.mit.simile.longwell.query.project.PropertyProjector;
import edu.mit.simile.longwell.query.project.SelfURIProjector;
import edu.mit.simile.longwell.schema.LearnedProperty;

public class Sorter {

    final static private Logger s_logger = Logger.getLogger(Sorter.class);

    final protected Profile m_profile;
    final protected QueryEngine m_engine;
    final protected Cache m_objectsToRankings;
    final protected Cache m_objectsToOrderingToSorted;

    public class Rankings {

        List m_rankings;
        Ranking m_topRankings;

        public Ranking getTopRanking() {
            return m_topRankings;
        }

        public List getRankings() {
            return m_rankings;
        }

        public Rankings(List rankings, Ranking topRanking) {
            m_rankings = rankings;
            m_topRankings = topRanking;
        }
    }

    static private class MyComparator implements Comparator {

        IComparator[] m_comparators;
        Map[] m_objectToValueMaps;
        String[] m_locales;
        boolean[] m_ascendings;

        public int compare(Object o1, Object o2) {
            int c = 0;

            for (int i = 0; c == 0 && i < m_comparators.length; i++) {
                Map map = m_objectToValueMaps[i];
                c = m_comparators[i].compare(map.get(o1), map.get(o2), m_locales[i], m_ascendings[i]);
            }

            if (c == 0) {
                c = o1.toString().compareTo(o2.toString());
            }
            
            return c;
        }

    }

    public Sorter(Profile profile, QueryEngine engine) {
        m_profile = profile;
        m_engine = engine;
        CacheFactory f = profile.getCacheFactory();
        m_objectsToRankings = f.getCache("objects-to-rankings", false);
        m_objectsToOrderingToSorted = f.getCache("objects", "ordering", false);
    }

    /**
     * Answer a list resulted by sorting the given objects in the given list of
     * Order's.
     * 
     * @param objects
     * @param orders
     * @param locale
     * @param fresh
     * @return
     */
    public List sort(Set objects, List orders, String locale, boolean fresh) throws QueryEvaluationException, RepositoryException {
        String orderString = "";
        Iterator i = orders.iterator();
        while (i.hasNext()) {
            Order order = (Order) i.next();

            orderString += order.m_projectorName + order.m_projectorParameter + order.m_comparatorName
                    + order.m_comparatorParameter + order.m_ascending + ";";
        }

        List results = (List) m_objectsToOrderingToSorted.get(objects, orderString);
        if (fresh || results == null) {
            results = internalSort(objects, orders, locale);
            m_objectsToOrderingToSorted.put(objects, orderString, results);
        }

        return results;
    }

    protected List internalSort(Set objects, List orders, String locale) throws QueryEvaluationException, RepositoryException {
        MyComparator c = new MyComparator();

        c.m_objectToValueMaps = new Map[orders.size()];
        c.m_locales = new String[orders.size()];
        c.m_comparators = new IComparator[orders.size()];
        c.m_ascendings = new boolean[orders.size()];

        int i = 0;

        Iterator j = orders.iterator();
        while (j.hasNext()) {
            Order order = (Order) j.next();
            IProjection projection = m_engine.getProjectorManager().getProjector(order.m_projectorName,
                    order.m_projectorParameter, locale).project(objects);
            IComparator comparator = m_engine.getComparatorManager().getComparator(order.m_comparatorName,
                    order.m_comparatorParameter);

            Map m = projection.getObjectToValueMap();
            if (m != null) {
                Map m2 = new HashMap();

                Iterator n = objects.iterator();
                while (n.hasNext()) {
                    Object object = n.next();
                    Object value = object instanceof URI ? m.get(object) : object;

                    m2.put(object, comparator.preprocess(value, locale));
                }

                c.m_objectToValueMaps[i] = m2;
                c.m_locales[i] = projection.getLocale();
                c.m_comparators[i] = comparator;
                c.m_ascendings[i] = order.m_ascending;
                i++;
            }
        }

        TreeSet treeSet = new TreeSet(c);
        treeSet.addAll(objects);
        return new ArrayList(treeSet);
    }

    /**
     * Answer a Rankings for resorting the given set of objects.
     * 
     * @param objects
     * @param restrictors
     * @param locale
     * @param fresh
     * @return
     */
    public Rankings suggestOrders(Set objects, SortedSet restrictors, String locale, boolean fresh)
            throws QueryEvaluationException, RepositoryException {

        Rankings rankings = (Rankings) m_objectsToRankings.get(objects);
        if (fresh || rankings == null) {
            rankings = internalSuggestOrders(objects, restrictors, locale);
            m_objectsToRankings.put(objects, rankings);
        }
        return rankings;
    }

    protected Rankings internalSuggestOrders(Set objects, SortedSet restrictors, String locale) throws QueryEvaluationException, RepositoryException {

        SortedSet<ScoredRanking> rankings = new TreeSet<ScoredRanking>(new Comparator() {
            public boolean equals(Object obj) {
                return false;
            }

            public int compare(Object o1, Object o2) {
                Ranking r1 = (Ranking) o1;
                Ranking r2 = (Ranking) o2;

                return r1.getLabel().compareToIgnoreCase(r2.getLabel());
            }
        });

        Ranking topRanking = null;
        float score = 0;

        Set<IProjector> uniqueProjectors = new HashSet<IProjector>();
        Iterator i = restrictors.iterator();
        while (i.hasNext()) {
            Restrictor restrictor = (Restrictor) i.next();

            if (restrictor.m_bucketer.matchesOneValue(restrictor.m_bucketerParameter)) {
                uniqueProjectors.add(restrictor.m_projector);
            }
        }

        i = m_profile.getSchemaModel().getSortedLearnedProperties(objects).iterator();

        outer: while (i.hasNext()) {
            try {
                ScoredRanking ranking = createRanking((LearnedProperty) i.next(), locale);

                IProjector projector = ranking.m_projector;
                Iterator j = uniqueProjectors.iterator();
                while (j.hasNext()) {
                    IProjector projector2 = (IProjector) j.next();

                    if (projector2.getClass().getName().equals(projector.getClass().getName())
                            && projector2.getParameter().equals(projector.getParameter())) {

                        continue outer;
                    }
                }

                rankings.add(ranking);

                if (ranking.m_score > score) {
                    topRanking = ranking;
                    score = ranking.m_score;
                }
            } catch (QueryEvaluationException e) {
                s_logger.error("Failed to create comparator", e);
            }
        }

        if (objects.size() > 100) {
            IProjector projector = m_engine.getProjectorManager().getProjector(SelfURIProjector.class.getName(), "",
                    locale);

            IComparator comparator = m_engine.getComparatorManager().getComparator(StringComparator.class.getName(),
                    null);

            topRanking = new Ranking(projector.getLabel(locale), projector, comparator);
        }

        return new Rankings(new ArrayList(rankings), topRanking);
    }

    protected ScoredRanking createRanking(LearnedProperty learnedProperty, String locale) throws QueryEvaluationException {

        String label = learnedProperty.getLabel(locale);

        String projectorName = PropertyProjector.class.getName();
        String projectorParameter = learnedProperty.getURI().toString();
        String comparatorName = StringComparator.class.getName();

        if (learnedProperty.getTypeConfidence(LearnedProperty.s_type_integer) > 0.5) {
            projectorName = IntegerProjector.class.getName();
            comparatorName = IntegerComparator.class.getName();
        } else if (learnedProperty.getTypeConfidence(LearnedProperty.s_type_numeric) > 0.5) {
            projectorName = DoubleProjector.class.getName();
            comparatorName = DoubleComparator.class.getName();
        } else if (learnedProperty.getTypeConfidence(LearnedProperty.s_type_dateTime) > 0.5) {
            projectorName = DateTimeProjector.class.getName();
            comparatorName = DateComparator.class.getName();
        }

        IProjector projector = m_engine.getProjectorManager().getProjector(projectorName, projectorParameter, locale);

        IComparator comparator = m_engine.getComparatorManager().getComparator(comparatorName, null);

        return new ScoredRanking(label, projector, comparator, projector.getUniqueness()
                * learnedProperty.countOccurrences());
    }

    static protected class ScoredRanking extends Ranking {
        final public float m_score;

        public ScoredRanking(String label, IProjector projector, IComparator comparator, float score) {
            super(label, projector, comparator);
            m_score = score;
        }
    }

}
