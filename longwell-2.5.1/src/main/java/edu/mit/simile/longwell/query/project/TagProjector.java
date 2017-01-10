package edu.mit.simile.longwell.query.project;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;

import org.apache.log4j.Logger;
import org.openrdf.model.Literal;
import org.openrdf.model.URI;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;

import edu.mit.simile.longwell.FixedSetBuilder;
import edu.mit.simile.longwell.Profile;
import edu.mit.simile.longwell.TagModel;

public class TagProjector extends ProjectorBase {

    final static protected Logger s_logger = Logger.getLogger(TagProjector.class);

    final protected TagModel m_tagModel;
    final protected String m_locale;

    public TagProjector(Profile profile, String parameter, String locale) {
        super(profile);
        m_tagModel = (TagModel) profile.getStructuredModel(TagModel.class);
        m_locale = locale;
    }

    protected class TagProjection extends ProjectionBase {
        final protected Set m_objects;

        TagProjection(Set objects, String locale) {
            super(locale, m_profile.getCacheFactory());
            m_objects = objects;
        }

        public Set getObjects(Object value) throws QueryEvaluationException, RepositoryException {
            return super.getObjects(value == null ? null : (value instanceof String ? value : ((Literal) value)
                    .getLabel()));
        }

        public Set getValues() {
            return m_tagModel.getTagLabels();
        }

        protected Object internalGetValue(URI object) {
            try {
                Set tagLabels = m_tagModel.getObjectTagLabels(object);
                return tagLabels.size() > 0 ? tagLabels.iterator().next() : null;
            } catch (Exception e) {
                s_logger.error(e);
                return null;
            }
        }

        protected Set internalGetObjects(Object value) {
            try {
                Set objects = m_tagModel.getObjects((String) value);

                if (m_objects != null) {
                    FixedSetBuilder builder = new FixedSetBuilder();

                    builder.addAll(objects);
                    builder.retainAll(m_objects);

                    objects = builder.buildFixedSet();
                }

                return objects;
            } catch (Exception e) {
                s_logger.error(e);
                return null;
            }
        }

        protected Map internalGetObjectToValueMap() {
            // TODO What do we do here?
            return null;
        }

        protected Map internalGetValueToObjectsMap() {
            Set tagLabels = m_tagModel.getTagLabels();
            Map m = new HashMap();

            Iterator i = tagLabels.iterator();
            while (i.hasNext()) {
                String tagLabel = (String) i.next();
                try {
                    Set objects = m_tagModel.getObjects(tagLabel);
                    if (m_objects != null) {
                        FixedSetBuilder builder = new FixedSetBuilder();

                        builder.addAll(objects);
                        builder.retainAll(m_objects);

                        objects = builder.buildFixedSet();
                    }
                    m.put(tagLabel, objects);
                } catch (Exception e) {
                    s_logger.error(e);
                }
            }
            return m;
        }

        public Set getObjects() {
            return m_objects != null ? m_objects : m_profile.getSchemaModel().getAllItems();
        }

        public float getUniqueness() {
            return 0;
        }
    }

    protected IProjection internalProject() {
        return new TagProjection(null, m_locale);
    }

    protected IProjection internalProject(Set objects) {
        return new TagProjection(objects, m_locale);
    }

    public boolean isEfficientForRootProjection() {
        return false;
    }

    public float getUniqueness() {
        return 0;
    }

    public String getParameter() {
        return "";
    }

    public String getLabel(String locale) {
        ResourceBundle resources = ResourceBundle.getBundle(this.getClass().getName(), locale == null ? Locale
                .getDefault() : new Locale(locale));

        return resources.getString("Label");
    }

    protected void onAfterChange(RepositoryConnection c) {
        super.onAfterChange(c);
        try {
            if (m_queryManager.containsProperty(c, new URIImpl(TagModel.s_tag))) {
                m_resultCache.clear();
                m_rootProjections.remove(this);
            }
        } catch (Exception e) {
            s_logger.error(e);
        }
    }
}
