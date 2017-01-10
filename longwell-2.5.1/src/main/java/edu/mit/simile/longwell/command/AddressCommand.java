package edu.mit.simile.longwell.command;

import javax.servlet.ServletException;

import edu.mit.simile.longwell.LongwellProfile;
import edu.mit.simile.longwell.Message;

public class AddressCommand implements Command {

    public void execute(Message msg) throws ServletException {

        String location = msg.m_request.getParameter("location");
        String key = msg.m_request.getParameter("key");
        
        try {
	        	String latlong = ((LongwellProfile) msg.getProfile()).getLatLong(location, key);
	        	
	        	msg.m_response.setContentType("text/javascript");
	        	if ("".equals(latlong)) {
	        		// makes a JSON object
	        		msg.m_response.getOutputStream().println("{name: \"" + location + "\",Status: {code: 602, request: \"geocode\"}}");
	        	} else {
	        		msg.m_response.getOutputStream().println(latlong);
	        	}
        } catch (Exception e) {
        		new ServletException("Error determining lat-long for '" + location + "'",e);
        }
    }
}
