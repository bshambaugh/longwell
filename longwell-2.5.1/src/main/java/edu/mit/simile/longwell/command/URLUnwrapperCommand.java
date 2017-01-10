package edu.mit.simile.longwell.command;

import java.net.URLDecoder;

import javax.servlet.ServletException;

import org.apache.log4j.Logger;

import edu.mit.simile.longwell.Message;

public class URLUnwrapperCommand implements Command {
    
	static protected Logger s_logger = Logger.getLogger(URLUnwrapperCommand.class);
    
    public void execute(Message msg) throws ServletException {

        String mimetype = msg.m_request.getParameter("mimetype");
        String content = msg.m_request.getParameter("content");

        try {
            if (mimetype == null) {
                mimetype = "text/plain";
            } else {
                mimetype = URLDecoder.decode(mimetype, "UTF-8");
            }
            
            if (content == null) {
                content = "";
            } else {
                content = URLDecoder.decode(content, "UTF-8");
            }

            msg.m_response.setContentType(mimetype);
            msg.m_response.getOutputStream().write(content.getBytes());
        } catch (Exception e) {
            s_logger.error("Exception found unwrapping URL",e);
            throw new ServletException("Exception found unwrapping URL", e);
        }
    }
}
