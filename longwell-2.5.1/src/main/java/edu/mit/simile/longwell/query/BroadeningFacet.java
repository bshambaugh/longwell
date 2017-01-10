package edu.mit.simile.longwell.query;

import java.util.List;

public class BroadeningFacet extends Facet {

    final public List m_existingBuckets;
    final public int m_restrictionID;

    /**
     * @param projectorName
     * @param projectorParameter
     * @param label
     * @param bucketThemes
     * @param restrictorID
     */
    public BroadeningFacet(String projectorName, String projectorParameter, String label, List bucketThemes,
            List existingBuckets, int restrictionID) {
        super(projectorName, projectorParameter, label, bucketThemes, false);
        m_existingBuckets = existingBuckets;
        m_restrictionID = restrictionID;
    }

    public List getExistingBuckets() {
        return m_existingBuckets;
    }

    public int getRestrictionID() {
        return m_restrictionID;
    }
}
