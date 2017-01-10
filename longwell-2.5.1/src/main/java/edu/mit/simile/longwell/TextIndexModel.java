package edu.mit.simile.longwell;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.apache.lucene.analysis.SimpleAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.Hits;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.openrdf.model.Literal;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.repository.RepositoryConnection;

import edu.mit.simile.longwell.schema.SchemaModel;

public class TextIndexModel extends StructuredModelBase {

    final static private Logger s_logger = Logger.getLogger(TextIndexModel.class);

    protected IndexReader m_indexReader;
    protected IndexSearcher m_indexSearcher;
    protected boolean m_initialized = false;
    protected Cache m_results;
    protected File m_dir;

    public TextIndexModel(Profile profile, RepositoryFactory factory) {
        super(profile);
        m_dir = factory.getDir(RepositoryFactory.TEXT_INDEX);
    }

    public Set search(String text) {
        FixedSetBuilder builder = new FixedSetBuilder();

        try {
            org.apache.lucene.search.Query query = QueryParser.parse("+" + text, "search", new SimpleAnalyzer());

            Hits hits = m_indexSearcher.search(query);

            for (int j = 0; j < hits.length(); j++) {
                String uri = hits.doc(j).getField("uri").stringValue();

                builder.add(new URIImpl(uri));
            }
        } catch (Exception e) {
            s_logger.error(e);
        }
        
        return builder.buildFixedSet();
    }

    public void index(boolean regenerate) {
        try {
            internalInitialize(regenerate);
        } catch (Exception e) {
            s_logger.error(e);
        }
    }
    
    public void dispose() {
        try {
            closeReaderSearcher();
        } catch (Exception e) {
            s_logger.error(e);
        }
    }
    
    public void optimize() {
        try {
            closeReaderSearcher();
            IndexWriter writer = new IndexWriter(m_dir.getAbsolutePath(), new SimpleAnalyzer(), false);
            writer.optimize();
            writer.close();
            openReaderSearcher();
        } catch (Exception e) {
            s_logger.error(e);
        }
    }
    
    public void onAfterAdd(RepositoryConnection c) {
        try {
            internalOnAfterAdd(c, false);
        } catch (Exception e) {
            s_logger.error(e);
        }
    }

    public void onAfterRemove(RepositoryConnection c) {
        internalOnAfterRemove(c);
    }

    protected void internalInitialize(boolean regenerate) {
        if (m_initialized) {
            return;
        }

        if (s_logger.isDebugEnabled()) s_logger.debug("> internalInitialize");
        try {
            if (!m_dir.exists() || regenerate) {
                m_dir.mkdirs();

                if (s_logger.isDebugEnabled()) s_logger.debug("> populating fulltext index");
                RepositoryConnection c = null;
                try {
                    c = m_profile.getRepository().getConnection();
                    internalOnAfterAdd(c, true);
                } finally {
                    if (c != null) c.close();
                }
                if (s_logger.isDebugEnabled()) s_logger.debug("< populating fulltext index");
            } else {
                openReaderSearcher();
            }
        } catch (Exception e) {
            s_logger.error("Failed to initialize fulltext model", e);
        }

        m_initialized = true;
        if (s_logger.isDebugEnabled()) s_logger.debug("< internalInitialize");
    }

    protected void internalOnAfterAdd(RepositoryConnection c, boolean create) throws IOException {
        if (s_logger.isDebugEnabled()) s_logger.debug("> internalOnAfterAdd");
        try {
            closeReaderSearcher();

            IndexWriter writer = new IndexWriter(m_dir.getAbsolutePath(), new SimpleAnalyzer(), create);

            Iterator i = m_queryManager.listSubjectsOfProperty(c, RDF.TYPE).iterator();
            while (i.hasNext()) {
                URI object = (URI) i.next();
                internalAddObject(c, writer, object);
            }

            writer.close();
        } catch (Exception e) {
            s_logger.error(e);
        } finally {
            openReaderSearcher();
        }
        if (s_logger.isDebugEnabled()) s_logger.debug("< internalOnAfterAdd");
    }

    protected void internalOnAfterRemove(RepositoryConnection c) {
        if (s_logger.isDebugEnabled()) s_logger.debug("> internalOnAfterRemove");
        try {
            Iterator i = m_queryManager.listSubjectsOfProperty(c, RDF.TYPE).iterator();
            while (i.hasNext()) {
                URI object = (URI) i.next();
                internalRemoveObject(object);
            }
        } catch (Exception e) {
            s_logger.error(e);
        }
        if (s_logger.isDebugEnabled()) s_logger.debug("< internalOnAfterRemove");
    }

    protected void internalAddObject(RepositoryConnection c, IndexWriter writer, URI object) throws Exception {
        SchemaModel schemaModel = m_profile.getSchemaModel();

        Document doc = new Document();
        doc.add(Field.Keyword("uri", object.toString()));

        Map<URI,Set<Value>> m = m_queryManager.mapForwardProperties(c, object);

        if (m != null) {
            Iterator<URI> i = m.keySet().iterator();
            while (i.hasNext()) {
                URI property = i.next();
                Set<Value> values = m.get(property);
                for (Value v : values) {
                    if (v instanceof Literal) {
                        doc.add(Field.Text("search", ((Literal) v).getLabel()));
                    } else if (v instanceof URI) {
                        doc.add(Field.Text("search", schemaModel.getLabel((URI) v, "")));
                    }
                }
            }
            writer.addDocument(doc);
        }
    }

    protected void internalRemoveObject(URI object) {
        try {
            Query query = QueryParser.parse("+" + object.toString(), "uri", new SimpleAnalyzer());
            Hits hits = m_indexSearcher.search(query);

            if (hits.length() > 0) {
                m_indexReader.delete(hits.id(0));
            }
        } catch (Exception e) {
            s_logger.error("Failed to remove object " + object, e);
        }
    }

    protected void openReaderSearcher() throws IOException {
        if (s_logger.isDebugEnabled()) s_logger.debug("> open index reader");
        m_indexReader = IndexReader.open(m_dir.getAbsolutePath());
        m_indexSearcher = new IndexSearcher(m_indexReader);
        if (s_logger.isDebugEnabled()) s_logger.debug("< open index reader");
    }

    protected void closeReaderSearcher() throws IOException {
        if (s_logger.isDebugEnabled()) s_logger.debug("> close index reader");
        if (m_indexReader != null) {
            m_indexReader.close();
            m_indexReader = null;
        }
        if (m_indexSearcher != null) {
            m_indexSearcher.close();
            m_indexSearcher = null;
        }
        if (s_logger.isDebugEnabled()) s_logger.debug("< close index reader");
    }
}
