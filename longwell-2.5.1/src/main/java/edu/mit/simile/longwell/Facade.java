package edu.mit.simile.longwell;

import java.util.Comparator;

public class Facade {

    final public String m_uri;
    final public String m_shortLabel;
    final public String m_contentDescription;
    final public String m_initialQuery;

    protected int m_count;

    public Facade(String uri, String shortLabel, String contentDescription, String initialQuery) {
        m_uri = uri;
        m_shortLabel = shortLabel;
        m_contentDescription = contentDescription;
        m_initialQuery = initialQuery;
    }

    public String getURI() {
        return m_uri;
    }

    public String getShortLabel() {
        return m_shortLabel;
    }

    public String getContentDescription() {
        return m_contentDescription;
    }

    public String getInitialQuery() {
        return m_initialQuery;
    }

    public int getCount() {
        return m_count;
    }

    static public class LabelComparator implements Comparator {
        public int compare(Object o1, Object o2) {
            Facade f1 = (Facade) o1;
            Facade f2 = (Facade) o2;
            return compareText(f1, f2);
        }
    }

    static public class CountComparator implements Comparator {
        public int compare(Object o1, Object o2) {
            Facade f1 = (Facade) o1;
            Facade f2 = (Facade) o2;

            int i = f1.m_count - f2.m_count;
            if (i == 0) {
                i = compareText(f1, f2);
            }
            return i;
        }
    }

    static protected int compareText(Facade f1, Facade f2) {
        int i = f1.m_shortLabel.compareTo(f2.m_shortLabel);
        if (i == 0) {
            i = f1.m_contentDescription.compareTo(f2.m_contentDescription);
        }
        if (i == 0) {
            i = f1.m_uri.compareTo(f2.m_uri);
        }
        return i;
    }
}
