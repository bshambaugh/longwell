package edu.mit.simile.longwell.query.project;

import java.util.Iterator;
import java.util.Set;

import org.openrdf.model.Literal;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;

import edu.mit.simile.longwell.CoordinateWindow;
import edu.mit.simile.longwell.Coordinates;
import edu.mit.simile.longwell.FixedSetBuilder;
import edu.mit.simile.longwell.Profile;
import edu.mit.simile.longwell.Utilities;

public class CoordinateWindowProjector extends PropertyProjector {

    public class CoordinateWindowProjection extends PropertyProjection {
		
        public CoordinateWindowProjection(Set objects) {
			super(CoordinateWindowProjector.this.m_profile,
					CoordinateWindowProjector.this.m_property,
					CoordinateWindowProjector.this.m_forward,
					CoordinateWindowProjector.this.m_locale,
					objects);
		}
		
		public Coordinates getCoordinates(URI object) throws QueryEvaluationException, RepositoryException {
			return (Coordinates) getValue(object);
		}
		
		protected Object internalGetValue(URI object) throws QueryEvaluationException, RepositoryException {
            Object o = null;
            RepositoryConnection c = null;
            try {
                c = this.m_profile.getRepository().getConnection();
                o = nodeToValue(this.m_profile.getQueryManager().getObjectOfProperty(c, object, this.m_property));
            } finally {
                if (c != null) c.close();
            }
            return o;
		}
		
		protected Set internalGetObjects(Object value) throws QueryEvaluationException, RepositoryException {
			Set allObjects = getObjects();
			FixedSetBuilder builder = new FixedSetBuilder();
			Coordinates c = (Coordinates) value;
			
			Iterator i = allObjects.iterator();
			while (i.hasNext()) {
				URI r = (URI) i.next();
				if (coordinatesEqual(c, (Coordinates) getValue(r))) {
					builder.add(r);
				}
			}
			return builder.buildFixedSet();
		}
		
		public Object nodeToValue(Value v) {
			return nodeToCoordinates(v);
		}
		
		// TODO(DH) how to model the geographic coordinate system?
		public Set getObjects(CoordinateWindow window) throws QueryEvaluationException, RepositoryException {
			Set allObjects = getObjects();
			FixedSetBuilder builder = new FixedSetBuilder();

			Iterator i = allObjects.iterator();
			while (i.hasNext()) {
				URI r = (URI) i.next();
				if (window.contains((Coordinates) getValue(r))) {
					builder.add(r);
				}
			}

			return builder.buildFixedSet();
		}

		
		public int countObjects(CoordinateWindow window) throws QueryEvaluationException, RepositoryException {
			Set allObjects = getObjects();
			int count = 0;

			Iterator i = allObjects.iterator();
			while (i.hasNext()) {
				URI r = (URI) i.next();
				if (window.contains((Coordinates) getValue(r))) {
					count++;
				}
			}

			return count;
		}
	}
	
	public CoordinateWindowProjector(Profile profile, String parameter, String locale) {
		super(profile, parameter, locale);
	}

    public boolean isEfficientForRootProjection() {
		return false;
	}
	
	protected IProjection internalProject() {
		return new CoordinateWindowProjection(null);
	}
	
	protected IProjection internalProject(Set objects) {
		return new CoordinateWindowProjection(objects);
	}
	
	final protected boolean coordinatesEqual(Coordinates c1, Coordinates c2) {
		if (c1 == null) {
			return c2 == null;
		}
		return c1.equals(c2);
	}
	
	final protected Coordinates nodeToCoordinates(Value v) {
		if (v instanceof Literal) {
			return Utilities.parseCoordinates(((Literal) v).getLabel());
		}
		return null;
	}
}
