package edu.mit.simile.longwell.command;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.apache.velocity.VelocityContext;

import com.janrain.openid.consumer.AuthRequest;
import com.janrain.openid.consumer.Consumer;
import com.janrain.openid.store.OpenIDStore;

import edu.mit.simile.longwell.Message;
import edu.mit.simile.velocity.InjectionManager;

public class LoginCommand extends CommandBase {
    final static private Logger s_logger = Logger.getLogger(LoginCommand.class);

	public LoginCommand(InjectionManager injectionManager, String template) {
        super(injectionManager, template);
	}

	public void execute(Message msg) throws ServletException {
		// hrm, skip on to a different step if there's already a session?  ditch the session in case it already exists?
		HttpSession session = msg.m_request.getSession();
		try {
			if (msg.m_servlet.getProxyContextPath().startsWith("/") || msg.m_servlet.getProxyContextPath().equals("")) {
				throw new Exception("Set longwell.url to this application's URL in WEB-INF/longwell.properties before using this feature.");
			}
			String openid_url = msg.m_query.getFirstParamValue("openid_url");
			if (openid_url == null) {
	            VelocityContext vcContext = createContext(msg);
	            vcContext.put("openid_url", "");
	            msg.m_ve.mergeTemplate(m_template, vcContext, msg.m_response.getWriter());				
			} else {
				Map sessionState = new HashMap();
				session.setAttribute("auth", sessionState);
				OpenIDStore store = msg.m_servlet.getOpenIDStore();
				Consumer consumer = new Consumer(sessionState, store);
				try {
					AuthRequest auth = consumer.begin(openid_url);
					auth.addExtensionArg("sreg","optional","nickname");
					msg.m_response.sendRedirect(auth.redirectUrl(msg.m_servlet.getProxyContextPath(), msg.m_servlet.getProxyContextPath() + "?command=authenticate"));
				} catch (IOException ioe) {
		            VelocityContext vcContext = createContext(msg);
		            vcContext.put("warning", ioe.getMessage());
		            vcContext.put("openid_url", openid_url);
		            msg.m_ve.mergeTemplate(m_template, vcContext, msg.m_response.getWriter());
				}
			}
		} catch (Exception e) {
			s_logger.error(e);
			e.printStackTrace();
		}
	}
}
