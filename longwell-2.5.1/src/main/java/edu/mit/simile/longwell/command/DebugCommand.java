package edu.mit.simile.longwell.command;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.apache.velocity.VelocityContext;
import org.openrdf.model.Resource;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryResult;

import edu.mit.simile.longwell.Message;
import edu.mit.simile.longwell.Profile;
import edu.mit.simile.velocity.InjectionManager;

public class DebugCommand extends CommandBase {
    final static private Logger s_logger = Logger.getLogger(DebugCommand.class);

    public DebugCommand(InjectionManager injectionManager, String template) {
        super(injectionManager, template);
    }

    public void execute(Message msg) throws ServletException {
        if (s_logger.isDebugEnabled()) s_logger.debug("> start");
        Profile profile = msg.getProfile();

        if (profile == null) {
            s_logger.error("Could not retrieve profile: " + msg.m_profileID);
            msg.m_response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        } else {
            try {
                Map<Resource,Long> sizes = new HashMap<Resource,Long>();

                RepositoryConnection c = null;
                try {
                    c = profile.getRepository().getConnection();
                    RepositoryResult<Resource> contexts = c.getContextIDs();
                    long l = c.size();
                    sizes.put(new URIImpl("urn:default"), new Long(l));
                    while (contexts.hasNext()) {
                        Resource r = contexts.next();
                        l = c.size(r);
                        sizes.put(r, new Long(l));
                        s_logger.warn(r + " -> " + l);
                    }
                } finally {
                    c.close();
                }

                VelocityContext vcContext = createContext(msg);
                vcContext.put("classes", profile.getSchemaModel().getLearnedClasses());
                vcContext.put("properties", profile.getSchemaModel().getLearnedProperties());
                vcContext.put("cache", profile.getCacheFactory().getCache());
                vcContext.put("queries", profile.getQueryManager());
                vcContext.put("sizes", sizes);
                if (s_logger.isDebugEnabled()) s_logger.debug("> mergeTemplate");
                msg.m_ve.mergeTemplate(m_template, vcContext, msg.m_response.getWriter());
                if (s_logger.isDebugEnabled()) s_logger.debug("< mergeTemplate");
            } catch (Exception e) {
                s_logger.error(e);
                e.printStackTrace();
            }
        }
        if (s_logger.isDebugEnabled()) s_logger.debug("< start");
    }
}
