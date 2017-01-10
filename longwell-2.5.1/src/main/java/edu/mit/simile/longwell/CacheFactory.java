package edu.mit.simile.longwell;

import java.util.Properties;

import org.apache.log4j.Logger;

import com.whirlycott.cache.CacheConfiguration;
import com.whirlycott.cache.CacheManager;

public class CacheFactory {

    final static private Logger s_logger = Logger.getLogger(CacheFactory.class);

    final static public String CACHE_SIZE = "longwell.cache.size";
    final static public String CACHE_POLICY = "longwell.cache.policy";
    final static public String CACHE_TUNER_SLEEP = "longwell.cache.tuner.sleeptime";

    private com.whirlycott.cache.Cache m_cache;
    private long m_lastModified = -1;

    public CacheFactory(Properties props) {
        try {
            int size = Integer.parseInt(props.getProperty(CACHE_SIZE, "10000"));
            int sleep = Integer.parseInt(props.getProperty(CACHE_TUNER_SLEEP, "30"));
            String policy = props.getProperty(CACHE_POLICY, "com.whirlycott.cache.policy.LFUMaintenancePolicy");
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("cache size: " + size);
                s_logger.debug("cache sleep time: " + sleep);
                s_logger.debug("cache cleanup policy: " + policy);
            }
            CacheManager m = CacheManager.getInstance();
            CacheConfiguration c = new CacheConfiguration();
            c.setName("longwell");
            c.setBackend("com.whirlycott.cache.impl.ConcurrentHashMapImpl");
            c.setMaxSize(size);
            c.setPolicy(policy);
            c.setTunerSleepTime(sleep);
            m_cache = m.createCache(c);
        } catch (Exception e) {
            s_logger.error("Could not initialize the cache factory", e);
        }
    }

    public com.whirlycott.cache.Cache getCache() {
        return m_cache;
    }
    
    public Cache getCache(String aName, String bName, boolean clearable) {
        return getCache(aName + " | " + bName, clearable);
    }

    public Cache getCache(String name, boolean clearable) {
        return new Cache(this, m_cache, name, clearable);
    }
    
    public long getLastModified() {
        return m_lastModified;
    }
    
    public void setLastModified(long lastModified) {
        this.m_lastModified = lastModified;
    }
}
