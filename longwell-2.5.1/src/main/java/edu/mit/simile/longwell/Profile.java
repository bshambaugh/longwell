package edu.mit.simile.longwell;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.openrdf.model.URI;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;

import edu.mit.simile.longwell.schema.SchemaModel;

public abstract class Profile {

    // NOTE(SM): This class was designed before the Sesame API introduced transactional
    // features which is the reason why some methods are passed entire repositories
    // which are just temporary memory repositories that act as transactions.
    
    final static private Logger s_logger = Logger.getLogger(Profile.class);

    final protected String m_profileID;

    protected Corpus m_corpus;
    protected CacheFactory m_cacheFactory;
    protected QueryManager m_queryManager;
    protected Set<IStructuredModel> m_structuredModels = new LinkedHashSet<IStructuredModel>();

    protected Profile(String profileID, Corpus corpus, CacheFactory cacheFactory) {
        m_queryManager = new QueryManager();
        m_profileID = profileID;
        m_corpus = corpus;
        m_cacheFactory = cacheFactory;

        // You need to create the various models in your subclass
    }

    public void index(boolean regenerate) {
        if (s_logger.isDebugEnabled()) s_logger.debug("> index(" + (regenerate ? "regenerate)" : "don't regenerate)"));
        Iterator i = m_structuredModels.iterator();
        while (i.hasNext()) {
            IStructuredModel model = (IStructuredModel) i.next();
            model.index(regenerate);
        }
        if (s_logger.isDebugEnabled()) s_logger.debug("< index(" + (regenerate ? "regenerate)" : "don't regenerate)"));
    }

    public void optimize() {
        if (s_logger.isDebugEnabled()) s_logger.debug("> optimize()");
        Iterator i = m_structuredModels.iterator();
        while (i.hasNext()) {
            IStructuredModel model = (IStructuredModel) i.next();
            model.optimize();
        }
        if (s_logger.isDebugEnabled()) s_logger.debug("< optimize()");
    }

    public void dispose() {
        if (s_logger.isDebugEnabled()) s_logger.debug("> dispose()");
        Iterator i = m_structuredModels.iterator();
        while (i.hasNext()) {
            IStructuredModel model = (IStructuredModel) i.next();
            model.dispose();
        }
        m_structuredModels.clear();
        m_cachedSchemaModel = null;

        m_corpus.dispose();
        m_corpus = null;
        if (s_logger.isDebugEnabled()) s_logger.debug("< dispose()");
    }

    public String getID() {
        return m_profileID;
    }

    public CacheFactory getCacheFactory() {
        return m_cacheFactory;
    }

    public QueryManager getQueryManager() {
        return m_queryManager;
    }
    
    /**
     * Answer the repository from which clients of this profile can query.
     * However, consider the returned model read-only. That is, data additions
     * and removals should be done through this profile rather than through the
     * returned repository.
     * 
     * @return
     */
    public Repository getRepository() {
        return m_corpus.getRepository();
    }

    /**
     * Answer the first structured model in this profile of the given
     * implementation type.
     * 
     * @param modelType
     * @return
     */
    public IStructuredModel getStructuredModel(Class<?> modelType) {
        Iterator i = m_structuredModels.iterator();
        while (i.hasNext()) {
            Object o = i.next();
            if (modelType.isAssignableFrom(o.getClass())) {
                return (IStructuredModel) o;
            }
        }
        return null;
    }

    /**
     * Answer the first structured model in this profile of the given
     * implementation type.
     * 
     * @param modelType
     * @return
     */
    public IStructuredModel getStructuredModel(String modelTypeName) {
        try {
            return getStructuredModel(Class.forName(modelTypeName));
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    public void addStructuredModel(IStructuredModel sModel) {
        m_structuredModels.add(sModel);
    }
    
    /**
     * Answer the (first) schema model in this profile.
     * 
     * @return
     */
    public SchemaModel getSchemaModel() {
        if (m_cachedSchemaModel == null) {
            m_cachedSchemaModel = (SchemaModel) getStructuredModel(SchemaModel.class);
        }
        return m_cachedSchemaModel;
    }

    private SchemaModel m_cachedSchemaModel = null;

    /**
     * Answer the (first) system model in this profile.
     * 
     * @return
     */
    public SystemModel getSystemModel() {
        if (m_cachedSystemModel == null) {
            m_cachedSystemModel = (SystemModel) getStructuredModel(SystemModel.class);
        }
        return m_cachedSystemModel;
    }

    private SystemModel m_cachedSystemModel = null;

    public boolean containsObject(URI object) {
        boolean contains = false;
        try {
            RepositoryConnection c = null;
            try {
                c = getRepository().getConnection();
                contains = m_queryManager.containsSubjectProperty(c, object, RDF.TYPE);
            } finally {
                if (c != null) c.close();
            }
        } catch (Exception e) {
            s_logger.error(e);
        }
        return contains;
    }

    public void extractObject(URI object, Repository repository) throws Exception {
        RepositoryConnection cFrom = null, cTo = null;
        try {
            cFrom = getRepository().getConnection();
            cTo = repository.getConnection();
            cTo.setAutoCommit(false);
            m_queryManager.copyItem(object, cFrom, cTo, new HashSet<String>());
            cTo.commit();
        } catch (RepositoryException e) {
            if (cTo != null) cTo.rollback();
        } finally {
            if (cTo != null) cTo.close();
            if (cFrom != null) cFrom.close(); 
        }
    }

    /**
     * Add the given RDF model to the corpus and notify all structured models of
     * that addition. If trusted, items of systemic types are flagged as
     * trusted; otherwise, all trusted flags are removed.
     * 
     * @param model
     * @param trusted
     */
    public void addData(Repository r, boolean trusted) throws RepositoryException {
        RepositoryConnection c = null;
        try {
            c = r.getConnection();
            Set<IStructuredModel> structuredModels = new HashSet<IStructuredModel>(m_structuredModels);
            try {
                if (trusted) {
                    getSystemModel().trustAllOfSystemicTypes((Repository) r);
                } else {
                    getSystemModel().filterOutTrustedItems((Repository) r);
                }

                onBeforeAdd(c, structuredModels);
                m_corpus.add(c);
                onAfterAdd(c, structuredModels);
            } catch (Exception e) {
                s_logger.error("Failed to add data to corpus " + m_corpus.getID(), e);
                onFailingAdd(c, structuredModels);
            }
        } catch (RepositoryException e) {
            // nothing to do here
        } finally {
            if (c != null) c.close();
        }
    }

    /**
     * Add RDF data read from the given file or directory hierarchy and notify
     * all structured models of that addition.
     * 
     * @param fileOrDir
     * @throws IOException
     */
    public void addData(File fileOrDir) throws Exception {
        if (fileOrDir.isFile()) {
            LongwellUtilities.loadDataFromFile(fileOrDir, this, true);
        } else {
            LongwellUtilities.loadDataFromDir(fileOrDir, this, true, true);
        }
    }

    /**
     * Remove the given RDF model from the corpus and notify all structured
     * models of that removal.
     * 
     * @param model
     */
    public void removeData(Repository r) throws RepositoryException {
        RepositoryConnection c = null;
        try {
            c = r.getConnection();
            Set<IStructuredModel> structuredModels = new HashSet<IStructuredModel>(m_structuredModels);
            try {
                onBeforeRemove(c, structuredModels);
                m_corpus.remove(c);
                onAfterRemove(c, structuredModels);
            } catch (Exception e) {
                s_logger.error("Failed to remove data from corpus " + m_corpus.getID(), e);
                onFailingRemove(c, structuredModels);
            }
        } catch (RepositoryException e) {
            // nothing to do here
        } finally {
            if (c != null) c.close();
        }
    }

    /**
     * Remove all data about the given object.
     * 
     * @param object
     * @throws Exception
     */
    public void removeObject(URI object) throws Exception {
        Repository r = Utilities.createMemoryRepository();
        try {
            extractObject(object, r);
            removeData(r);
        } finally {
            r.shutDown();
        }
    }

    protected void onBeforeAdd(RepositoryConnection c, Set structuredModels) {
        try {
            Iterator i = structuredModels.iterator();
            while (i.hasNext()) {
                IStructuredModel sModel = (IStructuredModel) i.next();
                sModel.onBeforeAdd(c);
            }
        } catch (Exception e) {
            s_logger.error(e);
        }
    }

    protected void onAfterAdd(RepositoryConnection c, Set structuredModels) {
        Iterator i = structuredModels.iterator();
        while (i.hasNext()) {
            IStructuredModel sModel = (IStructuredModel) i.next();
            sModel.onAfterAdd(c);
        }
    }

    protected void onFailingAdd(RepositoryConnection c, Set structuredModels) {
        Iterator i = structuredModels.iterator();
        while (i.hasNext()) {
            IStructuredModel sModel = (IStructuredModel) i.next();
            sModel.onFailingAdd(c);
        }
    }

    protected void onBeforeRemove(RepositoryConnection c, Set structuredModels) {
        Iterator i = structuredModels.iterator();
        while (i.hasNext()) {
            IStructuredModel sModel = (IStructuredModel) i.next();
            sModel.onBeforeRemove(c);
        }
    }

    protected void onAfterRemove(RepositoryConnection c, Set structuredModels) {
        Iterator i = structuredModels.iterator();
        while (i.hasNext()) {
            IStructuredModel sModel = (IStructuredModel) i.next();
            sModel.onAfterRemove(c);
        }
    }

    protected void onFailingRemove(RepositoryConnection c, Set structuredModels) {
        Iterator i = structuredModels.iterator();
        while (i.hasNext()) {
            IStructuredModel sModel = (IStructuredModel) i.next();
            sModel.onFailingRemove(c);
        }
    }
}
