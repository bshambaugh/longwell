package edu.mit.simile.longwell.query;

import java.util.List;

public class Facet {

    final public String m_projectorName;
    final public String m_projectorParameter;
    final public String m_label;
    final public List m_bucketThemes;
    final public boolean m_explicit;

    public Facet(String projectorName, String projectorParameter, String label, List bucketThemes, boolean explicit) {
        m_projectorName = projectorName;
        m_projectorParameter = projectorParameter;
        m_label = label;
        m_bucketThemes = bucketThemes;
        m_explicit = explicit;
    }

    public String getProjectorName() {
        return m_projectorName;
    }

    public String getProjectorParameter() {
        return m_projectorParameter;
    }

    public String getLabel() {
        return m_label;
    }

    public List getBucketThemes() {
        return m_bucketThemes;
    }

    public boolean isExplicit() {
        return m_explicit;
    }
}
