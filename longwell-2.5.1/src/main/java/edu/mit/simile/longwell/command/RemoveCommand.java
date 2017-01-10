/*
 * Created on Mar 6, 2005
 * Created by dfhuynh
 */
package edu.mit.simile.longwell.command;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.repository.Repository;

import edu.mit.simile.longwell.Message;
import edu.mit.simile.longwell.Profile;
import edu.mit.simile.longwell.Utilities;

/**
 * @author dfhuynh
 */
public class RemoveCommand implements Command {
    
    final static private Logger s_logger = Logger.getLogger(RemoveCommand.class);

    public void execute(Message msg) throws ServletException {
        Repository r = Utilities.createMemoryRepository();
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(msg.m_request.getInputStream(), "UTF-8"));

            String uri = br.readLine();
            Profile profile = msg.getProfile();
            profile.extractObject(new URIImpl(uri),r);
            profile.removeData(r);
            
        } catch (Exception e) {
            s_logger.error(e);
            msg.m_response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
}
