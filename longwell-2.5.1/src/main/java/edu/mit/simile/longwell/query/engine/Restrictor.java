package edu.mit.simile.longwell.query.engine;

import edu.mit.simile.longwell.query.bucket.IBucketer;
import edu.mit.simile.longwell.query.project.IProjector;

final public class Restrictor {

    final public IProjector m_projector;
    final public IBucketer m_bucketer;
    final public String m_bucketerParameter;
    final public int m_restrictionID;

    public Restrictor(IProjector projector, IBucketer bucketer, String bucketerParameter, int restrictionID) {
        m_projector = projector;
        m_bucketer = bucketer;
        m_bucketerParameter = bucketerParameter;
        m_restrictionID = restrictionID;
    }

    public IProjector getProjector() {
        return m_projector;
    }

    public IBucketer getBucketer() {
        return m_bucketer;
    }
}
