package edu.mit.simile.longwell;

import java.util.Iterator;

import org.apache.log4j.Logger;
import org.openrdf.model.URI;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;

public class PublishingModel extends StructuredModelBase {

    final static private Logger s_logger = Logger.getLogger(PublishingModel.class);
    
    final static public String s_namespace = "http://simile.mit.edu/2005/04/ontologies/publishing#";
    final static public String s_pub_status = s_namespace + "status";
    final static public String s_pub_contributedBy = s_namespace + "contributedBy";
    final static public String s_pub_Public = s_namespace + "Public";
    final static public String s_pub_Private = s_namespace + "Private";
    
	final protected URI m_publisher;
	protected Repository m_toRemove;

	public PublishingModel(Profile profile, URI publisher) {
		super(profile);
		m_publisher = publisher;
	}

	public void onAfterAdd(RepositoryConnection c) {
		super.onAfterAdd(c);
		try {
			((LongwellProfile) LongwellServlet.getLongwellService().getDefaultProfile()).publish(getPublishedObjects(c), m_publisher);
		} catch (Exception e) {
			s_logger.error(e);
		}
	}

	public void onBeforeRemove(RepositoryConnection c) {
		super.onBeforeRemove(c);
		try {
			m_toRemove = getPublishedObjects(c);
		} catch (Exception e) {
			s_logger.error(e);
		}
	}

	public void onAfterRemove(RepositoryConnection c) {
		super.onAfterRemove(c);

		if (m_toRemove != null) {
			((LongwellProfile) LongwellServlet.getLongwellService().getDefaultProfile()).retract(m_toRemove, m_publisher);
			m_toRemove = null;
		}
	}

	public void onFailingRemove(RepositoryConnection c) {
		super.onFailingRemove(c);
		m_toRemove = null;
	}

	protected Repository getPublishedObjects(RepositoryConnection c) throws Exception {
		Repository r2 = Utilities.createMemoryRepository();
        QueryManager qm = LongwellServlet.getLongwellService().getDefaultProfile().getQueryManager();
		Iterator i = qm.listSubjectsOfProperty(c, new URIImpl(s_pub_status), new URIImpl(s_pub_Public)).iterator();

		while (i.hasNext()) {
			URI object = (URI) i.next();
			m_profile.extractObject(object, r2);
		}

		return r2;
	}
}
