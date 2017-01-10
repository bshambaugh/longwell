package edu.mit.simile.longwell.query.engine;

import java.util.ArrayList;
import java.util.List;

import edu.mit.simile.longwell.query.Query;

public class Page {
    
    final protected List m_allObjects;
    final protected int m_pageNumber;
    final protected int m_startIndex;
    final protected int m_count;
    final protected Query m_query;

    protected List m_objects;

    public Page(List allObjects, int pageNumber, int startIndex, int count, Query query) {
        m_allObjects = allObjects;

        m_pageNumber = pageNumber;
        m_startIndex = startIndex;
        m_count = count;

        m_query = query;
    }

    public int getPageNumber() {
        return m_pageNumber;
    }

    public int getStartIndex() {
        return m_startIndex + 1;
    }

    public int getEndIndex() {
        return getStartIndex() + getCount() - 1;
    }

    public int getCount() {
        return m_count;
    }

    public List getObjects() {
        if (m_objects == null) {
            m_objects = new ArrayList(m_allObjects.subList(m_startIndex, m_startIndex + m_count));
        }
        return m_objects;
    }

    public Query getQuery() {
        return m_query;
    }
}
