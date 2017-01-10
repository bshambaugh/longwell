package edu.mit.simile.longwell;

import org.openrdf.repository.RepositoryConnection;

public interface IProfileListener {
    
    public void onBeforeAdd(RepositoryConnection connection);

    public void onAfterAdd(RepositoryConnection connection);

    public void onFailingAdd(RepositoryConnection connection);

    public void onBeforeRemove(RepositoryConnection connection);

    public void onAfterRemove(RepositoryConnection connection);

    public void onFailingRemove(RepositoryConnection connection);

}
