package edu.mit.simile.longwell.query;

import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.commons.lang.StringUtils;

import edu.mit.simile.longwell.Utilities;

public class Query {

    protected List<Restriction> m_restrictions;
    protected List<Order> m_orders;
    protected SortedSet<String> m_explicitFacets;
    protected List<QueryTerm> m_others;
    protected int m_restrictorID;

    final static public String s_restrictionPrefix = "-";
    final static public String s_orderPrefix = "~";
    final static public String s_explicitFacetPrefix = "+";

    protected Query() {
        m_restrictions = new ArrayList<Restriction>();
        m_orders = new ArrayList<Order>();
        m_explicitFacets = new TreeSet<String>();
        m_others = new ArrayList<QueryTerm>();
    }

    public Query(Query q) {
        m_restrictions = new ArrayList<Restriction>(q.m_restrictions);
        m_orders = new ArrayList<Order>(q.m_orders);
        m_explicitFacets = new TreeSet<String>(q.m_explicitFacets);
        m_others = new ArrayList<QueryTerm>(q.m_others);
    }

    public Query(String query) {
        this();

        parse(query);
    }

    public Query(URL url) {
        this();

        parse(url.getQuery());
    }

    public Query dup() {
        return new Query(this);
    }

    public void setParameter(String name, String value) {
        removeParameter(name);

        m_others.add(new QueryTerm(name, value));
    }

    public void removeParameter(String name) {
        Iterator i = m_others.iterator();
        while (i.hasNext()) {
            QueryTerm t = (QueryTerm) i.next();

            if (t.getName().equals(name)) {
                i.remove();
            }
        }
    }

    public void removeOtherParameters() {
        m_others.clear();
    }

    public String getFirstParamValue(String name) {
        Iterator i = m_others.iterator();
        while (i.hasNext()) {
            QueryTerm t = (QueryTerm) i.next();

            if (t.getName().equals(name)) {
                return t.getValue();
            }
        }

        return null;
    }

    public String getFirstParamValue2(String name) {
        Iterator i = m_others.iterator();
        while (i.hasNext()) {
            QueryTerm t = (QueryTerm) i.next();

            if (t.getName().equals(name)) {
                return t.getValue();
            }
        }

        return "";
    }

    public List getRestrictions() {
        return m_restrictions;
    }

    public void clearRestrictions() {
        m_restrictions.clear();
    }

    public void addRestriction(String projectorName, String projectorParameter, String bucketerName,
            String bucketerParameter) {
        addRestriction(projectorName, projectorParameter, bucketerName, bucketerParameter, null);
    }

    public void addRestriction(String projectorName, String projectorParameter, String bucketerName,
            String bucketerParameter, String escapedURLString) {
        m_restrictions.add(new Restriction(m_restrictorID++, projectorName, projectorParameter, bucketerName,
                bucketerParameter, escapedURLString));
    }

    public void removeRestriction(int id) {
        Iterator i = m_restrictions.iterator();
        while (i.hasNext()) {
            Restriction r = (Restriction) i.next();

            if (r.m_id == id) {
                i.remove();
            }
        }
    }

    public void addExplicitFacet(String propertyURI) {
        m_explicitFacets.add(propertyURI);
    }

    public void removeExplicitFacet(String propertyURI) {
        m_explicitFacets.remove(propertyURI);
    }

    public void clearExplicitFacets() {
        m_explicitFacets.clear();
    }

    public SortedSet getExplicitFacets() {
        return m_explicitFacets;
    }

    public List<Order> getOrders() {
        return m_orders;
    }

    public void clearOrders() {
        m_orders.clear();
    }

    public void addOrder(String projectorName, String projectorParameter, String comparatorName,
            String comparatorParameter, boolean ascending) {
        addOrder(projectorName, projectorParameter, comparatorName, comparatorParameter, ascending, null);
    }

    public void addOrder(String projectorName, String projectorParameter, String comparatorName,
            String comparatorParameter, boolean ascending, String escapedURLString) {
        m_orders.add(new Order(projectorName, projectorParameter, comparatorName, comparatorParameter, ascending,
                escapedURLString));
    }

    public List getOthers() {
        return m_others;
    }

    final static public char s_separator = ';';

    final static public String s_encoded_separator = Utilities.encode(";");

    public String toString() {
        StringBuffer b = new StringBuffer();
        b.append("Query:");

        if (!m_restrictions.isEmpty()) {
            b.append("; restrictions:");
            Iterator i = m_restrictions.iterator();
            while (i.hasNext()) {
                Restriction res = (Restriction) i.next();
                b.append(" ");
                b.append(res.toString());
            }
        }

        if (!m_explicitFacets.isEmpty()) {
            b.append("; explicit facets:");
            Iterator i = m_explicitFacets.iterator();
            while (i.hasNext()) {
                String facet = (String) i.next();
                b.append(" ");
                b.append(facet);
            }
        }

        if (!m_orders.isEmpty()) {
            b.append("; orders:");
            Iterator i = m_orders.iterator();
            while (i.hasNext()) {
                Order order = (Order) i.next();
                b.append(" ");
                b.append(order.toString());
            }
        }

        if (!m_others.isEmpty()) {
            Iterator i = m_others.iterator();
            while (i.hasNext()) {
                QueryTerm term = (QueryTerm) i.next();
                b.append(" ");
                b.append(term.toString());
            }
        }

        b.append(".");
        return b.toString();
    }

    public String toURLQueryString() {
        StringBuffer b = new StringBuffer();
        populateURLQueryString(b);
        return b.toString();
    }

    public void populateURLQueryString(StringBuffer s) {
        Iterator i = m_others.iterator();
        while (i.hasNext()) {
            QueryTerm t = (QueryTerm) i.next();
            s.append(Utilities.encode(t.getName()));
            s.append('=');
            s.append(Utilities.encode(t.getValue()));
            s.append('&');
        }

        i = m_restrictions.iterator();
        while (i.hasNext()) {
            Restriction r = (Restriction) i.next();
            s.append(s_restrictionPrefix);
            s.append('=');
            s.append(r.m_escapedURLString);
            s.append('&');
        }

        i = m_orders.iterator();
        while (i.hasNext()) {
            Order o = (Order) i.next();
            s.append(s_orderPrefix);
            s.append('=');
            s.append(o.m_escapedURLString);
            s.append('&');
        }

        i = m_explicitFacets.iterator();
        while (i.hasNext()) {
            s.append(Utilities.encode(s_explicitFacetPrefix));
            s.append('=');
            s.append(Utilities.encode((String) i.next()));
            s.append('&');
        }
    }

    protected void parse(String query) {
        if (query == null) {
            return;
        }

        String[] params = StringUtils.splitPreserveAllTokens(query, '&');
        for (int i = 0; i < params.length; i++) {
            String param = params[i];
            int equalIndex = param.indexOf('=');

            if (equalIndex >= 0) {
                String rawName = param.substring(0, equalIndex);
                String rawValue = param.substring(equalIndex + 1);

                String name = Utilities.decode(rawName);
                String value = Utilities.decode(rawValue);

                if (name.startsWith(s_restrictionPrefix)) {
                    parseRestriction(value, rawValue);
                } else if (name.startsWith(s_orderPrefix)) {
                    parseOrder(value, rawValue);
                } else if (name.startsWith(s_explicitFacetPrefix)) {
                    parseExplicitFacet(value);
                } else {
                    m_others.add(new QueryTerm(name, value));
                }
            }
        }
    }

    protected void parseRestriction(String s, String raw) {
        String[] ses = StringUtils.splitPreserveAllTokens(s, s_separator);

        String projectorName = unescapeSeparator(unescapeLongwell(ses[0]));
        String projectorParameter = unescapeSeparator(ses.length > 1 ? ses[1] : "");
        String bucketerName = unescapeSeparator(ses.length > 2 ? unescapeLongwell(ses[2]) : "");
        String bucketerParameter = unescapeSeparator(ses.length > 3 ? ses[3] : "");

        addRestriction(projectorName, projectorParameter, bucketerName, bucketerParameter, raw);
    }

    protected void parseOrder(String s, String raw) {
        String[] ses = StringUtils.splitPreserveAllTokens(s, s_separator);

        String projectorName = unescapeSeparator(unescapeLongwell(ses[0]));
        String projectorParameter = unescapeSeparator(ses.length > 1 ? ses[1] : "");
        String comparatorName = unescapeSeparator(ses.length > 2 ? unescapeLongwell(ses[2]) : "");
        String comparatorParameter = unescapeSeparator(ses.length > 3 ? ses[3] : "");
        boolean ascending = ses.length > 4 ? !ses[4].equals("d") : true;

        m_orders.add(new Order(projectorName, projectorParameter, comparatorName, comparatorParameter, ascending, raw));
    }

    protected void parseExplicitFacet(String s) {
        addExplicitFacet(s);
    }

    static String escapeSeparator(String s) {
        return s == null ? "" : Utilities.escape(s, s_separator, '`', 's');
    }

    static String unescapeSeparator(String s) {
        return s == null ? "" : Utilities.unescape(s, s_separator, '`', 's');
    }

    static public String escapeLongwell(String s) {
        return s == null ? null : StringUtils.replace(s, "edu.mit.simile.longwell.query", "@lwq");
    }

    static public String unescapeLongwell(String s) {
        return s == null ? null : StringUtils.replace(s, "@lwq", "edu.mit.simile.longwell.query");
    }
}
