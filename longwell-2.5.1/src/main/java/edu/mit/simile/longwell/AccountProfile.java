package edu.mit.simile.longwell;

import java.io.File;

import org.openrdf.model.URI;

public class AccountProfile extends LongwellProfile {
	
    final protected URI m_owner;

	public AccountProfile(String profileID, Corpus corpus, CacheFactory cacheFactory, RepositoryFactory factory, File dir, URI owner) {
		super(profileID, corpus, cacheFactory, factory, dir);
		m_owner = owner;

        if (m_owner != null) {
            addStructuredModel(new FacadeStructuredModel(this));
            addStructuredModel(new PublishingModel(this, m_owner));
            addStructuredModel(new TagModel(this, m_factory));
        }
	}

}
