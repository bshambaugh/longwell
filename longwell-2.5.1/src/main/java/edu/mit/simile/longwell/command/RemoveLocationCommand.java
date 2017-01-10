package edu.mit.simile.longwell.command;

import javax.servlet.ServletException;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;

import edu.mit.simile.longwell.Message;

public class RemoveLocationCommand implements Command {
    
    final static private Logger s_logger = Logger.getLogger(RemoveLocationCommand.class);

    public void execute(Message msg) throws ServletException {
        try {
            String objectURI = msg.m_query.getFirstParamValue("objectURI");
            URI object = new URIImpl(objectURI);

            RepositoryConnection c = null;
            try {
                c = msg.getProfile().getRepository().getConnection();
                c.setAutoCommit(false);

                String[] propertyURIs = StringUtils.splitPreserveAllTokens(msg.m_query.getFirstParamValue("latlong"), ';');
                for (int i = 0; i < propertyURIs.length; i++) {
                    String propertyURI = propertyURIs[i];
                    if (propertyURI != null && propertyURI.length() > 0) {
                        URI property = new URIImpl(propertyURI);
                        c.remove((Resource) null, property, object);
                    }
                }

                c.commit();
            } catch (RepositoryException e) {
                if (c != null) c.rollback();
            } finally {
                if (c != null) c.close();
            }
        } catch (Throwable e) {
            s_logger.error(e);
        }
    }
}
