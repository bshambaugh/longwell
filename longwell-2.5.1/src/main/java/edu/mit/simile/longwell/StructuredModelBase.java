package edu.mit.simile.longwell;

public abstract class StructuredModelBase extends ProfileListenerBase implements IStructuredModel {

    final protected Profile m_profile;
    final protected QueryManager m_queryManager;

    protected StructuredModelBase(Profile profile) {
        m_profile = profile;
        m_queryManager = profile.getQueryManager();
    }

    public void index(boolean regenerate) {
        // by default do nothing
    }
    
    public void dispose() {
        // by default do nothing
    }
    
    public void optimize() {
        // by default do nothing
    }
}
