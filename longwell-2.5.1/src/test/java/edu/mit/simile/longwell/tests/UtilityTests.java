package edu.mit.simile.longwell.tests;

import java.io.File;
import java.util.Set;

import junit.framework.TestCase;

import org.apache.log4j.Logger;
import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;

import edu.mit.simile.longwell.QueryManager;
import edu.mit.simile.longwell.Utilities;

public class UtilityTests extends TestCase {

    final static protected String namespace = "http://simile.mit.edu/longwell/tests#";
    
    final static protected Resource S = new URIImpl(namespace + "subject");
    final static protected URI P = new URIImpl(namespace + "object");
    final static protected Value O = new URIImpl(namespace + "predicate");
    final static protected Resource C = new URIImpl(namespace + "context");
    
    final static protected Logger logger = Logger.getLogger(UtilityTests.class);

    public void testMemoryStore() throws Exception {

        long size = -1;
        
        Repository repo = Utilities.createMemoryRepository();
        RepositoryConnection c = null;
        try {
            c = repo.getConnection();
            c.add(S,P,O,C);
            c.commit();
            size = c.size();
        } catch (RepositoryException e) {
            if (c != null) c.rollback();
        } finally {
            if (c != null) c.close();
        }
        repo.shutDown();
            
        assertTrue(size == 1);
    }

    public void testNativeStore() throws Exception {

        File dir = new File("tmp");
        if (dir.exists()) {
            dir.mkdirs();
        }
        
        long size = -1;
        
        Repository repo = Utilities.createNativeRepository(dir);
        RepositoryConnection c = null;
        try {
            c = repo.getConnection();
            c.add(S,P,O,C);
            c.commit();
            size = c.size();
        } catch (RepositoryException e) {
            if (c != null) c.rollback();
        } finally {
            if (c != null) c.close();
        }
        repo.shutDown();
        
        Utilities.deleteDirectory(dir);
            
        assertTrue(size == 1);
    }
    
    public void testLoadDataFromDir() throws Exception {
        Repository repo = Utilities.createMemoryRepository();
        File dir = new File("src/data");
        Utilities.loadDataFromDir(dir, repo, true);
        RepositoryConnection c = null;
        QueryManager qm = new QueryManager();
        try {
            c = repo.getConnection();
            Set<Value> subjects = qm.listSubjects(c);
            logger.info("subjects: " + subjects.size());
            Set<Value> properties = qm.listProperties(c);
            logger.info("properties: " + properties.size());
        } finally {
            if (c != null) c.close();
        }
        repo.shutDown();
        assertTrue(true);
    }
}
