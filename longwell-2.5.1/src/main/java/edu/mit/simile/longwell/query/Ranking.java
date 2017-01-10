package edu.mit.simile.longwell.query;

import edu.mit.simile.longwell.Utilities;
import edu.mit.simile.longwell.query.compare.IComparator;
import edu.mit.simile.longwell.query.project.IProjector;

public class Ranking {

    final public String m_label;
    final public IProjector m_projector;
    final public IComparator m_comparator;

    public Ranking(String label, IProjector projector, IComparator comparator) {
        m_label = label;
        m_projector = projector;
        m_comparator = comparator;
    }

    public String getLabel() {
        return m_label;
    }

    public IComparator getComparator() {
        return m_comparator;
    }

    public String toURLParameter(boolean ascending) {
        return Query.s_orderPrefix
                + "="
                + Utilities.encode(Query.escapeLongwell(m_projector.getClass().getName()) + Query.s_separator
                        + m_projector.getParameter() + Query.s_separator
                        + Query.escapeLongwell(m_comparator.getClass().getName()) + Query.s_separator
                        + m_comparator.getParameter() + Query.s_separator + (ascending ? "a" : "d"));
    }
}
