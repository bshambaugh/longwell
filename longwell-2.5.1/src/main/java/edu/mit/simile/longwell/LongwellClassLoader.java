package edu.mit.simile.longwell;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.log4j.Logger;

/**
 * The Longwell classloader is responsible to load classes contained
 * inside the various configurations.
 */
public class LongwellClassLoader extends URLClassLoader {

    final static private Logger s_logger = Logger.getLogger(LongwellClassLoader.class);
    final static long DELAY = 1000;
    final static long PERIOD = 1000;
    
    private Timer timer = new Timer();
    private LongwellClassLoaderWatcher watcher;

    public LongwellClassLoader(ClassLoader parent, Runnable trigger) {
        this(parent, trigger, DELAY, PERIOD);
    }
    
    public LongwellClassLoader(ClassLoader parent, Runnable trigger, long delay, long repeatRate) {
        super(new URL[0], parent);
        this.watcher = new LongwellClassLoaderWatcher(trigger);
        timer.schedule(this.watcher, delay, repeatRate);
        s_logger.info("Classloader and classloader watcher initialized");
    }

    protected Class loadClass(String name, boolean resolve) throws ClassNotFoundException {

        Class clazz = findLoadedClass(name);

        if (clazz == null) {
            try {
                if (s_logger.isDebugEnabled()) s_logger.debug("Loading: " + name);
                clazz = findClass(name);
                if (s_logger.isDebugEnabled()) s_logger.debug("Loaded: " + name);
            } catch (ClassNotFoundException cnfe) {
            	try {
                    if (s_logger.isDebugEnabled()) s_logger.debug("Parent loading: " + name);
                    ClassLoader parent = getParent();
                    clazz = parent.loadClass(name);
                    if (s_logger.isDebugEnabled()) s_logger.debug("Parent loaded: " + name);
            	} catch (ClassNotFoundException cnfe2) {
            		try {
	                    if (s_logger.isDebugEnabled()) s_logger.debug("Current loading: " + name);
	            		ClassLoader current = this.getClass().getClassLoader();
	            		clazz = current.loadClass(name);
	                    if (s_logger.isDebugEnabled()) s_logger.debug("Current loaded: " + name);
            		} catch (ClassNotFoundException cnfe3) {
	                    if (s_logger.isDebugEnabled()) s_logger.debug("System loading: " + name);
	            		ClassLoader system = ClassLoader.getSystemClassLoader();
	            		clazz = system.loadClass(name);
	                    if (s_logger.isDebugEnabled()) s_logger.debug("System loaded: " + name);
            		}
            	}
            }
        }

        if (resolve) {
            resolveClass(clazz);
        }

        return clazz;
    }

    public void addRepository(File repository) {
        s_logger.info("Processing class repository: " + repository);

        if (repository.exists()) {
            if (repository.isDirectory()) {
                File[] jars = repository.listFiles();
                try  {
                    s_logger.info("Adding folder: " + repository);
                    super.addURL(repository.toURL());
                    this.watcher.addFile(repository);
                } catch (MalformedURLException e) {
                    throw new IllegalArgumentException(e.toString());
                }

                for (int i = 0; i < jars.length; i++) {
                    if (jars[i].getAbsolutePath().endsWith(".jar")) {
                        addJar(jars[i]);
                    }
                }
            } else {
                addJar(repository);
            }
        } else {
            s_logger.info("Repository " + repository + " does not exist");
        }
    }
    
    private void addJar(File file) {
        try  {
            URL url = file.toURL();
            s_logger.info("Adding jar: " + file);
            super.addURL(url);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(e.toString());
        }
    }
    
    public void dispose() {
    		this.timer.cancel();
    }
}

class LongwellClassLoaderWatcher extends TimerTask {

    private List<File> files = new ArrayList<File>();
    private Map<File,Long> lastModifieds = new HashMap<File,Long>();
    private Logger logger = Logger.getLogger(LongwellClassLoaderWatcher.class);
    private Runnable trigger;

    LongwellClassLoaderWatcher (Runnable t) {
        this.trigger = t;
    }
        
    protected void addFile(File f) {
        if (f.isDirectory()) {
            File[] files = f.listFiles();
            for (int i = 0; i < files.length; i++) {
                addFile(files[i]);
            }
        } else {
            if (f.getName().endsWith(".jar") || f.getName().endsWith(".class")) {
                if (this.logger.isInfoEnabled()) this.logger.info("Watching " + f);
                this.files.add(f);
                this.lastModifieds.put(f, new Long(f.lastModified()));
            } else {
                if (this.logger.isDebugEnabled()) this.logger.debug("Not watching " + f + " since it's not java bytecode.");
            }
        }
    }
    
    public void run() {
        int counter = 0;
        Iterator i = this.files.iterator();
        
        while (i.hasNext()) {
            File f = (File) i.next();
            if (f.lastModified() > ((Long) this.lastModifieds.get(f)).longValue()) {
                this.logger.info(f + " has changed");
                this.lastModifieds.put(f, new Long(f.lastModified()));
                counter++;
            }
        }
        
        if (counter > 0) {
            this.logger.info("Classloading space has changed. Triggering the signal...");
            this.trigger.run();
            this.logger.info("..done");
        }
    }
}
