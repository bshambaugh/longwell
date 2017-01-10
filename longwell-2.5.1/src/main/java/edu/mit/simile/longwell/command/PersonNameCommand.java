package edu.mit.simile.longwell.command;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.apache.velocity.VelocityContext;
import org.openrdf.model.URI;

import edu.mit.simile.longwell.Message;
import edu.mit.simile.longwell.Profile;
import edu.mit.simile.longwell.query.Query;
import edu.mit.simile.longwell.query.engine.QueryEngine;
import edu.mit.simile.longwell.query.engine.QueryEngine.Answer;
import edu.mit.simile.longwell.schema.SchemaModel;
import edu.mit.simile.velocity.InjectionManager;

public class PersonNameCommand extends CommandBase {

	final static private Logger s_logger = Logger.getLogger(PersonNameCommand.class);

	public PersonNameCommand(InjectionManager injectionManager, String template) {
		super(injectionManager, template);
	}

    public void execute(Message msg) throws ServletException {
        if (s_logger.isDebugEnabled()) s_logger.debug("> personName");
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(msg.m_request.getInputStream(), "UTF-8"));

            String subCommand = br.readLine();
            if ("complete".equals(subCommand)) {
                String prefix = br.readLine();
                doComplete(msg, prefix.toLowerCase());
            }
        } catch (Exception e) {
            s_logger.error(e);
            e.printStackTrace();
            msg.m_response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
        if (s_logger.isDebugEnabled()) s_logger.debug("< personName");
    }

    protected void doComplete(Message msg, String prefix) throws Exception {
    		VelocityContext vcContext = createContext(msg);

        Profile profile = msg.getProfile();
        if (profile == null) {
            s_logger.error("Could not retrieve profile: " + msg.m_profileID);
            msg.m_response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        
        QueryEngine queryModel = (QueryEngine) profile.getStructuredModel(QueryEngine.class);
        SchemaModel schemaModel = profile.getSchemaModel();

        Query query = new Query(
                "-=%40lwq.project.PropertyProjector%3Bhttp%3A%2F%2Fwww.w3.org%2F1999%2F02%2F22-rdf-syntax-ns%23type%3B%40lwq.bucket.DistinctValueBucketer%3Brhttp%3A%2F%2Fxmlns.com%2Ffoaf%2F0.1%2FPerson");
        Answer answer = queryModel.query(query, false);
        Set completions = answer.getObjects();
        Iterator i = completions.iterator();

        Vector<NameMatch> matches = new Vector<NameMatch>();
        while (i.hasNext()) {
            URI obj = (URI) i.next();
            String label = schemaModel.getLabel(obj, "");
            String label2 = label.toLowerCase();
            if (label2.startsWith(prefix)) {
                matches.add(new NameMatch(label, obj.toString()));
            }
        }
        vcContext.put("prefix", prefix);
        vcContext.put("matches", matches);
        if (s_logger.isDebugEnabled()) s_logger.debug("> mergeTemplate");
        msg.m_ve.mergeTemplate(m_template, vcContext, msg.m_response.getWriter());
        if (s_logger.isDebugEnabled()) s_logger.debug("< mergeTemplate");
    }

    public class NameMatch {
    		private String _label;
    		private String _uri;
    		
    		public NameMatch(String label, String uri) {
    			_label = label;
    			_uri = uri;
    		}
    		
    		public String getLabel() { return _label; }
    		
    		public String getUri() { return _uri; }
    }
}
