package edu.mit.simile.longwell.account;

import java.io.IOException;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;

import edu.mit.simile.openid.OpenIdPrincipal;

public class LongwellCallbackHandler implements CallbackHandler {
	private OpenIdPrincipal _principal;
	
	public LongwellCallbackHandler(OpenIdPrincipal p) {
		this._principal = p;
	}
	
	public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
		for (int i = 0; i < callbacks.length; i++) {
			if (callbacks[i] instanceof LongwellCallback) {
				((LongwellCallback) callbacks[i]).setPrincipal(this._principal);
			} else {
				throw new UnsupportedCallbackException(callbacks[i], "Callback class not supported");
			}
		}
	}

}
