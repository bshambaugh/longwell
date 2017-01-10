package edu.mit.simile.longwell;

import org.apache.log4j.Logger;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.util.RDFInserter;
import org.openrdf.repository.util.RDFRemover;

public class Corpus {

    final static private Logger s_logger = Logger.getLogger(Corpus.class);

    final protected String m_id;
    final protected RepositoryFactory m_factory;
    final protected int m_type;

    protected boolean m_initialized = false;

    protected Repository m_repository;

    public Corpus(String id, RepositoryFactory factory, int type) {
        m_id = id;
        m_factory = factory;
        m_type = type;
    }

    private void initialize() {
        if (!m_initialized) {
            try {
                m_repository = m_factory.getRepository(m_type);
                m_initialized = true;
            } catch (Exception e) {
                s_logger.error("Error creating the default repository");
            }
        }
    }
    
    public void dispose() {
        if (m_initialized) {
            try {
                m_repository.shutDown();
                m_initialized = false;
            } catch (Exception e) {
                s_logger.error("Error disposing the default repository", e);
            }
        }
    }

    public String getID() {
        return m_id;
    }

    public Repository getRepository() {
        initialize();
        return m_repository;
    }

    public void add(RepositoryConnection conn) throws Exception {
        initialize();
        RepositoryConnection m_conn = null;
        try {
            m_conn = m_repository.getConnection();
            m_conn.setAutoCommit(false);
            RDFInserter handler = new RDFInserter(m_conn);
            conn.exportStatements(null, null, null, true, handler);
            m_conn.commit();
        } catch (RepositoryException e) {
            if (m_conn != null) m_conn.rollback();
        } finally {
            if (m_conn != null) m_conn.close();
        }
    }

    public void remove(RepositoryConnection conn) throws Exception {
        initialize();
        RepositoryConnection m_conn = null;
        try {
            m_conn = m_repository.getConnection();
            m_conn.setAutoCommit(false);
            RDFRemover handler = new RDFRemover(m_conn);
            conn.exportStatements(null, null, null, true, handler);
            m_conn.commit();
        } catch (RepositoryException e) {
            if (m_conn != null) m_conn.rollback();
        } finally {
            if (m_conn != null) m_conn.close();
        }
    }

    public void removeAll() throws Exception {
        initialize();
        RepositoryConnection conn = null;
        try {
            conn = m_repository.getConnection();
            conn.setAutoCommit(false);
            conn.clear();
            conn.commit();
        } catch (RepositoryException e) {
            if (conn != null) conn.rollback();
        } finally {
            if (conn != null) conn.close();
        }
    }

}
