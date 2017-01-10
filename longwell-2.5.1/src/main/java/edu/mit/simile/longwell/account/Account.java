package edu.mit.simile.longwell.account;

import edu.mit.simile.longwell.Profile;

public interface Account {
	public String getNickname();
	
	public String getOpenIdURL();
	
	public Profile getProfile();
}
