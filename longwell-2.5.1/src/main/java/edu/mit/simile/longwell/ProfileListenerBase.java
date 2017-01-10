package edu.mit.simile.longwell;

import org.openrdf.repository.RepositoryConnection;

public class ProfileListenerBase implements IProfileListener {

    public void onBeforeAdd(RepositoryConnection c) {
        onBeforeChange(c);
    }

    public void onAfterAdd(RepositoryConnection c) {
        onAfterChange(c);
    }

    public void onFailingAdd(RepositoryConnection c) {
        onFailingChange(c);
    }

    public void onBeforeRemove(RepositoryConnection c) {
        onBeforeChange(c);
    }

    public void onAfterRemove(RepositoryConnection c) {
        onAfterChange(c);
    }

    public void onFailingRemove(RepositoryConnection c) {
        onFailingChange(c);
    }

    protected void onBeforeChange(RepositoryConnection c) {
        // No work to do by default
    }

    protected void onAfterChange(RepositoryConnection c) {
        // No work to do by default
    }

    protected void onFailingChange(RepositoryConnection c) {
        // No work to do by default
    }
}
