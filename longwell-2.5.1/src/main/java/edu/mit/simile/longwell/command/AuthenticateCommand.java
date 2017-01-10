package edu.mit.simile.longwell.command;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.apache.velocity.VelocityContext;

import com.janrain.openid.consumer.Consumer;
import com.janrain.openid.consumer.FailureResponse;
import com.janrain.openid.consumer.Response;
import com.janrain.openid.store.OpenIDStore;

import edu.mit.simile.longwell.LongwellServlet;
import edu.mit.simile.longwell.LongwellURL;
import edu.mit.simile.longwell.Message;
import edu.mit.simile.openid.OpenIdCallbackHandler;
import edu.mit.simile.openid.OpenIdPrincipal;
import edu.mit.simile.velocity.InjectionManager;

public class AuthenticateCommand extends CommandBase {
	final static private Logger s_logger = Logger.getLogger(LoginCommand.class);
	
	public AuthenticateCommand(InjectionManager injectionManager, String template) {
		super(injectionManager, template);
	}
	
	public void execute(Message msg) throws ServletException {
		HttpSession userSession = msg.m_request.getSession();
		// logout any existing subject
		userSession.removeAttribute("subject");

		Map session = (Map) userSession.getAttribute("auth");
		// temporary subject for nickname in account creation
		Subject temp = (Subject) userSession.getAttribute("openid");
		Subject user = null;
		Response done = null;
		
		boolean redirect = false;
		if (temp == null && session != null) {
			// go through openid authentication dance
			OpenIDStore store = msg.m_servlet.getOpenIDStore();
			Consumer consumer = new Consumer(session, store);
			// parametermap maps strings to string[]s, need to have string-string mapping for consumer
			Map stringOnlyMap = processParameterMap(msg.m_request.getParameterMap());
			done = consumer.complete(stringOnlyMap);
			try {
				CallbackHandler cbh = new OpenIdCallbackHandler(done);
				LoginContext lc = new LoginContext("OpenIdLogin", cbh);
				lc.login();
				user = lc.getSubject();
			} catch (LoginException le) {
				s_logger.error(le);
				le.printStackTrace();			
			}
		} else if (temp != null) {
			// already went through openid authentication dance, getting a nickname
			// for account creation
			user = temp;
		}
		
		try {
			VelocityContext vcContext = createContext(msg);
			if (done instanceof FailureResponse) // openid authentication failed
				vcContext.put("status", ((FailureResponse) done).getMessage());
			else if (user == null) // user is just looking at this page
				vcContext.put("status", "No login information provided.");
			else {
				// determine whether account creation or login is taking place
				OpenIdPrincipal principal = (OpenIdPrincipal) user.getPrincipals().iterator().next();
				String nickname = principal.hasNickname() ? principal.getNickname() : msg.m_request.getParameter("nickname");
				// @@@TODO: very, very wrong and insecure; this should check the OpenID URL.
				// so instead, create profile directory based on hash of url, nickname is just a pretty label stored somewhere...
				if (nickname != null && LongwellServlet.getLongwellService().hasProfile(nickname)) {
					// @@@ don't do any real login now
					// associate a longwell JAAS profile
					// LongwellCallbackHandler lch = new LongwellCallbackHandler(principal);
					// LoginContext lc = new LoginContext("LongwellLogin", lch);
					// lc.login();
					// user = lc.getSubject();
					userSession.setAttribute("subject", user);
					// redirect
					redirect = true;
				} else {
					// there isn't a profile, time to create a new account
					if (nickname != null) {
						// @@@ don't do any real login now
						// if there's a nickname, then go ahead and do it
						// LongwellServlet.getLongwellService().createProfile(principal.getName(), nickname);
						// LongwellCallbackHandler lch = new LongwellCallbackHandler(principal);
						// LoginContext lc = new LoginContext("LongwellLogin", lch);
						// lc.login();
						// user = lc.getSubject();
						userSession.setAttribute("subject", user);
						redirect = true;
					} else {
						// if not, ask for a nickname, and move on to account creation step
						vcContext.put("needsNickname", "yes");
						// temporary session attribute
						userSession.setAttribute("openid", user);
					}
				}
			}
			
			if (redirect)
				msg.m_response.sendRedirect(new LongwellURL(msg).changeCommandQuery("start", "").toURLString());
			else
				msg.m_ve.mergeTemplate(m_template, vcContext, msg.m_response.getWriter());
		}  catch (Throwable e) {
			s_logger.error(e);
			e.printStackTrace();
		}
	}
	
	// if, somehow, there's more than one value per parameter, only the first will
	// be used; this probably isn't consistent with the openid handshake protocol,
	// but there's not really a great choice
	private Map processParameterMap(Map params) {
		HashMap result = new HashMap();
		Set keys = params.keySet();
		if (!keys.isEmpty()) {
			Iterator it = keys.iterator();
			while (it.hasNext()) {
				String key = (String) it.next();
				String[] vals = (String[]) params.get(key);
				if (vals.length >= 1) {
					result.put(key, vals[0]);
				}
			}
		}
		return result;
	}
}
