package edu.mit.simile.longwell;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Iterator;

import org.apache.log4j.Logger;
import org.openrdf.model.URI;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.util.RDFInserter;

import edu.mit.simile.longwell.dq.DynamicQueryModel;
import edu.mit.simile.longwell.query.engine.QueryEngine;
import edu.mit.simile.longwell.schema.SchemaModel;

public class LongwellProfile extends Profile {

    final static private Logger s_logger = Logger.getLogger(LongwellProfile.class);
    
    final static public String s_sb_contributor = Longwell.s_sb_namespace + "contributor";
	
    protected Cache m_cache;
    protected File m_dir;
    protected RepositoryFactory m_factory;
    
	public LongwellProfile(String profileID, Corpus corpus, CacheFactory cacheFactory, RepositoryFactory factory, File dir) {
        super(profileID, corpus, cacheFactory);

        m_dir = dir;
        m_factory = factory;
        m_cache = cacheFactory.getCache("latlong", false);
        
        addStructuredModel(new SystemModel(this));
        addStructuredModel(new QueryEngine(this));
        addStructuredModel(new SchemaModel(this, m_factory));
        addStructuredModel(new TextIndexModel(this, m_factory));
        addStructuredModel(new DynamicQueryModel(this));
        addStructuredModel(new FacadeStructuredModel(this));
        addStructuredModel(new TagModel(this, m_factory));
	}

	public void publish(Repository r, URI contributor) {
		try {
            Repository r2 = Utilities.createMemoryRepository();
            RepositoryConnection c = null, c2 = null;
            try {
                c = r.getConnection(); 
                c2 = r2.getConnection();
                c2.setAutoCommit(false);

                RDFInserter handler = new RDFInserter(c2);
                c.exportStatements(null, null, null, true, handler);
                
    			URI contributorProperty = new URIImpl(s_sb_contributor);
    			
    			Iterator i = m_queryManager.listSubjectsOfProperty(c, RDF.TYPE).iterator();
    			while (i.hasNext()) {
    				URI object = (URI) i.next();
    				c2.add(object, contributorProperty, contributor);
    			}
                
                c2.commit();
            } catch (RepositoryException e) {
                if (c2 != null) c2.rollback();
            } finally {
                if (c2 != null) c2.close();
                if (c != null) c.close();
            }
            addData(r2, false);
            r2.shutDown();
		} catch (Exception e) {
			s_logger.error(e);
		} 
	}
	
	public void retract(Repository r, URI contributor) {
		try {
            Repository myR = getRepository();
            Repository r2 = Utilities.createMemoryRepository();
            RepositoryConnection myC = null, c = null, c2 = null;

            try {
                c = r.getConnection();
    			c2 = r2.getConnection();
                c2.setAutoCommit(false);
                myC = myR.getConnection();
    			
    			URI contributorProperty = new URIImpl(s_sb_contributor);
    		
    			Iterator i = m_queryManager.listSubjectsOfProperty(c, RDF.TYPE).iterator();
    			while (i.hasNext()) {
    				URI object = (URI) i.next();
    				
    				if (m_queryManager.containsStatement(myC, object, contributorProperty, contributor)) {
    					
    					Iterator j = m_queryManager.listObjectsOfProperty(myC, object, contributorProperty).iterator();
    
    					boolean sameContributor = true;
    					
    					while (j.hasNext()) {
    						if (!j.next().equals(contributor)) {
    							sameContributor = false;
    							break;
    						}
    					}
    					
    					if (sameContributor) {
    						extractObject(object, r2);
    					}
                        
    					c2.add(object, contributorProperty, contributor);
    				}
    			}
    			c2.commit();
            } catch (RepositoryException e) {
                if (c2 != null) c2.rollback();
            } finally {
                if (c2 != null) c2.close();
                if (c != null) c.close();
                if (myC != null) myC.close();
            }
			
			removeData(r2);
            r2.shutDown();
		} catch (Exception e) {
			s_logger.error(e);
		}
	}
	
	/** 
     * actually returns a JSON representation, not a set of coordinates
     * always returns a JSON object, caches failures and successes alike
	 */
    public String getLatLong(String location, String key) {
        String content = (String) m_cache.get(location);
        if (content == null) {
            content = "";
            try {
                InputStream is = new URL("http://maps.google.com/maps/geo?key=" + key + "&output=json&q=" + URLEncoder.encode(location, "UTF-8")).openStream();
                InputStreamReader isr = new InputStreamReader(is, "UTF-8");
                StringBuffer sb = new StringBuffer();
                char[] chars = new char[1024];
                int c;
    
                while ((c = isr.read(chars)) > 0) {
                    sb.append(chars, 0, c);
                }
    
                content = sb.toString();
       
                is.close();
            } catch (Exception e) {
                s_logger.error("Error retriving location '" + location + "'", e);
            }
            m_cache.put(location, content);
        }
        return content;
    }
    
    public boolean publishedObject(URI object) {
        boolean published = false;
        try {
            RepositoryConnection c = null;
            try {
                c = getRepository().getConnection();
                published = m_queryManager.containsStatement(c, object, new URIImpl(PublishingModel.s_pub_status), new URIImpl(PublishingModel.s_pub_Public));
            } finally {
                if (c != null) c.close();
            }
        } catch (Exception e) {
            s_logger.error(e);
        }
        return published;
    }    
}
