package edu.mit.simile.longwell.query.compare;

import org.openrdf.model.Literal;
import org.openrdf.model.URI;
import org.openrdf.repository.RepositoryException;

import edu.mit.simile.longwell.Profile;

/**
 * Simple comparator which turns objects to strings and compares them
 * lexicographically. (URIs are turned into human-readable labels instead?)
 *
 * A parameter of "c" gives case-sensitive comparison.
 */
public class StringComparator extends ComparatorBase {

    final protected boolean m_caseSensitive;

    public StringComparator(Profile profile, String parameter) {
        super(profile, parameter);
        m_caseSensitive = "c".equals(parameter);
    }

    public Object preprocess(Object value, String locale) {
        return objectToString(value, locale);
    }

    protected int internalCompareAscending(Object v1, Object v2, String locale) {
        // TODO(DH) Use a collator here:
        // Collator.getInstance(getLocale(locale)).compare(...)
        String s1 = v1 == null ? "" : (String) v1;
        String s2 = v2 == null ? "" : (String) v2;

        if (m_caseSensitive) {
            return s1.compareTo(s2);
        }
        return s1.compareToIgnoreCase(s2);
    }

    protected String objectToString(Object v, String locale) {
        String s = null;
        if (v instanceof Literal) {
            s = ((Literal) v).getLabel();
        } else if (v instanceof URI) {
            try {
                s = m_profile.getSchemaModel().getLabel((URI) v, locale);
            } catch (RepositoryException e) {
                // ignore
            }
        } else if (v != null) {
            s = v.toString();
        }

        if (s == null) {
            s = "";
        }
        return s;
    }
}
