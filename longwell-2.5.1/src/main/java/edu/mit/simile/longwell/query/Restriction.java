package edu.mit.simile.longwell.query;

import edu.mit.simile.longwell.Utilities;

/**
 * An element of a Query which limits the results.
 *
 * The results are projected to a particular data type and then
 * filtered down according to a particular criterion. (The projector
 * and the bucketer are only specified by name, at this point. They
 * will be loaded by a ProjectorManager and a BucketerManager later
 * on.)
 */
public class Restriction {

    final public int m_id;
    final public String m_projectorName;
    final public String m_projectorParameter;
    final public String m_bucketerName;
    final public String m_bucketerParameter;
    final public String m_escapedURLString;

    public Restriction(int id, String projectorName, String projectorParameter, String bucketerName,
            String bucketerParameter, String escapedURLString) {
        m_id = id;
        m_projectorName = projectorName;
        m_projectorParameter = projectorParameter;
        m_bucketerName = bucketerName;
        m_bucketerParameter = bucketerParameter;
        m_escapedURLString = escapedURLString != null ? escapedURLString : escape(projectorName, projectorParameter,
                bucketerName, bucketerParameter);
    }

    public String toString() {
        StringBuffer s = new StringBuffer();

        s.append("projector=");
        s.append(m_projectorName);
        s.append("(");
        s.append(m_projectorParameter);
        s.append("),");
        s.append("bucketer=");
        s.append(m_bucketerName);
        s.append("(");
        s.append(m_bucketerParameter);
        s.append(")");

        return s.toString();
    }

    static public String escape(String projectorName, String projectorParameter, String bucketerName,
            String bucketerParameter) {
        StringBuffer s = new StringBuffer();

        s.append(Utilities.encode(Query.escapeSeparator(Query.escapeLongwell(projectorName))));
        s.append(Query.s_encoded_separator);
        s.append(Utilities.encode(Query.escapeSeparator(projectorParameter)));
        s.append(Query.s_encoded_separator);
        s.append(Utilities.encode(Query.escapeSeparator(Query.escapeLongwell(bucketerName))));
        s.append(Query.s_encoded_separator);
        s.append(Utilities.encode(Query.escapeSeparator(bucketerParameter)));

        return s.toString();
    }
}
