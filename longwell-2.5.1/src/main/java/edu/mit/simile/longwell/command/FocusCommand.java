package edu.mit.simile.longwell.command;

import javax.servlet.ServletException;

import org.apache.log4j.Logger;
import org.apache.velocity.VelocityContext;
import org.openrdf.model.impl.URIImpl;

import edu.mit.simile.longwell.Message;
import edu.mit.simile.velocity.InjectionManager;

public class FocusCommand extends CommandBase {

    final static private Logger s_logger = Logger.getLogger(FocusCommand.class);

    public FocusCommand(InjectionManager injectionManager, String template) {
        super(injectionManager, template);
    }

    public void execute(Message msg) throws ServletException {
        try {
            VelocityContext vcContext = createContext(msg);

            String objectURI = msg.m_query.getFirstParamValue("objectURI");

            vcContext.put("object", new URIImpl(objectURI));
            vcContext.put("fresnelGroup", "");

            msg.m_ve.mergeTemplate(m_template, vcContext, msg.m_response.getWriter());
        } catch (Throwable e) {
            s_logger.error(e);
            e.printStackTrace();
        }
    }

}
