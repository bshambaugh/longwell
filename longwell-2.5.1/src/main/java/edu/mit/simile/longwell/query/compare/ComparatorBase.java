package edu.mit.simile.longwell.query.compare;

import java.util.Locale;
import java.util.ResourceBundle;

import edu.mit.simile.longwell.Profile;

public abstract class ComparatorBase implements IComparator {

    final protected Profile m_profile;
    final protected String m_parameter;

    protected ComparatorBase(Profile profile, String parameter) {
        m_profile = profile;
        m_parameter = parameter;
    }

    public int compare(Object value1, Object value2, String locale, boolean ascending) {
        return (ascending ? 1 : -1) * internalCompareAscending(value1, value2, locale);
    }

    public String getParameter() {
        return m_parameter;
    }

    public String getAscendingLabel(String locale) {
        return getResources(locale).getString("AscendingLabel");
    }

    public String getDescendingLabel(String locale) {
        return getResources(locale).getString("DescendingLabel");
    }

    public boolean isAscendingByDefault(String locale) {
        return "true".equals(getResources(locale).getString("AscendingByDefault"));
    }

    abstract protected int internalCompareAscending(Object v1, Object v2, String locale);

    protected ResourceBundle getResources(String locale) {
        return ResourceBundle.getBundle(this.getClass().getName(), getLocale(locale));
    }

    protected Locale getLocale(String locale) {
        return locale == null ? Locale.getDefault() : new Locale(locale);
    }
}
