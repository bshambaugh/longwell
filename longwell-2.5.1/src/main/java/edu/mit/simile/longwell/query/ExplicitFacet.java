package edu.mit.simile.longwell.query;

import java.util.List;

public class ExplicitFacet extends Facet {
    String m_propertyURI;

    public ExplicitFacet(String projectorName, String projectorParameter, String label, List bucketThemes,
            String propertyURI) {
        super(projectorName, projectorParameter, label, bucketThemes, true);
        m_propertyURI = propertyURI;
    }

    public String getPropertyURI() {
        return m_propertyURI;
    }
}
