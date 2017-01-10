package edu.mit.simile.longwell.query.project;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;

import org.openrdf.model.Literal;
import org.openrdf.model.URI;
import org.openrdf.model.impl.LiteralImpl;
import org.openrdf.model.impl.URIImpl;

import edu.mit.simile.longwell.Profile;

public class SelfURIProjector extends ProjectorBase {

    public SelfURIProjector(Profile profile, String parameter, String locale) {
        super(profile);
    }

    protected IProjection internalProject() {
        return new SelfURIProjection(null, "");
    }

    protected IProjection internalProject(Set objects) {
        return new SelfURIProjection(objects, "");
    }

    public boolean isEfficientForRootProjection() {
        return true;
    }

    public float getUniqueness() {
        return 1;
    }

    public String getParameter() {
        return "";
    }

    public String getLabel(String locale) {
        return ResourceBundle.getBundle(SelfURIProjector.class.getName()).getString("Label");
    }

    class SelfURIProjection extends ProjectionBase {
        Set m_objects;

        SelfURIProjection(Set objects, String locale) {
            super(locale, m_profile.getCacheFactory());
            m_objects = objects;
        }

        protected Object internalGetValue(URI object) {
            return new LiteralImpl(object.toString());
        }

        protected Set internalGetObjects(Object value) {
            Set<URI> s = new HashSet<URI>();
            if (value instanceof String) {
                s.add(new URIImpl((String) value));
            } else if (value instanceof Literal) {
                s.add(new URIImpl(((Literal) value).getLabel()));
            } else if (value instanceof URI) {
                s.add((URI) value);
            }
            return s;
        }

        protected Map internalGetObjectToValueMap() {
            Map<URI,Literal> m = new HashMap<URI,Literal>();
            Iterator i = getObjects().iterator();
            while (i.hasNext()) {
                URI uri = (URI) i.next();
                m.put(uri, new LiteralImpl(uri.toString()));
            }
            return m;
        }

        protected Map internalGetValueToObjectsMap() {
            throw new InternalError("Not implemented");
        }

        public Set getObjects() {
            return m_objects != null ? m_objects : m_profile.getSchemaModel().getAllItems();
        }

        public float getUniqueness() {
            return 1;
        }
    }
}
