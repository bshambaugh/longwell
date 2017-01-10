package edu.mit.simile.longwell;

import java.net.MalformedURLException;
import java.net.URL;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.velocity.app.VelocityEngine;
import org.openrdf.model.Literal;
import org.openrdf.model.URI;
import org.openrdf.model.Value;

import edu.mit.simile.longwell.query.Query;

/**
 * An HTTP request which has been parsed into useful parts.
 *
 * The Message also carries references to a few other things which are
 * helpful (or necessary) for carrying out the message: the HTTP
 * request, the HTTP response object, and the Velocity templating
 * engine.
 * 
 * A Message is executed by passing it to the Command.execute() method
 * of the Command it refers to. (If there is no "&command=..." part of
 * the query, the command "start" is assumed.)
 */
public class Message {
    final public LongwellServlet m_servlet;

    final public HttpServletRequest m_request;

    final public HttpServletResponse m_response;

    final public VelocityEngine m_ve;

    final public String m_profileID;

    final public Query m_query;

    final public String m_locale;

    public Message(LongwellServlet servlet, HttpServletRequest request, HttpServletResponse response,
            VelocityEngine ve, String profileID, Query query, String locale) {
        m_servlet = servlet;

        m_request = request;
        m_response = response;
        m_ve = ve;

        m_profileID = profileID;
        m_query = query;
        m_locale = locale;
    }

    public Profile getProfile() {
        return LongwellServlet.getLongwellService().getProfile(m_profileID);
    }

    public Query getQuery() {
        return m_query;
    }

    public String getURL(Value v) {
        String s = "";
        if (v instanceof URI) {
            URI uri = (URI) v;
            s = uri.toString();

            if (getProfile().containsObject(uri)) {
                LongwellURL longwellURL = new LongwellURL(this).changeCommandQuery("focus", "objectURI=" + Utilities.encode(s));
                return longwellURL.toURLString();
            }
        } else if (v instanceof Literal) {
            s = ((Literal) v).getLabel();
        }

        try {
            URL url = new URL(s);

            return url.toExternalForm();
        } catch (MalformedURLException e) {
            return "";
        }
    }
    
    public String getContextPath() {
        String proxyContextPath = m_servlet.getProxyContextPath();
        return proxyContextPath != null ? proxyContextPath : m_request.getContextPath();
    }
}
