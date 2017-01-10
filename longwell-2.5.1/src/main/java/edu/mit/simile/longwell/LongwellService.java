package edu.mit.simile.longwell;

import java.util.Collection;
import java.util.Properties;

import javax.xml.transform.Templates;

import org.openrdf.model.URI;

import edu.mit.simile.fresnel.configuration.Configuration;

/**
 * Used by LongwellServlet to manage the Longwell object.
 * 
 * Required by Piggy Bank.  Do not remove.
 */
public interface LongwellService {

    public void dispose();
    
    public Properties getMapProperties();

    public void setMapProperty(String key, String value);

    public Profile getDefaultProfile();

    public Configuration getFresnelConfiguration();

    public void setFacetGuessing(boolean guess);

    public boolean getFacetGuessing();

    public Templates getTemplates(String type);
    
    public void reloadConfiguration(Collection configDirs);
    
    public Profile getProfile(String id);
    
    public boolean hasProfile(String id);

    public void createProfile(String id) throws Exception;
    
    public void createProfile(String openid, String id) throws Exception;

    public String retrieveAccountID(URI accountURI) throws Exception;

    public CacheFactory getCacheFactory();
    
    public void addProfile(String id, Profile profile);
    
    public void removeProfile(String id);
}
