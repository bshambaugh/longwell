package edu.mit.simile.longwell.query.project;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.openrdf.query.QueryEvaluationException;
import org.openrdf.repository.RepositoryConnection;

import edu.mit.simile.longwell.Cache;
import edu.mit.simile.longwell.Profile;
import edu.mit.simile.longwell.ProfileListenerBase;

public class ProjectorManager extends ProfileListenerBase {

    final protected Profile m_profile;
    final protected Map<String,ProjectorFamily> m_projectorNameToFamily = new HashMap<String,ProjectorFamily>();

    protected Set m_cachedProjectors;

    protected class ProjectorFamily {
        Constructor m_constructor;
        Cache m_localeToParameterToProjector;
    }

    public ProjectorManager(Profile profile) {
        m_profile = profile;
    }

    public IProjector getProjector(String name, String parameter, String locale) throws QueryEvaluationException {

        if (name == null) {
            name = PropertyProjector.class.getName();
        }
        if (locale == null) {
            locale = "";
        }

        ProjectorFamily family = (ProjectorFamily) m_projectorNameToFamily.get(name);

        if (family == null) {
            try {
                family = new ProjectorFamily();
                family.m_constructor = Class.forName(name).getConstructor(
                        new Class[] { Profile.class, String.class, String.class });
                family.m_localeToParameterToProjector = m_profile.getCacheFactory().getCache("locale", "parameter",
                        true);

                m_projectorNameToFamily.put(name, family);
            } catch (Exception e) {
                throw new QueryEvaluationException("Failed to create projector family of name " + name, e);
            }
        }

        IProjector projector = (IProjector) family.m_localeToParameterToProjector.get(locale, parameter);

        if (projector == null) {
            try {
                projector = (IProjector) family.m_constructor
                        .newInstance(new Object[] { m_profile, parameter, locale });

                family.m_localeToParameterToProjector.put(locale, parameter, projector);

                m_cachedProjectors = null;
            } catch (Exception e) {
                e.printStackTrace();
                throw new QueryEvaluationException("Failed to create projector of name " + name, e);
            }
        }

        return projector;
    }

    public void onBeforeAdd(RepositoryConnection c) {
        Iterator i = getProjectors().iterator();
        while (i.hasNext()) {
            IProjector projector = (IProjector) i.next();

            projector.onBeforeAdd(c);
        }
    }

    public void onAfterAdd(RepositoryConnection c) {
        Iterator i = getProjectors().iterator();
        while (i.hasNext()) {
            IProjector projector = (IProjector) i.next();

            projector.onAfterAdd(c);
        }
    }

    public void onFailingAdd(RepositoryConnection c) {
        Iterator i = getProjectors().iterator();
        while (i.hasNext()) {
            IProjector projector = (IProjector) i.next();

            projector.onFailingAdd(c);
        }
    }

    public void onBeforeRemove(RepositoryConnection c) {
        Iterator i = getProjectors().iterator();
        while (i.hasNext()) {
            IProjector projector = (IProjector) i.next();

            projector.onBeforeRemove(c);
        }
    }

    public void onAfterRemove(RepositoryConnection c) {
        Iterator i = getProjectors().iterator();
        while (i.hasNext()) {
            IProjector projector = (IProjector) i.next();

            projector.onAfterRemove(c);
        }
    }

    public void onFailingRemove(RepositoryConnection c) {
        Iterator i = getProjectors().iterator();
        while (i.hasNext()) {
            IProjector projector = (IProjector) i.next();

            projector.onFailingRemove(c);
        }
    }

    protected Set getProjectors() {
        if (m_cachedProjectors == null) {
            m_cachedProjectors = new HashSet();

            Iterator i = m_projectorNameToFamily.values().iterator();
            while (i.hasNext()) {
                ProjectorFamily family = (ProjectorFamily) i.next();

                family.m_localeToParameterToProjector.collectValues(m_cachedProjectors);
            }
        }
        return m_cachedProjectors;
    }

}
