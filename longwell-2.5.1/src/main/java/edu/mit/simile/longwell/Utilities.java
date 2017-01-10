package edu.mit.simile.longwell;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import org.apache.commons.codec.net.URLCodec;
import org.apache.log4j.Logger;
import org.openrdf.model.BNode;
import org.openrdf.model.Literal;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.impl.LiteralImpl;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.rio.ParseErrorListener;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFHandler;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFParser;
import org.openrdf.rio.RDFWriter;
import org.openrdf.rio.n3.N3Writer;
import org.openrdf.rio.ntriples.NTriplesParser;
import org.openrdf.rio.ntriples.NTriplesWriter;
import org.openrdf.rio.rdfxml.RDFXMLParser;
import org.openrdf.rio.rdfxml.RDFXMLWriter;
import org.openrdf.rio.turtle.TurtleParser;
import org.openrdf.rio.turtle.TurtleWriter;
import org.openrdf.sail.Sail;
import org.openrdf.sail.memory.MemoryStore;
import org.openrdf.sail.nativerdf.NativeStore;

public class Utilities {

    final static public String s_bnodePrefix = "urn:bnode:";

    final static protected Logger s_logger = Logger.getLogger(Utilities.class);

    static public Repository createMemoryRepository() {
        try {
            Repository r = new SailRepository(new MemoryStore());
            r.initialize();
            return r;
        } catch (Exception e) {
            s_logger.error(e);
            return null;
        }
    }

    static public Repository createNativeRepository(File dir) {
        try {
            Sail sail = new NativeStore();
            sail.setDataDir(dir);
            ((NativeStore) sail).setTripleIndexes("spoc,posc,opsc");
            Repository r = new SailRepository(sail);
            r.initialize();
            return r;
        } catch (Exception e) {
            s_logger.error(e);
            return null;
        }
    }
    
    static public double parseDouble(String s, double def) {
        try {
            return Double.parseDouble(s);
        } catch (Exception e) {
            return def;
        }
    }
    
    static public long parseLong(String s, long def) {
        try {
            return Long.parseLong(s);
        } catch (Exception e) {
            return def;
        }
    }

    static public String fileToModelLang(File file) {
        return filenameToLang(file.getName());
    }

    static public String urlToModelLang(URL url, String contentType) {
        return urlToModelLang(url.getPath(), contentType);
    }

    static public String urlToModelLang(String url, String contentType) {
        String lang = null;

        if (contentType != null) {
            lang = contentTypeToLang(contentType);
        }
        if (lang == null) {
            lang = filenameToLang(url);
        }

        if (s_logger.isDebugEnabled()) s_logger.debug(url + " -> " + lang);
        return lang;
    }

    static public String contentTypeToLang(String contentType) {
        String lang = null;
        if ("application/rss+xml".equals(contentType) || "application/atom+xml".equals(contentType)) {
            lang = "RSS";
        } else if ("application/rdf+xml".equals(contentType) || "text/xml".equals(contentType)) {
            lang = "RDFXML";
        } else if ("application/n3".equals(contentType) || "text/rdf+n3".equals(contentType)
                || "application/turtle".equals(contentType) || "application/x-turtle".equals(contentType)) {
            lang = "N3";
        }
        if (s_logger.isDebugEnabled()) s_logger.debug(contentType + " -> " + lang);
        return lang;
    }

    static public String filenameToLang(String filename) {
        String contentType = URLConnection.guessContentTypeFromName(filename);
        String lang = null;

        if (contentType != null) {
            lang = contentTypeToLang(contentType);
        }

        if (lang == null) {
            if (filename.endsWith(".gz")) {
                filename = filename.substring(0, filename.length() - ".gz".length());
            }
            if (filename.endsWith(".n3")) {
                lang = "N3";
            } else if (filename.endsWith(".turtle")) {
                lang = "TURTLE";
            } else if (filename.endsWith(".ntriples")) {
                lang = "NTRIPLES";
            } else if (filename.endsWith(".rss")) {
                lang = "RSS";
            } else if (filename.endsWith(".rdf") 
                    || filename.endsWith(".rdfs") 
                    || filename.endsWith(".xml")
                    || filename.endsWith(".owl")) {
                lang = "RDFXML";
            }
        }

        return lang;
    }

    static public RDFFormat langToFormat(String lang) {
        return RDFFormat.valueOf(lang);
    }

    static public String uriToFilename(String uri) {
        return uri.replace(':', '_').replace('/', '_');
    }

    static public void loadDataFromDir(File dir, Repository repository, boolean forgiving) throws Exception {

        if (!dir.exists()) {
            throw new FileNotFoundException("Cannot load data from " + dir.getAbsolutePath());
        }

        File[] files = dir.listFiles();

        for (int i = 0; i < files.length; i++) {
            File file = files[i];

            if (!file.isHidden()) {
                if (file.isDirectory()) {
                    loadDataFromDir(file, repository, forgiving);
                } else {
                    if (forgiving) {
                        try {
                            loadDataFromFile(file, repository);
                        } catch (Exception e) {
                            s_logger.warn("Failed to load data from " + file.getCanonicalPath(), e);
                        }
                    } else {
                        loadDataFromFile(file, repository);
                    }
                }
            }
        }
    }

    static public InputStream getStreamForFile(File file) throws Exception {
        InputStream stream = new FileInputStream(file);
        String name = file.getName();
        if (name.endsWith(".gz")) {
            stream = new GZIPInputStream(stream);
        }
        return stream;
    }
    
    static public void loadDataFromFile(File file, Repository repository) throws Exception {
        String lang = fileToModelLang(file);
        if (lang != null) {
            InputStream fis = getStreamForFile(file);
            try {
                loadDataFromStream(fis, file.toURL().toExternalForm(), lang, repository);
            } catch (Exception e) {
                throw new RuntimeException("Error loading data from file: " + file + " " + e.getMessage());
            } finally {
                fis.close();
            }
        } else {
            throw new ModelReadFromFileException("Unknown data format in " + file.getAbsolutePath());
        }
    }

    static public void loadDataFromConnection(URLConnection conn, URL url, String lang, String contentType,
            Repository repository) throws Exception {
        if (lang != null) {
            InputStream stream = conn.getInputStream();
            try {
                loadDataFromStream(stream, url.toExternalForm(), lang, repository);
            } catch (Exception e) {
                throw new RuntimeException("Error loading data from URL: " + url + " " + e.getMessage());
            } finally {
                stream.close();
            }
        } else {
            throw new ModelReadFromFileException("Unknown data format in " + url.toExternalForm());
        }
    }

    static public void loadDataFromURL(URL url, String contentType, Repository repository) throws Exception {
        String lang = urlToModelLang(url, contentType);
        if (lang != null) {
            URLConnection conn = url.openConnection();
            setRequestHeaders(conn, LongwellUtilities.getLabel() + "/" + LongwellUtilities.getVersion());
            conn.connect();
            InputStream stream = conn.getInputStream();
            try {
                loadDataFromStream(stream, url.toExternalForm(), lang, repository);
            } catch (Exception e) {
                throw new RuntimeException("Error loading data from URL: " + url + " " + e.getMessage());
            } finally {
                stream.close();
            }
        } else {
            throw new ModelReadFromFileException("Unknown data format in " + url.toExternalForm());
        }
    }

    static public void setRequestHeaders(URLConnection conn, String ua) {
        conn.setRequestProperty("User-Agent", ua);
        conn.setRequestProperty("Accept", "application/rdf+xml, text/rdf+n3");
    }

    static public void loadDataFromStream(InputStream stream, String sourceURL, String lang, Repository repository) throws Exception {

        Repository r = createMemoryRepository();

        try {
            RDFParser parser = null;
            lang = lang.toLowerCase();
            if ("rdfxml".equals(lang)) {
                parser = new RDFXMLParser(r.getValueFactory());
            } else if ("n3".equals(lang) || "turtle".equals(lang)) {
                parser = new TurtleParser(r.getValueFactory());
            } else if ("ntriples".equals(lang)) {
                parser = new NTriplesParser(r.getValueFactory());
            }

            RepositoryConnection c = null;
            try {
                c = repository.getConnection();
                c.setAutoCommit(false);
                BNodeConverterStatementHandler handler = new BNodeConverterStatementHandler(c);

                parser.setRDFHandler(handler);
                parser.setParseErrorListener(new LoggingParseErrorListener(sourceURL));
                parser.setVerifyData(false);
                parser.setStopAtFirstError(false);

                parser.parse(stream, sourceURL);
                
                c.commit();

                s_logger.info("Read " + handler.m_count + " statements from '" + sourceURL + "'");
            } catch (RepositoryException e) {
                if (c != null) c.rollback();
            } finally {
                if (c != null) c.close();
            }

        } catch (Exception e) {
            throw new ModelReadFromFileException("Failed to read data from '" + sourceURL + "'", e);
        } finally {
            stream.close();
        }
    }

    static public void writeRDF(Repository r, OutputStream os, String originalFormat) throws Exception {

        String format = originalFormat.toLowerCase();
        RDFWriter rdfWriter = null;
        if ("rdfxml".equals(format)) {
            rdfWriter = new RDFXMLWriter(os);
        } else if ("ntriples".equals(format)) {
            rdfWriter = new NTriplesWriter(os);
        } else if ("n3".equals(format)) {
            rdfWriter = new N3Writer(os);
        } else if ("turtle".equals(format)) {
            rdfWriter = new TurtleWriter(os);
        } else { // default to RDF/XML
            rdfWriter = new RDFXMLWriter(os);
        }

        RepositoryConnection conn = null;
        try {
            conn = r.getConnection();
            conn.exportStatements(null, null, null, true, rdfWriter);
        } finally {
            if (conn != null) conn.close();
        }
    }

    static public File uriToFile(String uri) throws URISyntaxException {
        int percentU = uri.indexOf("%u");
        if (percentU >= 0) {
            StringBuffer sb = new StringBuffer(uri.length());

            int start = 0;
            while (percentU > 0) {
                sb.append(uri.substring(start, percentU));

                char c = (char) Integer.parseInt(uri.substring(percentU + 2, percentU + 6), 16);

                sb.append(c);

                start = percentU + 6;
                percentU = uri.indexOf("%u", start);
            }

            sb.append(uri.substring(start));
            uri = sb.toString();
        }
        return new File(new java.net.URI(uri));
    }

    static public void deleteDirectory(File dir) {
        if (dir.exists()) {
            deleteDirectoryContent(dir);
            dir.delete();
        }
    }

    static public void deleteDirectoryContent(File dir) {
        File[] files = dir.listFiles();
        if (dir.isDirectory() && files != null) {
            for (int i = 0; i < files.length; i++) {
                File file = files[i];
                if (file.isDirectory()) {
                    deleteDirectory(file);
                } else {
                    file.delete();
                }
            }
        }
    }

    static public boolean isBNode(URI uri) {
        return uri.toString().startsWith(s_bnodePrefix);
    }

    static public Coordinates parseCoordinates(String s) {
            if (null == s) return null;
            String[] coords = s.split(",");
            if (coords.length != 2) {
                return null;
            }
            double lat, lng;
            try {
                lat = Double.parseDouble(coords[0]);
                lng = Double.parseDouble(coords[1]);
            } catch (Exception e) {
                return null;
            }
            Coordinates c = new Coordinates(lat, lng);
            return c;
    }
    
    static public Date parseDate(String s) {
        Date d = null;
        
        if (s != null) {
        
            int length = s.length();
    
            if (length == 25 || (length == 20 && s.charAt(19) == 'Z')) {
    
                char c = s.charAt(length - 1);
                int endOfTime;
        
                if (c == 'Z' || c == 'z') {
                    endOfTime = length - 1;
                } else {
                    endOfTime = 19;
                }
        
                try {
                    d = new SimpleDateFormat("yyyy-MM-dd'T'H:m:s").parse(s.substring(0, endOfTime));
                } catch (ParseException e) {
                    return null;
                }
        
                if (endOfTime < length - 1) {
                    int sign = s.charAt(endOfTime) == '+' ? -1 : 1;
                    int hour = Integer.parseInt(s.substring(endOfTime + 1, endOfTime + 3));
                    int minute = Integer.parseInt(s.substring(endOfTime + 4));
        
                    d = new Date(d.getTime() + sign * (hour * 60 + minute) * 60000);
                }
            } else if (length == 10) {
                try {
                    d = new SimpleDateFormat("yyyy-MM-dd").parse(s);
                } catch (ParseException e) {
                    return null;
                }
            }
        }

        return d;
    }

    static public String unparseDate(Date d) {
        // return d != null ? XSDDatatype.XSDdateTime.unparse(d) : null;

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
        String s = sdf.format(d);
        int l = s.length() - 2;

        return s.substring(0, l) + ":" + s.substring(l);
    }

    static public String reformatDate(String s) {
        Date d = parseDate(s);
        if (d != null) {
            return formatDate(d);
        }
        return s;
    }

    static public String formatDate(Date d) {
        return d == null ? null : DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.LONG).format(d);
    }

    static public String abbreviateURI(String uri) {
        int index = uri.lastIndexOf('#') + 1;
        if (index < 1) {
            index = uri.lastIndexOf('/') + 1;
        }
        if (index == uri.length()) {
            return uri;
        }
        return uri.substring(index);
    }

    /**
     * Given a string, convert all instances of [toEscape] to the
     * sequence [escaping][escaped]. Also convert instances of
     * [escaping] to [escaping][escaping].
     *
     * Thus
     *   escape("ten $ = a sawbuck", '$', '=', 'd')
     * returns 
     *   "ten =d == a sawbuck".
     */
    static public String escape(String s, char toEscape, char escaping, char escaped) {
        StringBuffer sb = new StringBuffer();
        char[] chars = s.toCharArray();

        for (int i = 0; i < chars.length; i++) {
            char c = chars[i];
            if (c == escaping) {
                sb.append(c).append(c);
            } else if (c == toEscape) {
                sb.append(escaping).append(escaped);
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * Inverse of escape().
     */
    static public String unescape(String s, char toEscape, char escaping, char escaped) {
        StringBuffer sb = new StringBuffer();
        char[] chars = s.toCharArray();

        for (int i = 0; i < chars.length; i++) {
            char c = chars[i];
            if (c == escaping && i < chars.length + 1) {
                char c2 = chars[i + 1];
                if (c2 == escaping) {
                    sb.append(escaping);
                    i++;
                } else if (c2 == escaped) {
                    sb.append(toEscape);
                    i++;
                } else {
                    sb.append(c);
                }
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static final String URL_ENCODING = "UTF-8";

    private static final URLCodec codec = new URLCodec();

    static public String encode(String s) {
        try {
            return codec.encode(s, URL_ENCODING);
        } catch (Exception e) {
            throw new RuntimeException("Exception encoding " + s + " with " + URL_ENCODING + " encoding.");
        }
    }

    static public String decode(String s) {
        try {
            return codec.decode(s, URL_ENCODING);
        } catch (Exception e) {
            throw new RuntimeException("Exception decoding " + s + " with " + URL_ENCODING + " encoding.");
        }
    }

    static class ModelReadFromFileException extends IOException {
        private static final long serialVersionUID = -5802084055919147883L;

        ModelReadFromFileException(String s) {
            super(s);
        }

        ModelReadFromFileException(String s, Throwable e) {
            super(s);
            initCause(e);
        }
    }

    static class BNodeConverterStatementHandler implements RDFHandler {

        RepositoryConnection m_connection;
        
        long m_count;

        URI m_uri = null;

        Map<String,URI> m_bnodeIDToURI = new HashMap<String,URI>();

        BNodeConverterStatementHandler(RepositoryConnection conn) throws RepositoryException {
            m_connection = conn;
        }

        public void handleStatement(Statement st, Resource context) throws RDFHandlerException {
            Resource s = st.getSubject();
            URI p = st.getPredicate();
            Value o = st.getObject();
            if (s instanceof BNode) {
                String sid = ((BNode) s).getID();
                s = (URI) m_bnodeIDToURI.get(sid);
                if (s == null) {
                    s = addBNode(sid);
                }
            } else {
                m_uri = (URI) s;
            }

            if (o instanceof BNode) {
                String oid = ((BNode) o).getID();
                o = (URI) m_bnodeIDToURI.get(oid);
                if (o == null) {
                    o = addBNode(oid);
                }
            }

            try {
                m_connection.add(s, p, o);
                m_count++;
            } catch (RepositoryException e) {
                s_logger.error(e);
            }
        }

        URI addBNode(String bnode) {
            URI uri = new URIImpl(s_bnodePrefix
                    + (TagModel.getTagSuffixHash() != null ? TagModel.getTagSuffixHash() + ":" : "")
                    + (m_uri != null ? m_uri.toString() + ":" : "") + System.currentTimeMillis() + ":" + bnode);

            m_bnodeIDToURI.put(bnode, uri);

            return uri;
        }

        public void startRDF() throws RDFHandlerException {
            try {
                m_connection.setAutoCommit(false);
            } catch (RepositoryException e) {
                throw new RDFHandlerException(e);
            }
        }

        public void endRDF() throws RDFHandlerException {
            try {
                m_connection.commit();
                m_connection.setAutoCommit(true);
            } catch (RepositoryException e) {
                try {
                    m_connection.rollback();
                } catch (Exception ee) {
                    // ignore
                }
                throw new RDFHandlerException(e);
            }
        }

        public void handleNamespace(String arg0, String arg1) throws RDFHandlerException {
            // 
        }

        public void handleComment(String arg0) throws RDFHandlerException {
            // 
        }

        public void handleStatement(Statement arg0) throws RDFHandlerException {
            handleStatement(arg0, null);
        }
    }
        
    static class LoggingParseErrorListener implements ParseErrorListener {
        String m_source;

        LoggingParseErrorListener(File file) {
            m_source = file.getAbsolutePath();
        }

        LoggingParseErrorListener(URL url) {
            m_source = url.toExternalForm();
        }

        LoggingParseErrorListener(String source) {
            m_source = source;
        }

        public void warning(String msg, int line, int column) {
            s_logger.warn("Warning: " + msg + " at " + m_source + " [" + line + "," + column + "]");
        }

        public void error(String msg, int line, int column) {
            s_logger.error("Error: " + msg + " at " + m_source + " [" + line + "," + column + "]");
        }

        public void fatalError(String msg, int line, int column) {
            s_logger.error("Fatal error: " + msg + " at " + m_source + " [" + line + "," + column + "]");
        }
    }
    
    public static URI dupURI(URI uri) {
        return new URIImpl(uri.toString());
    }
    
    public static Value dupValue(Value v) {
        if (v instanceof Literal) {
            return new LiteralImpl(((Literal) v).getLabel());
        } else {
            return dupURI((URI) v);
        }
    }
    
}
