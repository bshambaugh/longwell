package edu.mit.simile.longwell;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Constructor;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.transform.Templates;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamSource;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;

import com.janrain.openid.store.MemoryStore;
import com.janrain.openid.store.OpenIDStore;

import edu.mit.simile.longwell.command.Command;
import edu.mit.simile.longwell.query.Query;
import edu.mit.simile.longwell.renderer.MapMarkerRenderer;
import edu.mit.simile.longwell.renderer.Renderer;
import edu.mit.simile.velocity.Inject;
import edu.mit.simile.velocity.InjectionManager;

/**
 * The web-server component of Longwell. This gathers configuration
 * information (mostly from Java properties), and then creates a
 * Longwell object.
 *
 * It then accepts HTTP requests (see doGet() / doPost()) and
 * dispatches them appropriately. Some requests return simple
 * documents; others turn into Longwell commands and are handed off to
 * the Longwell object.
 *
 * (The configuration.properties file determines what Commands are
 * available by GET and POST.)
 */
public class LongwellServlet extends HttpServlet {

    final static long serialVersionUID = -2778565755928786481L;

    final static public String ID = "longwell";
    final static public String NAMESPACE = "http://simile.mit.edu/2005/04/flair#";
    final static public String BASE_URI = "http://127.0.0.1:8080/";
    final static public String ENCODING = "UTF-8";

    final static private Logger s_logger = Logger.getLogger(LongwellServlet.class);

    private static LongwellService s_longwell;
    
    private ClassLoader m_classLoader;
    private Configuration m_configuration;
    private Properties m_properties = new Properties();
    private InjectionManager m_injections = new InjectionManager();
    private CacheFactory m_cacheFactory;
    private VelocityEngine m_ve;
    private Map<String,Command> m_getCommands = new HashMap<String,Command>();
    private Map<String,Command> m_postCommands = new HashMap<String,Command>();
    private ServletContext m_context;
    private File m_homeDir;
    private File m_rootDir;
    private List<File> m_configurationPaths = new ArrayList<File>();
    private String m_proxyContextPath = null;
    private boolean m_httpcaching = false;
    private OpenIDStore m_openIDStore = null;
    
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        m_context = config.getServletContext();
        m_homeDir = new File(m_context.getRealPath("/"));
        
        // Finding the right common root
        File[] roots = File.listRoots();
        m_rootDir = roots[0];        
        String homePath = m_homeDir.getAbsolutePath();
        for (int r = 1; r < roots.length; r++) {
            if (homePath.startsWith(roots[r].getAbsolutePath())) {
                m_rootDir = roots[r];
                break;
            }
        }
        
        File webInfDir = new File(m_homeDir, "WEB-INF");
        File logProperties = new File(webInfDir, "log4j.properties");
        
        if (logProperties.exists() && (System.getProperty("longwell.log.skipInitialization") == null)) {
            PropertyConfigurator.configure(logProperties.getAbsolutePath());
        }

        m_classLoader = new LongwellClassLoader(Thread.currentThread().getContextClassLoader(), new Trigger(m_homeDir));
        Thread.currentThread().setContextClassLoader(m_classLoader);

        if (s_logger.isDebugEnabled()) s_logger.debug("> init");
        
        // Load the longwell properties
        s_logger.info("> load longwell properties");
        try {
            m_configuration = load(m_properties, m_injections, m_classLoader);
            if (s_logger.isInfoEnabled()) s_logger.info("Starting configuration: " + m_configuration.getName());
        } catch (FileNotFoundException e) {
            s_logger.error("Could not find longwell configuration", e);
            throw new ServletException("Could not find longwell configuration", e);
        } catch (Exception e) {
            s_logger.error("Failed to load longwell properties", e);
            throw new ServletException("Failed to load longwell properties", e);
        }

        m_proxyContextPath = m_properties.getProperty("longwell.url");
        if (s_logger.isDebugEnabled()) s_logger.debug("Proxy context URL: " + m_proxyContextPath);

        m_httpcaching = Boolean.parseBoolean(m_properties.getProperty("longwell.http.caching"));
        if (s_logger.isDebugEnabled()) s_logger.debug("HTTP Caching? " + m_httpcaching);

        s_logger.info("< load longwell properties");

        // Load the OpenID store
        s_logger.info("> load OpenID store");
        File openIDStoreFile = new File(webInfDir, "openid");
        setOpenIDStore(readSavedOpenIDStore(openIDStoreFile));
        s_logger.info("< load OpenID store");
        
        // Initalize the Fresnel configurations
        s_logger.info("> initialize fresnel");
        Configuration configuration = m_configuration;
        Collection<File> fresnelConfigs = new ArrayList<File>();
        File f = new File(webInfDir, "fresnel-defaults.n3");
        if (s_logger.isDebugEnabled()) s_logger.debug("adding to fresnel configurations: " + f.getAbsolutePath());
        if (f.exists()) fresnelConfigs.add(f);

        do {
            f = new File(configuration.getLocation(),"fresnel.n3");
            if (f.exists()) fresnelConfigs.add(f);
            if (s_logger.isDebugEnabled()) s_logger.debug("adding to fresnel configurations: " + f.getAbsolutePath());
            configuration = configuration.getParent();
        } while (configuration != null) ;
        s_logger.info("< initialize fresnel");

        // Parse the transformations
        s_logger.info("> initialize XSLT transformations");
        configuration = m_configuration;
        Map<String, Templates> transformations = new HashMap<String,Templates>();

        FilenameFilter filter = new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.endsWith(".xslt") || name.endsWith(".xsl");
            }
        };

        TransformerFactory tFactory = TransformerFactory.newInstance();
        
        do {
            f = new File(configuration.getLocation(),"transformations");
            if (f.exists()) {
                File[] stylesheets = f.listFiles(filter);
                for (int i = 0; i < stylesheets.length; i++) {
                    String name = stylesheets[i].getName();
                    String key = name.substring(0,name.lastIndexOf('.'));
                    try {
                        Templates templates = tFactory.newTemplates(new StreamSource(stylesheets[i]));
                        if (!transformations.containsKey(key)) {
                            transformations.put(key, templates);
                            if (s_logger.isDebugEnabled()) s_logger.debug("adding to transformation: " + stylesheets[i].getAbsolutePath());
                        }
                    } catch (Exception e) {
                        s_logger.error("error parsing transformations: " + stylesheets[i].getAbsolutePath(), e);
                    }
                }
            }
            configuration = configuration.getParent();
        } while (configuration != null) ;
        s_logger.info("< initialize XSLT transformations");
        
        // Initialize Velocity
        s_logger.info("> initialize velocity");
        try {
            Properties velocityProperties = new Properties();
            
            // Templates
            velocityProperties.setProperty(
                    RuntimeConstants.FILE_RESOURCE_LOADER_PATH, 
                    m_rootDir.getAbsolutePath());

            // The Macros in WEB-INF take precedence
            String s = "";
            File[] files = new File(webInfDir, "macros").listFiles();

            if (files != null) {
                for (int i = 0; i < files.length; i++) {
                    File file = files[i];
                    if (!file.isHidden() && file.getName().endsWith(".vm")) {
                        s = s + (s.length() == 0 ? "" : ",") + makeRelativePath(file, m_rootDir);
                    }
                }
            }

            Configuration c = m_configuration;
            do {
                files = new File(c.getLocation(), "macros").listFiles();
                if (files != null) {
                    for (int i = 0; i < files.length; i++) {
                        File file = files[i];
                        if (!file.isHidden() && file.getName().endsWith(".vm")) {
                            s = s + (s.length() == 0 ? "" : ",") + makeRelativePath(file, m_rootDir);
                        }
                    }
                }
                c = c.getParent();
            } while (c != null) ;

            if (s_logger.isDebugEnabled()) s_logger.debug("Velocity macro path: " + s);

            velocityProperties.setProperty(RuntimeConstants.VM_LIBRARY, s);

            // Set our special injection directive
            velocityProperties.setProperty("userdirective", Inject.class.getName());

            // Other properties
            FileInputStream fis = new FileInputStream(new File(webInfDir, "velocity.properties"));
            velocityProperties.load(fis);
            fis.close();

            m_ve = new VelocityEngine();
            m_ve.init(velocityProperties);
        } catch (Exception e) {
            s_logger.error("Failed to initialize Velocity engine", e);
            throw new ServletException("Failed to initialize Velocity engine", e);
        }

        for (Iterator i = m_properties.keySet().iterator(); i.hasNext(); ) {
            String key = (String) i.next();
            if (key.startsWith("longwell.command.")) {
                String subkey = key.substring("longwell.command.".length());
                if (subkey.startsWith("get.")) {
                    if (!subkey.endsWith(".template")) {
                        String commandName = subkey.substring("get.".length()); 
                        String className = m_properties.getProperty(key);
                        String templateKey = key + ".template";
                        Command command = null;
                        try {
                            String template = m_properties.getProperty(templateKey);
                            command = createCommand(className, template);
                            if (s_logger.isDebugEnabled()) s_logger.debug("GET command '" + commandName + "' : " + className + " " + template);
                            m_getCommands.put(commandName, command);
                        } catch (Exception e) {
                            s_logger.warn("Error constructing GET command '" + commandName + "'", e);
                        }
                    }
                } else if (subkey.startsWith("post.")) {
                    if (!subkey.endsWith(".template")) {
                        String commandName = subkey.substring("post.".length()); 
                        String className = m_properties.getProperty(key);
                        String templateKey = key + ".template";
                        Command command = null;
                        try {
                            String template = m_properties.getProperty(templateKey);
                            command = createCommand(className, template);
                            if (s_logger.isDebugEnabled()) s_logger.debug("POST command '" + commandName + "' : " + className + " " + template);
                            m_postCommands.put(commandName, command);
                        } catch (Exception e) {
                            s_logger.warn("Error constructing POST command '" + commandName, e);
                        }
                    }
                } else {
                    s_logger.error("Only GET and POST methods can be instrumented at this time");
                }
            }
        }
        s_logger.info("< initialize velocity");
                
        // Set the Tag ID
        TagModel.setIDForGeneratingURIs(m_properties.getProperty("longwell.tag.id", ""));
        
        // Set locale defaults
        String language = m_properties.getProperty("longwell.locale.language");
        String country = m_properties.getProperty("longwell.locale.country");
        String variant = m_properties.getProperty("longwell.locale.variant");
        if (language != null) {
            if (country != null) {
                if (variant != null) {
                    Locale.setDefault(new Locale(language, country, variant));
                } else {
                    Locale.setDefault(new Locale(language, country));
                }
            } else {
                Locale.setDefault(new Locale(language));
            }
        }

        String timeZone = m_properties.getProperty("longwell.timeZone");
        if (timeZone != null) {
            TimeZone.setDefault(TimeZone.getTimeZone(timeZone));
        }

        // Get the database location
        File databaseDir = absolutize(m_properties.getProperty("longwell.store.dir","WEB-INF/database"));
        
        // Create the repository factory
        RepositoryFactory repoFactory = new RepositoryFactory(m_properties, databaseDir);

        // Create the cache factory
        m_cacheFactory = new CacheFactory(m_properties);
        
        // Create default Corpus
        Corpus corpus = null;
        try {
            corpus = new Corpus("default", repoFactory, RepositoryFactory.MAIN);
        } catch (Exception e) {
            s_logger.error("Failed to create default corpus", e);
            throw new ServletException("Failed to create default corpus", e);
        }
        
        String baseURI = m_properties.getProperty("longwell.baseURI",BASE_URI);
        
        s_logger.info("> initialize the longwell engine");
        try {
            s_longwell = new Longwell(
                    baseURI,
                    corpus, 
                    m_cacheFactory, 
                    repoFactory,
                    databaseDir,
                    fresnelConfigs,
                    transformations,
                    m_properties
            );

            s_longwell.setFacetGuessing(Boolean.valueOf(m_properties.getProperty("longwell.facet.guessing", "false")).booleanValue());
        } catch (Exception e) {
            s_logger.error("Failed to create longwell server", e);
            throw new ServletException("Failed to create longwell server", e);
        }
        s_logger.info("< initialize the longwell engine");
        
        Profile defaultProfile = s_longwell.getDefaultProfile();
        
        // if not already initialize, prefill the triple store with necessary data
        s_logger.info("> load data into longwell");
        if (!isInitialized(defaultProfile.getRepository())) {
            try {
                // initialize with the default RDF that longwell needs to function
                File data = new File(webInfDir, "configurations.n3");
                defaultProfile.addData(data);
                // Initialize with the data that each configuration provides
                loadProfile(defaultProfile, m_configuration);
            } catch (Exception e) {
                s_logger.error("Failed to load RDF configurations", e);
                throw new ServletException("Failed to load RDF configurations", e);
            }
        }
        
        // if set, load initialization data
        if (m_properties.containsKey("longwell.data")) {
            String value = m_properties.getProperty("longwell.data");
            s_logger.info("Loading data from " + value);
            if (value != null && !"".equals(value)) {
                File data = new File(value);
                try {
                    defaultProfile.addData(data);
                } catch (Exception e) {
                    s_logger.error("Failed to load RDF data from " + data, e);
                    throw new ServletException("Failed to load RDF data from " + data, e);
                }
            }
        }
        s_logger.info("< load data into longwell");
        
        s_logger.info("> index the longwell data");
        
        // now that we have loaded the data, allow the structured models to index it
        defaultProfile.index(Boolean.parseBoolean(m_properties.getProperty("longwell.index.regenerate")));

        if (Boolean.getBoolean(m_properties.getProperty("longwell.index.optimize"))) {
            // optimize the profile after we have indexed it
            defaultProfile.optimize();
        }
        s_logger.info("< index the longwell data");
        
        s_logger.debug("< init");
    }
    
    public void destroy() {
        s_logger.debug("> destroy");
    	super.destroy();
        File openIDStoreFile = new File(m_homeDir, "WEB-INF/openid");
        saveOpenIDStore(openIDStoreFile);
    	((LongwellClassLoader) m_classLoader).dispose();
    	getLongwellService().dispose();
        s_logger.debug("< destroy");
    }

    public static LongwellService getLongwellService() {
        return s_longwell;
    }
    
    public static void setLongwellService(LongwellService longwell) {
    	s_longwell = longwell;
    }
    
    public long getLastModified(HttpServletRequest req) {
        if (m_httpcaching) {
            return m_cacheFactory.getLastModified() / 1000 * 1000;
        } else {
            return -1;
        }
    }

    public String getProxyContextPath() {
        return m_proxyContextPath;
    }
    
    public OpenIDStore getOpenIDStore() {
    	return m_openIDStore;
    }

    Pattern images_pattern = Pattern.compile("^/resources/.*\\.(jpg|gif|png)$");
    Pattern scripts_pattern = Pattern.compile("^/resources/.*\\.js$");
    Pattern styles_pattern = Pattern.compile("^/resources/.*\\.css$");
    Pattern content_pattern = Pattern.compile("^/resources/.*\\.html$");
    Pattern data_pattern = Pattern.compile("^/resources/.*\\.xml$");
    Pattern jar_pattern = Pattern.compile("^/resources/.*\\.jar$");
    Pattern stylesheets_pattern = Pattern.compile("^(/resources/)(.*)\\.(xslt?)$");
    Pattern favicon_pattern = Pattern.compile("^/?favicon.ico$");
    Pattern marker_pattern = Pattern.compile("^/resources/marker$");
    Pattern metainf_pattern = Pattern.compile("^/META-INF/.*$");
    
    Renderer markerRenderer = new MapMarkerRenderer();

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        String path = request.getPathInfo();
        String urlQuery = request.getQueryString();
        if (s_logger.isDebugEnabled()) s_logger.debug("> doGet " + path + ((urlQuery != null) ? "?" + urlQuery : ""));

        Matcher m = null;

        // --------------------- static resources ---------------------

        m = scripts_pattern.matcher(path);
        if (m.matches()) {
            read(request, response, path, "text/javascript");
            if (s_logger.isDebugEnabled()) s_logger.debug("< doGet");
            return;
        }

        m = images_pattern.matcher(path);
        if (m.matches()) {
            read(request, response, path, "images/" + m.group(1));
            if (s_logger.isDebugEnabled()) s_logger.debug("< doGet");
            return;
        }

        m = styles_pattern.matcher(path);
        if (m.matches()) {
            read(request, response, path, "text/css");
            if (s_logger.isDebugEnabled()) s_logger.debug("< doGet");
            return;
        }

        m = content_pattern.matcher(path);
        if (m.matches()) {
            read(request, response, path, "text/html");
            if (s_logger.isDebugEnabled()) s_logger.debug("< doGet");
            return;
        }

        m = data_pattern.matcher(path);
        if (m.matches()) {
            read(request, response, path, "text/xml");
            if (s_logger.isDebugEnabled()) s_logger.debug("< doGet");
            return;
        }

        m = jar_pattern.matcher(path);
        if (m.matches()) {
            read(request, response, path, "application/java-archive");
            if (s_logger.isDebugEnabled()) s_logger.debug("< doGet");
            return;
        }

        // NOTE(SM): these kinds of requests are triggered by the java virtual machine
        // when running applets (no idea how to avoid this from happening)
        m = metainf_pattern.matcher(path);
        if (m.matches()) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            if (s_logger.isDebugEnabled()) s_logger.debug("< doGet");
            return;
        }
        
        m = stylesheets_pattern.matcher(path);
        if (m.matches()) {
            read(request, response, path, "application/xslt+xml");
            if (s_logger.isDebugEnabled()) s_logger.debug("< doGet");
            return;
        }
        
        m = favicon_pattern.matcher(path);
        if (m.matches()) {
            read(request, response, "resources/images/icon.png", "image/png");
            if (s_logger.isDebugEnabled()) s_logger.debug("< doGet");
            return;
        }
        
        // ------------------------- dynamic ------------------------

        m = marker_pattern.matcher(path);
        if (m.matches()) {
            render(request, response, markerRenderer);
            if (s_logger.isDebugEnabled()) s_logger.debug("< doGet");
            return;
        }
        
        Message msg = makeMessage(request, response);
        String command = msg.m_query.getFirstParamValue("command");
        if (command == null) {
            command = "start";
        }

        Command longwellCommand = (Command) m_getCommands.get(command);
        if (longwellCommand != null) {
            response.setCharacterEncoding(ENCODING);
            response.setContentType("text/html");
            longwellCommand.execute(msg);
        } else {
            if (s_logger.isInfoEnabled()) s_logger.info("Command '" + longwellCommand + "' is not registered.");
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        }

        if (s_logger.isDebugEnabled()) s_logger.debug("< doGet");
    }

    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        String path = request.getPathInfo();
        String urlQuery = request.getQueryString();
        if (s_logger.isDebugEnabled()) s_logger.debug("> doPost " + path + ((urlQuery != null) ? "?" + urlQuery : ""));

        Message msg = makeMessage(request, response);
        String command = msg.m_query.getFirstParamValue("command");

        Command longwellCommand = (Command) m_postCommands.get(command);
        if (longwellCommand != null) {
            response.setCharacterEncoding(ENCODING);
            response.setContentType("text/html");
            longwellCommand.execute(msg);
        } else {
            if (s_logger.isInfoEnabled()) s_logger.info("Command '" + longwellCommand + "' is not registered.");
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        }
        
        if (s_logger.isDebugEnabled()) s_logger.debug("< doPost");
    }

    // -----------------------------------------------------------------------------------
    
    protected File absolutize(String location) {
        if (location.charAt(0) != '/' && location.indexOf(':') < 0) {
            return new File(m_homeDir, location);
        } else {
            return new File(location);
        }
    }
    
    protected String makeRelativePath(File file, File relativeTo) {
        String relativePath = relativeTo.getAbsolutePath();
        if (!relativePath.endsWith(File.separator)) {
            relativePath += File.separator;
        }
        return file.getAbsolutePath().substring(relativePath.length());
    }
    
    protected File findConfiguration(String name) throws FileNotFoundException {
        name = name.trim();
        Iterator i = m_configurationPaths.iterator();
        while (i.hasNext()) {
            File path = (File) i.next();
            File home = new File(path,name);
            File properties = new File(home, "configuration.properties");
            if (properties.exists() && properties.canRead()) {
                return home;
            }
        }
        throw new FileNotFoundException(name);
    }

    protected Configuration load(Properties properties, InjectionManager injections, ClassLoader classLoader) throws Exception {

        // Load the default longwell.properties
        FileInputStream fis = new FileInputStream(new File(new File(m_homeDir, "WEB-INF"), "longwell.properties"));
        properties.load(fis);
        fis.close();
        
        // Overoad with properties set from the command line 
        // using the -Dkey=value parameters to the JVM
        overloadProperties(properties);

        // Obtain the paths in where to look for configurations
        String path = m_properties.getProperty("longwell.configuration.path");
        if (path != null || "".equals(path)) {
            String[] paths = path.split(File.pathSeparator);
            for (int i = 0; i < paths.length; i++) {
                m_configurationPaths.add(absolutize(paths[i]));
            }
        }
        // no matter what path, always look for configurations in the home dir last
        m_configurationPaths.add(m_homeDir);
        
        // Get the starting configuration
        String startConf = m_properties.getProperty("longwell.configuration");
        if (startConf == null || startConf.equals("")) {
            s_logger.warn("Could not find valid starting configuration, will start with default.");
            startConf = "longwell";
        }
                
        // Load the configuration properties and make them overload the current ones
        Configuration configuration = loadConfiguration(startConf, injections, classLoader);
        properties.putAll(configuration.getProperties());
        
        // Redo the overloading in case configurations overloaded them
        overloadProperties(properties);
        
        return configuration;
    }

    protected void overloadProperties(Properties properties) {
        Properties systemProperties = System.getProperties();
        for (Iterator i = systemProperties.keySet().iterator(); i.hasNext(); ) {
            String key = (String) i.next();
            if (key.startsWith("longwell.") || key.startsWith("map.")) {
                String value = systemProperties.getProperty(key);
                properties.setProperty(key, value);
            }
        }
    }
    
    protected Configuration loadConfiguration(String name, InjectionManager injections, ClassLoader classLoader) throws IOException {
        if (s_logger.isDebugEnabled()) s_logger.debug("> loadConfiguration(" + name + ")");
        // get configuration location
        File home = findConfiguration(name);
        
        // load its property files
        Properties properties = new Properties();
        File propsFile = new File(home,"configuration.properties");
        if (s_logger.isInfoEnabled()) s_logger.info("Loading properties for configuration '" + name + "' from " + propsFile);
        FileInputStream fis = new FileInputStream(propsFile);
        properties.load(fis);
        fis.close();
        if (s_logger.isDebugEnabled()) s_logger.debug(" loaded " + properties.size() + " properties");

        Configuration configuration = null;
        Configuration extendedConfiguration = null;
        Properties extendedProperties = null;

        // if the configuration extends another, process the parent first
        // NOTE: this is important for the order of the injection overloads
        String extended = properties.getProperty("longwell.configuration.extends");
        if (extended != null) {
            if (s_logger.isInfoEnabled()) s_logger.info(name + " extends " + extended);
            extendedConfiguration = loadConfiguration(extended, injections, classLoader);
            extendedProperties = extendedConfiguration.getProperties();
        }

        // now that the parent has been processed, process the currente properties
        // and absolutize the template locations
        for (Iterator i = properties.keySet().iterator(); i.hasNext(); ) {
            String key = (String) i.next();
            if (key.indexOf("template") > 0) {
                String location = properties.getProperty(key);
                location = makeRelativePath(new File(new File(home, "templates"), location), m_rootDir);
                properties.put(key, location);
            }
            if (key.startsWith("longwell.template.")) {
                String template = key.substring("longwell.template.".length());
                String location = properties.getProperty(key);
                if (s_logger.isDebugEnabled()) s_logger.debug("Configuration " + name + ": injected template '" + template + "' at " + location);
                injections.setInjection(template,location);
            }
        }

        if (extendedProperties != null) {
            extendedProperties.putAll(properties);
            properties = extendedProperties;
        }

        // if the classloader is a longwell classloader, add the configuration bytecode paths
        if (classLoader instanceof LongwellClassLoader) {
            LongwellClassLoader longwellClassLoader = (LongwellClassLoader) classLoader;
            longwellClassLoader.addRepository(new File(home, "classes"));
            longwellClassLoader.addRepository(new File(home, "lib"));
        }
        
        configuration = new Configuration(name, extendedConfiguration, home, properties); 
        if (s_logger.isDebugEnabled()) s_logger.debug("Created configuration: " + configuration);
        
        if (s_logger.isDebugEnabled()) s_logger.debug("< loadConfiguration(" + name + ")");
        return configuration;
    }

    protected void loadProfile(Profile profile, Configuration configuration) throws Exception {
        if (s_logger.isDebugEnabled()) s_logger.debug("> loadProfile('" + configuration.getName() + "')");
        if (configuration.getParent() != null) {
            loadProfile(profile, configuration.getParent());
        }
        File data = new File(configuration.getLocation(),"data");
        if (data.exists() && data.canRead()) {
            profile.addData(data);
        }
        if (s_logger.isDebugEnabled()) s_logger.debug("< loadProfile('" + configuration.getName() + "')");
    }

    /**
     * Construct a Command. At startup time, two groups of Commands
     * are created (representing valid HTTP GET and POST commands) and
     * stored in the m_getCommands and m_postCommands tables. The
     * configuration.properties file determines what Commands go into
     * each group.
     *
     * During operation, the two tables are consulted as HTTP messages
     * arrive.
     */
    protected Command createCommand(String className, String template) throws Exception {
        if (s_logger.isDebugEnabled()) s_logger.debug("> createCommand");
        Command command = null;
        
        Class clazz = m_classLoader.loadClass(className);
        Class injectionManagerClass = m_classLoader.loadClass(InjectionManager.class.getName());
        Class stringClass = m_classLoader.loadClass("java.lang.String");
        Class[] types = new Class[] {injectionManagerClass, stringClass};
        Object[] values = new Object[] {m_injections, template};

        try {
            Constructor constructor = clazz.getConstructor(types);
            command = (Command) constructor.newInstance(values);
            if (s_logger.isDebugEnabled()) s_logger.debug("loaded with (InjectionManager, String) constructor");
        } catch (NoSuchMethodException e) {
            try {
                types = new Class[] {injectionManagerClass};
                values = new Object[] {m_injections};
                Constructor constructor = clazz.getConstructor(types);
                command = (Command) constructor.newInstance(values);
                if (s_logger.isDebugEnabled()) s_logger.debug("loaded with (InjectionManager) constructor");
            } catch (NoSuchMethodException e2) {
                if (s_logger.isDebugEnabled()) s_logger.debug("loaded with () constructor");
                command = (Command) clazz.newInstance();
            }
        }
        
        if (s_logger.isDebugEnabled()) s_logger.debug("< createCommand");
        return command;
    }
    
    /**
     * Parse the HTTP request into parts, and return a Message object
     * containing those parts.
     */
    protected Message makeMessage(HttpServletRequest request, HttpServletResponse response) {
        if (s_logger.isDebugEnabled()) s_logger.debug("> makeMessage");

        String profileID = s_longwell.getDefaultProfile().getID();

        String extraPath = request.getPathInfo();
        if (extraPath.length() > 0) {
            try {
                String s = URLDecoder.decode(extraPath, ENCODING);
                
                int pound = s.indexOf('#');
                if (pound >= 0) {
                    s = s.substring(0, pound);
                }

                if (s.length() > 0) {
                    profileID = s.replaceFirst("/*", "");
                }
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException("Unsupported encoding (" + ENCODING + ") in URL encoding.");
            }
        }

        Query query = new Query(request.getQueryString());
        Message msg = new Message(this, request, response, m_ve, profileID, query, "");

        if (s_logger.isDebugEnabled()) s_logger.debug("< makeMessage");
        return msg;
    }

    protected void render(HttpServletRequest request, HttpServletResponse response, Renderer renderer) {

        double scale = Utilities.parseDouble(request.getParameter("s"), 1);
        long pixelX = Utilities.parseLong(request.getParameter("x"), 0);
        long pixelY = Utilities.parseLong(request.getParameter("y"), 0);
        long pixelWidth = Utilities.parseLong(request.getParameter("w"), 100);
        long pixelHeight = Utilities.parseLong(request.getParameter("h"), 100);
        
        double realX = pixelX / scale;
        double realY = pixelY / scale;
        double realWidth = pixelWidth / scale;
        double realHeight = pixelHeight / scale;
        
        Rectangle2D crop = new Rectangle2D.Double(realX, realY, realWidth, realHeight);
        BufferedImage image = new BufferedImage((int) pixelWidth, (int) pixelHeight, BufferedImage.TYPE_4BYTE_ABGR);
        Graphics2D g2d = (Graphics2D) image.getGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        response.setContentType("image/png");

        try {
            g2d.scale(scale, scale);
            g2d.translate(-realX, -realY);
            renderer.render(g2d, crop, request);
            ImageIO.write(image, "png", response.getOutputStream());
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        } finally {
            g2d.dispose();
            image.flush();
        }
    }

    protected URL findResource(Configuration configuration, String file) {
        if (s_logger.isDebugEnabled()) s_logger.debug("> Looking for " + file + " in configuration " + configuration.getName());
        File location = configuration.getLocation();
        File f = new File(location, file);
        URL result = null;
        if (f.exists()) {
            try {
                result = f.toURL();
                if (s_logger.isDebugEnabled()) s_logger.debug(" " + file + " was found");
            } catch (MalformedURLException e) {
                s_logger.error("Caught exception looking for " + file + " in configuration " + configuration.getName(), e);
            }
        } else {
            if (configuration.getParent() != null) {
                result = findResource(configuration.getParent(), file);
            } else {
                if (s_logger.isDebugEnabled()) s_logger.debug(" " + file + " could not be found");
            }
        }
        if (s_logger.isDebugEnabled()) s_logger.debug("< Looking for " + file + " in configuration " + configuration.getName());
        return result;
    }
    
    protected void read(HttpServletRequest request, HttpServletResponse response, String file, String mimeType)
            throws IOException {

        if (s_logger.isDebugEnabled()) s_logger.debug("> read " + file);

        URL resource = findResource(m_configuration, file);

        if (resource != null) {
            URLConnection urlConnection = resource.openConnection();
            long lastModified = urlConnection.getLastModified();

            long ifModifiedSince = request.getDateHeader("If-Modified-Since");
            if (ifModifiedSince / 1000 < lastModified / 1000) {
                response.setDateHeader("Last-Modified", lastModified);
                InputStream input = null;
                OutputStream output = null;
                try {
                    input = new BufferedInputStream(urlConnection.getInputStream());
                    response.setHeader("Content-Type", mimeType);
                    output = response.getOutputStream();
                    byte[] buffer = new byte[4096];
                    int length = 0;
                    while ((length = input.read(buffer)) > -1) {
                        output.write(buffer, 0, length);
                    }
                } catch (Exception e) {
                    s_logger.error("Error processing " + resource, e);
                } finally {
                    if (input != null) input.close();
                    if (output != null) output.close();
                }
            } else {
                response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
            }
        } else {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        }

        if (s_logger.isDebugEnabled()) s_logger.debug("< read");
    }
    
    protected void setOpenIDStore(MemoryStore store) {
    	m_openIDStore = store;
    }
    
    protected MemoryStore readSavedOpenIDStore(File storeFile) {
        if (s_logger.isDebugEnabled()) s_logger.debug("> readSavedOpenIDStore from " + storeFile.getAbsolutePath());
		MemoryStore store = null;
		FileInputStream fis = null;
		ObjectInputStream ois = null;
		try {
			fis = new FileInputStream(storeFile);
			ois = new ObjectInputStream(fis);
			store = (MemoryStore) ois.readObject();
			ois.close();
			fis.close();
		} catch (IOException e) {
			if (s_logger.isDebugEnabled()) s_logger.debug("No existing store found, creating new store");
			store = new MemoryStore();
		} catch (Exception e) {
			s_logger.debug("Failed to make OpenID store", e);
		}
		if (s_logger.isDebugEnabled()) s_logger.debug("< readSavedOpenIDStore");
		return store;
    }
    
    protected void saveOpenIDStore(File storeFile) {
        if (s_logger.isDebugEnabled()) s_logger.debug("> saveOpenIDStore to " + storeFile.getAbsolutePath());
		MemoryStore store = (MemoryStore) getOpenIDStore();
		FileOutputStream fos = null;
		ObjectOutputStream oos = null;
		try {
			fos = new FileOutputStream(storeFile);
			oos = new ObjectOutputStream(fos);
			oos.writeObject(store);
			oos.close();
			fos.close();
		} catch (IOException e) {
			s_logger.error("Failed to save OpenID store", e);
		}
    	if (s_logger.isDebugEnabled()) s_logger.debug("< saveOpenIDStore");
    }
    
    protected boolean isInitialized(Repository r)  {
        boolean status = false;
        RepositoryConnection c = null;
        try {
            c = r.getConnection();
            status = s_longwell.getDefaultProfile().getQueryManager().containsSubject(c, new URIImpl("http://simile.mit.edu/2005/04/longwell#Trusted"));
        } catch (RepositoryException e) {
            // ignore
        } finally {
            try {
                if (c != null) c.close();
            } catch (Exception e) {
                // ignore;
            }
        }
        return status;
    }

}
