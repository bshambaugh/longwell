package edu.mit.simile.longwell.query.project;

import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.openrdf.model.Literal;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.repository.RepositoryException;

import edu.mit.simile.longwell.Profile;

public class URIProjector extends PropertyProjector {
    
    public class URIProjection extends PropertyProjection {

        public URIProjection(Set objects) {
            super(URIProjector.this.m_profile, URIProjector.this.m_property, URIProjector.this.m_forward,
                    URIProjector.this.m_locale, objects);
        }

        protected Set internalGetObjects(Object value) throws QueryEvaluationException, RepositoryException {
            Set allObjects = getObjects();
            Set<URI> objects = new HashSet<URI>();
            URI uri = (URI) value;

            Iterator i = allObjects.iterator();
            while (i.hasNext()) {
                URI r = (URI) i.next();
                if (urisEqual(uri, (URI) getValue(r))) {
                    objects.add(r);
                }
            }
            return objects;
        }

        public Object nodeToValue(Value v) {
            return nodeToURI(v);
        }

        /**
         * Return the projected value of the given object as a URI.
         */
        public java.net.URI getURI(URI object) throws QueryEvaluationException, RepositoryException {
            return (java.net.URI) getValue(object);
        }
    }

    public URIProjector(Profile profile, String parameter, String locale) {
        super(profile, parameter, locale);
    }

    public boolean isEfficientForRootProjection() {
        return false;
    }

    protected IProjection internalProject(Set objects) {
        return new URIProjection(objects);
    }

    final protected boolean urisEqual(URI uri1, URI uri2) {
        if (uri1 == null) {
            return uri2 == null;
        }
        return uri1.equals(uri2);
    }

    final protected java.net.URI nodeToURI(Value v) {
        String s = null;

        if (v instanceof Literal) {
            s = ((Literal) v).getLabel();
        } else if (v instanceof URI) {
            s = ((URI) v).toString();
        }

        if (s != null) {
            try {
                return new java.net.URI(s);
            } catch (URISyntaxException e) {
                // Ignore
            }
        }
        return null;
    }
}
