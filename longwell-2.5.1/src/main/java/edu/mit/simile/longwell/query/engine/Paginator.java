package edu.mit.simile.longwell.query.engine;

import java.util.ArrayList;
import java.util.List;

import edu.mit.simile.longwell.Cache;
import edu.mit.simile.longwell.Profile;
import edu.mit.simile.longwell.query.Query;

public class Paginator {
    
    final protected Profile m_profile;
    final protected QueryEngine m_engine;
    final protected Cache m_paginationCache;

    Paginator(Profile profile, QueryEngine engine) {
        m_profile = profile;
        m_engine = engine;
        m_paginationCache = profile.getCacheFactory().getCache("pagination", false);
    }

    public List paginate(List sortedObjects, Query query, int itemsPerPage, boolean fresh) {

        List<Page> pages = null;

        if (fresh) {
            m_paginationCache.clear();
        } else {
            pages = (List) m_paginationCache.get(sortedObjects);
        }

        if (pages == null) {
            pages = new ArrayList<Page>();

            if (sortedObjects.size() > 0) {
                int totalCount = sortedObjects.size();

                int index = 0;
                int pageIndex = 1;

                while (index < totalCount) {
                    int endIndex = Math.min(index + itemsPerPage, totalCount);

                    Query query2 = query.dup();
                    query2.setParameter("page", Integer.toString(pageIndex - 1));

                    pages.add(new Page(sortedObjects, pageIndex, index, endIndex - index, query2));

                    index += itemsPerPage;
                    pageIndex++;
                }

                m_paginationCache.put(sortedObjects, pages);
            }
        }

        return pages;
    }

}
