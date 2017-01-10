package edu.mit.simile.longwell.command;

import javax.servlet.ServletException;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;

import edu.mit.simile.longwell.LongwellURL;
import edu.mit.simile.longwell.Message;
import edu.mit.simile.velocity.InjectionManager;

public class LogoutCommand extends CommandBase {
	final static private Logger s_logger = Logger.getLogger(LoginCommand.class);

	public LogoutCommand(InjectionManager injectionManager, String template) {
		super(injectionManager, template);
	}
	
	public void execute(Message msg) throws ServletException {
		try {
			HttpSession userSession = msg.m_request.getSession();
			userSession.removeAttribute("subject");
			msg.m_response.sendRedirect(new LongwellURL(msg).changeCommandQuery("start", "").toURLString());
		} catch (Exception e) {
			s_logger.error(e);
			e.printStackTrace();
		}
	}
}
