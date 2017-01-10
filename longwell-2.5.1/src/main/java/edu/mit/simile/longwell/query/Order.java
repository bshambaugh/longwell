package edu.mit.simile.longwell.query;

import edu.mit.simile.longwell.Utilities;

/**
 * An element of a Query which specifies an order for the results.
 *
 * The results are projected to a particular data type and then
 * ordered according to a particular criterion. (The projector and the
 * comparator are only specified by name, at this point. They will be
 * loaded by a ProjectorManager and a ComparatorManager later on.)
 */
public class Order {

    final public String m_projectorName;
    final public String m_projectorParameter;
    final public String m_comparatorName;
    final public String m_comparatorParameter;
    final public boolean m_ascending;
    final public String m_escapedURLString;

    public Order(String projectorName, String projectorParameter, String comparatorName, String comparatorParameter,
            boolean ascending, String escapedURLString) {
        m_projectorName = projectorName;
        m_projectorParameter = projectorParameter;
        m_comparatorName = comparatorName;
        m_comparatorParameter = comparatorParameter;
        m_ascending = ascending;
        m_escapedURLString = escapedURLString != null ? escapedURLString : escape(projectorName, projectorParameter,
                comparatorName, comparatorParameter, ascending);
    }

    public String toString() {
        StringBuffer s = new StringBuffer();

        s.append("projector=");
        s.append(m_projectorName);
        s.append("(");
        s.append(m_projectorParameter);
        s.append("),");
        s.append("comparator=");
        s.append(m_comparatorName);
        s.append("(");
        s.append(m_comparatorParameter);
        s.append("),");
        s.append(m_ascending ? "ascending" : "descending");

        return s.toString();
    }

    static public String escape(String projectorName, String projectorParameter, String comparatorName,
            String comparatorParameter, boolean ascending) {
        StringBuffer s = new StringBuffer();

        s.append(Utilities.encode(Query.escapeSeparator(Query.escapeLongwell(projectorName))));
        s.append(Query.s_encoded_separator);
        s.append(Utilities.encode(Query.escapeSeparator(projectorParameter)));
        s.append(Query.s_encoded_separator);
        s.append(Utilities.encode(Query.escapeSeparator(Query.escapeLongwell(comparatorName))));
        s.append(Query.s_encoded_separator);
        s.append(Utilities.encode(Query.escapeSeparator(comparatorParameter)));
        s.append(Query.s_encoded_separator);
        s.append(ascending ? 'a' : 'd');

        return s.toString();
    }
}
