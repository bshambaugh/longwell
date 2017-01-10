package edu.mit.simile.longwell.account;

import java.util.Iterator;
import java.util.Map;
import java.util.Vector;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginException;

import edu.mit.simile.openid.OpenIdPrincipal;

// add the longwell account principal and the role principal here
public class AccountLoginModule {
	
	// initial state
	private CallbackHandler _cbh;
	private Subject _subject;
	
	// success
	private boolean _success;
	
	// temp state
	private Vector _tempCredentials;
	private Vector<AccountPrincipal> _tempPrincipals;
	
	private boolean _debug;
	
	public AccountLoginModule() {
		this._tempCredentials = new Vector();
		this._tempPrincipals = new Vector<AccountPrincipal>();
		this._success = false;
		this._debug = false;
	}
	
	public void initialize(Subject subject, CallbackHandler cbh, Map sharedState, Map options) {
		this._subject = subject;
		this._cbh = cbh;
		
		if (options.containsKey("debug"))
			this._debug = "true".equalsIgnoreCase((String)options.get("debug"));
		
		if (this._debug) {
			System.out.println("\t\t[LongwellLoginModule] initialize");
		}
	}
	
	public boolean login() throws LoginException {		
		if (this._debug) {
			System.out.println("\t\t[LongwellLoginModule] login");
		}
		
		if (this._cbh == null)
			throw new LoginException("Error: no CallbackHandler available " +
			"to garner authentication information from the user");
		
		try {
			// Setup default callback handlers.
			Callback[] callbacks = new Callback[] {
					new LongwellCallback()
			};
			
			this._cbh.handle(callbacks);
			
			OpenIdPrincipal p = ((LongwellCallback) callbacks[0]).getPrincipal();
			
			// start OpenID Consumer here?
			this._success = getProfile(p);
			
			callbacks[0] = null;
			
			if (!this._success)
				throw new LoginException("Profile not found");
			
			return true;
		} catch (LoginException ex) {
			throw ex;
		} catch (Exception ex) {
			this._success = false;
			throw new LoginException(ex.getMessage());
		}
	}
	
	public boolean commit() throws LoginException {
		if (this._debug) {
			System.out.println("\t\t[LongwellLoginModule] commit");
		}
		
		if (this._success) {
			if (this._subject.isReadOnly()) {
				throw new LoginException ("Subject is read-only");
			}
			
			try {
				Iterator it = this._tempPrincipals.iterator();
				
				if (this._debug) {
					while (it.hasNext())
						System.out.println("\t\t[LongwellLoginModule] Principal: " + it.next().toString());
				}
				
				this._subject.getPrincipals().addAll(this._tempPrincipals);
				this._subject.getPublicCredentials().addAll(this._tempCredentials);
				
				this._tempPrincipals.clear();
				this._tempCredentials.clear();
				
				return true;
			} catch (Exception ex) {
				ex.printStackTrace(System.out);
				throw new LoginException(ex.getMessage());
			}
		} else {
			this._tempPrincipals.clear();
			this._tempCredentials.clear();
			return true;
		}
	}
	
	public boolean abort() throws LoginException {
		if (this._debug)
			System.out.println("\t\t[LongwellLoginModule] abort");
		
		// Clean out state
		this._success = false;
		
		this._tempPrincipals.clear();
		this._tempCredentials.clear();
				
		logout();
		
		return true;
	}
	
	public boolean logout() throws LoginException {
		if (this._debug)
			System.out.println("\t\t[LongwellLoginModule] logout");
		
		this._tempPrincipals.clear();
		this._tempCredentials.clear();
		
		// remove the principals the login module added
		Iterator it = this._subject.getPrincipals(AccountPrincipal.class).iterator();
		while (it.hasNext()) {
			AccountPrincipal p = (AccountPrincipal) it.next();
			if (this._debug)
				System.out.println("\t\t[LongwellLoginModule] removing principal "+ p.toString());
			this._subject.getPrincipals().remove(p);
		}
		
		return true;
	}
	
	private boolean getProfile(OpenIdPrincipal p) {
		AccountPrincipal account = new AccountPrincipal(p.getName(), p.getNickname());
		this._tempPrincipals.add(account);
		
		//@@@ fetch and associate role principals here
		
		return true;
	}
}
