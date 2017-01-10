package edu.mit.simile.longwell;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.openrdf.model.BNode;
import org.openrdf.model.Literal;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.query.BindingSet;
import org.openrdf.query.GraphQuery;
import org.openrdf.query.GraphQueryResult;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.query.TupleQueryResultHandlerBase;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.RepositoryResult;

import edu.mit.simile.longwell.query.project.IProjection;

public class QueryManager {

    final static private Logger s_logger = Logger.getLogger(QueryManager.class);

    // -=-------- API queries -------------------------------
    
    public Value getObjectOfProperty(RepositoryConnection c, Resource subject, URI predicate) throws QueryEvaluationException, RepositoryException {
        getObjectOfProperty++;
        Value out = null;

        RepositoryResult<Statement> r = c.getStatements(subject, predicate, null, true);
        while (r.hasNext()) {
            Statement s = r.next();
            out = s.getObject();
            break;
        }

        return out;
    }

    public String getStringOfProperty(RepositoryConnection c, Resource subject, URI predicate) throws QueryEvaluationException, RepositoryException {
        getStringOfProperty++;
        Value v = getObjectOfProperty(c, subject, predicate);
        return v instanceof Literal ? ((Literal) v).getLabel() : null;
    }

    public Set<Value> listObjectsOfProperty(RepositoryConnection c, Resource subject, URI predicate) throws QueryEvaluationException, RepositoryException {
        listObjectsOfPropertySP++;
        FixedSetBuilder builder = new FixedSetBuilder();
        RepositoryResult<Statement> r = c.getStatements(subject, predicate, null, true);

        while (r.hasNext()) {
            Statement s = r.next();
            builder.add(Utilities.dupValue(s.getObject()));
        }

        return builder.buildFixedSet();
    }

    public Set<Value> listObjectsOfProperty(RepositoryConnection c, URI predicate) throws QueryEvaluationException, RepositoryException {
        listObjectsOfPropertyP++;

        FixedSetBuilder builder = new FixedSetBuilder();
        RepositoryResult<Statement> r = c.getStatements(null, predicate, null, true);

        while (r.hasNext()) {
            Statement s = r.next();
            builder.add(Utilities.dupValue(s.getObject()));
        }

        return builder.buildFixedSet();
    }

    public Set<Value> listSubjectsOfProperty(RepositoryConnection c, URI predicate) throws QueryEvaluationException, RepositoryException {
        listSubjectsOfPropertyP++;
        RepositoryResult<Statement> r = c.getStatements(null, predicate, null, true);

        FixedSetBuilder builder = new FixedSetBuilder();
        
        while (r.hasNext()) {
            Statement s = r.next();
            builder.add(Utilities.dupValue(s.getSubject()));
        }
        
        return builder.buildFixedSet();
    }

    public Map<URI, Set<Value>> mapForwardProperties(RepositoryConnection c, URI subject)
            throws QueryEvaluationException, RepositoryException {
        mapForwardProperties++;
        RepositoryResult<Statement> r = c.getStatements(subject, null, null, true);
        
        Map<URI, Set<Value>> map = new HashMap<URI, Set<Value>>();

        while (r.hasNext()) {
            Statement s = r.next();
            URI p = Utilities.dupURI((URI) s.getPredicate());
            Value o = Utilities.dupValue(s.getObject());

            Set<Value> set = map.get(p);
            if (set == null) {
                set = new HashSet<Value>();
                map.put(p, set);
            }
            set.add(o);
        }

        return map;
    }

    public Set<Value> listForwardProperties(RepositoryConnection c, URI subject) throws QueryEvaluationException,
            RepositoryException {
        listForwardProperties++;
        RepositoryResult<Statement> r = c.getStatements(subject, null, null, true);

        FixedSetBuilder builder = new FixedSetBuilder();

        while (r.hasNext()) {
            Statement s = r.next();
            builder.add(Utilities.dupValue(s.getPredicate()));
        }

        return builder.buildFixedSet();
    }

    public Set<Value> listBackwardProperties(RepositoryConnection c, URI subject) throws QueryEvaluationException,
            RepositoryException {
        listBackwardProperties++;
        RepositoryResult<Statement> r = c.getStatements(null, null, subject, true);

        FixedSetBuilder builder = new FixedSetBuilder();

        while (r.hasNext()) {
            Statement s = r.next();
            builder.add(Utilities.dupValue(s.getPredicate()));
        }

        return builder.buildFixedSet();
    }

    public void fillObjectToValueMap(RepositoryConnection c, URI property, boolean forward, Map m, IProjection projection) throws QueryEvaluationException, RepositoryException {
        fillObjectToValueMap++;
        RepositoryResult<Statement> r = c.getStatements(null, property, null, true);

        if (forward) {
            while (r.hasNext()) {
                Statement s = r.next();
                m.put(Utilities.dupValue(s.getSubject()), projection.nodeToValue(Utilities.dupValue(s.getObject())));
            }
        } else {
            while (r.hasNext()) {
                Statement s = r.next();
                m.put(Utilities.dupValue(s.getObject()), projection.nodeToValue(Utilities.dupValue(s.getSubject())));
            }
        }
    }
    
    public void fillValueToObjectMap(RepositoryConnection c, URI property, boolean forward, Map m, Set objects) throws QueryEvaluationException, RepositoryException {
        fillValueToObjectMap++;
        RepositoryResult<Statement> r = c.getStatements(null, property, null, true);

        while (r.hasNext()) {
            Statement s = r.next();
            Value object = Utilities.dupValue((forward) ? s.getSubject() : s.getObject());
            Value value = Utilities.dupValue((forward) ? s.getObject() : s.getSubject());
            
            if (objects == null || objects.contains(object)) {
                Set objs = (Set) m.get(value);
                if (objs == null) {
                    objs = new HashSet();
                    m.put(value, objs);
                }
                objs.add(object);
            }
        }
    }
    
    public boolean containsSubject(RepositoryConnection c, URI subject) {
        containsSubject++;
        return contains(c, subject, null, null);
    }

    public boolean containsSubjectProperty(RepositoryConnection c, URI subject, URI property) {
        containsSubjectProperty++;
        return contains(c, subject, property, null);
    }

    public boolean containsObject(RepositoryConnection c, Value object) {
        containsObject++;
        return contains(c, null, null, object);
    }

    public boolean containsPropertyObject(RepositoryConnection c, URI property, Value object) {
        containsPropertyObject++;
        return contains(c, null, property, object);
    }

    public boolean containsProperty(RepositoryConnection c, URI property) {
        containsProperty++;
        return contains(c, null, property, null);
    }

    public boolean containsStatement(RepositoryConnection c, URI subject, URI predicate, Value object) {
        containsStatement++;
        return contains(c, subject, predicate, object);
    }

    public long countStatementsWithPredicate(RepositoryConnection c, URI predicate) {
        countStatementsWithPredicate++;
        return count(c, null, predicate, null);
    }
    
    // ---------- SELECT queries ----------------------------
    
    public Set<Value> listSubjects(RepositoryConnection c) throws QueryEvaluationException, RepositoryException {
        listSubjects++;
        TupleQueryResult results = performSeRQLTupleQuery(c, "SELECT DISTINCT s FROM {s} p {}");

        FixedSetBuilder builder = new FixedSetBuilder();

        while (results.hasNext()) {
            BindingSet r = results.next();
            Value value = r.getValue("s");
            builder.add(Utilities.dupValue(value));
        }

        return builder.buildFixedSet();
    }

    public Set<Value> listProperties(RepositoryConnection c) throws QueryEvaluationException, RepositoryException {
        listProperties++;
        TupleQueryResult results = performSeRQLTupleQuery(c, "SELECT DISTINCT p FROM {} p {}");

        FixedSetBuilder builder = new FixedSetBuilder();

        while (results.hasNext()) {
            BindingSet r = results.next();
            builder.add(Utilities.dupValue(r.getValue("p")));
        }

        return builder.buildFixedSet();
    }

    public Set<Value[]> listSubjectObjectPairsOfProperty(RepositoryConnection c, URI predicate)
    throws QueryEvaluationException, RepositoryException {
        listSubjectObjectPairsOfProperty++;
        TupleQueryResult results = performSeRQLTupleQuery(c, "SELECT DISTINCT s, o FROM {s} " + toQueryTerm(predicate) + " {o}");
        
        FixedSetBuilder builder = new FixedSetBuilder(new Comparator() {
            public boolean equals(Object obj) {
                return false;
            }
        
            public int compare(Object o1, Object o2) {
                if (o1 == null) {
                    return o2 == null ? 0 : -1;
                } else if (o2 == null) {
                    return 1;
                } else {
                    Value[] p1 = (Value[]) o1;
                    Value[] p2 = (Value[]) o2;
                    int i = compareValues(p1[0], (p2[0]));
                    if (i == 0) {
                        i = compareValues(p1[1], (p2[1]));
                    }
                    return i;
                }
            }
        });
        
        while (results.hasNext()) {
            BindingSet r = results.next();
            builder.add(new Value[] { Utilities.dupValue(r.getValue("s")), Utilities.dupValue(r.getValue("o")) });
        }
        
        return builder.buildFixedSet();
    }

    
    public long countSubjectObjectPairsOfProperty(RepositoryConnection c, URI predicate) {
        countSubjectObjectPairsOfProperty++;
        return countSeRQLTupleQuery(c, "SELECT DISTINCT s, o FROM {s} " + toQueryTerm(predicate) + " {o}");
    }

    public Value getSubjectOfProperty(RepositoryConnection c, URI predicate, Value object)
            throws QueryEvaluationException, RepositoryException {
        getSubjectOfProperty++;
        Value out = null;
        String query = "";
        if (object instanceof Literal) {
            query = "SELECT DISTINCT s FROM {s} " + toQueryTerm(predicate) + " {o} " + "WHERE label(o) = "
                    + toQueryTerm(object);
        } else {
            query = "SELECT DISTINCT s FROM {s} " + toQueryTerm(predicate) + " {" + toQueryTerm(object) + "}";
        }
        TupleQueryResult results = performSeRQLTupleQuery(c, query);

        while (results.hasNext()) {
            BindingSet r = results.next();
            out = Utilities.dupValue(r.getValue("s"));
        }

        return out;
    }

    public Set<Value> listSubjectsOfProperty(RepositoryConnection c, URI predicate, Value object)
            throws QueryEvaluationException, RepositoryException {
        listSubjectsOfPropertyPO++;
        String query = "";
        if (object instanceof Literal) {
            query = "SELECT DISTINCT s FROM {s} " + toQueryTerm(predicate) + " {o} " + "WHERE label(o) = " + toQueryTerm(object);
        } else {
            query = "SELECT DISTINCT s FROM {s} " + toQueryTerm(predicate) + " {" + toQueryTerm(object) + "}";
        }
        TupleQueryResult results = performSeRQLTupleQuery(c, query);

        FixedSetBuilder builder = new FixedSetBuilder();

        while (results.hasNext()) {
            BindingSet r = results.next();
            builder.add(Utilities.dupValue(r.getValue("s")));
        }

        return builder.buildFixedSet();
    }

    public Map<URI, Set<URI>> mapBackwardProperties(RepositoryConnection c, Value object) throws QueryEvaluationException,
            RepositoryException {
        mapBackwardProperties++;
        String query = "";
        if (object instanceof Literal) {
            query = "SELECT DISTINCT s, p FROM {s} p {o} " + "WHERE label(o) = " + toQueryTerm(object);
        } else {
            query = "SELECT DISTINCT s, p FROM {s} p " + "{" + toQueryTerm(object) + "}";
        }
        TupleQueryResult results = performSeRQLTupleQuery(c, query);

        Map<URI, Set<URI>> map = new HashMap<URI, Set<URI>>();

        while (results.hasNext()) {
            BindingSet r = results.next();
            URI s = Utilities.dupURI((URI) r.getValue("s"));
            URI p = Utilities.dupURI((URI) r.getValue("p"));

            Set<URI> set = map.get(p);
            if (set == null) {
                set = new HashSet<URI>();
                map.put(p, set);
            }
            set.add(s);
        }

        return map;
    }

    public Set<Value> listForwardPropertiesOfClass(RepositoryConnection c, URI klass) throws QueryEvaluationException,
            RepositoryException {
        listForwardPropertiesOfClass++;
        TupleQueryResult results = performSeRQLTupleQuery(c, "SELECT DISTINCT p FROM {} rdf:type {" + toQueryTerm(klass) + "} ; p {}");

        Set<Value> values = new HashSet<Value>();

        while (results.hasNext()) {
            BindingSet s = results.next();
            values.add(Utilities.dupValue(s.getValue("p")));
        }

        results.close();

        return values;
    }

    public Set listDomainClasses(RepositoryConnection c, URI property) throws QueryEvaluationException,
            RepositoryException {
        listDomainClasses++;
        TupleQueryResult results = performSeRQLTupleQuery(c, "SELECT DISTINCT c FROM {} rdf:type {c} ; " + toQueryTerm(property) + " {}");

        Set<Value> values = new HashSet<Value>();

        while (results.hasNext()) {
            BindingSet s = results.next();
            values.add(Utilities.dupValue(s.getValue("c")));
        }

        results.close();

        return values;
    }

    public Set listRangeClasses(RepositoryConnection c, URI property) throws QueryEvaluationException,
            RepositoryException {
        listRangeClasses++;
        TupleQueryResult results = performSeRQLTupleQuery(c, "SELECT DISTINCT c FROM {} " + toQueryTerm(property) + " {} rdf:type {c}");

        Set<Value> values = new HashSet<Value>();

        while (results.hasNext()) {
            BindingSet s = results.next();
            values.add(Utilities.dupValue(s.getValue("c")));
        }

        results.close();

        return values;
    }

    public Set<Value> listBackwardPropertiesOfClass(RepositoryConnection c, URI klass) throws QueryEvaluationException,
            RepositoryException {
        listBackwardPropertiesOfClass++;
        TupleQueryResult results = performSeRQLTupleQuery(c, "SELECT DISTINCT p FROM {} p {} rdf:type {" + toQueryTerm(klass) + "}");

        Set<Value> values = new HashSet<Value>();

        while (results.hasNext()) {
            BindingSet s = results.next();
            values.add(Utilities.dupValue(s.getValue("p")));
        }

        results.close();

        return values;
    }
 
    // ---------- CONSTRUCT queries ----------------------------

    public void copyItem(URI item, RepositoryConnection from, RepositoryConnection to, Set<String> copiedObjects) throws QueryEvaluationException, RepositoryException{
        String objectTerm = toQueryTerm(item);
        String queryStr = "CONSTRUCT {" + objectTerm + "} p {o} FROM {" + objectTerm + "} p {o}";
        GraphQueryResult result = performSeRQLGraphQuery(from, queryStr);

        copiedObjects.add(item.toString());

        while (result.hasNext()) {
            Statement s = result.next();
            to.add(s.getSubject(), s.getPredicate(), s.getObject(), s.getContext());
            Value o = s.getObject();
            if (o instanceof URI) {
                URI object2 = (URI) o;
                if (!copiedObjects.contains(object2.toString()) && Utilities.isBNode(object2)) {
                    copyItem(object2, from, to, copiedObjects);
                }
            }
        }
    }

    public void copyItemWithProperty(URI item, URI property, RepositoryConnection from, RepositoryConnection to) throws QueryEvaluationException, RepositoryException{
        String statement = "{" + toQueryTerm(item) + "} " + toQueryTerm(property) + " {o}";
        String queryStr = "CONSTRUCT " + statement + " FROM " + statement;
        GraphQueryResult result = performSeRQLGraphQuery(from, queryStr);
        while(result.hasNext()) {
            Statement s = result.next();
            to.add(s.getSubject(), s.getPredicate(), s.getObject());
        }
    }
    
    // ---------- Private Methods ----------------------------
    
    private String toQueryTerm(Value v) {
        if (v instanceof Literal) {
            return "\"" + StringUtils.replace(((Literal) v).getLabel(), "\"", "\\\"") + "\"";
        } else if (v instanceof URI) {
            return "<" + ((URI) v).toString() + ">";
        } else {
            return "_:" + ((BNode) v).getID();
        }
    }

    private TupleQueryResult performSeRQLTupleQuery(RepositoryConnection c, String queryStr) {
        try {
            if (s_logger.isDebugEnabled()) s_logger.debug("> performSeRQLTupleQuery: " + queryStr);
            TupleQuery query = c.prepareTupleQuery(QueryLanguage.SERQL, queryStr);
            TupleQueryResult result =  query.evaluate();
            if (s_logger.isDebugEnabled()) s_logger.debug("< performSeRQLTupleQuery: " + queryStr);
            return result;
        } catch (OutOfMemoryError em) {
            s_logger.error("SeRQL Query caused out of memory error: " + queryStr);
            em.printStackTrace();
            throw em;
        } catch (Exception e) {
            throw new RuntimeException("SeRQL Query failed: " + queryStr + "\n   " + e.getMessage());
        }
    }

    private GraphQueryResult performSeRQLGraphQuery(RepositoryConnection c, String queryStr) {
        try {
            if (s_logger.isDebugEnabled()) s_logger.debug("> performSeRQLGraphQuery: " + queryStr);
            GraphQuery query = c.prepareGraphQuery(QueryLanguage.SERQL, queryStr);
            GraphQueryResult result =  query.evaluate();
            if (s_logger.isDebugEnabled()) s_logger.debug("< performSeRQLGraphQuery: " + queryStr);
            return result;
        } catch (OutOfMemoryError em) {
            s_logger.error("SeRQL Query caused out of memory error: " + queryStr);
            em.printStackTrace();
            throw em;
        } catch (Exception e) {
            throw new RuntimeException("SeRQL Query failed: " + queryStr + "\n   " + e.getMessage());
        }
    }
    
    private int countSeRQLTupleQuery(RepositoryConnection c, String queryStr) {
        try {
            if (s_logger.isDebugEnabled()) s_logger.debug("> countSeRQLTupleQuery: " + queryStr);
            TupleQuery query = c.prepareTupleQuery(QueryLanguage.SERQL, queryStr);
            CountTupleQueryResultHandler handler = new CountTupleQueryResultHandler();
            query.evaluate(handler);
            if (s_logger.isDebugEnabled()) s_logger.debug("< countSeRQLTupleQuery: " + queryStr);
            return handler.m_count;
        } catch (OutOfMemoryError em) {
            s_logger.error("SeRQL Query caused out of memory error:  " + queryStr);
            throw em;
        } catch (Exception e) {
            throw new RuntimeException("SeRQL Query failed: " + queryStr + "\n   " + e.getMessage());
        }
    }
    
    private boolean contains(RepositoryConnection c, Resource subject, URI predicate, Value object) {
        boolean out = false;
        try {
            RepositoryResult<Statement> r = c.getStatements(subject, predicate, object, true);
            out = r.hasNext();
            r.close();
        } catch (Exception e) {
            s_logger.info(e);
        }
        return out;
    }

    private long count(RepositoryConnection c, Resource subject, URI predicate, Value object) {
        long counter = 0l;
        try {
            RepositoryResult<Statement> r = c.getStatements(subject, predicate, object, false);
            while (r.hasNext()) {
            	r.next();
                counter++;
            }
            r.close();
        } catch (Exception e) {
            s_logger.info(e);
        }
        return counter;
    }
    
    private int compareValues(Value v1, Value v2) {
        int result = 1;
        if (v1 == v2) {
            result = 0;
        } else if (v1 instanceof URI && v2 instanceof URI) {
            result = ((URI) v1).toString().compareTo(((URI) v2).toString());
        } else if (v1 instanceof BNode && v2 instanceof BNode) {
            result = ((BNode) v1).getID().compareTo(((BNode) v2).getID());
        } else if (v1 instanceof Literal && v2 instanceof Literal) {
            Literal l1 = (Literal) v1;
            Literal l2 = (Literal) v2;
            result = l1.getLabel().compareTo(l2.getLabel());
            if (result == 0) {
                if (l1.getDatatype() == null) {
                    if (l2.getDatatype() != null) {
                        result = -1;
                    }
                } else if (l2.getDatatype() == null) {
                    result = 1;
                } else {
                    result = l1.getDatatype().toString().compareTo(l2.getDatatype().toString());
                }
            }

            if (result == 0) {
                if (l1.getLanguage() == null) {
                    if (l2.getLanguage() != null) {
                        result = -1;
                    }
                } else if (l2.getLanguage() == null) {
                    result = 1;
                } else {
                    result = l1.getLanguage().compareTo(l2.getLanguage());
                }
            }
        } else if (v1 instanceof Literal && !(v2 instanceof Literal)) {
            result = 1;
        } else if (v2 instanceof Literal && !(v1 instanceof Literal)) {
            result = -1;
        } else if (v1 instanceof URI && v2 instanceof BNode) {
            result = 1;
        } else if (v1 instanceof BNode && v2 instanceof URI) {
            result = -1;
        }

        return result;
    }

    class CountTupleQueryResultHandler extends TupleQueryResultHandlerBase {
        int m_count;

        public void endTupleSet() {
            m_count++;
        }
    }
    
    // ---------- profiling counters ----------------------------

    public class Stat implements Comparable {
        private int counter;
        private String name;

        public Stat(String name, int counter) {
            this.counter = counter;
            this.name = name;
        }

        public int compareTo(Object n) {
            return ((Stat) n).counter - this.counter;
        }        
        
        public String getName() {
            return name;
        }
        
        public int getCounter() {
            return counter;
        }
    }
    
    public int listSubjects = 0;
    public int listSubjectObjectPairsOfProperty = 0;
    public int listProperties = 0;
    public int countSubjectObjectPairsOfProperty = 0;
    public int getObjectOfProperty = 0;
    public int getStringOfProperty = 0;
    public int listObjectsOfPropertySP = 0;
    public int listObjectsOfPropertyP = 0;
    public int getSubjectOfProperty = 0;
    public int listSubjectsOfPropertyPO = 0;
    public int listSubjectsOfPropertyP = 0;
    public int mapForwardProperties = 0;
    public int listForwardProperties = 0;
    public int mapBackwardProperties = 0;
    public int listBackwardProperties = 0;
    public int listForwardPropertiesOfClass = 0;
    public int listBackwardPropertiesOfClass = 0;
    public int listDomainClasses = 0;
    public int listRangeClasses = 0;
    public int containsSubject = 0;
    public int containsSubjectProperty = 0;
    public int containsObject = 0;
    public int containsPropertyObject = 0;
    public int containsProperty = 0;
    public int containsStatement = 0;
    public int countStatementsWithPredicate = 0;
    public int fillObjectToValueMap = 0;
    public int fillValueToObjectMap = 0;
    
    public Set getStats() {
        Set<Stat> set = new TreeSet<Stat>();
        set.add(new Stat("listSubjects",listSubjects));
        set.add(new Stat("listSubjectObjectPairsOfProperty",listSubjectObjectPairsOfProperty));
        set.add(new Stat("listProperties",listProperties));
        set.add(new Stat("countSubjectObjectPairsOfProperty",countSubjectObjectPairsOfProperty));
        set.add(new Stat("getObjectOfProperty",getObjectOfProperty));
        set.add(new Stat("getStringOfProperty",getStringOfProperty));
        set.add(new Stat("listObjectsOfPropertySP",listObjectsOfPropertySP));
        set.add(new Stat("listObjectsOfPropertyP",listObjectsOfPropertyP));
        set.add(new Stat("getSubjectOfProperty",getSubjectOfProperty));
        set.add(new Stat("mapForwardProperties",mapForwardProperties));
        set.add(new Stat("listForwardProperties",listForwardProperties));
        set.add(new Stat("mapBackwardProperties",mapBackwardProperties));
        set.add(new Stat("listForwardPropertiesOfClass",listForwardPropertiesOfClass));
        set.add(new Stat("listBackwardPropertiesOfClass",listBackwardPropertiesOfClass));
        set.add(new Stat("listDomainClasses",listDomainClasses));
        set.add(new Stat("listRangeClasses",listRangeClasses));
        set.add(new Stat("containsSubject",containsSubject));
        set.add(new Stat("containsSubjectProperty",containsSubjectProperty));
        set.add(new Stat("containsObject",containsObject));
        set.add(new Stat("containsPropertyObject",containsPropertyObject));
        set.add(new Stat("containsProperty",containsProperty));
        set.add(new Stat("containsStatement",containsStatement));
        set.add(new Stat("countStatementsWithPredicate",countStatementsWithPredicate));
        set.add(new Stat("fillObjectToValueMap",fillObjectToValueMap));
        set.add(new Stat("fillValueToObjectMap",fillValueToObjectMap));
        return set;
    }
    
}
