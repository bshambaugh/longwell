package edu.mit.simile.longwell.command;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import edu.mit.simile.longwell.LongwellServlet;
import edu.mit.simile.longwell.Message;

public class CreateAccountCommand implements Command {
	
    final static private Logger s_logger = Logger.getLogger(CreateAccountCommand.class);

	public void execute(Message msg) throws ServletException {
		try {
			//String passwordHash = msg.m_request.getHeader("x-sembank-password-hash");
			//String email = msg.m_request.getHeader("x-sembank-email");

			if (LongwellServlet.getLongwellService().hasProfile(msg.m_profileID)) {
				msg.m_response.setStatus(HttpServletResponse.SC_CONFLICT);

				s_logger.info("Attempted to create existing account " + msg.m_profileID);
			} else {
                LongwellServlet.getLongwellService().createProfile(msg.m_profileID);
				msg.m_response.setStatus(HttpServletResponse.SC_CREATED);

				s_logger.info("Created account " + msg.m_profileID);
			}
		} catch (Exception e) {
			s_logger.error(e);
			e.printStackTrace();

			msg.m_response.setStatus(
				HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}
	}

}
