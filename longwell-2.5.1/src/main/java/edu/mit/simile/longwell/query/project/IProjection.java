package edu.mit.simile.longwell.query.project;

import java.util.Map;
import java.util.Set;

import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.repository.RepositoryException;

public interface IProjection {

    /**
     * Answer the set of values in this project.
     * 
     * @return
     */
    public Set getValues() throws QueryEvaluationException, RepositoryException;

    /**
     * Answer the projected value of the given object.
     * 
     * @param object
     * @return
     */
    public Object getValue(URI object) throws QueryEvaluationException, RepositoryException ;

    /**
     * Answer all objects.
     * 
     * @return
     */
    public Set getObjects() throws QueryEvaluationException, RepositoryException;

    /**
     * Answer the number of all objects.
     * 
     * @return
     */
    public int countObjects() throws QueryEvaluationException, RepositoryException;

    /**
     * Answer the number of objects projected to the given value.
     * 
     * @param value
     * @return
     */
    public int countObjects(Object value) throws QueryEvaluationException, RepositoryException;

    /**
     * Answer the set of objects projected to the given value.
     * 
     * @param value
     * @return
     */
    public Set getObjects(Object value) throws QueryEvaluationException, RepositoryException;

    /**
     * Answer the number of objects projected to any of the given values.
     * 
     * @param values
     * @return
     */
    public int countObjectsWithValues(Set values) throws QueryEvaluationException, RepositoryException;

    /**
     * Answer the set of objects projected to any of the given values.
     * 
     * @param values
     * @return
     */
    public Set getObjectsWithValues(Set values) throws QueryEvaluationException, RepositoryException;

    /**
     * Answer a map from object to value for all the projected objects.
     * 
     * @return
     */
    public Map getObjectToValueMap() throws QueryEvaluationException, RepositoryException;

    /**
     * Answer a map from value to set of objects for all the projected objects.
     * 
     * @return
     */
    public Map getValueToObjectsMap() throws QueryEvaluationException, RepositoryException;

    /**
     * Answer the locale in which the projection has been made. Can be null.
     * 
     * @return
     */
    public String getLocale() throws QueryEvaluationException, RepositoryException;

    /**
     * Answer a number from 0 to 1 indicating how unique the projected values
     * are. Smaller result implies less unique.
     * 
     * @return
     */
    public float getUniqueness() throws QueryEvaluationException, RepositoryException;
    
    /**
     * Parse the literal value and return the right datatype object for it
     */
    public Object nodeToValue(Value v);
}
