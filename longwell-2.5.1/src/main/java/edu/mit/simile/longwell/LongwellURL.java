package edu.mit.simile.longwell;

import org.openrdf.model.Literal;
import org.openrdf.model.URI;
import org.openrdf.model.Value;

import edu.mit.simile.longwell.query.Query;
import edu.mit.simile.longwell.query.bucket.DistinctValueBucketer;
import edu.mit.simile.longwell.query.project.PropertyProjector;
import edu.mit.simile.longwell.schema.LearnedProperty;
import edu.mit.simile.longwell.schema.SchemaModel;

public class LongwellURL {

    final public String m_contextPath;
    final public String m_profileID;
    final public Query m_query;

    public LongwellURL(String contextPath, String profileID, Query query) {
        m_contextPath = contextPath;
        m_profileID = Utilities.encode(profileID);
        m_query = new Query(query);
    }

    public LongwellURL(LongwellURL furl) {
        this(furl.m_contextPath, furl.m_profileID, furl.m_query);
    }

    public LongwellURL(Message msg) {
        this(msg.getContextPath(), msg.m_profileID, msg.m_query);
    }

    public LongwellURL dup() {
        return new LongwellURL(this);
    }

    public LongwellURL changeCommand(String command) {
        LongwellURL url = new LongwellURL(m_contextPath, m_profileID, m_query);

        url.getQuery().setParameter("command", command);

        return url;
    }

    public LongwellURL changeCommandProfile(String command, String profileID) {
        LongwellURL url = new LongwellURL(m_contextPath, profileID, m_query);

        url.getQuery().setParameter("command", command);

        return url;
    }

    public LongwellURL changeCommandQuery(String command, String query) {
        LongwellURL url = new LongwellURL(m_contextPath, m_profileID, new Query(query));

        url.getQuery().setParameter("command", command);

        return url;
    }

    public LongwellURL changeQuery(Query query) {
        LongwellURL url = new LongwellURL(m_contextPath, m_profileID, query);

        String command = getCommand();
        if (command != null) {
            url.getQuery().setParameter("command", command);
        }

        return url;
    }

    public LongwellURL changeQuery(String query) {
        LongwellURL url = new LongwellURL(m_contextPath, m_profileID, new Query(query));

        String command = getCommand();
        if (command != null) {
            url.getQuery().setParameter("command", command);
        }

        return url;
    }

    public String getContextPath() {
        return m_contextPath;
    }

    public String getCommand() {
        return m_query.getFirstParamValue("command");
    }

    public String getProfileID() {
        return m_profileID != null ? m_profileID : LongwellServlet.getLongwellService().getDefaultProfile().getID();
    }

    public Profile getProfile() {
        return LongwellServlet.getLongwellService().getProfile(getProfileID());
    }

    public Query getQuery() {
        return m_query;
    }

    public String toURLString() {
        StringBuffer s = new StringBuffer(m_contextPath);

        if (m_profileID != null) {
            s.append('/');
            s.append(m_profileID);
        }

        s.append('?');
        m_query.populateURLQueryString(s);

        return s.toString();
    }

    public String toURLStringEncoded() {
        return Utilities.encode(toURLString());
    }

    public String getNarrowingURL(URI property, Value value, boolean clearOtherRestrictions, boolean forward) {
        Profile profile = getProfile();
        SchemaModel schemaModel = profile.getSchemaModel();

        if (forward) {
            LearnedProperty learnedProperty = schemaModel.getLearnedProperty(property);

            if (learnedProperty != null) {
                if (learnedProperty.getTypeConfidence(LearnedProperty.s_type_integer) > 0.5) {
                    return "";
                } else if (learnedProperty.getTypeConfidence(LearnedProperty.s_type_dateTime) > 0.5) {
                    return "";
                }

                String projectorName = PropertyProjector.class.getName();
                String bucketerName = DistinctValueBucketer.class.getName();
                String bucketerParameter = value instanceof Literal ? ("l" + ((Literal) value).getLabel())
                        : ("r" + ((URI) value).toString());

                LongwellURL longwellURL = clearOtherRestrictions ? new LongwellURL(this).changeCommandQuery("browse", "")
                        : new LongwellURL(this).changeCommand("browse");

                longwellURL.getQuery().addRestriction(projectorName, property.toString(), bucketerName, bucketerParameter,
                        null);

                return longwellURL.toURLString();
            }

            return "";
        }

        String projectorName = PropertyProjector.class.getName();
        String bucketerName = DistinctValueBucketer.class.getName();
        String bucketerParameter = "r" + ((URI) value).toString();

        LongwellURL longwellURL = clearOtherRestrictions ? new LongwellURL(this).changeCommandQuery("browse", "")
                : new LongwellURL(this).changeCommand("browse");

        longwellURL.getQuery().addRestriction(projectorName, "!" + property.toString(), bucketerName, bucketerParameter,
                null);

        return longwellURL.toURLString();
    }
}
