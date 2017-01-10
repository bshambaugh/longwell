package edu.mit.simile.longwell.query.bucket;

import java.util.List;

public class BucketTheme {

    final public String m_label;
    final public String m_themeID;
    final public List m_bucketActors; // narrowers or broadeners

    public BucketTheme(String label, String themeID, List bucketActors) {
        m_label = label;
        m_themeID = themeID;
        m_bucketActors = bucketActors;
    }

    public String getLabel() {
        return m_label;
    }

    public String getThemeID() {
        return m_themeID;
    }

    public List getBuckets() {
        return m_bucketActors;
    }
}
