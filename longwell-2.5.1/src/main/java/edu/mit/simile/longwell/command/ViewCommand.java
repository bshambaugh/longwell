package edu.mit.simile.longwell.command;

import java.io.StringWriter;
import java.util.Enumeration;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.log4j.Logger;
import org.apache.velocity.VelocityContext;
import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.repository.Repository;
import org.w3c.dom.Document;

import edu.mit.simile.fresnel.configuration.Configuration;
import edu.mit.simile.fresnel.configuration.Group;
import edu.mit.simile.fresnel.results.Selection;
import edu.mit.simile.longwell.LongwellServlet;
import edu.mit.simile.longwell.LongwellURL;
import edu.mit.simile.longwell.Message;
import edu.mit.simile.longwell.query.Query;
import edu.mit.simile.velocity.InjectionManager;

public class ViewCommand extends CommandBase {

    final static private Logger s_logger = Logger.getLogger(ViewCommand.class);

    public ViewCommand(InjectionManager injectionManager, String template) {
        super(injectionManager, template);
    }

    public void execute(Message msg) throws ServletException {
        try {
            VelocityContext vcContext = createContext(msg);

            String objectURI = msg.m_request.getParameter("objectURI");
            URI obj = new URIImpl(objectURI);
            vcContext.put("object", obj);

            Configuration conf = LongwellServlet.getLongwellService().getFresnelConfiguration();

            String format = msg.m_request.getParameter("format");
            if (format == null) format = "html";
            
            String group = msg.m_request.getParameter("group");
            String engine = msg.m_request.getParameter("engine");

            boolean defaultView = false;
            boolean fresnel = (conf != null);
            
            if (fresnel && "fresnel".equals(engine)) {
                Selection selected;
                Repository allData = (Repository) msg.getProfile().getRepository();
                try {
                    if (null != group) {
                        Resource groupRes = new URIImpl(group);
                        Group fresnelGroup = conf.groupLookup(groupRes);
                        selected = conf.select(allData, (Resource) obj, fresnelGroup);
                    } else {
                        selected = conf.select(allData, (Resource) obj);
                    }
                    selected = conf.format(allData, selected);
                    Document src = selected.render();

                    StringWriter stringOut = new StringWriter();
                    StreamResult result = new StreamResult(stringOut);
                    Transformer transformer = null;
                    if ("xml".equals(format)) {
                        transformer = TransformerFactory.newInstance().newTransformer(); 
                    } else {
                        Templates templates = LongwellServlet.getLongwellService().getTemplates("fresnel2html");
                        transformer = (templates != null) ? templates.newTransformer() : TransformerFactory.newInstance().newTransformer();
                    }
                    transformer.setOutputProperty(OutputKeys.INDENT, "yes");
                    transformer.transform(new DOMSource(src), result);

                    vcContext.put("fresnelResult", stringOut);
                } catch (Exception e) {
                    if (s_logger.isDebugEnabled()) s_logger.debug("error during fresnel rendering: " + e.getMessage());
                    defaultView = true;
                    fresnel = false;
                }
            } else {
                defaultView = true;
            }

            if (defaultView) {
                Properties properties = new Properties();
                properties.load(msg.m_request.getInputStream());

                vcContext.put("outerURL", "");

                Enumeration e = properties.keys();
                while (e.hasMoreElements()) {
                    String key = (String) e.nextElement();
                    String value = properties.getProperty(key);

                    if ("outerQuery".equals(key)) {
                        vcContext.put("outerURL", new LongwellURL(msg.getContextPath(), msg.m_profileID,
                                new Query(value)));
                    }

                    vcContext.put(key, value);
                }
            }

            vcContext.put("fresnel", new Boolean(fresnel));
            vcContext.put("format", format);
            
            msg.m_ve.mergeTemplate(m_template, vcContext, msg.m_response.getWriter());
        } catch (Throwable e) {
            s_logger.error(e);
            e.printStackTrace();
        }
    }
}
