package edu.mit.simile.longwell.query.compare;

import edu.mit.simile.longwell.Coordinates;
import edu.mit.simile.longwell.Profile;

// TODO: is this necessary?  a comparator isn't going to be correct trying to linearize a multidimensional space
public class CoordinatesComparator extends ComparatorBase {

	public CoordinatesComparator(Profile profile, String parameter) {
		super(profile, parameter);
	}

	protected int internalCompareAscending(Object v1, Object v2, String locale) {
        if (v1 == null) {
            return v2 != null ? -1 : 0;
        }
        return v2 != null ? ((Coordinates) v1).compareTo((Coordinates) v2) : 1;
	}

	public Object preprocess(Object value, String locale) {
		return value;
	}

}
