package edu.mit.simile.longwell;

public interface IStructuredModel extends IProfileListener {

    /**
     * Resets the internal state by reloading from the main repository.
     */
    public void index(boolean regenerate);

    /**
     * Dispose this structured model and free up its resources.
     */
    public void dispose();
    
    /**
     * Compacts the structured model and optimizes its state for performance.
     */
    public void optimize();
    
}
