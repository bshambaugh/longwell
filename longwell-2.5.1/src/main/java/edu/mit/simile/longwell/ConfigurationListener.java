package edu.mit.simile.longwell;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TimerTask;

import org.apache.log4j.Logger;

public class ConfigurationListener extends TimerTask {

    private Collection _configDirs;
    private Map<File, Long> _lastModifieds;
    private Logger _logger;

    public ConfigurationListener(Collection configDirs, Logger logger) {
        this._logger = logger;
        this._configDirs = configDirs;
        this._lastModifieds = new HashMap<File, Long>();
        Iterator i = configDirs.iterator();
        while (i.hasNext()) {
            File configFile = (File) i.next();
            this._lastModifieds.put(configFile,new Long(configFile.lastModified()));
        }
    }

    public void run() {
        int counter = 0;
        Iterator i = this._configDirs.iterator();
        while (i.hasNext()) {
            File f = (File) i.next();
            if (f.lastModified() > ((Long) this._lastModifieds.get(f)).longValue()) {
                this._lastModifieds.put(f, new Long(f.lastModified()));
                counter++;
            }
        }
        if (counter > 0) {
            this._logger.info("Reloading Fresnel configuration");
            LongwellServlet.getLongwellService().reloadConfiguration(this._configDirs);
        }
    }
}
