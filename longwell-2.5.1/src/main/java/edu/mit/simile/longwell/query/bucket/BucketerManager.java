package edu.mit.simile.longwell.query.bucket;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.openrdf.query.QueryEvaluationException;
import org.openrdf.repository.RepositoryConnection;

import edu.mit.simile.longwell.Profile;
import edu.mit.simile.longwell.ProfileListenerBase;

public class BucketerManager extends ProfileListenerBase {

    final protected Profile m_profile;

    final protected Map<String,IBucketer> m_bucketerNameToInstance = new HashMap<String,IBucketer>();

    public BucketerManager(Profile profile) {
        m_profile = profile;
    }

    public IBucketer getBucketer(String name) throws QueryEvaluationException {
        if (name == null) {
            name = DistinctValueBucketer.class.getName();
        }

        IBucketer bucketer = (IBucketer) m_bucketerNameToInstance.get(name);

        if (bucketer == null) {
            try {
                Constructor constructor = Class.forName(name).getConstructor(new Class[] { Profile.class });

                bucketer = (IBucketer) constructor.newInstance(new Object[] { m_profile });

                m_bucketerNameToInstance.put(name, bucketer);
            } catch (Exception e) {
                throw new QueryEvaluationException("Failed to create bucketer of name " + name, e);
            }
        }

        return bucketer;
    }

    public void onBeforeAdd(RepositoryConnection c) {
        Iterator i = m_bucketerNameToInstance.values().iterator();
        while (i.hasNext()) {
            IBucketer bucketer = (IBucketer) i.next();

            bucketer.onBeforeAdd(c);
        }
    }

    public void onAfterAdd(RepositoryConnection c) {
        Iterator i = m_bucketerNameToInstance.values().iterator();
        while (i.hasNext()) {
            IBucketer bucketer = (IBucketer) i.next();

            bucketer.onAfterAdd(c);
        }
    }

    public void onFailingAdd(RepositoryConnection c) {
        Iterator i = m_bucketerNameToInstance.values().iterator();
        while (i.hasNext()) {
            IBucketer bucketer = (IBucketer) i.next();

            bucketer.onFailingAdd(c);
        }
    }

    public void onBeforeRemove(RepositoryConnection c) {
        Iterator i = m_bucketerNameToInstance.values().iterator();
        while (i.hasNext()) {
            IBucketer bucketer = (IBucketer) i.next();

            bucketer.onBeforeRemove(c);
        }
    }

    public void onAfterRemove(RepositoryConnection c) {
        Iterator i = m_bucketerNameToInstance.values().iterator();
        while (i.hasNext()) {
            IBucketer bucketer = (IBucketer) i.next();

            bucketer.onAfterRemove(c);
        }
    }

    public void onFailingRemove(RepositoryConnection c) {
        Iterator i = m_bucketerNameToInstance.values().iterator();
        while (i.hasNext()) {
            IBucketer bucketer = (IBucketer) i.next();

            bucketer.onAfterAdd(c);
        }
    }
}
