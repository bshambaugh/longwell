package edu.mit.simile.longwell.account;

import java.security.Permission;

// recognize longwell command execution... is it possible?
// also make sure admin implies user
public class CommandPermission extends Permission {
	/**
	 * 
	 */
	private static final long serialVersionUID = -5675826015222917695L;

	public CommandPermission(String arg0) {
		super(arg0);
		// TODO Auto-generated constructor stub
	}

	public boolean implies(Permission arg0) {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean equals(Object arg0) {
		// TODO Auto-generated method stub
		return false;
	}

	public int hashCode() {
		// TODO Auto-generated method stub
		return 0;
	}

	public String getActions() {
		// TODO Auto-generated method stub
		return null;
	}
}
