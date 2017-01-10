package edu.mit.simile.longwell.query.project;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;

import org.apache.log4j.Logger;
import org.openrdf.model.URI;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;

import edu.mit.simile.longwell.Profile;

public class PropertyProjector extends ProjectorBase {

    final static private Logger s_logger = Logger.getLogger(PropertyProjector.class);

    final protected URI m_property;
    final protected String m_locale;
    final protected boolean m_forward;

    protected Map m_objectToValue;

    public PropertyProjector(Profile profile, String parameter, String locale) {
        super(profile);

        if (parameter.startsWith("!")) {
            m_forward = false;
            parameter = parameter.substring(1);
        } else {
            m_forward = true;
        }

        int i = parameter.indexOf('@');
        if (i < 0) {
            m_property = new URIImpl(parameter);
            m_locale = locale != null ? locale : "";
        } else {
            m_property = new URIImpl(parameter.substring(0, i));
            m_locale = parameter.substring(i + 1);
        }
    }

    public boolean isEfficientForRootProjection() {
        return true;
    }

    public float getUniqueness() {
        if (s_logger.isDebugEnabled()) s_logger.debug("getUniqueness('" + m_property + "')");
        float uniqueness = m_profile.getSchemaModel().getLearnedProperty(m_property).getUniqueness();
        if (s_logger.isDebugEnabled()) s_logger.debug("getUniqueness('" + m_property + "') = " + uniqueness);
        return uniqueness; 
    }

    public String getParameter() {
        s_logger.debug("getParameter");
        String s = m_forward ? "" : "!";

        s += m_property.toString();
        if (m_locale != null && m_locale.length() > 0) {
            s += "@" + m_locale;
        }

        return s;
    }

    public String getLabel(String locale) {
        s_logger.debug("getLabel");
        String label = "";
        
        try {
            label = m_profile.getSchemaModel().getLabel(m_property, locale);
        } catch (RepositoryException e) {
            // ignore
        }

        if (m_forward) {
            return label;
        }

        ResourceBundle resources = ResourceBundle.getBundle(PropertyProjector.class.getName());

        return MessageFormat.format(resources.getString("BackwardPropertyLabelFormat"), new Object[] { label });
    }

    protected IProjection internalProject() {
        s_logger.debug("internalProject");
        return new PropertyProjection(m_profile, m_property, m_forward, m_locale, null) {

            protected Map internalGetObjectToValueMap() {
                if (m_objectToValue == null) {
                    m_objectToValue = new HashMap();
                    fillObjectToValueMap(m_objectToValue);
                }
                return m_objectToValue;
            }
        };
    }

    protected IProjection internalProject(Set objects) {
        s_logger.debug("internalProject");
        return new PropertyProjection(m_profile, m_property, m_forward, m_locale, objects) {

            protected Map internalGetObjectToValueMap() {
                if (m_objectToValue == null) {
                    m_objectToValue = new HashMap();
                    fillObjectToValueMap(m_objectToValue);
                }
                return m_objectToValue;
            }
        };
    }

    protected void onAfterChange(RepositoryConnection c) {
        s_logger.debug("onAfterChange");
        try {
            if (m_queryManager.containsProperty(c, m_property)) {
                m_resultCache.clear();
                if (m_objectToValue != null) {
                    m_objectToValue.clear();
                    m_objectToValue = null;
                }
                m_rootProjections.remove(this);
            }
        } catch (Exception e) {
            s_logger.error(e);
        }
    }
}
