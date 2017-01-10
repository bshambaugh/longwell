package edu.mit.simile.longwell;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.log4j.Logger;
import org.openrdf.model.Literal;
import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.impl.LiteralImpl;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;

public class SystemModel extends StructuredModelBase {

    final static private Logger s_logger = Logger.getLogger(SystemModel.class);

    protected SystemModel(Profile profile) {
        super(profile);
    }

    final static public String s_longwell_systemic = Longwell.s_namespace + "systemic";
    final static public String s_longwell_systemStatus = Longwell.s_namespace + "systemStatus";
    final static public String s_longwell_Trusted = Longwell.s_namespace + "Trusted";

    public boolean isOfSystemicType(URI object) {
        boolean isSystemic = false;
        try {
            RepositoryConnection c = null;
            try {
                c = m_profile.getRepository().getConnection();
                URI systemic = new URIImpl(s_longwell_systemic);
                Literal trueLiteral = new LiteralImpl("true");

                Iterator i = m_queryManager.listObjectsOfProperty(c, object, RDF.TYPE).iterator();
                while (i.hasNext()) {
                    URI typeURI = (URI) i.next();
                    isSystemic = m_queryManager.containsStatement(c, typeURI, systemic, trueLiteral);
                    if (isSystemic) break;
                }
            } finally {
                if (c != null) c.close();
            }
        } catch (Exception e) {
            s_logger.error(e);
        }
        return isSystemic;
    }

    public boolean isTrusted(URI object) {
        boolean trusted = false;
        try {
            RepositoryConnection c = null;
            try {
                c = m_profile.getRepository().getConnection();
                trusted = m_queryManager.containsStatement(c, object, new URIImpl(s_longwell_systemStatus), new URIImpl(s_longwell_Trusted));
            } finally {
                if (c != null) c.close();
            }
        } catch (Exception e) {
            s_logger.error(e);
        }
        return trusted;
    }

    public void trust(URI object) throws Exception {
    	Repository r = null;
        RepositoryConnection c = null;
        try {
        	r = Utilities.createMemoryRepository();
        	c = r.getConnection();
            c.add(object, new URIImpl(s_longwell_systemStatus), new URIImpl(s_longwell_Trusted));
            m_profile.addData(r, true);
        } catch (RepositoryException e) {
        	s_logger.error(e);
        } finally {
            if (c != null) c.close();
            if (r != null) r.shutDown();
        }
    }

    public void distrust(URI object) throws Exception {
    	Repository r = null;
        RepositoryConnection c = null;
        try {
        	r = Utilities.createMemoryRepository();
        	c = r.getConnection();
        	c.setAutoCommit(false);
            c.add(object, new URIImpl(s_longwell_systemStatus), new URIImpl(s_longwell_Trusted));
            c.commit();
            m_profile.removeData(r);
        } catch (RepositoryException e) {
        	s_logger.error(e);
        } finally {
            if (c != null) c.close();
            if (r != null) r.shutDown();
        }
    }

    public void trustAllOfSystemicTypes(Repository r) throws Exception {
    	if (s_logger.isDebugEnabled()) s_logger.debug("> trustAllOfSystemicTypes()");
        RepositoryConnection c = null, c2 = null;
        
    	try {
            c = r.getConnection();
            c.setAutoCommit(false);
        	c2 = m_profile.getRepository().getConnection();
        	c2.setAutoCommit(false);
            
            URI systemStatus = new URIImpl(s_longwell_systemStatus);
            URI Trusted = new URIImpl(s_longwell_Trusted);

            Set<Value> systemicTypes = new HashSet<Value>();

            systemicTypes.addAll(m_queryManager.listSubjectsOfProperty(c, new URIImpl(s_longwell_systemic), new LiteralImpl("true")));
            systemicTypes.addAll(m_queryManager.listSubjectsOfProperty(c2, new URIImpl(s_longwell_systemic), new LiteralImpl("true")));

            Iterator i = systemicTypes.iterator();
            while (i.hasNext()) {
                URI typeURI = (URI) i.next();
                Iterator j = m_queryManager.listSubjectsOfProperty(c, RDF.TYPE, typeURI).iterator();
                while (j.hasNext()) {
                    c.add((Resource) j.next(), systemStatus, Trusted);
                }
            }

            c.commit();
        } catch (RepositoryException e) {
            if (c != null) c.rollback();
            s_logger.error(e);
        } finally {
            if (c != null) c.close();
            if (c2 != null) c2.close();
        }
    	if (s_logger.isDebugEnabled()) s_logger.debug("< trustAllOfSystemicTypes()");
    }

    public void filterOutTrustedItems(Repository r) throws RepositoryException {
    	if (s_logger.isDebugEnabled()) s_logger.debug("> filterOutTrustedItems()");
        RepositoryConnection c = null;
        try {
            c = r.getConnection();
            c.setAutoCommit(false);
            c.remove((Resource) null, new URIImpl(s_longwell_systemStatus), new URIImpl(s_longwell_Trusted));
            c.commit();
        } catch (RepositoryException e) {
            s_logger.error(e);
        } finally {
            if (c != null) c.close();
        }
    	if (s_logger.isDebugEnabled()) s_logger.debug("< filterOutTrustedItems()");
    }
}
