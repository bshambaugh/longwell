package edu.mit.simile.longwell.account;

import java.security.Principal;

public class RolePrincipal implements Principal, Role {

	private String _roleName;
	
	public RolePrincipal(String roleName) {
		this._roleName = roleName;
	}
	
	public String getName() {
		return this._roleName;
	}

	public String getRole() {
		return this._roleName;
	}
}
