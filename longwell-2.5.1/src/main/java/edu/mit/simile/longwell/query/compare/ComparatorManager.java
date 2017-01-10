package edu.mit.simile.longwell.query.compare;

import java.lang.reflect.Constructor;

import org.openrdf.query.QueryEvaluationException;

import edu.mit.simile.longwell.Profile;
import edu.mit.simile.longwell.ProfileListenerBase;

public class ComparatorManager extends ProfileListenerBase {
    final protected Profile m_profile;

    public ComparatorManager(Profile profile) {
        m_profile = profile;
    }

    public IComparator getComparator(String name, String parameter) throws QueryEvaluationException {
        if (name == null) {
            name = ComparatorBase.class.getName();
        }
        if (parameter == null) {
            parameter = "";
        }

        try {
            Constructor constructor = Class.forName(name).getConstructor(new Class[] { Profile.class, String.class });

            return (IComparator) constructor.newInstance(new Object[] { m_profile, parameter });
        } catch (Exception e) {
            throw new QueryEvaluationException("Failed to create comparator of name " + name, e);
        }
    }
}
