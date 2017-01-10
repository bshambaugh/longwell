package edu.mit.simile.longwell.query.compare;

import java.util.Date;

import edu.mit.simile.longwell.Profile;

public class DateComparator extends ComparatorBase {

    public DateComparator(Profile profile, String parameter) {
        super(profile, parameter);
    }

    public Object preprocess(Object value, String locale) {
        return value;
    }

    protected int internalCompareAscending(Object v1, Object v2, String locale) {
        if (v1 == null) {
            return v2 != null ? -1 : 0;
        }
        return v2 != null ? ((Date) v1).compareTo((Date) v2) : 1;
    }

}
