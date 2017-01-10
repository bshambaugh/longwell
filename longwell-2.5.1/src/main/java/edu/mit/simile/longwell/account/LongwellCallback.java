package edu.mit.simile.longwell.account;

import javax.security.auth.callback.Callback;

import edu.mit.simile.openid.OpenIdPrincipal;

public class LongwellCallback implements Callback {
	private OpenIdPrincipal _principal;
	
	public LongwellCallback() {
		super();
	}
	
	public OpenIdPrincipal getPrincipal() {
		return this._principal;
	}
	
	public void setPrincipal(OpenIdPrincipal p) {
		this._principal = p;
	}

}
