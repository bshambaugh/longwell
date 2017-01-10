package edu.mit.simile.longwell.command;

import java.util.Date;

import javax.servlet.ServletException;

import org.apache.log4j.Logger;
import org.apache.velocity.VelocityContext;

import edu.mit.simile.longwell.LongwellServlet;
import edu.mit.simile.longwell.LongwellURL;
import edu.mit.simile.longwell.Message;
import edu.mit.simile.longwell.Profile;
import edu.mit.simile.longwell.VelocityUtilities;
import edu.mit.simile.velocity.InjectionManager;

public class CommandBase implements Command {

    final static private Logger s_logger = Logger.getLogger(CommandBase.class);
    
    final protected InjectionManager m_injectionManager;

    final protected String m_template;

    public CommandBase(InjectionManager injectionManager) {
        m_injectionManager = injectionManager;
        m_template = null;
    }

    public CommandBase(InjectionManager injectionManager, String template) {
        m_injectionManager = injectionManager;
        m_template = template;
    }
    
    protected VelocityContext createContext(Message msg) {
        VelocityContext vcContext = new VelocityContext();
        LongwellURL url = new LongwellURL(msg);
        Profile profile = msg.getProfile();

        if (profile == null) {
            return null;
        } else {
            vcContext.put("longwell", LongwellServlet.getLongwellService());
            vcContext.put("profile", profile);
            vcContext.put("schemaModel", profile.getSchemaModel());

            vcContext.put("msg", msg);
            vcContext.put("url", url);
            vcContext.put("outerURL", url);
            vcContext.put("contextPath", url.getContextPath());
            vcContext.put("resourcePath", url.getContextPath() + "/resources");
            vcContext.put("locale", "");

            vcContext.put("currentTime", new Date());
            vcContext.put("utilities", new VelocityUtilities());

            vcContext.put("injections", m_injectionManager);

            vcContext.put("response", msg.m_response);

            return vcContext;
        }
    }
    
    public void execute(Message msg) throws ServletException {
        try {
            VelocityContext vcContext = createContext(msg);
        
            if (s_logger.isDebugEnabled()) s_logger.debug("> mergeTemplate " + m_template);
            msg.m_ve.mergeTemplate(m_template, vcContext, msg.m_response.getWriter());
            if (s_logger.isDebugEnabled()) s_logger.debug("< mergeTemplate " + m_template);
        } catch (Throwable e) {
            s_logger.error(e);
            e.printStackTrace();
        }
    }
}
