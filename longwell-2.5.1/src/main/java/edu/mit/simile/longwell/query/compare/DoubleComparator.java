package edu.mit.simile.longwell.query.compare;

import edu.mit.simile.longwell.Profile;

public class DoubleComparator extends ComparatorBase {

    public DoubleComparator(Profile profile, String parameter) {
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
            return ((Double) v1).compareTo((Double) v2);
        }
    }

}
