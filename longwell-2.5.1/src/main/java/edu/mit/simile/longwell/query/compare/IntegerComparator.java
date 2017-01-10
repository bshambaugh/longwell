package edu.mit.simile.longwell.query.compare;

import edu.mit.simile.longwell.Profile;

/**
 * Comparator which casts objects to Integer and compares them numerically.
 */
public class IntegerComparator extends ComparatorBase {

    public IntegerComparator(Profile profile, String parameter) {
        super(profile, parameter);
    }

    public Object preprocess(Object value, String locale) {
        return value;
    }

    protected int internalCompareAscending(Object v1, Object v2, String locale) {
        if (v1 == null) {
            return v2 == null ? 0 : -1;
        } else if (v2 == null) {
            return 1;
        } else {
            long l1 = ((Number) v1).longValue();
            long l2 = ((Number) v2).longValue();
            if (l1 < l2)
                return -1;
            if (l1 > l2)
                return 1;
            return 0;
        }
    }

}
