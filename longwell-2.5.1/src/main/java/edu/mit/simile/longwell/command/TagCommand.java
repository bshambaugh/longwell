package edu.mit.simile.longwell.command;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.openrdf.model.URI;
import org.openrdf.model.impl.URIImpl;

import edu.mit.simile.longwell.Message;
import edu.mit.simile.longwell.Profile;
import edu.mit.simile.longwell.TagModel;
import edu.mit.simile.longwell.query.Query;
import edu.mit.simile.longwell.query.engine.QueryEngine;

public class TagCommand implements Command {
    
    final static private Logger s_logger = Logger.getLogger(TagCommand.class);

    public void execute(Message msg) throws ServletException {
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(msg.m_request.getInputStream(), "UTF-8"));

            String subCommand = br.readLine();
            if ("complete".equals(subCommand)) {
                String prefix = br.readLine();
                doComplete(msg, prefix);
            } else if ("tag".equals(subCommand)) {
                String objectURI = br.readLine();
                String tags = br.readLine();
                doTag(msg, objectURI, tags);
            } else if ("tagAll".equals(subCommand)) {
                String tags = br.readLine();
                doTagAll(msg, tags);
            }
        } catch (Exception e) {
            s_logger.error(e);
            e.printStackTrace();
            msg.m_response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    protected void doComplete(Message msg, String prefix) throws Exception {
        TagModel tagModel = (TagModel) msg.getProfile().getStructuredModel(TagModel.class);

        List completions = tagModel.completeTags(prefix, 10);

        OutputStream os = msg.m_response.getOutputStream();
        PrintStream ps = new PrintStream(os);

        ps.print(prefix + "\n");

        Iterator i = completions.iterator();
        while (i.hasNext()) {
            String s = (String) i.next();
            ps.print(s + "\n");
        }
    }

    protected void doTag(Message msg, String objectURI, String tags) throws Exception {
        URI object = new URIImpl(objectURI);

        TagModel tagModel = (TagModel) msg.getProfile().getStructuredModel(TagModel.class);

        tagModel.clearTags(object);
        if (tags != null) {
            tagModel.assignTags(object, StringUtils.splitPreserveAllTokens(tags, ','));
        }
    }

    protected void doTagAll(Message msg, String tags) throws Exception {
        Profile profile = msg.getProfile();

        Query query = msg.m_query;
        QueryEngine queryModel = (QueryEngine) profile.getStructuredModel(QueryEngine.class);

        Set objects = queryModel.queryObjects(query, false);

        TagModel tagModel = (TagModel) msg.getProfile().getStructuredModel(TagModel.class);

        // tagModel.clearTags(objects);
        if (tags != null) {
            tagModel.assignTags(objects, StringUtils.splitPreserveAllTokens(tags, ','));
        }
    }
}
