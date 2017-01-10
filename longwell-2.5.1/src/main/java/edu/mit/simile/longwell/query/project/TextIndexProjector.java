package edu.mit.simile.longwell.query.project;

import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;

import org.apache.log4j.Logger;
import org.openrdf.model.URI;

import edu.mit.simile.longwell.Cache;
import edu.mit.simile.longwell.FixedSetBuilder;
import edu.mit.simile.longwell.Profile;
import edu.mit.simile.longwell.TextIndexModel;

public class TextIndexProjector extends ProjectorBase {
    
    final static protected Logger s_logger = Logger.getLogger(TextIndexProjector.class);

    final protected String m_parameter;

    final protected String m_locale;

    public TextIndexProjector(Profile profile, String parameter, String locale) {
        super(profile);
        m_parameter = parameter;
        m_locale = locale;
    }

    protected IProjection internalProject() {
        return new TextIndexProjection(m_locale, null);
    }

    protected IProjection internalProject(Set objects) {
        return new TextIndexProjection(m_locale, objects);
    }

    public boolean isEfficientForRootProjection() {
        return true;
    }

    public float getUniqueness() {
        return 1;
    }

    public String getParameter() {
        return m_parameter;
    }

    public String getLabel(String locale) {
        if (locale == null) {
            locale = m_locale;
        }

        ResourceBundle resources = ResourceBundle.getBundle(this.getClass().getName(), locale == null ? Locale
                .getDefault() : new Locale(locale));

        return resources.getString("TextSearch");
    }

    protected TextIndexModel getTextIndexModel() {
        return (TextIndexModel) m_profile.getStructuredModel(TextIndexModel.class);
    }

    public class TextIndexProjection extends ProjectionBase {

        final protected Set m_objects;

        final protected Cache m_results = m_profile.getCacheFactory().getCache("results", false);

        protected TextIndexProjection(String locale, Set objects) {
            super(locale, m_profile.getCacheFactory());
            m_objects = objects;
        }

        protected Object internalGetValue(URI object) {
            throw new InternalError("Not implemented");
        }

        protected Set internalGetObjects(Object value) {
            throw new InternalError("Not implemented");
        }

        protected Map internalGetObjectToValueMap() {
            throw new InternalError("Not implemented");
        }

        protected Map internalGetValueToObjectsMap() {
            throw new InternalError("Not implemented");
        }

        public Set search(String text) {
            Set results = (Set) m_results.get(text);
            if (results == null) {
                results = internalSearch(text);
                m_results.put(text, results);
            }
            return results;
        }

        public Set getObjects() {
            return m_objects != null ? m_objects : m_profile.getSchemaModel().getAllItems();
        }

        public float getUniqueness() {
            return 1;
        }

        protected Set internalSearch(String text) {
            TextIndexModel tim = getTextIndexModel();

            if (tim != null) {
                Set objects = tim.search(text);
                if (m_objects != null) {
                    FixedSetBuilder builder = new FixedSetBuilder();
    
                    builder.addAll(m_objects);
                    builder.retainAll(objects);
    
                    objects = builder.buildFixedSet();
                }
                return objects;
            } else {
                return new HashSet();
            }
        }
    }
}
