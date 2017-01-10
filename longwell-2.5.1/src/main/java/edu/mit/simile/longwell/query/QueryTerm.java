package edu.mit.simile.longwell.query;

/**
 * A generic element of a Query: a simple name-value pair.
 */
public class QueryTerm {
    final private String m_name;

    final private String m_value;

    public QueryTerm(String name, String value) {
        m_name = name;
        m_value = value;
    }

    public String getName() {
        return m_name;
    }

    public String getValue() {
        return m_value;
    }

    public String toString() {
        return (getName() + '=' + getValue());
    }
}
