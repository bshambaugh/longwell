package edu.mit.simile.longwell;

import java.io.File;
import java.util.Date;

/*
 * This private class is invoked by the classloader when its own loaded files change
 * NOTE: this is *not* invoked when files are added to the classpath
 */
class Trigger implements Runnable {

    private File servletFile;
    
    Trigger(File context) {
        this.servletFile = new File(context, "WEB-INF/classes/edu/mit/simile/longwell/Trigger.class");
    }
    
    public void run() {
        if (this.servletFile.exists()) {
            this.servletFile.setLastModified((new Date()).getTime());
        }
    }
}
