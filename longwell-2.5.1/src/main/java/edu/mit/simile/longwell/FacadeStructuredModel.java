package edu.mit.simile.longwell;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.openrdf.model.Literal;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.repository.RepositoryConnection;

public class FacadeStructuredModel extends StructuredModelBase {

    final static public String s_longwell_shortLabel = LongwellServlet.NAMESPACE + "shortLabel";
    final static public String s_longwell_contentDescription = LongwellServlet.NAMESPACE + "contentDescription";
    final static public String s_longwell_Facade = LongwellServlet.NAMESPACE + "Facade";
    final static public String s_longwell_SchemaBasedFacade = LongwellServlet.NAMESPACE + "SchemaBasedFacade";
    final static public String s_longwell_QueryBasedFacade = LongwellServlet.NAMESPACE + "QueryBasedFacade";
    final static private Logger s_logger = Logger.getLogger(FacadeStructuredModel.class);

    protected boolean m_initialized;

    protected Map<String,QueryBasedFacade> m_facades = new HashMap<String,QueryBasedFacade>();

    public FacadeStructuredModel(Profile profile) {
        super(profile);
    }

    public Collection<QueryBasedFacade> getFacades() {
        internalInitialize();
        return m_facades.values();
    }

    protected void internalInitialize() {
        if (m_initialized) {
            return;
        }

        try {
            RepositoryConnection c = null;
            try {
                c = m_profile.getRepository().getConnection();
            	c.setAutoCommit(false);
                internalOnAfterAdd(c);
            } finally {
                if (c != null) c.close();
            }
        } catch (Exception e) {
            s_logger.error(e);
        }

        m_initialized = true;
    }

    public void onAfterAdd(RepositoryConnection c) {
        super.onAfterAdd(c);
    	if (s_logger.isDebugEnabled()) s_logger.debug("> onAfterAdd()");

        internalInitialize();
        try {
            internalOnAfterAdd(c);
        } catch (Exception e) {
            s_logger.error(e);
        }
    	if (s_logger.isDebugEnabled()) s_logger.debug("< onAfterAdd()");
    }

    public void internalOnAfterAdd(RepositoryConnection c) throws Exception {
    	if (s_logger.isDebugEnabled()) s_logger.debug("> internalOnAfterAdd()");
        URI shortLabelPredicate = new URIImpl(s_longwell_shortLabel);
        URI contentDescriptionPredicate = new URIImpl(s_longwell_contentDescription);

        SystemModel systemModel = m_profile.getSystemModel();

        Set<Value> newFacades = new HashSet<Value>();

        newFacades.addAll(m_queryManager.listSubjectsOfProperty(c, RDF.TYPE, new URIImpl(s_longwell_QueryBasedFacade)));
        newFacades.addAll(m_queryManager.listSubjectsOfProperty(c, new URIImpl(SystemModel.s_longwell_systemStatus),
                new URIImpl(SystemModel.s_longwell_Trusted)));

        Iterator i = newFacades.iterator();
        while (i.hasNext()) {
            URI object = (URI) i.next();
            if (systemModel.isTrusted(object)) {
            	RepositoryConnection c2 = m_profile.getRepository().getConnection();
            	c2.setAutoCommit(false);
            	
                String shortLabel = tryGetStringProperty(c2, object, shortLabelPredicate, object.toString());
                String contentDescription = tryGetStringProperty(c2, object, contentDescriptionPredicate, "");

                try {
                    m_facades.put(object.toString(), QueryBasedFacade.constructFacade(m_profile.getQueryManager(), object, shortLabel, contentDescription, c2, m_profile));
                } catch (Exception e) {
                    s_logger.error(e);
                }
                
                c2.close();
            }
        }
    	if (s_logger.isDebugEnabled()) s_logger.debug("< internalOnAfterAdd()");
    }

    public void onAfterRemove(RepositoryConnection c) {
        super.onAfterRemove(c);
    	if (s_logger.isDebugEnabled()) s_logger.debug("> internalOnAfterRemove()");

        try {
            Set<Value> oldFacades = new HashSet<Value>();

            oldFacades.addAll(m_queryManager.listSubjectsOfProperty(c, RDF.TYPE, new URIImpl(s_longwell_QueryBasedFacade)));
            oldFacades.addAll(m_queryManager.listSubjectsOfProperty(c, new URIImpl(SystemModel.s_longwell_systemStatus),
                    new URIImpl(SystemModel.s_longwell_Trusted)));

            Iterator i = oldFacades.iterator();
            while (i.hasNext()) {
                URI uri = (URI) i.next();

                m_facades.remove(uri.toString());
            }
        } catch (Exception e) {
            s_logger.error(e);
        }
    	if (s_logger.isDebugEnabled()) s_logger.debug("< internalOnAfterRemove()");
    }

    protected String tryGetStringProperty(RepositoryConnection c, URI object, URI predicate, String defaultValue) {
        try {
            return ((Literal) m_queryManager.getObjectOfProperty(c, object, predicate)).getLabel();
        } catch (Exception e) {
            return defaultValue;
        }
    }
}

    