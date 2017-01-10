package edu.mit.simile.longwell.command;

import java.io.File;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.log4j.Logger;
import org.apache.velocity.VelocityContext;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import edu.mit.simile.longwell.Message;
import edu.mit.simile.velocity.InjectionManager;

public class APICommand  extends CommandBase {
    final static private Logger s_logger = Logger.getLogger(APICommand.class);

    public APICommand(InjectionManager injectionManager, String template) {
        super(injectionManager, template);
    }

    public void execute(Message msg) {
        try {
            VelocityContext vcContext = createContext(msg);
            
            if (msg.m_request.getMethod() == "POST") {
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                DocumentBuilder builder = factory.newDocumentBuilder();
                Document document = builder.parse(msg.m_request.getInputStream());
                
                Element root = document.getDocumentElement();
                
                vcContext.put("xmlRoot", root);
            }
            
            String call = msg.getQuery().getFirstParamValue("call");
            
            String template = "/api/" + call + ".vt";
            template = m_template + template.replace('/', File.separatorChar);
        
            if (s_logger.isDebugEnabled()) s_logger.debug("> mergeTemplate " + template);
            
            msg.m_ve.mergeTemplate(template, vcContext, msg.m_response.getWriter());
            
            if (s_logger.isDebugEnabled()) s_logger.debug("< mergeTemplate " + template);
        } catch (Throwable e) {
            s_logger.error(e);
            e.printStackTrace();
        }
    }
}
