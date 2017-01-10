package edu.mit.simile.longwell.query.engine;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.openrdf.model.URI;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.repository.RepositoryException;

import edu.mit.simile.longwell.Cache;
import edu.mit.simile.longwell.CacheFactory;
import edu.mit.simile.longwell.LongwellServlet;
import edu.mit.simile.longwell.Profile;
import edu.mit.simile.longwell.query.ExplicitFacet;
import edu.mit.simile.longwell.query.Facet;
import edu.mit.simile.longwell.query.bucket.IBucketer;
import edu.mit.simile.longwell.query.engine.EngineUtilities.FacetInfo;
import edu.mit.simile.longwell.query.project.IProjection;
import edu.mit.simile.longwell.query.project.IProjector;
import edu.mit.simile.longwell.query.project.URIProjector;
import edu.mit.simile.longwell.schema.LearnedProperty;
import edu.mit.simile.longwell.schema.SchemaModel;

public class Narrower {

    final static private Logger s_logger = Logger.getLogger(Narrower.class);

    final protected Profile m_profile;
    final protected QueryEngine m_engine;
    final protected Cache m_explicitFacetStringToObjectsToRecords;
    final protected Cache m_explicitFacetStringToObjectsToGuessedRecords;
    final protected Cache m_explicitFacetStringToObjectsToConfiguredRecords;

    protected class NarrowingRecord {
        String m_propertyURI;
        boolean m_explicit;
        float m_desirable;
        Facet m_facet;
    }

    Narrower(Profile profile, QueryEngine engine) {
        m_profile = profile;
        m_engine = engine;
        CacheFactory f = profile.getCacheFactory();
        m_explicitFacetStringToObjectsToRecords = f.getCache("explicitFacetString", "objects", false);
        m_explicitFacetStringToObjectsToGuessedRecords = f.getCache("explicitFacetString", "objects", false);
        m_explicitFacetStringToObjectsToConfiguredRecords = f.getCache("explicitFacetString", "objects", false);
    }

    /**
     * Answer a list of Facet's that help narrow down the given set of objects.
     * The explicit facets must be included in the result.
     * 
     * @param objects
     * @param explicitFacets
     * @param locale
     * @param fresh
     * @return
     */
    public List narrow(Set objects, SortedSet explicitFacets, String locale, boolean fresh) throws QueryEvaluationException, RepositoryException {
        URIProjector projector = (URIProjector) m_engine.getProjectorManager().getProjector(
                URIProjector.class.getName(), RDF.TYPE.toString(), locale);
        IProjection projection = projector.project(objects);
        Set types = projection.getValues();
        List configured = LongwellServlet.getLongwellService().getFresnelConfiguration().facets(types);

        List<LearnedProperty> facets = new ArrayList<LearnedProperty>();
        for (Iterator i = configured.iterator(); i.hasNext();) {
            facets.add(m_profile.getSchemaModel().getLearnedProperty((URI) i.next()));
        }
        return facets;
    }
    
    /**
     * Answers a list of Facets that should not be calculated or displayed
     * 
     * @param objects Set of resources to determine what rdf:types are in use
     * @param locale
     * @return A List of Facets
     * @throws QueryEvaluationException
     */
    public List hide(Set objects, String locale) throws QueryEvaluationException, RepositoryException {
        URIProjector projector = (URIProjector) m_engine.getProjectorManager().getProjector(
                URIProjector.class.getName(), RDF.TYPE.toString(), locale);
        IProjection projection = projector.project(objects);
        Set types = projection.getValues();
        List hidden = LongwellServlet.getLongwellService().getFresnelConfiguration().hiddenFacets(types);

        List<LearnedProperty> facets = new ArrayList<LearnedProperty>();
        for (Iterator i = hidden.iterator(); i.hasNext();) {
            facets.add(m_profile.getSchemaModel().getLearnedProperty((URI) i.next()));
        }
        return facets;
    }

    /**
     * Answer a single Facet constructed from the given property URI.
     */
    public Facet narrow(Set objects, String propertyURI, String locale, boolean fresh) throws RepositoryException, QueryEvaluationException {
        SchemaModel schemaModel = m_profile.getSchemaModel();
        LearnedProperty sProperty = schemaModel.getLearnedProperty(new URIImpl(propertyURI));
        Set records = createNarrowingRecordTreeSet();
        if (createFacet(objects, sProperty, locale, null, records, 1, true)) {
            return ((NarrowingRecord) records.iterator().next()).m_facet;
        } else {
            return null;
        }
    }

    protected List internalNarrowConfiguredFacets(Set objects, List configuredFacets, String locale) throws RepositoryException {
        ArrayList records = new ArrayList();
        SchemaModel schemaModel = m_profile.getSchemaModel();

        Iterator i = configuredFacets.iterator();
        while (i.hasNext()) {
            LearnedProperty sProperty = schemaModel.getLearnedProperty((URI) i.next());

            try {
                // they may try to declare a totally bogus property, skip it if
                // so
                if (null != sProperty) createFacet(objects, sProperty, locale, null, records, 1, false);
            } catch (QueryEvaluationException e) {
                s_logger.warn("Failed to create facet", e);
            }
        }

        return records;
    }

    protected List internalNarrowExplicitFacets(Set objects, Set explicitFacets, String locale) throws RepositoryException {
        Set records = createNarrowingRecordTreeSet();
        SchemaModel schemaModel = m_profile.getSchemaModel();

        Iterator i = explicitFacets.iterator();
        while (i.hasNext()) {
            String propertyURI = (String) i.next();
            LearnedProperty sProperty = schemaModel.getLearnedProperty(new URIImpl(propertyURI));

            try {
                createFacet(objects, sProperty, locale, null, records, 1, true);
            } catch (QueryEvaluationException e) {
                s_logger.warn("Failed to create facet", e);
            }
        }

        return new ArrayList(records);
    }

    protected List internalNarrowByGuessing(Set objects, Set explicitFacets, String locale) throws RepositoryException {
        List precedentFacets = new ArrayList();
        Set otherFacets = createNarrowingRecordTreeSet();

        final int maxCount = 5;

        Iterator i = m_profile.getSchemaModel().getSortedLearnedProperties(objects).iterator();
        while (i.hasNext() && precedentFacets.size() + otherFacets.size() < maxCount) {
            LearnedProperty learnedProperty = (LearnedProperty) i.next();
            if (!explicitFacets.contains(learnedProperty.getURI().toString())) {
                int count = precedentFacets.size() + otherFacets.size();

                try {
                    if (!createFacet(objects, learnedProperty, locale, precedentFacets, otherFacets,
                            (maxCount - count - 1) / (float) maxCount, false)) {
                        break;
                    }
                    count++;
                } catch (QueryEvaluationException e) {
                    s_logger.warn("Failed to create facet", e);
                }
            }
        }

        precedentFacets.addAll(otherFacets);

        return precedentFacets;
    }

    protected void updateRecords(List guessedRecords, Set objects, String locale) throws RepositoryException, QueryEvaluationException {
        Iterator i = guessedRecords.iterator();
        while (i.hasNext()) {
            NarrowingRecord record = (NarrowingRecord) i.next();

            updateRecord(record, objects, locale);
        }
    }

    protected boolean createFacet(Set objects, LearnedProperty learnedProperty, String locale, List precedentRecords,
            Collection otherRecords, float desirable, boolean explicit) throws RepositoryException, QueryEvaluationException {
        FacetInfo info = EngineUtilities.getFacetInfo(learnedProperty, true, locale, m_profile);

        IProjector projector = m_engine.getProjectorManager().getProjector(info.m_projectorName,
                info.m_projectorParameter, locale);

        IProjection projection = projector.project(objects);
        s_logger.debug("Considering facet property " + learnedProperty.getURI() + " " + learnedProperty.getUniqueness()
                + " " + learnedProperty.countOccurrences() + " " + (objects.size() / 10) +
                // " " + projection.countObjects((Object) null) +
                " " + (objects.size() / 3));

        // TODO (SM) should this facet selection magic be configurable? or more adaptive?
        int count = objects.size();
        if (desirable < 1 && learnedProperty.countOccurrences() < count / 5 + 20 * Math.exp(-count / 20)) {
            return false;
        }

        /*
         * Get bucketing suggestions
         */
        IBucketer bucketer = m_engine.getBucketerManager().getBucketer(info.m_bucketerName);
        List bucketThemes = bucketer.suggestNarrowingBuckets(projection, desirable);
        if (explicit || bucketThemes.size() > 0) {
            String propertyURI = learnedProperty.getURI().toString();

            Facet facet = new ExplicitFacet(info.m_projectorName, info.m_projectorParameter, info.m_label,
                    bucketThemes, propertyURI);

            NarrowingRecord record = new NarrowingRecord();
            record.m_propertyURI = propertyURI;
            record.m_explicit = explicit;
            record.m_desirable = desirable;
            record.m_facet = facet;

            if (precedentRecords != null && learnedProperty.getURI().equals(RDF.TYPE)) {

                precedentRecords.add(record);
            } else {
                otherRecords.add(record);
            }
        }

        return true;
    }

    protected void updateRecord(NarrowingRecord record, Set objects, String locale) throws RepositoryException, QueryEvaluationException {
        FacetInfo info = EngineUtilities.getFacetInfo(record.m_propertyURI, true, locale, m_profile);

        IProjector projector = m_engine.getProjectorManager().getProjector(info.m_projectorName,
                info.m_projectorParameter, locale);

        IProjection projection = projector.project(objects);

        IBucketer bucketer = m_engine.getBucketerManager().getBucketer(info.m_bucketerName);
        List bucketThemes = bucketer.suggestNarrowingBuckets(projection, record.m_desirable);

        record.m_facet = record.m_explicit ? new ExplicitFacet(info.m_projectorName, info.m_projectorParameter,
                info.m_label, bucketThemes, record.m_propertyURI) : new Facet(info.m_projectorName,
                info.m_projectorParameter, info.m_label, bucketThemes, false);
    }

    protected TreeSet createNarrowingRecordTreeSet() {
        return new TreeSet(new Comparator() {
            public boolean equals(Object obj) {
                return false;
            }

            public int compare(Object o1, Object o2) {
                NarrowingRecord r1 = (NarrowingRecord) o1;
                NarrowingRecord r2 = (NarrowingRecord) o2;
                Facet f1 = r1.m_facet;
                Facet f2 = r2.m_facet;
                int i = f1.m_label.compareToIgnoreCase(f2.m_label);

                if (i == 0) {
                    i = f1.m_projectorName.compareTo(f2.m_projectorName);
                }
                if (i == 0) {
                    i = o1.toString().compareTo(o2.toString());
                }
                return i;
            }
        });
    }
}
