package edu.mit.simile.longwell.command;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.apache.velocity.VelocityContext;

import edu.mit.simile.longwell.Message;
import edu.mit.simile.longwell.Profile;
import edu.mit.simile.velocity.InjectionManager;

public class SimpleStartCommand extends CommandBase {
    final static private Logger s_logger = Logger.getLogger(SimpleStartCommand.class);

    public SimpleStartCommand(InjectionManager injectionManager, String template) {
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
                VelocityContext vcContext = createContext(msg);
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
