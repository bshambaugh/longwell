package edu.mit.simile.longwell.schema;

import java.text.DateFormat;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.openrdf.model.Literal;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.model.vocabulary.RDFS;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.RepositoryResult;

import edu.mit.simile.longwell.Cache;
import edu.mit.simile.longwell.CacheFactory;
import edu.mit.simile.longwell.FixedSetBuilder;
import edu.mit.simile.longwell.Namespaces;
import edu.mit.simile.longwell.Profile;
import edu.mit.simile.longwell.QueryManager;
import edu.mit.simile.longwell.RepositoryFactory;
import edu.mit.simile.longwell.StructuredModelBase;
import edu.mit.simile.longwell.TagModel;
import edu.mit.simile.longwell.Utilities;

public class SchemaModel extends StructuredModelBase {

    final static private Logger s_logger = Logger.getLogger(SchemaModel.class);

    final static String s_learnedNamespace = "urn:simile.mit.edu:learned:";
    final static String s_learned_Class = s_learnedNamespace + "Class";
    final static String s_learned_Property = s_learnedNamespace + "Property";

    protected Map<URI,URI> m_typeLabelPropertyCache = new HashMap<URI,URI>();

    protected Cache m_itemToLabel;
    protected Cache m_itemToType;
    protected Cache m_itemsToProperties;
    protected QueryManager m_queryManager;
    protected Set<Value> m_allItems;

    protected Map<URI,LearnedClass> m_classes = new HashMap<URI,LearnedClass>();
    protected Map<URI,LearnedProperty> m_properties = new HashMap<URI,LearnedProperty>();

    protected boolean m_initialized;
    protected Repository m_repository;
    protected RepositoryFactory m_factory;
    
    public class LabeledValue {
        final public Value m_value;

        final public String m_label;

        public LabeledValue(Value value, String label) {
            m_value = value;
            m_label = label;
        }

        public Value getValue() {
            return m_value;
        }

        public String getLabel() {
            return m_label;
        }

        public boolean isLiteral() {
            return m_value instanceof Literal;
        }
    }

    public class PropertyValuesPair {
        final public LabeledValue m_property;

        final public SortedSet m_values;

        final public boolean m_isLikelyLiteral;

        public PropertyValuesPair(LabeledValue property, SortedSet values, boolean isLikelyLiteral) {
            m_property = property;
            m_values = values;
            m_isLikelyLiteral = isLikelyLiteral;
        }

        public LabeledValue getProperty() {
            return m_property;
        }

        public SortedSet getValues() {
            return m_values;
        }

        public boolean isLikelyLiteral() {
            return m_isLikelyLiteral;
        }
    }
    
    /**
     * @param profile
     * @param dir
     */
    public SchemaModel(Profile profile, RepositoryFactory factory) {
        super(profile);
        CacheFactory f = profile.getCacheFactory();
        m_queryManager = profile.getQueryManager();
        m_itemToLabel = f.getCache("items-to-label", true);
        m_itemToType = f.getCache("items-to-type", true);
        m_itemsToProperties = f.getCache("items-to-properties", true);
        m_factory = factory;
    }

    public void index(boolean regenerate) {
        if (s_logger.isDebugEnabled()) s_logger.debug("> index()");
        internalInitialize();
        if (s_logger.isDebugEnabled()) s_logger.debug("< index()");
    }
    
    public void dispose() {
        if (s_logger.isDebugEnabled()) s_logger.debug("> dispose()");
        flushCaches();
        m_repository = null;
        if (s_logger.isDebugEnabled()) s_logger.debug("< dispose()");
    }

    public Set<Value> getAllItems() {
        internalInitialize();
        if (m_allItems == null) {
            m_allItems = internalGetAllItems();
        }
        return m_allItems;
    }

    public Set getItemsOfClass(URI klass) {
        internalInitialize();

        LearnedClass learnedClass = m_classes.get(klass);

        return learnedClass != null ? learnedClass.getItems() : new HashSet();
    }

    public Set<LearnedClass> getItemsOfClasses(Set<URI> classes) {
        internalInitialize();

        FixedSetBuilder builder = new FixedSetBuilder() {
            Set<URI> m_classes;

            protected FixedSet createFixedSet(Object[] elmts, Comparator c) {
                return new ItemsOfClassesSet(SchemaModel.this, m_classes, elmts, c);
            }

            FixedSetBuilder init(Set<URI> classes) {
                m_classes = new HashSet<URI>(classes);
                return this;
            }
        }.init(classes);

        Iterator i = classes.iterator();
        while (i.hasNext()) {
            URI klass = (URI) i.next();
            LearnedClass learnedClass = m_classes.get(klass);
            if (learnedClass != null) {
                builder.addAll(learnedClass.getItems());
            }
        }
        return builder.buildFixedSet();
    }

    public Collection<LearnedClass> getLearnedClasses() {
        internalInitialize();
        return m_classes.values();
    }
    
    public Collection<LearnedProperty> getLearnedProperties() {
        internalInitialize();
        return m_properties.values();
    }

    public Set<URI> getLearnedClassURIs() {
        internalInitialize();
        return new HashSet<URI>(m_classes.keySet());
    }

    public LearnedClass getLearnedClass(URI klass) {
        internalInitialize();

        return (LearnedClass) m_classes.get(klass);
    }

    public LearnedClass getLearnedClassOfItem(URI item) {
        return getLearnedClass(getLearnedClassURIOfItem(item));
    }

    public URI getLearnedClassURIOfItem(URI item) {
        internalInitialize();
        return getClass(item);
    }

    public LearnedProperty getLearnedProperty(URI property) {
        internalInitialize();
        return m_properties.get(property);
    }

    public Collection<LearnedClass> getLearnedClassesOfItems(Set items) {
        internalInitialize();

        if (items instanceof AllItemsSet) {
            return getLearnedClasses();
        } else if (items instanceof ItemsOfClassesSet) {
            Set classURIs = ((ItemsOfClassesSet) items).m_classes;
            Set<LearnedClass> classes = new HashSet<LearnedClass>();

            Iterator i = classURIs.iterator();
            while (i.hasNext()) {
                LearnedClass learnedClass = getLearnedClass((URI) i.next());
                if (learnedClass != null) {
                    classes.add(learnedClass);
                }
            }
            return classes;
        } else {
            Set<LearnedClass> classes = new HashSet<LearnedClass>();

            Iterator<LearnedClass> i = new HashSet<LearnedClass>(m_classes.values()).iterator();
            while (i.hasNext()) {
                LearnedClass learnedClass = i.next();
                if (learnedClass.getItems().hashCode() == items.hashCode()) {
                    classes.add(learnedClass);
                    return classes;
                }
            }

            i = items.iterator();
            while (i.hasNext()) {
                Object o = i.next();
                if (o instanceof URI) {
                    LearnedClass learnedClass = getLearnedClassOfItem((URI) o);
                    if (learnedClass != null) {
                        classes.add(learnedClass);
                    }
                }
            }
            return classes;
        }
    }

    public Set getLearnedClassURIsOfItems(Set items) {
        internalInitialize();

        if (items instanceof AllItemsSet) {
            return getLearnedClassURIs();
        } else if (items instanceof ItemsOfClassesSet) {
            return ((ItemsOfClassesSet) items).m_classes;
        } else {
            Set<URI> classes = new HashSet<URI>();

            Iterator<LearnedClass> i = new HashSet<LearnedClass>(m_classes.values()).iterator();
            while (i.hasNext()) {
                LearnedClass learnedClass = i.next();
                if (learnedClass.getItems().hashCode() == items.hashCode()) {
                    classes.add(learnedClass.getURI());
                }
            }
            if (classes.size() > 0) {
                return classes;
            }

            return null;
            
/* TODO: This is too expensive to do when the set of items isn't in any cache.
 * 
            i = items.iterator();
            while (i.hasNext()) {
                URI classURI = getLearnedClassURIOfItem((URI) i.next());
                if (classURI != null) {
                    classes.add(classURI);
                }
            }
            return classes;
*/
        }
    }

    public SortedSet<LearnedProperty> getSortedLearnedProperties(Set items) throws RepositoryException {
        internalInitialize();

        SortedSet<LearnedProperty> learnedProperties = (SortedSet) m_itemsToProperties.get(items);
        if (learnedProperties == null) {
            SchemaModel schemaModel = m_profile.getSchemaModel();
            Collection<LearnedClass> sClasses = schemaModel.getLearnedClassesOfItems(items);

            learnedProperties = new TreeSet<LearnedProperty>(new Comparator() {
                public boolean equals(Object obj) {
                    return false;
                }

                public int compare(Object o1, Object o2) {
                    LearnedProperty p1 = (LearnedProperty) o1;
                    LearnedProperty p2 = (LearnedProperty) o2;

                    long f1 = p1.countOccurrences();
                    long f2 = p2.countOccurrences();
                    int i = f1 == f2 ? 0 : (f1 > f2 ? -1 : 1);
                    if (i == 0) {
                        i = p1.getURI().toString().compareTo(p2.getURI().toString());
                    }
                    return i;
                }
            });

            Iterator i = sClasses.iterator();
            while (i.hasNext()) {
                LearnedClass learnedClass = (LearnedClass) i.next();

                if (learnedClass.countItems() > 0) {
                    Iterator j = learnedClass.getProperties(true).iterator();
                    while (j.hasNext()) {
                        URI propertyURI = (URI) j.next();
                        LearnedProperty learnedProperty = getLearnedProperty(propertyURI);
                        if (learnedProperty == null) {
                            learnedProperty = new LearnedProperty(this, propertyURI);
                            m_properties.put(propertyURI, learnedProperty);

                            RepositoryConnection repoConnection = null, cacheConnection = null;
                            try {
                                repoConnection = m_profile.getRepository().getConnection();
                                cacheConnection = m_repository.getConnection();
                                cacheConnection.setAutoCommit(false);
                                ((LearnedProperty) learnedProperty).learnAddition(repoConnection, cacheConnection);
                                cacheConnection.commit();
                            } catch (RepositoryException e) {
                                if (cacheConnection != null) cacheConnection.rollback();
                            } finally {
                                if (repoConnection != null) repoConnection.close();
                                if (cacheConnection != null) cacheConnection.close();
                            }
                        }
                        learnedProperties.add(learnedProperty);
                    }
                }
            }

            m_itemsToProperties.put(items, learnedProperties);
        }
        return learnedProperties;
    }

    public String getLabel(URI item, String locale) throws RepositoryException {

        internalInitialize();

        String label = (String) m_itemToLabel.get(item);
        if (label == null) {
            label = getItemLabel(item, m_profile.getRepository());
            if (label == null) label = "";
            m_itemToLabel.put(item, label);
        }

        return label;
    }

    public SortedSet getPropertyValuesPairs(URI object, String locale, boolean forward) {
        internalInitialize();

        TreeSet<PropertyValuesPair> pairs = new TreeSet<PropertyValuesPair>(new PropertyValuePairsComparator());
        try {
            RepositoryConnection c = null;
            try {
                c = m_profile.getRepository().getConnection();
                Map m = forward ? m_queryManager.mapForwardProperties(c, object) : m_queryManager.mapBackwardProperties(c, object);
    
                Iterator i = m.keySet().iterator();
                while (i.hasNext()) {
                    URI p = (URI) i.next();
                    LearnedProperty lProperty = getLearnedProperty(p);
    
                    boolean isLiteral = lProperty == null ? false : (lProperty
                            .getTypeConfidence(LearnedProperty.s_type_literal) > 0.5);
                    boolean isDate = lProperty == null ? false : (lProperty
                            .getTypeConfidence(LearnedProperty.s_type_dateTime) > 0.5);
    
                    TreeSet<LabeledValue> values = new TreeSet<LabeledValue>(new LabeledValueComparator());
    
                    Iterator j = ((Set) m.get(p)).iterator();
                    while (j.hasNext()) {
                        Value v = (Value) j.next();
                        if (v instanceof URI) {
                            values.add(makeLabeledResource((URI) v, locale));
                        } else {
                            Literal l = (Literal) v;
                            values.add(makeLabeledLiteral(l, locale, isDate));
                        }
                    }
    
                    if (values.size() > 0) {
                        pairs.add(new PropertyValuesPair(makeLabeledResource(p, locale), values, isLiteral));
                    }
                }
    
                m.clear();
            } finally {
                if (c != null) c.close();
            }
        } catch (Exception e) {
            s_logger.error(e);
        }
        return pairs;
    }

    public Profile getProfile() {
        return m_profile;
    }

    public void onBeforeAdd(RepositoryConnection c) {
        super.onBeforeAdd(c);
        internalInitialize();
    }

    public void onAfterAdd(RepositoryConnection c) {
        super.onAfterAdd(c);
        internalInitialize();
        try {
            RepositoryConnection cacheC = null;
            try {
                cacheC = m_repository.getConnection();
                cacheC.setAutoCommit(false);
                learnAddition(c, cacheC);
                cacheC.commit();
            } catch (RepositoryException e) {
                if (cacheC != null) cacheC.rollback();
            } finally {
                cacheC.close();
            }
        } catch (RepositoryException e) {
            s_logger.error(e);
        }
    }

    public void onAfterRemove(RepositoryConnection c) {
        super.onAfterRemove(c);
        internalInitialize();
        learnRemoval(c);
    }

    protected void internalInitialize() {
        if (m_initialized) {
            return;
        }

        if (s_logger.isDebugEnabled()) s_logger.debug("> internalInitialize()");
        try {
            m_repository = m_factory.getRepository(RepositoryFactory.SCHEMA_CACHE);

            RepositoryConnection cacheConnection = null;
            try {
                cacheConnection = m_repository.getConnection();
                cacheConnection.setAutoCommit(false);
    
                if (cacheConnection.isEmpty()) {
                    RepositoryConnection profileConnection = null;
                    try {
                        profileConnection = m_profile.getRepository().getConnection();
                        learnAddition(profileConnection, cacheConnection);
                    } finally {
                        if (profileConnection != null) profileConnection.close();
                    }
                } else {
                    load(cacheConnection);
                }
    
                cacheConnection.commit();
            } catch (RepositoryException e) {
                if (cacheConnection != null) cacheConnection.rollback();
            } finally {
                if (cacheConnection != null) cacheConnection.close();
            }

            m_initialized = true;
        } catch (Exception e) {
            s_logger.error(e);
        }
        if (s_logger.isDebugEnabled()) s_logger.debug("< internalInitialize()");
    }

    protected void load(RepositoryConnection cacheConnection) throws RepositoryException {
        if (s_logger.isDebugEnabled()) s_logger.debug("> load() - classes");
        try {
            RepositoryResult<Statement> results = cacheConnection.getStatements(null, RDF.TYPE, new URIImpl(SchemaModel.s_learned_Class), true);

            while (results.hasNext()) {
                URI klass = Utilities.dupURI((URI) ((Statement) results.next()).getSubject());

                LearnedClass learnedClass = (LearnedClass) m_classes.get(klass);
                if (learnedClass == null) {
                    learnedClass = new LearnedClass(this, klass);
                    m_classes.put(klass, learnedClass);
                }
                learnedClass.load(cacheConnection);
            }
            
            results.close();
        } catch (Exception e) {
            s_logger.error(e);
        }
        if (s_logger.isDebugEnabled()) s_logger.debug("< load() - classes");

        if (s_logger.isDebugEnabled()) s_logger.debug("> load() - properties");
        try {
            RepositoryResult<Statement> results = cacheConnection.getStatements(null, RDF.TYPE, new URIImpl(SchemaModel.s_learned_Property), true);

            while (results.hasNext()) {
                URI property = Utilities.dupURI((URI) ((Statement) results.next()).getSubject());

                LearnedProperty learnedProperty = (LearnedProperty) m_properties.get(property);
                if (learnedProperty == null) {
                    learnedProperty = new LearnedProperty(this, property);
                    m_properties.put(property, learnedProperty);
                }
                learnedProperty.load(cacheConnection);
            }
            
            results.close();
        } catch (Exception e) {
            s_logger.error(e);
        }
        if (s_logger.isDebugEnabled()) s_logger.debug("< load() - properties");
    }

    protected void learnAddition(RepositoryConnection profileConnection, RepositoryConnection cacheConnection) {
        if (s_logger.isDebugEnabled()) s_logger.debug("> learnAddition() - properties");
        try {
            Set properties = m_queryManager.listProperties(profileConnection);

            if (properties.contains(RDF.TYPE)) {
                m_allItems = null;
            }

            if (properties.contains(RDFS.LABEL) || properties.contains(DC_TITLE)) {
                flushCaches();
            }

            Iterator i = properties.iterator();
            while (i.hasNext()) {
                URI property = (URI) i.next();

                LearnedProperty learnedProperty = (LearnedProperty) m_properties.get(property);
                if (learnedProperty == null) {
                    learnedProperty = new LearnedProperty(this, property);
                    m_properties.put(property, learnedProperty);
                }
                learnedProperty.learnAddition(profileConnection, cacheConnection);
            }
        } catch (Exception e) {
            s_logger.error(e);
        }
        if (s_logger.isDebugEnabled()) s_logger.debug("< learnAddition() - properties");

        if (s_logger.isDebugEnabled()) s_logger.debug("> learnAddition() - classes");
        try {
            Iterator i = m_queryManager.listObjectsOfProperty(profileConnection, RDF.TYPE).iterator();
            while (i.hasNext()) {
                URI klass = (URI) i.next();

                LearnedClass learnedClass = (LearnedClass) m_classes.get(klass);
                if (learnedClass == null) {
                    learnedClass = new LearnedClass(this, klass);
                    m_classes.put(klass, learnedClass);
                }
                learnedClass.learnAddition(profileConnection, cacheConnection);
            }
        } catch (Exception e) {
            s_logger.error(e);
        }
        if (s_logger.isDebugEnabled()) s_logger.debug("< learnAddition() - classes");
    }

    protected void learnRemoval(RepositoryConnection profileConnection) {
        if (s_logger.isDebugEnabled()) s_logger.debug("> learnRemoval() - properties");
        try {
            Set properties = m_queryManager.listProperties(profileConnection);

            if (properties.contains(RDF.TYPE)) {
                m_allItems = null;
            }

            if (properties.contains(RDFS.LABEL) || properties.contains(DC_TITLE)) {
                flushCaches();
            }

            Iterator i = properties.iterator();
            while (i.hasNext()) {
                URI property = (URI) i.next();

                LearnedProperty learnedProperty = (LearnedProperty) m_properties.get(property);
                if (learnedProperty != null) {
                    learnedProperty.learnRemoval(profileConnection);
                }
            }
        } catch (Exception e) {
            s_logger.error(e);
        }
        if (s_logger.isDebugEnabled()) s_logger.debug("< learnRemoval() - properties");

        if (s_logger.isDebugEnabled()) s_logger.debug("> learnRemoval() - classes");
        try {
            Iterator i = m_queryManager.listObjectsOfProperty(profileConnection, RDF.TYPE).iterator();
            while (i.hasNext()) {
                URI klass = (URI) i.next();

                LearnedClass learnedClass = (LearnedClass) m_classes.get(klass);
                if (learnedClass != null) {
                    learnedClass.learnRemoval(profileConnection);
                }
            }
        } catch (Exception e) {
            s_logger.error(e);
        }
        if (s_logger.isDebugEnabled()) s_logger.debug("< learnRemoval() - classes");
    }

    protected LabeledValue makeLabeledResource(URI r, String locale) throws RepositoryException {
        return new LabeledValue(r, getLabel(r, locale));
    }

    protected LabeledValue makeLabeledLiteral(Literal l, String locale, boolean isDate) {
        String s = l.getLabel();
        if (isDate) {
            try {
                s = DateFormat.getDateTimeInstance().format(Utilities.parseDate(s));
            } catch (Exception e) {
            }
        }
        return new LabeledValue(l, s);
    }

    protected Set<Value> internalGetAllItems() {
        FixedSetBuilder builder = new FixedSetBuilder() {
            protected FixedSet createFixedSet(Object[] elmts, Comparator c) {
                return new AllItemsSet(SchemaModel.this, elmts, c);
            }
        };

        Iterator i = getLearnedClasses().iterator();
        while (i.hasNext()) {
            LearnedClass learnedClass = (LearnedClass) i.next();

            builder.addAll(learnedClass.getItems());
        }
        return builder.buildFixedSet();
    }

    protected URI getClass(URI item) {
        if (s_logger.isDebugEnabled()) s_logger.debug("> getClass(" + item + ")");
        URI klass = (URI) m_itemToType.get(item);
        if (klass == null) {
            try {
                RepositoryConnection c = null;
                try {
                    c = m_profile.getRepository().getConnection();
                    Value v = m_queryManager.getObjectOfProperty(c, item, RDF.TYPE);
                    if (v instanceof URI) {
                        klass = (URI) v;
                        m_itemToType.put(item, klass);
                    }
                } finally {
                    if (c != null) c.close();
                }
            } catch (Exception e) {
                s_logger.error(e);
            }
        }
        if (s_logger.isDebugEnabled()) s_logger.debug("< getClass(" + item + ")");
        return klass;
    }

    final private URI DC_TITLE = new URIImpl(Namespaces.s_dc + "title");

    private ResourceBundle resources = ResourceBundle.getBundle(SchemaModel.class.getName());
    
    protected String getItemLabel(URI item, Repository r) throws RepositoryException {
        String label = null;

        RepositoryConnection c = null;
        
        try {
            c = r.getConnection();
            
            /**
             * TODO [dfhuynh]: This dependency on the tag model is not a good thing.
             * We need to redesign this label retrieval out somehow.
             */
            if (item.toString().startsWith(TagModel.s_tagNamespace)) {
                try {
                    TagModel tagModel = (TagModel) m_profile.getStructuredModel(TagModel.class);
                    return tagModel.getTagLabel(item);
                } catch (Exception e) {
                    // Continue with the rest of the function
                }
            }
    
            Map<URI,Set<Value>> properties = m_queryManager.mapForwardProperties(c, item);

            if (label == null && properties.containsKey(RDFS.LABEL)) {
                Iterator<Value> v = properties.get(RDFS.LABEL).iterator();
                if (v.hasNext()) {
                    label = v.next().toString();
                }
            }
            
            if (label == null && properties.containsKey(RDF.VALUE)) {
                Iterator<Value> v = properties.get(RDF.VALUE).iterator();
                if (v.hasNext()) {
                    label = v.next().toString();
                }
            }

            if (label == null && properties.containsKey(DC_TITLE)) {
                Iterator<Value> v = properties.get(DC_TITLE).iterator();
                if (v.hasNext()) {
                    label = v.next().toString();
                }
            }
            
            if (label == null) {
                if (Utilities.isBNode(item)) {
                    label = resources.getString("AnonymousLabel");
                } else {
                    label = Utilities.abbreviateURI(item.toString());
                }
            }
        } catch (Exception e) {
            s_logger.error("Exception trying to get a label for " + item, e);
        } finally {
            if (c != null) c.close();
        }

        return label;
    }

    protected void flushCaches() {
        if (s_logger.isDebugEnabled()) s_logger.debug("> flushCaches()");
        m_typeLabelPropertyCache.clear();
        m_itemToLabel.clear();
        m_itemToType.clear();
        m_itemsToProperties.clear();
        m_allItems = null;
        if (s_logger.isDebugEnabled()) s_logger.debug("< flushCaches()");
    }
}

class LabeledValueComparator implements Comparator {

    public int compare(Object o1, Object o2) {
        return compareLabeledValues((SchemaModel.LabeledValue) o1, (SchemaModel.LabeledValue) o2);
    }

    static int compareLabeledValues(SchemaModel.LabeledValue lv1, SchemaModel.LabeledValue lv2) {

        int i = lv1.m_label.compareToIgnoreCase(lv2.m_label);
        if (i == 0) {
            i = lv1.m_value.toString().compareTo(lv2.m_value.toString());
        }
        return i;
    }
}

class PropertyValuePairsComparator implements Comparator {

    public int compare(Object o1, Object o2) {
        SchemaModel.PropertyValuesPair p1 = (SchemaModel.PropertyValuesPair) o1;
        SchemaModel.PropertyValuesPair p2 = (SchemaModel.PropertyValuesPair) o2;

        return LabeledValueComparator.compareLabeledValues(p1.m_property, p2.m_property);
    }
}

class AllItemsSet extends FixedSetBuilder.FixedSet {
    final SchemaModel m_schemaModel;

    AllItemsSet(SchemaModel schemaModel, Object[] elmts, Comparator comparator) {
        super(elmts, comparator);
        m_schemaModel = schemaModel;
    }
}

class ItemsOfClassesSet extends FixedSetBuilder.FixedSet {
    final SchemaModel m_schemaModel;

    final Set m_classes;

    ItemsOfClassesSet(SchemaModel schemaModel, Set classes, Object[] elmts, Comparator comparator) {
        super(elmts, comparator);
        m_schemaModel = schemaModel;
        m_classes = classes;
    }
}
