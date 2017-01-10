package edu.mit.simile.longwell.query.compare;

/**
 * Interface for an object which can compare two objects according to
 * some ordering.
 */
public interface IComparator {

    /**
     * Convert a generic value (such as an RDFNode) into some custom object that
     * later can be called on compare(). This is for performance.
     * 
     * @param value
     * @param locale
     * @return
     */
    public Object preprocess(Object value, String locale);

    /**
     * Compare the two given objects through the given projection.
     * 
     * @param projection
     * @param object1
     * @param object2
     * @return
     */
    public int compare(Object value1, Object value2, String locale, boolean ascending);

    /**
     * Answer how the parameter to comparator should be encoded.
     * 
     * @return
     */
    public String getParameter();

    /**
     * Answer the label for this comparator in the ascending direction in the
     * given locale (e.g., "Ascending", "Earliest first", "Youngest first").
     * 
     * @param locale
     * @return
     */
    public String getAscendingLabel(String locale);

    /**
     * Answer the label for this comparator in the descending direction in the
     * given locale (e.g., "Descending", "Latest first", "Oldest first").
     * 
     * @param locale
     * @return
     */
    public String getDescendingLabel(String locale);

    /**
     * Answer whether this comparator works in ascending order in the given
     * locale by default.
     * 
     * @param locale
     * @return
     */
    public boolean isAscendingByDefault(String locale);
}
