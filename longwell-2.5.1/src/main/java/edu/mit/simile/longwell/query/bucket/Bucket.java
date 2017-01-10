package edu.mit.simile.longwell.query.bucket;

import java.util.Comparator;

public class Bucket {

    final public String m_bucketerName;
    final public String m_label;
    final public String m_bucketerParameter;
    final public int m_count;

    public Bucket(String bucketerName, String bucketerParameter, String label, int count) {
        m_bucketerName = bucketerName;
        m_bucketerParameter = bucketerParameter;
        m_label = label;
        m_count = count;
    }

    public String getLabel() {
        return m_label;
    }

    public String getBucketerName() {
        return m_bucketerName;
    }

    public int getCount() {
        return m_count;
    }

    public String getBucketerParameter() {
        return m_bucketerParameter;
    }

    static class BucketComparator implements Comparator {
        public int compare(Object o1, Object o2) {
            Bucket b1 = (Bucket) o1;
            Bucket b2 = (Bucket) o2;
            int i = 0;
            if (b1.m_count < b2.m_count) {
                i = 1;
            } else if (b1.m_count > b2.m_count) {
                i = -1;
            } else {
                i = b1.m_label.compareToIgnoreCase(b2.m_label);
                if (i == 0) {
                    i = b1.m_bucketerName.compareTo(b2.m_bucketerName);
                }
            }
            return i;
        }

    }
}
