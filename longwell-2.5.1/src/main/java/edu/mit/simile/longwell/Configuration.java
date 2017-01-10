package edu.mit.simile.longwell;

import java.io.File;
import java.util.Properties;

public class Configuration {
    
    private String name;
    private Configuration parent;
    private File location;
    private Properties properties;
    
    public Configuration(String n, Configuration p, File l, Properties pr) {
        this.name = n;
        this.parent = p;
        this.location = l;
        this.properties = pr;
    }
    
    public String getName() {
        return this.name;
    }
    
    public Configuration getParent() {
        return this.parent;
    }
    
    public File getLocation() {
        return this.location;
    }

    public Properties getProperties() {
        return this.properties;
    }
    
    public String toString() {
        StringBuffer buffer = new StringBuffer();
        buffer.append(this.name);
        buffer.append(' ');
        buffer.append(this.location);
        buffer.append(' ');
        if (this.parent != null) {
            buffer.append(" extends ");
            buffer.append(this.parent.getName());
        }
        return buffer.toString();
    }
}