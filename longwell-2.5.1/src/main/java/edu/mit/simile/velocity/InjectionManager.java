package edu.mit.simile.velocity;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.log4j.Logger;

public class InjectionManager {

    final static private Logger s_logger = Logger.getLogger(InjectionManager.class);

    private Map<String,String> nameToTemplate = new HashMap<String,String>();
    private Map<String,String> templateToOverloadedTemplate = new HashMap<String,String>();
    
    public void setInjection(String name, String template) {
        if (hasInjection(name)) {
            String injection = getInjection(name);
            if (s_logger.isDebugEnabled()) s_logger.debug("Injection '" + name + "' found at " + template);
            this.nameToTemplate.put(name, template);
            if (s_logger.isDebugEnabled()) s_logger.debug("Template " + template + " extends " + injection);
            this.templateToOverloadedTemplate.put(template, injection);
        } else {
            this.nameToTemplate.put(name, template);
        }
    }
    
    public boolean hasInjection(String name) {
        if (s_logger.isDebugEnabled()) s_logger.debug("Requested injection '" + name + "'");
        return this.nameToTemplate.containsKey(name);
    }

    public String getInjection(String name) {
        String template = (String) this.nameToTemplate.get(name);
        if (s_logger.isDebugEnabled()) s_logger.debug("Found injection '" + name + "' at " + template);
        return template;
    }

    public boolean hasParentInjection(String template) {
        boolean found = this.templateToOverloadedTemplate.containsKey(template); 
        if (s_logger.isDebugEnabled()) s_logger.debug("Requested parent of " + template + ((found) ? " which was found" : "which was NOT found!"));
        if (!found) {
            for (Iterator i = this.templateToOverloadedTemplate.keySet().iterator(); i.hasNext(); ) {
                String key = (String) i.next();
                String value = (String) this.templateToOverloadedTemplate.get(key);
                s_logger.debug("  " + key + " -> " + value);
            }
        }
        return found; 
    }
    
    public String getParentInjection(String template) {
        String parentTemplate = (String) this.templateToOverloadedTemplate.get(template);
        if (s_logger.isDebugEnabled()) s_logger.debug("Found parent of " + template + " at " + parentTemplate);
        return parentTemplate;
    }
    
}
