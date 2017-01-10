package edu.mit.simile.longwell.command;

import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;

import javax.servlet.ServletException;

import org.apache.log4j.Logger;
import org.apache.velocity.VelocityContext;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.repository.RepositoryException;

import edu.mit.simile.longwell.LongwellServlet;
import edu.mit.simile.longwell.LongwellURL;
import edu.mit.simile.longwell.Message;
import edu.mit.simile.longwell.Profile;
import edu.mit.simile.longwell.TagModel;
import edu.mit.simile.longwell.query.Query;
import edu.mit.simile.longwell.query.engine.QueryEngine;
import edu.mit.simile.longwell.query.engine.QueryEngine.Answer;
import edu.mit.simile.longwell.schema.LearnedProperty;
import edu.mit.simile.velocity.InjectionManager;

public class BrowseCommand extends CommandBase {

    final static private Logger s_logger = Logger.getLogger(BrowseCommand.class);

    public BrowseCommand(InjectionManager injectionManager, String template) {
        super(injectionManager, template);
    }

    public void execute(Message msg) throws ServletException {
        if (s_logger.isDebugEnabled()) s_logger.debug("> browse");

        try {
            VelocityContext vcContext = createContext(msg);

            if (msg.m_query.getRestrictions().size() > 0) {
                Answer answer = performQuery(msg);

                vcContext.put("answer", answer);

                int pageIndex = 0;
                String page = msg.m_query.getFirstParamValue("page");
                if (page != null) {
                    try {
                        pageIndex = Integer.parseInt(page);
                        pageIndex = Math.max(0, Math.min(pageIndex, answer.getPages().size() - 1));
                    } catch (NumberFormatException e) {
                        s_logger.warn("Bad page number " + page, e);
                    }
                }
                vcContext.put("pageIndex", new Integer(pageIndex));

                vcContext.put("commonProperties", getCommonProperties(answer.getObjects(), answer.getNarrowingFacets(),
                        answer.getHiddenFacets(), msg, false));

                vcContext.put("remainingFacets", getCommonProperties(answer.getObjects(), answer.getNarrowingFacets(),
                        answer.getHiddenFacets(), msg, true));

                String fresnelGroup = msg.m_query.getFirstParamValue("group");
                vcContext.put("fresnelGroup", (fresnelGroup == null) ? LongwellServlet.getLongwellService()
                        .getFresnelConfiguration().getCurrentGroup().getIdentifier().toString() : fresnelGroup);

                if ("map".equals(msg.m_query.getFirstParamValue("resultsView"))) {
                    vcContext.put("resultsView", "map-results-pane");
                } else if ("calendar".equals(msg.m_query.getFirstParamValue("resultsView"))) {
                    vcContext.put("resultsView", "calendar-results-pane");
                } else if ("timeline".equals(msg.m_query.getFirstParamValue("resultsView"))) {
                    vcContext.put("resultsView", "timeline-results-pane");
                } else if ("graph".equals(msg.m_query.getFirstParamValue("resultsView"))) {
                    vcContext.put("resultsView", "graph-results-pane");
                } else {
                    vcContext.put("resultsView", "results-pane");
                }

                if (s_logger.isDebugEnabled()) s_logger.debug("> mergeTemplate");
                msg.m_ve.mergeTemplate(m_template, vcContext, msg.m_response.getWriter());
                if (s_logger.isDebugEnabled()) s_logger.debug("< mergeTemplate");
            } else {
                msg.m_response.sendRedirect(new LongwellURL(msg).changeCommandQuery("start", "").toURLString());
            }
        } catch (Throwable e) {
            s_logger.error(e);
            e.printStackTrace();
        }

        if (s_logger.isDebugEnabled()) s_logger.debug("< browse");
    }

    protected Answer performQuery(Message msg) throws QueryEvaluationException {
        if (s_logger.isDebugEnabled()) s_logger.debug("> performQuery");
        Query query = msg.m_query;
        QueryEngine queryModel = (QueryEngine) msg.getProfile().getStructuredModel(QueryEngine.class);
        Answer answer = queryModel.query(query, false);
        if (s_logger.isDebugEnabled()) s_logger.debug("< performQuery");
        return answer;
    }

    protected SortedSet getCommonProperties(Set objects, List configured, List hidden, Message msg, boolean includeConfiguredFacets) throws RepositoryException {
        if (s_logger.isDebugEnabled()) s_logger.debug("> getCommonProperties");
        Profile profile = msg.getProfile();

        SortedSet<LearnedProperty> properties = profile.getSchemaModel().getSortedLearnedProperties(objects);

        LearnedProperty sProperty = profile.getSchemaModel().getLearnedProperty(new URIImpl(TagModel.s_tag));

        if (sProperty != null) {
            properties.add(sProperty);
        }

        if (includeConfiguredFacets) {
        	for (Iterator i = configured.iterator(); i.hasNext();) {
        		LearnedProperty prop = (LearnedProperty) i.next();
        		if (null != prop) properties.remove(prop);
        	}

        	for (Iterator i = hidden.iterator(); i.hasNext();) {
        		LearnedProperty prop = (LearnedProperty) i.next();
        		if (null != prop) properties.remove(prop);
        	}
        }

        if (s_logger.isDebugEnabled()) s_logger.debug("< getCommonProperties");

        return properties;
    }
    
    protected SortedSet getCommonProperties(Set objects, List configured, List hidden, Message msg) throws RepositoryException {
        if (s_logger.isDebugEnabled()) s_logger.debug("> getCommonProperties");
        Profile profile = msg.getProfile();

        SortedSet<LearnedProperty> properties = profile.getSchemaModel().getSortedLearnedProperties(objects);

        LearnedProperty sProperty = profile.getSchemaModel().getLearnedProperty(new URIImpl(TagModel.s_tag));

        if (sProperty != null) {
            properties.add(sProperty);
        }

        for (Iterator i = configured.iterator(); i.hasNext();) {
            LearnedProperty prop = (LearnedProperty) i.next();
            if (null != prop) properties.remove(prop);
        }

        for (Iterator i = hidden.iterator(); i.hasNext();) {
            LearnedProperty prop = (LearnedProperty) i.next();
            if (null != prop) properties.remove(prop);
        }

        if (s_logger.isDebugEnabled()) s_logger.debug("< getCommonProperties");

        return properties;
    }
}
