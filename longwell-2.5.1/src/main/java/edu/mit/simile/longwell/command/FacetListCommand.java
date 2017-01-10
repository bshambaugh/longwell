package edu.mit.simile.longwell.command;

import javax.servlet.ServletException;

import org.apache.log4j.Logger;
import org.apache.velocity.VelocityContext;
import org.openrdf.query.QueryEvaluationException;

import edu.mit.simile.longwell.Message;
import edu.mit.simile.longwell.query.Facet;
import edu.mit.simile.longwell.query.Query;
import edu.mit.simile.longwell.query.engine.QueryEngine;
import edu.mit.simile.longwell.query.engine.QueryEngine.Answer;
import edu.mit.simile.velocity.InjectionManager;

public class FacetListCommand extends CommandBase {

    final static private Logger s_logger = Logger.getLogger(FacetListCommand.class);

    public FacetListCommand(InjectionManager injectionManager, String template) {
        super(injectionManager, template);
    }

    public void execute(Message msg) throws ServletException {
        try {
            VelocityContext vcContext = createContext(msg);

            String facetURI = msg.m_request.getParameter("facetURI");
            String phase = msg.m_request.getParameter("phase");
            String resultQuery = msg.m_request.getParameter("outerQuery");

            Query query = new Query(resultQuery);
            Message nmsg = new Message(msg.m_servlet, msg.m_request, msg.m_response, msg.m_ve,
                    msg.m_profileID, query, msg.m_locale);

            if (query.getRestrictions().size() > 0) {
                Answer answer = performQuery(nmsg);
                QueryEngine queryModel = (QueryEngine) msg.getProfile().getStructuredModel(QueryEngine.class);
                Facet f = queryModel.getNarrower().narrow(answer.getObjects(), facetURI, nmsg.m_locale, true);
                vcContext.put("facet", f);
                vcContext.put("phase", phase);
                vcContext.put("outerQuery", resultQuery);
            }

            nmsg.m_ve.mergeTemplate(m_template, vcContext, nmsg.m_response.getWriter());
        } catch (Throwable e) {
            s_logger.error(e);
            e.printStackTrace();
        }
    }

    protected Answer performQuery(Message msg) throws QueryEvaluationException {
        if (s_logger.isDebugEnabled()) s_logger.debug("> performQuery");
        QueryEngine queryModel = (QueryEngine) msg.getProfile().getStructuredModel(QueryEngine.class);
        Answer answer = queryModel.query(msg.m_query, false);
        if (s_logger.isDebugEnabled()) s_logger.debug("< performQuery");
        return answer;
    }
}
