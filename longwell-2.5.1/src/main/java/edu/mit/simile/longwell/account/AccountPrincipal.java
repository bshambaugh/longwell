package edu.mit.simile.longwell.account;

import java.security.Principal;

import edu.mit.simile.longwell.LongwellServlet;
import edu.mit.simile.longwell.Profile;

public class AccountPrincipal implements Principal, Account {

	private String _username;
	private String _url;
	
	public AccountPrincipal(String url, String username) {
		super();
		this._username = username;
		this._url = url;
	}

	/* note that this is different from the OpenIdPrincipal;
	 * the true name of the account is the nickname, not the url
	 */
	public String getName() {
		return getNickname();
	}

	public String getNickname() {
		return this._username;
	}

	public String getOpenIdURL() {
		return this._url;
	}

	public Profile getProfile() {
		return LongwellServlet.getLongwellService().getProfile(getNickname());
	}
}
