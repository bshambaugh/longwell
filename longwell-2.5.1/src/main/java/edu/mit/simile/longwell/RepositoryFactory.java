package edu.mit.simile.longwell;

import java.io.File;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.openrdf.model.URI;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.sail.Sail;
import org.openrdf.sail.memory.MemoryStore;
import org.openrdf.sail.nativerdf.NativeStore;

import edu.mit.simile.banach.counter.Counter;
import edu.mit.simile.banach.smoosher.Smoosher;

public class RepositoryFactory {

    final static private Logger s_logger = Logger.getLogger(RepositoryFactory.class);

    final static public int MAIN = 0;
    final static public int ACCOUNT = 1;
    final static public int SCHEMA_CACHE = 2;
    final static public int TEXT_INDEX = 3;
    final static public int TAG_CACHE = 4;
    final static public int TEMPORARY = 100;
    
    final static public URI typesContext = new URIImpl("urn:longwell:types");
    
    protected Properties m_props;
    protected File m_dir;
    protected String m_flavor;
    
    public RepositoryFactory(Properties props, File dir) {
        m_props = props;
        m_flavor = props.getProperty("longwell.store.type","native").toLowerCase();
        m_dir = dir;
    }
    
    public Repository getRepository(int type) throws RepositoryException {
        
        Sail sail = null;
        
        if (m_flavor.equals("memory") || type == TEMPORARY) {
            sail = new MemoryStore();
        } else if (m_flavor.equals("native")) {
            sail = new NativeStore();
            sail.setDataDir(getDir(type));
            ((NativeStore) sail).setTripleIndexes("spoc,posc,opsc");
//        } else if (m_type.equals("biggles")) {
//            BiggleStore biggles = new BiggleStore();
//            biggles.setURL(m_props.getProperty("longwell.store.db.url"));
//            biggles.setUser(m_props.getProperty("longwell.store.db.user"));
//            biggles.setPassword(m_props.getProperty("longwell.store.db.passwd"));
//            sail = biggles;
        }

        if (type == MAIN || type == ACCOUNT) {
            // adding a counting stackable sail to keep track of how many statements
            // there are in each context
            sail = new Counter(sail);
            
            // enable type distilling
            // boolean distilling = Boolean.parseBoolean(m_props.getProperty("longwell.store.distilling"));
            // if (distilling) {
            //     s_logger.info("Enabling distilling on the triple store");
            //     sail = new Distiller(sail, RDF.TYPE, null, null, false, null, typesContext, false);
            //}
            
            // enable smooshing in the triple store with the Banach smooshing operator
            boolean smooshing = Boolean.parseBoolean(m_props.getProperty("longwell.store.smooshing"));
            if (smooshing) {
                s_logger.info("Enabling smooshing on the triple store");
                sail = new Smoosher(sail);
            }
        }
        
        Repository repo = new SailRepository(sail);
        repo.initialize();
        
        return repo;
    }
        
    public Properties getProperties() {
        return m_props;
    }
        
    public String getFlavor() {
        return m_flavor;
    }
    
    public File getDir(int type) {
        if (type == MAIN) {
            return m_dir;
        } else if (type == ACCOUNT) {
            return new File(m_dir, "database");
        } else if (type == SCHEMA_CACHE) {
            return new File(m_dir, "system/schema");
        } else if (type == TEXT_INDEX) {
            return new File(m_dir, "system/text-index");
        } else if (type == TAG_CACHE) {
            return new File(m_dir, "system/tag");
        } else { 
            return null;
        }
    }
}
