package edu.mit.simile.longwell.query.engine;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;

import edu.mit.simile.longwell.Profile;
import edu.mit.simile.longwell.StructuredModelBase;
import edu.mit.simile.longwell.query.Order;
import edu.mit.simile.longwell.query.Query;
import edu.mit.simile.longwell.query.Ranking;
import edu.mit.simile.longwell.query.Restriction;
import edu.mit.simile.longwell.query.bucket.BucketerManager;
import edu.mit.simile.longwell.query.compare.ComparatorManager;
import edu.mit.simile.longwell.query.compare.IComparator;
import edu.mit.simile.longwell.query.engine.Sorter.Rankings;
import edu.mit.simile.longwell.query.project.IProjector;
import edu.mit.simile.longwell.query.project.ProjectorManager;

public class QueryEngine extends StructuredModelBase {
    
    final static protected Logger s_logger = Logger.getLogger(QueryEngine.class);

    final protected Refiner m_refiner;
    final protected Narrower m_narrower;
    final protected Broadener m_broadener;
    final protected Sorter m_sorter;
    final protected Indexer m_indexer;
    final protected Paginator m_paginator;
    final protected ProjectorManager m_projectorManager;
    final protected BucketerManager m_bucketerManager;
    final protected ComparatorManager m_comparatorManager;

    public class Answer {
        
        public class OrderInfo {
            final public String m_label;

            final public String m_directionLabel;

            public OrderInfo(String label, String directionLabel) {
                m_label = label;
                m_directionLabel = directionLabel;
            }

            public String getLabel() {
                return m_label;
            }

            public String getDirectionLabel() {
                return m_directionLabel;
            }
        }
        
        final protected Query m_query;
        final protected String m_locale;
        final protected boolean m_fresh;
        final protected SortedSet m_restrictors;
        protected Set m_objects;
        protected List m_sortedObjects;
        protected List m_pages;
        protected List m_index;
        protected List<Order> m_orders;
        protected List<OrderInfo> m_orderInfos;
        protected Rankings m_rankings;
        protected List m_narrowingFacets;
        protected List m_hiddenFacets;
        protected Set m_broadeningFacets;
        protected Set m_relatingQueries;

        protected Answer(Query query, boolean fresh) throws QueryEvaluationException {
            m_query = query;
            m_locale = getLocale(query);
            m_fresh = fresh;
            m_restrictors = makeRestrictors(query.getRestrictions(), m_locale);
        }

        public Set getObjects() throws QueryEvaluationException, RepositoryException {
            if (m_objects == null) {
                if (s_logger.isDebugEnabled()) s_logger.debug("> refiner()");
                m_objects = m_refiner.refine(m_restrictors, m_locale, m_fresh);
                if (s_logger.isDebugEnabled()) s_logger.debug("< refiner()");
            }
            return m_objects;
        }

        public List getSortedObjects() throws QueryEvaluationException, RepositoryException {
            if (m_sortedObjects == null) {
                Set objects = getObjects();
                getOrderInfos();
                if (s_logger.isDebugEnabled()) s_logger.debug("> sorter()");
                m_sortedObjects = m_sorter.sort(objects, m_orders, m_locale, m_fresh);
                if (s_logger.isDebugEnabled()) s_logger.debug("< sorter()");
            }
            return m_sortedObjects;
        }

        public List getPages() throws QueryEvaluationException {
            if (m_pages == null) {
                try {
                    List sortedObjects = getSortedObjects();

                    if (s_logger.isDebugEnabled()) s_logger.debug("> paginator()");
                    int itemsPerPage = 10;
                    try {
                        itemsPerPage = Math.max(2, Integer.parseInt(m_query.getFirstParamValue("itemsPerPage")));
                    } catch (Exception e) {
                        // silent
                    }

                    m_pages = m_paginator.paginate(sortedObjects, m_query, itemsPerPage, m_fresh);

                    if (s_logger.isDebugEnabled()) s_logger.debug("< paginator()");
                } catch (Exception e) {
                    s_logger.error(e);
                }
            }
            return m_pages;
        }

        public List getIndex() throws QueryEvaluationException, RepositoryException {
            if (m_index == null) {
                Set objects = getObjects();
                List sortedObjects = getSortedObjects();

                if (s_logger.isDebugEnabled()) s_logger.debug("> indexer()");
                m_index = m_indexer.index(objects, sortedObjects, m_orders, 10, m_locale, m_fresh);
                if (s_logger.isDebugEnabled()) s_logger.debug("< indexer()");
            }
            return m_index;
        }

        public List getOrderInfos() throws QueryEvaluationException, RepositoryException {
            if (m_orderInfos == null) {
                Rankings rankings = getRankings();

                if (s_logger.isDebugEnabled()) s_logger.debug("> order infos");

                m_orders = m_query.getOrders();
                m_orderInfos = new ArrayList<OrderInfo>();

                if (m_orders.size() == 0) {
                    if (rankings.getTopRanking() != null) {
                        m_orders = new ArrayList<Order>();

                        Ranking ranking = rankings.getTopRanking();

                        m_orders.add(new Order(ranking.m_projector.getClass().getName(), ranking.m_projector
                                .getParameter(), ranking.m_comparator.getClass().getName(), ranking.m_comparator
                                .getParameter(), ranking.m_comparator.isAscendingByDefault(m_locale), null));
                        m_orderInfos.add(new OrderInfo(ranking.m_projector.getLabel(m_locale), ranking.m_comparator
                                .isAscendingByDefault(m_locale) ? ranking.m_comparator.getAscendingLabel(m_locale)
                                : ranking.m_comparator.getDescendingLabel(m_locale)));
                    }
                } else {
                    Iterator i = m_orders.iterator();
                    while (i.hasNext()) {
                        Order order = (Order) i.next();

                        IProjector projector = m_projectorManager.getProjector(order.m_projectorName,
                                order.m_projectorParameter, m_locale);
                        IComparator comparator = m_comparatorManager.getComparator(order.m_comparatorName,
                                order.m_comparatorParameter);

                        m_orderInfos.add(new OrderInfo(projector.getLabel(m_locale), order.m_ascending ? comparator
                                .getAscendingLabel(m_locale) : comparator.getDescendingLabel(m_locale)));
                    }
                }

                if (s_logger.isDebugEnabled()) s_logger.debug("< order infos");
            }
            return m_orderInfos;
        }

        public Rankings getRankings() throws QueryEvaluationException, RepositoryException {
            if (m_rankings == null) {
                Set objects = getObjects();

                if (s_logger.isDebugEnabled()) s_logger.debug("> rankings()");
                m_rankings = m_sorter.suggestOrders(objects, m_restrictors, m_locale, m_fresh);
                if (s_logger.isDebugEnabled()) s_logger.debug("< rankings()");
            }
            return m_rankings;
        }

        public List getNarrowingFacets() throws QueryEvaluationException, RepositoryException {
            if (m_narrowingFacets == null) {
                Set objects = getObjects();

                if (s_logger.isDebugEnabled()) s_logger.debug("> narrower()");
                m_narrowingFacets = m_narrower.narrow(objects, m_query.getExplicitFacets(), m_locale, m_fresh);
                if (s_logger.isDebugEnabled()) s_logger.debug("< narrower()");
            }
            return m_narrowingFacets;
        }

		public List getHiddenFacets() throws QueryEvaluationException, RepositoryException {
            if (m_hiddenFacets == null) {
                Set objects = getObjects();

                if (s_logger.isDebugEnabled()) s_logger.debug("> hidden facets");
                m_hiddenFacets = m_narrower.hide(objects, m_locale);
                if (s_logger.isDebugEnabled()) s_logger.debug("< hidden facets");
            }
            return m_hiddenFacets;
		}

        public Set getBroadeningFacets() throws QueryEvaluationException, RepositoryException {
            if (m_broadeningFacets == null) {
                Set objects = getObjects();

                if (s_logger.isDebugEnabled()) s_logger.debug("> broadener()");
                m_broadeningFacets = m_broadener.broaden(objects, m_restrictors, m_locale, m_fresh);
                if (s_logger.isDebugEnabled()) s_logger.debug("< broadener()");
            }
            return m_broadeningFacets;
        }
    }

    public QueryEngine(Profile profile) {
        super(profile);
        m_refiner = new Refiner(profile, this);
        m_narrower = new Narrower(profile, this);
        m_broadener = new Broadener(profile, this);
        m_sorter = new Sorter(profile, this);
        m_indexer = new Indexer(profile, this);
        m_paginator = new Paginator(profile, this);

        m_projectorManager = new ProjectorManager(profile);
        m_bucketerManager = new BucketerManager(profile);
        m_comparatorManager = new ComparatorManager(profile);
    }

    public Answer query(Query query, boolean fresh) throws QueryEvaluationException {
        return new Answer(query, fresh);
    }

    public Set queryObjects(Query query, boolean fresh) throws QueryEvaluationException, RepositoryException {
        String locale = getLocale(query);
        SortedSet restrictors = makeRestrictors(query.getRestrictions(), locale);

        return m_refiner.refine(restrictors, locale, fresh);
    }

    public int queryCount(Query query, boolean fresh) throws QueryEvaluationException, RepositoryException {
        Set objects = queryObjects(query, fresh);
        if (objects != null) {
            return objects.size();
        }
        return 0;
    }

    public Refiner getRefiner() {
        return m_refiner;
    }

    public Narrower getNarrower() {
        return m_narrower;
    }

    public Broadener getBroadener() {
        return m_broadener;
    }

    public Sorter getSorter() {
        return m_sorter;
    }

    public Indexer getIndexer() {
        return m_indexer;
    }

    public Paginator getPaginator() {
        return m_paginator;
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

    protected String getLocale(Query query) {
        return query.getFirstParamValue("locale");
    }

    public void onBeforeAdd(RepositoryConnection c) {
        m_projectorManager.onBeforeAdd(c);
        m_comparatorManager.onBeforeAdd(c);
        m_bucketerManager.onBeforeAdd(c);
    }

    public void onAfterAdd(RepositoryConnection c) {
        m_projectorManager.onAfterAdd(c);
        m_comparatorManager.onAfterAdd(c);
        m_bucketerManager.onAfterAdd(c);
    }

    public void onFailingAdd(RepositoryConnection c) {
        m_projectorManager.onFailingAdd(c);
        m_comparatorManager.onFailingAdd(c);
        m_bucketerManager.onFailingAdd(c);
    }

    public void onBeforeRemove(RepositoryConnection c) {
        m_projectorManager.onBeforeRemove(c);
        m_comparatorManager.onBeforeRemove(c);
        m_bucketerManager.onBeforeRemove(c);
    }

    public void onAfterRemove(RepositoryConnection c) {
        m_projectorManager.onAfterRemove(c);
        m_comparatorManager.onAfterRemove(c);
        m_bucketerManager.onAfterRemove(c);
    }

    public void onFailingRemove(RepositoryConnection c) {
        m_projectorManager.onFailingRemove(c);
        m_comparatorManager.onFailingRemove(c);
        m_bucketerManager.onFailingRemove(c);
    }

    protected SortedSet makeRestrictors(Collection restrictions, String locale) throws QueryEvaluationException {
        SortedSet<Restrictor> restrictors = new TreeSet<Restrictor>(new Comparator() {
            public boolean equals(Object obj) {
                return false;
            }

            public int compare(Object o1, Object o2) {
                Restrictor r1 = (Restrictor) o1;
                Restrictor r2 = (Restrictor) o2;

                int i = 0;
                if (r1.m_projector.isEfficientForRootProjection()) {
                    if (!r2.m_projector.isEfficientForRootProjection()) {
                        i = -1;
                    }
                } else if (r2.m_projector.isEfficientForRootProjection()) {
                    i = 1;
                }

                if (i == 0) {
                    i = r1.m_projector.getClass().getName().compareTo(r2.m_projector.getClass().getName());
                }
                if (i == 0) {
                    i = r1.m_projector.getParameter().compareTo(r2.m_projector.getParameter());
                }
                if (i == 0) {
                    i = r1.m_bucketer.getClass().getName().compareTo(r2.m_bucketer.getClass().getName());
                }
                if (i == 0) {
                    i = r1.m_bucketerParameter.compareTo(r2.m_bucketerParameter);
                }

                return i;
            }
        });

        Iterator i = restrictions.iterator();
        while (i.hasNext()) {
            Restriction restriction = (Restriction) i.next();

            restrictors.add(new Restrictor(m_projectorManager.getProjector(restriction.m_projectorName,
                    restriction.m_projectorParameter, locale), m_bucketerManager
                    .getBucketer(restriction.m_bucketerName), restriction.m_bucketerParameter, restriction.m_id));
        }

        return restrictors;
    }

}
