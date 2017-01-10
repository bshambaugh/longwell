package edu.mit.simile.longwell.tests;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.xml.transform.Templates;

import junit.framework.TestCase;

import org.apache.log4j.Logger;

import edu.mit.simile.longwell.CacheFactory;
import edu.mit.simile.longwell.Corpus;
import edu.mit.simile.longwell.Longwell;
import edu.mit.simile.longwell.LongwellServlet;
import edu.mit.simile.longwell.RepositoryFactory;

public class LongwellServiceTests extends TestCase {
    
    final static protected Logger logger = Logger.getLogger(LongwellServiceTests.class);

    public void testService() throws Exception {

        Properties properties = new Properties();
        InputStream is = new FileInputStream(absolutize("WEB-INF/longwell.properties"));
        properties.load(is);
        is.close();

        // Get the database location
        File databaseDir = absolutize(properties.getProperty("longwell.store.dir","WEB-INF/database"));
        
        // Create the repository factory
        RepositoryFactory repoFactory = new RepositoryFactory(properties, databaseDir);

        // Create the cache factory
        CacheFactory cacheFactory = new CacheFactory(properties);
        
        // Create default Corpus
        Corpus corpus = null;
        try {
            corpus = new Corpus("default", repoFactory, RepositoryFactory.MAIN);
        } catch (Exception e) {
            logger.error("Failed to create default corpus", e);
            throw new ServletException("Failed to create default corpus", e);
        }
        
        String baseURI = properties.getProperty("longwell.baseURI",LongwellServlet.BASE_URI);
        
        Collection<File> fresnelConfigs = new ArrayList<File>();
        File f = absolutize("WEB-INF/fresnel-defaults.n3");
        if (f.exists()) fresnelConfigs.add(f);
        
        Map<String,Templates> transformations = new HashMap<String,Templates>();
        
        Longwell longwell = new Longwell(
                baseURI,
                corpus, 
                cacheFactory, 
                repoFactory,
                databaseDir,
                fresnelConfigs, 
                transformations,
                properties
        );

        longwell.setFacetGuessing(Boolean.valueOf(properties.getProperty("longwell.facet.guessing", "false")).booleanValue());
        
        longwell.getDefaultProfile().addData(new File("src/data"));
        
        assertTrue(true);
    }

    protected File absolutize(String location) {
        if (location.charAt(0) != '/' && location.indexOf(':') < 0) {
            return new File("src/main/webapp", location);
        } else {
            return new File(location);
        }
    }

}
