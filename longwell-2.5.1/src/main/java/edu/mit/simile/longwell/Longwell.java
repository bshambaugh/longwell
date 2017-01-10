package edu.mit.simile.longwell;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Timer;

import javax.xml.transform.Templates;

import org.apache.log4j.Logger;
import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.model.impl.LiteralImpl;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;

import edu.mit.simile.fresnel.configuration.Configuration;
import edu.mit.simile.fresnel.selection.ParsingException;
import edu.mit.simile.fresnel.selection.UnresolvableException;

/**
 * The main Longwell engine.
 */
public class Longwell implements LongwellService {
    
    final static private Logger s_logger = Logger.getLogger(Longwell.class);

    final static public String s_namespace = "http://simile.mit.edu/2005/04/longwell#";

    final static public String s_sb_namespace = "http://simile.mit.edu/2005/04/semantic-bank#";
    final static public String s_sb_Account = s_sb_namespace + "Account";
    final static public String s_sb_accountID = s_sb_namespace + "accountID";

    final static public Resource CONTEXT = new URIImpl("urn:edu.mit.simile.longwell:default");
    
    final protected List<Profile> m_history = new ArrayList<Profile>();
    final protected String m_baseURI;
    final protected Properties m_properties;

    protected Profile m_defaultProfile;
    protected CacheFactory m_cacheFactory;
    protected RepositoryFactory m_repoFactory;
    protected File m_accountDir;
    protected Map<String,Templates> m_transformations;
    protected Map<String,Profile> m_profiles = new HashMap<String,Profile>();
    protected Configuration m_config;
    protected boolean m_facetGuess = false;

    final private long TIMER_DELAY = 60000;
    final private long TIMER_REPEAT_RATE = 10000;
    private boolean m_listening = false;
    private ConfigurationListener m_listener;
    private Timer m_configTimer;

    public Longwell(
            String        baseURI,
            Corpus        corpus,
            CacheFactory  cacheFactory,
            RepositoryFactory repoFactory,
            File          dir,
            Collection    configDirs,
            Map<String,Templates> transformations, 
            Properties    properties
    ) throws Exception {

        try {
            m_baseURI = baseURI;
            m_accountDir = new File(dir, "accounts");
            m_cacheFactory = cacheFactory;
            m_repoFactory = repoFactory;
            m_defaultProfile = createDefaultProfile(corpus, dir);
            m_transformations = transformations;
            m_properties = properties;
            m_config = setupConfiguration(configDirs);

            if (!m_listening) {
                m_configTimer = new Timer();
                m_listener = new ConfigurationListener(configDirs, s_logger);
                m_configTimer.schedule(m_listener, TIMER_DELAY, TIMER_REPEAT_RATE);
                s_logger.info("Configuration file watcher initialized.");
                m_listening = true;
            }

        } catch (Exception e) {
            s_logger.error("Failed to construct Longwell server", e);
            throw e;
        }
    }

    /**
     * Dispose the server and free up resources.
     */
    public void dispose() {
        for (Iterator i = m_profiles.values().iterator(); i.hasNext();) {
            ((Profile) i.next()).dispose();
        }

        m_profiles.clear();

        if (m_defaultProfile != null) {
            m_defaultProfile.dispose();
            m_defaultProfile = null;
        }
        
        m_configTimer.cancel();
    }

    public Properties getMapProperties() {
        return this.m_properties;
    }

    public void setMapProperty(String key, String value) {
        this.m_properties.put(key, value);
    }

    public Profile getDefaultProfile() {
        return m_defaultProfile;
    }

    public Configuration getFresnelConfiguration() {
        return m_config;
    }

    public void setFacetGuessing(boolean guess) {
        m_facetGuess = guess;
    }

    public boolean getFacetGuessing() {
        return m_facetGuess;
    }

    public Templates getTemplates(String type) {
        return m_transformations.get(type);
    }
    
    public void reloadConfiguration(Collection configDirs) {
        m_config = setupConfiguration(configDirs);
    }
        
    public Profile getProfile(String id) {
        Profile profile = null;
        if ("".equals(id) || m_defaultProfile.getID().equals(id)) {
            profile = getDefaultProfile();
        } else if (m_profiles.containsKey(id)) {
            profile = (Profile) m_profiles.get(id);
        } else {
            try {
                profile = getAccountProfile(id, profileIDToAccountOwner(id));
            } catch (Exception e) {
                s_logger.error(e);
            }
        }
        
        if (profile == null) {
            throw new RuntimeException("Could not find profile with id: " + id);
        }

        return profile;
    }
    
    public boolean hasProfile(String id) {
        return profileIDToDir(id).exists();
    }

    public void createProfile(String id) throws Exception {
    	profileIDToDir(id).mkdirs();
    	URI owner = profileIDToAccountOwner(id);
    	AccountProfile accountProfile = getAccountProfile(id, owner);
    	Profile bankProfile = getDefaultProfile();
    	Repository r = Utilities.createMemoryRepository();
        RepositoryConnection c = null;
        try {
            c = r.getConnection();
            c.setAutoCommit(false);
            c.add(owner, RDF.TYPE, new URIImpl(s_sb_Account));
            c.add(owner, new URIImpl(s_sb_accountID), new LiteralImpl(id));
            c.add(owner, new URIImpl(Namespaces.s_dc + "title"), new LiteralImpl(id));
            c.add(owner, new URIImpl(Namespaces.s_dc + "date"), new LiteralImpl(Utilities.unparseDate(new Date())));
            c.commit();
            accountProfile.addData(r, true);
            bankProfile.addData(r, true);
        } catch (RepositoryException e) {
            if (c != null) c.rollback();
        } finally {
            if (c != null) c.close();
        }
		r.shutDown();
     }

    public void createProfile(String openid, String id) throws Exception {
   		URI owner = new URIImpl(openid);
   		createProfile(owner, id);
    }
    
    public void createProfile(URI owner, String id) throws Exception {
    	profileIDToDir(id).mkdirs();
    	AccountProfile accountProfile = getAccountProfile(id, owner);
    	Profile bankProfile = getDefaultProfile();
    	Repository r = Utilities.createMemoryRepository();
        RepositoryConnection c = null;
        try {
            c = r.getConnection();
            c.setAutoCommit(false);
            c.add(owner, RDF.TYPE, new URIImpl(s_sb_Account));
            c.add(owner, new URIImpl(s_sb_accountID), new LiteralImpl(id));
            c.add(owner, new URIImpl(Namespaces.s_dc + "title"), new LiteralImpl(id));
            c.add(owner, new URIImpl(Namespaces.s_dc + "date"), new LiteralImpl(Utilities.unparseDate(new Date())));
            c.commit();
            accountProfile.addData(r, true);
            bankProfile.addData(r, true);
        } catch (RepositoryException e) {
            if (c != null) c.rollback();
        } finally {
            if (c != null) c.close();
        }
        r.shutDown();
    }
    
    public String retrieveAccountID(URI accountURI) throws Exception {
        String property = "";
        RepositoryConnection c = null;
        try {
            c = getDefaultProfile().getRepository().getConnection();
            if (getDefaultProfile().getQueryManager().containsStatement(c, accountURI, RDF.TYPE, new URIImpl(s_sb_Account))) {
                property = getDefaultProfile().getQueryManager().getStringOfProperty(c, accountURI, new URIImpl(s_sb_accountID));
            }
        } finally {
            if (c != null) c.close();
        }
        return property;
    }

    public CacheFactory getCacheFactory() {
        return m_cacheFactory;
    }
    
    protected Configuration setupConfiguration(Collection configDirs) {
        Configuration conf = null;
        try {
            Repository confRepo = Utilities.createMemoryRepository();
            
            for (Iterator i = configDirs.iterator(); i.hasNext(); ) {
                File f = (File) i.next();
                Utilities.loadDataFromFile(f, confRepo);
            }
            conf = new Configuration(confRepo, getDefaultProfile().getRepository());
        } catch (ParsingException pe) {
            // problem with parsing fresnel configuration
            s_logger.error("Could not parse Fresnel configuration, will not render using Fresnel engine");
        } catch (UnresolvableException ue) {
            // problem in setting up fresnel
            s_logger.error("Could not configure Fresnel properly, will not render using Fresnel engine");
        } catch (Exception e) {
            // error loading data from directory
            s_logger.error("Could not load Fresnel data, will not render using Fresnel engine");
        }
        return conf;
    }
    
    public void addProfile(String id, Profile profile) { 
    	m_profiles.put(id, profile); 
    } 

    public void removeProfile(String id) { 
    	m_profiles.remove(id); 
    } 
    	
    protected Profile createDefaultProfile(Corpus corpus, File dir) {
        return new LongwellProfile("default", corpus, m_cacheFactory, m_repoFactory, dir);
    }
   
    // ---------------------------------------------------------------------------------------------------------------
    
    protected AccountProfile getAccountProfile(String profileID, URI owner) throws Exception {

        File dir = profileIDToDir(profileID);
        if (!dir.exists()) {
            return null;
        }

        AccountProfile profile = loadAccountProfile(profileID, owner);

        m_history.add(profile);
        if (m_history.size() > 100) {
            AccountProfile profile2 = (AccountProfile) m_history.remove(0);
            m_profiles.remove(profile2.getID());
            profile2.dispose();
        }

        m_profiles.put(profileID, profile);

        return profile;
    }

    protected AccountProfile loadAccountProfile(String profileID, URI owner) throws Exception {

        File dir = profileIDToDir(profileID);

        RepositoryFactory repoFactory = new RepositoryFactory(m_repoFactory.getProperties(), dir);
        Corpus corpus = new Corpus("account#" + profileID, repoFactory, RepositoryFactory.ACCOUNT);

        AccountProfile profile = new AccountProfile(profileID, corpus, m_cacheFactory, m_repoFactory, dir, owner);

        return profile;
    }

    protected URI profileIDToAccountOwner(String id) {
        return new URIImpl(m_baseURI + id);
    }

    protected File profileIDToDir(String profileID) {
        if (profileID == null || profileID.length() == 0) {
            throw new RuntimeException("The ProfileID is null or empty.");
        }
        
        if (profileID.startsWith("_")) {
            return new File(m_accountDir, profileID);
        }

        File base = new File(m_accountDir, profileID.substring(0, 1));
        return new File(base, profileID);
    }
}
