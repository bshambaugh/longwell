package edu.mit.simile.longwell.command;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.openrdf.repository.Repository;

import edu.mit.simile.longwell.Message;
import edu.mit.simile.longwell.Profile;
import edu.mit.simile.longwell.Utilities;

public class UploadCommand implements Command {

    final static private Logger s_logger = Logger.getLogger(UploadCommand.class);

    public void execute(Message msg) throws ServletException {
        Repository r = Utilities.createMemoryRepository();
        try {
            String url = msg.m_request.getRequestURL().toString();
            int i = url.indexOf('?');
            if (i > 0) {
                url = url.substring(0, i);
            }
            
            String format = msg.m_query.getFirstParamValue("format");
            if (format == null || "".equals(format)) {
                    format = "n3";
            }
            
            Utilities.loadDataFromStream(
                msg.m_request.getInputStream(),
                url + "#",
                format,
                r
            );

            Profile profile = msg.getProfile();
            if (profile != null) {
                profile.addData(r, false);
                s_logger.info("Uploaded to account " + msg.m_profileID);
            } else {
                s_logger.error("Could not retrieve profile: " + msg.m_profileID);
                msg.m_response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            }
        } catch (Exception e) {
            s_logger.error(e);
            msg.m_response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
}
