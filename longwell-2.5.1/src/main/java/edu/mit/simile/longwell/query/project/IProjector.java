package edu.mit.simile.longwell.query.project;

import java.util.Set;

import edu.mit.simile.longwell.IProfileListener;

/**
 * Integer for an object which can convert a set of objects to a
 * specific type.
 */
public interface IProjector extends IProfileListener {

    /**
     * Project whatever RDF model this projector is tied to.
     * 
     * @return
     */
    public IProjection project();

    /**
     * Project a given set of objects.
     * 
     * @param objects
     * @return
     */
    public IProjection project(Set objects);

    public boolean isEfficientForRootProjection();

    public float getUniqueness();

    public String getParameter();

    public String getLabel(String locale);
}
