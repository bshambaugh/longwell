package edu.mit.simile.longwell;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.openrdf.repository.Repository;

public class LongwellUtilities {
    
    final static private Logger s_logger = Logger.getLogger(LongwellUtilities.class);
            
    static public void loadDataFromDir(File dir, Profile profile, boolean forgiving, boolean trusted) throws Exception {
        if (!dir.exists()) {
            throw new FileNotFoundException("Cannot load data from " + dir.getAbsolutePath());
        }

        File[] files = dir.listFiles();

        for (int i = 0; i < files.length; i++) {
            File file = files[i];

            if (!file.isHidden()) {
                if (file.isDirectory()) {
                    loadDataFromDir(file, profile, forgiving, trusted);
                } else {
                    if (forgiving) {
                        try {
                            loadDataFromFile(file, profile, trusted);
                        } catch (Exception e) {
                            s_logger.warn("Failed to load data from " + file.getAbsolutePath(), e);
                        }
                    } else {
                        loadDataFromFile(file, profile, trusted);
                    }
                }
            }
        }
    }

    static public void loadDataFromFile(File file, Profile profile, boolean trusted) throws Exception {
        Repository r = Utilities.createMemoryRepository();
        Utilities.loadDataFromFile(file, r);
        profile.addData(r, trusted);
        r.shutDown();
    }

    static public String getLabel() {
        String label = "Longwell";
        Properties lProp = new Properties();
        try {
            lProp.load(LongwellUtilities.class.getResourceAsStream("Longwell.properties"));
            label = lProp.getProperty("longwell.label");
        } catch (Exception e) {
            // ignore
        }
        return label;
    }

    static public String getVersion() {
        String version = "unknown";
        Properties lProp = new Properties();
        try {
            lProp.load(LongwellUtilities.class.getResourceAsStream("Longwell.properties"));
            version = lProp.getProperty("longwell.version");
        } catch (Exception e) {
            // ignore
        }
        return version;
    }
}
