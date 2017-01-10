package edu.mit.simile.longwell;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.openrdf.model.Literal;
import org.openrdf.model.URI;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.model.vocabulary.RDFS;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;

import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

public class TagModel extends StructuredModelBase {

    final static private Logger s_logger = Logger.getLogger(TagModel.class);

    final static private String SEPARATOR = "_";

    final static public String s_tagNamespace = "urn:tag:";
    final static public String s_namespace = "http://simile.mit.edu/2005/04/ontologies/tags#";
    final static public String s_tag = s_namespace + "tag";
    
    static protected String s_idForGeneratingURIs;
    static protected String s_tagSuffixHash;

    static public void setIDForGeneratingURIs(String id) {
        s_idForGeneratingURIs = id != null ? id : "unknown";
        s_tagSuffixHash = md5hash(id);
    }

    static public String getIDForGeneratingURIs() {
        return s_idForGeneratingURIs;
    }

    static public String getTagSuffixHash() {
        return s_tagSuffixHash;
    }

    protected boolean m_initialized;

    final protected Map<URI,Set<String>> m_tagToLabels = new HashMap<URI,Set<String>>();
    final protected Map<String,Set<URI>> m_labelToTags = new HashMap<String,Set<URI>>();

    final protected SortedSet<String> m_cachedTagLabels = new TreeSet<String>(new Comparator() {
        public boolean equals(Object obj) {
            return false;
        }

        public int compare(Object o1, Object o2) {
            String s1 = (String) o1;
            String s2 = (String) o2;
            int i = s1.compareToIgnoreCase(s2);
            if (i == 0) {
                i = s1.compareTo(s2);
            }
            return i;
        }
    });

    final protected Cache m_tagToObjects;
    final protected Cache m_labelToObjects;
    final protected Cache m_objectToTags;
    final protected Cache m_objectToTagLabels;
    final protected Cache m_objectsToTags;
    final protected Cache m_objectsToTagLabels;
    final protected File m_dir;
    
    public TagModel(Profile profile, RepositoryFactory factory) {
        super(profile);
        m_dir = factory.getDir(RepositoryFactory.TAG_CACHE);
        CacheFactory f = profile.getCacheFactory();
        m_tagToObjects = f.getCache("tag-to-objects", true);
        m_labelToObjects = f.getCache("label-to-objects", true);
        m_objectToTags = f.getCache("object-to-tags", true);
        m_objectToTagLabels = f.getCache("object-to-tag-labels", true);
        m_objectsToTags = f.getCache("objects-to-tags", true);
        m_objectsToTagLabels = f.getCache("objects-to-tag-labels", true);
    }

    public void dispose() {
        if (s_logger.isDebugEnabled()) s_logger.debug("> dispose()");
        if (m_initialized) {
            try {
                saveToDir();
                new File(m_dir, "lock-file").delete();
            } catch (Exception e) {
                s_logger.warn(e);
            }
        }
        super.dispose();
        if (s_logger.isDebugEnabled()) s_logger.debug("< dispose()");
    }

    public Set<URI> getTags() {
        internalInitialize();
        return new HashSet<URI>(m_tagToLabels.keySet());
    }

    public Set<String> getTagLabels() {
    	if (s_logger.isDebugEnabled()) s_logger.debug("> getTagLabels()");
        internalInitialize();
    	if (s_logger.isDebugEnabled()) s_logger.debug("< getTagLabels()");
        return new HashSet<String>(m_cachedTagLabels);
    }

    public String getTagLabel(URI tag) throws Exception {
        internalInitialize();

        Set<String> labels = new HashSet<String>();

        tagToLabels(tag, labels);

        return labels.size() > 0 ? (String) labels.iterator().next() : "";
    }

    public Set getTagsFromLabel(String label) throws Exception {
        internalInitialize();

        Set<URI> tags = new HashSet<URI>();

        labelToTags(label, tags, true);

        return tags;
    }

    public Set getObjects(URI tag) throws Exception {
        internalInitialize();

        Set objects = (Set) m_tagToObjects.get(tag);
        if (objects == null) {
            objects = internalGetObjects(tag);
            m_tagToObjects.put(tag, objects);
        }
        return objects;
    }

    public int countObjects(URI tag) throws Exception {
        return getObjects(tag).size();
    }

    public Set getObjects(String tagLabel) throws Exception {
        internalInitialize();

        Set objects = (Set) m_labelToObjects.get(tagLabel);
        if (objects == null) {
            Set<URI> tags = new HashSet<URI>();

            labelToTags(tagLabel, tags, false);

            FixedSetBuilder builder = new FixedSetBuilder();

            Iterator i = tags.iterator();
            while (i.hasNext()) {
                URI tag = (URI) i.next();

                builder.addAll(getObjects(tag));
            }

            objects = builder.buildFixedSet();

            m_labelToObjects.put(tagLabel, objects);
        }
        return objects;
    }

    public int countObjects(String tagLabel) throws Exception {
        return getObjects(tagLabel).size();
    }

    public Set<URI> getObjectTags(URI object) throws Exception {
        internalInitialize();

        Set<URI> tags = (Set) m_objectToTags.get(object);
        if (tags == null) {
            tags = internalGetTags(object);
            m_objectToTags.put(object, tags);
        }
        return tags;
    }

    public Set<URI> getObjectTags(Set objects) throws Exception {
        internalInitialize();

        Set<URI> tags = (Set) m_objectsToTags.get(objects);
        if (tags == null) {
            tags = new HashSet<URI>();

            Iterator i = objects.iterator();
            while (i.hasNext()) {
                Set<URI> tags2 = getObjectTags((URI) i.next());
                tags.addAll(tags2);
            }

            m_objectsToTags.put(objects, tags);
        }
        return tags;
    }

    public Set<String> getObjectTagLabels(URI object) throws Exception {
        internalInitialize();

        Set<String> tagLabels = (Set) m_objectToTagLabels.get(object);
        if (tagLabels == null) {
            tagLabels = internalGetTagLabels(object);
            m_objectToTagLabels.put(object, tagLabels);
        }
        return tagLabels;
    }

    public Set getObjectTagLabels(Set objects) throws Exception {
        internalInitialize();

        Set<String> tagLabels = (Set) m_objectsToTagLabels.get(objects);
        if (tagLabels == null) {
            tagLabels = new HashSet<String>();

            Iterator i = objects.iterator();
            while (i.hasNext()) {
                Set<String> tagLabels2 = getObjectTagLabels((URI) i.next());
                tagLabels.addAll(tagLabels2);
            }

            m_objectsToTagLabels.put(objects, tagLabels);
        }
        return tagLabels;
    }

    public String getObjectTagString(URI object) throws Exception {
        SortedSet<String> tags = new TreeSet<String>(getObjectTagLabels(object));
        String separator = "";
        String s = "";

        Iterator i = tags.iterator();
        while (i.hasNext()) {
            s = s + separator + i.next();
            separator = ", ";
        }

        return s;
    }

    public List<String> completeTags(String prefix, int max) throws Exception {
        internalInitialize();

        List<String> tags = new ArrayList<String>();

        prefix = prefix.toLowerCase();

        SortedSet tailSet = m_cachedTagLabels.tailSet(prefix);
        Iterator i = tailSet.iterator();
        while (i.hasNext() && max > 0) {
            String tag = (String) i.next();
            if (tag.toLowerCase().startsWith(prefix)) {
                tags.add(tag);
                max--;
            } else {
                break;
            }
        }

        return tags;
    }

    public void clearTags(URI item) throws Exception {
        Set<URI> set = new HashSet<URI>();
        set.add(item);
        clearTags(set);
    }

    public void clearTags(Set items) throws Exception {
        Repository to = null, from = null;
        RepositoryConnection toC = null, fromC = null; 
        
        try {
            to = Utilities.createMemoryRepository();
            toC = to.getConnection();
            toC.setAutoCommit(false);

            from = m_profile.getRepository();
            fromC = from.getConnection();

            URI property = new URIImpl(s_tag);

            Iterator i = items.iterator();
            while (i.hasNext()) {
                URI item = (URI) i.next();
                m_queryManager.copyItemWithProperty(item, property, fromC, toC);                
            }

            m_objectsToTags.remove(items);
            m_objectsToTagLabels.remove(items);
            m_profile.removeData(to);

            toC.commit();
        } catch (RepositoryException e) {
            if (toC != null) toC.rollback();
        } finally {
            if (fromC != null) fromC.close();
            if (toC != null) toC.close();
            if (to != null) to.shutDown();
        }
    }

    public void assignTags(URI object, String[] tagLabels) throws Exception {
        Repository local = null;
        RepositoryConnection localC = null; 
        
        try {
            local = Utilities.createMemoryRepository();
            localC = local.getConnection();
            localC.setAutoCommit(false);

            URI tagProperty = new URIImpl(s_tag);

            Set<URI> tags = new HashSet<URI>();
            for (int i = 0; i < tagLabels.length; i++) {
                labelToTags(tagLabels[i].trim(), tags, true);
            }

            Iterator i = tags.iterator();
            while (i.hasNext()) {
                URI tag = (URI) i.next();
                localC.add(object, tagProperty, tag);
            }

            localC.commit();
            localC.close();
            m_profile.addData(local, false);
        } catch (RepositoryException e) {
        	s_logger.error(e);
        } finally {
            if (local != null) local.shutDown();
        }
    }

    public void assignTags(Set objects, String[] tagLabels) throws Exception {
        Repository local = null;
        RepositoryConnection localC = null; 
        
        try {
            local = Utilities.createMemoryRepository();
            localC = local.getConnection();
            localC.setAutoCommit(false);

            URI tagProperty = new URIImpl(s_tag);

            Set<URI> tags = new HashSet<URI>();
            for (int i = 0; i < tagLabels.length; i++) {
                labelToTags(tagLabels[i].trim(), tags, true);
            }

            Iterator j = objects.iterator();
            while (j.hasNext()) {
                URI object = (URI) j.next();

                Iterator i = tags.iterator();
                while (i.hasNext()) {
                    URI tag = (URI) i.next();
                    localC.add(object, tagProperty, tag);
                }
            }

            localC.commit();
            localC.close();
            m_profile.addData(local, false);
        } catch (RepositoryException e) {
        	s_logger.error(e);
        } finally {
            if (local != null) local.shutDown();
        }
    }

    protected void internalInitialize() {
    	if (s_logger.isDebugEnabled()) s_logger.debug("> internalInitialize() " + m_initialized);
        if (m_initialized) {
        	if (s_logger.isDebugEnabled()) s_logger.debug("< internalInitialize()");
            return;
        }

        File lockFile = new File(m_dir, "lock-file");

        if (m_dir.exists() && !lockFile.exists()) {
            try {
                loadFromDir();
                lockFile.createNewFile();
            } catch (Exception e) {
                s_logger.error(e);

                m_labelToTags.clear();
                m_tagToLabels.clear();
                m_cachedTagLabels.clear();

                try {
                    RepositoryConnection c = null;
                    try {
                        c = m_profile.getRepository().getConnection();
                        c.setAutoCommit(false);
                        internalOnAfterAdd(c);
                        c.commit();
                    } catch (RepositoryException e2) {
                        if (c != null) c.rollback();
                    } finally {
                        if (c != null) c.close();
                    }
                    saveToDir();
                } catch (Exception e1) {
                    s_logger.error(e1);
                }
            }
        } else {
            m_dir.mkdirs();
            try {
                RepositoryConnection c = null;
                try {
                    c = m_profile.getRepository().getConnection();
                    c.setAutoCommit(false);
                    internalOnAfterAdd(c);
                    c.commit();
                } catch (RepositoryException e2) {
                    if (c != null) c.rollback();
                } finally {
                    if (c != null) c.close();
                }
                saveToDir();
            } catch (Exception e1) {
                s_logger.error(e1);
            }
        }
    	
    	m_initialized = true;

        if (s_logger.isDebugEnabled()) s_logger.debug("< internalInitialize()");
    }

    protected void loadFromDir() throws Exception {
        File file = new File(m_dir, "tags");
        if (file.exists()) {
            FileInputStream fis = null;
            try {
                fis = new FileInputStream(file);

                ObjectInputStream ois = new ObjectInputStream(fis);

                int tagCount = ois.readInt();
                for (int i = 0; i < tagCount; i++) {
                    String tagURI = (String) ois.readObject();
                    URI tag = new URIImpl(tagURI);

                    int labelCount = ois.readInt();
                    Set<String> labels = new HashSet<String>();
                    for (int j = 0; j < labelCount; j++) {
                    	String label = (String) ois.readObject();
                    	labels.add(label);
                    }
                    m_tagToLabels.put(tag, labels);
                }

                int labelCount = ois.readInt();
                for (int i = 0; i < labelCount; i++) {
                    String label = (String) ois.readObject();
                    m_cachedTagLabels.add(label);

                    int tagURICount = ois.readInt();
                    Set<URI> tags = new HashSet<URI>();
                    for (int j = 0; j < tagURICount; j++) {
                    	String tagURI = (String) ois.readObject();
                    	tags.add(new URIImpl(tagURI));
                    }
                    m_labelToTags.put(label, tags);
                }
            } finally {
                if (fis != null) fis.close();
            }
        }
    }

    protected void saveToDir() throws Exception {
        File file = new File(m_dir, "tags");

        FileOutputStream fos = null;
        ObjectOutputStream oos = null;
        try {
            fos = new FileOutputStream(file);
            oos = new ObjectOutputStream(fos);
            Set<URI> tags = new HashSet<URI>(m_tagToLabels.keySet());

            oos.writeInt(tags.size());

            Iterator t = tags.iterator();
            while (t.hasNext()) {
                URI tag = (URI) t.next();

                oos.writeObject(tag.toString());

                Set<String> labels = m_tagToLabels.get(tag);
                if (null != labels) {
                    oos.writeInt(labels.size());

                    for (String label : labels) {
                        oos.writeObject(label);
                    }
                } else {
                    oos.writeInt(0);
                }
            }

            Set<String> labels = m_labelToTags.keySet();

            oos.writeInt(labels.size());

            for (String label : labels) {
                oos.writeObject(label);

                tags = m_labelToTags.get(label);
                if (null != tags) {
                    oos.writeInt(tags.size());

                    for (URI taguri : tags) {
                        oos.writeObject(taguri.toString());
                    }
                } else {
                    oos.writeInt(0);
                }
            }
        } finally {
            if (oos != null) oos.close();
            if (fos != null) fos.close();
        }
    }

    public void onAfterAdd(RepositoryConnection c) {
        internalInitialize();
        try {
            internalOnAfterAdd(c);
        } catch (Exception e) {
            s_logger.error(e);
        }
    }

    public void onAfterRemove(RepositoryConnection c) {
        internalInitialize();
        try {
            internalOnAfterRemove(c);
        } catch (Exception e) {
            s_logger.error(e);
        }
    }

    protected void internalOnAfterAdd(RepositoryConnection c) throws Exception {
    	if (s_logger.isDebugEnabled()) s_logger.debug("> internalOnAfterAdd()");
        URI labelProperty = RDFS.LABEL;
        URI tagProperty = new URIImpl(s_tag);

        // Update existing tag labels
        if (m_queryManager.countStatementsWithPredicate(c, labelProperty) > 0) {
            m_labelToObjects.clear();
        }

        Set<URI> oldTags = m_tagToLabels.keySet();

        for (URI tag : oldTags) {
            Set addedLabels = m_queryManager.listObjectsOfProperty(c, tag, labelProperty);

            if (addedLabels.size() > 0) {
                String derivedLabel = deriveTagLabel(tag);
                Set<String> labels = new HashSet<String>();

                Set<String> tagLabels = m_tagToLabels.get(tag);
                labels.addAll(tagLabels);

                labels.remove(derivedLabel);

                Iterator j = addedLabels.iterator();
                while (j.hasNext()) {
                    Object o = j.next();
                    if (o instanceof Literal) {
                        String label = ((Literal) o).getLabel();
                        labels.add(label);
                        enterLabelForTag(label, tag);
                    }
                }

                m_tagToLabels.put(tag, labels);
            }
        }

        // Update associations between tags and objects and detect new tags
        Set newTags = m_queryManager.listObjectsOfProperty(c, tagProperty);
        if (newTags.size() > 0) {
            m_objectsToTags.clear();
            m_objectsToTagLabels.clear();
            m_labelToObjects.clear();
        }

        Iterator i = newTags.iterator();
        while (i.hasNext()) {
            Object o = i.next();
            if (!(o instanceof URI)) {
                continue;
            }

            URI tag = (URI) o;

            if (!m_tagToLabels.containsKey(tag)) {
                onNewTag(tag, c);
            }

            Set taggedObjects = m_queryManager.listSubjectsOfProperty(c, tagProperty, tag);

            Set objects = (Set) m_tagToObjects.get(tag);
            if (objects != null) {
                FixedSetBuilder builder = new FixedSetBuilder();

                builder.addAll(objects);
                builder.addAll(taggedObjects);

                // Caching strategy: set changes, create new set
                m_tagToObjects.put(tag, builder.buildFixedSet());
            }

            Iterator j = taggedObjects.iterator();
            while (j.hasNext()) {
                URI object = (URI) j.next();

                Set<URI> tags = (Set) m_objectToTags.get(object);
                if (tags != null) {
                    tags.add(tag);
                    // Caching strategy: set changes, create new set
                    m_objectToTags.put(object, new HashSet<URI>(tags));
                }
                Set<String> tagLabels = (Set) m_objectToTagLabels.get(object);
                if (tagLabels != null) {
                    tagToLabels(tag, tagLabels);
                    // Caching strategy: set changes, create new set
                    m_objectToTagLabels.put(object, new HashSet<String>(tagLabels));
                }
            }
        }
    	if (s_logger.isDebugEnabled()) s_logger.debug("< internalOnAfterAdd()");
    }

    protected void onNewTag(URI tag, RepositoryConnection c) throws Exception {
        Set<String> labels = new HashSet<String>();

        Iterator i = m_queryManager.listObjectsOfProperty(c, tag, RDFS.LABEL).iterator();
        while (i.hasNext()) {
            Object o = i.next();
            if (o instanceof Literal) {
                String label = ((Literal) o).getLabel();
                labels.add(label);
                enterLabelForTag(label, tag);
            }
        }

        if (labels.size() > 0) {
            m_tagToLabels.put(tag, labels);
        } else {
            String label = deriveTagLabel(tag);
            Set<String> tagLabels = new HashSet<String>();
            tagLabels.add(label);
            m_tagToLabels.put(tag, tagLabels);
            enterLabelForTag(label, tag);
        }
    }

    protected void internalOnAfterRemove(RepositoryConnection c) throws Exception {
        URI labelProperty = RDFS.LABEL;
        URI tagProperty = new URIImpl(s_tag);

        // Update existing tag labels
        if (m_queryManager.countStatementsWithPredicate(c, labelProperty) > 0) {
            m_labelToObjects.clear();
        }

        Set<URI> oldTags = m_tagToLabels.keySet();

        for (URI tag : oldTags) {
            Set removedLabels = m_queryManager.listObjectsOfProperty(c, tag, labelProperty);

            if (removedLabels.size() > 0) {
                String derivedLabel = deriveTagLabel(tag);
                Set<String> labels = new HashSet<String>();

                Set<String> tagLabels = m_tagToLabels.get(tag);
                labels.addAll(tagLabels);

                labels.remove(derivedLabel);

                Iterator j = removedLabels.iterator();
                while (j.hasNext()) {
                    Object o = j.next();
                    if (o instanceof Literal) {
                        String label = ((Literal) o).getLabel();
                        labels.remove(label);
                        removeLabelForTag(label, tag);
                    }
                }

                if (labels.size() > 0) {
                    m_tagToLabels.put(tag, labels);
                } else {
                	Set<String> derivedLabels = new HashSet<String>();
                	derivedLabels.add(deriveTagLabel(tag));
                    m_tagToLabels.put(tag, derivedLabels);
                }
            }
        }

        // Update associations between objects and tags
        Set newTags = m_queryManager.listObjectsOfProperty(c, tagProperty);
        if (newTags.size() > 0) {
            m_objectsToTags.clear();
            m_objectsToTagLabels.clear();
            m_labelToObjects.clear();
        }

        Iterator i = newTags.iterator();
        while (i.hasNext()) {
            Object o = i.next();
            if (!(o instanceof URI)) {
                continue;
            }

            URI tag = (URI) o;

            Set taggedObjects = m_queryManager.listSubjectsOfProperty(c, tagProperty, tag);
            Set objects = (Set) m_tagToObjects.get(tag);
            if (objects != null) {
                FixedSetBuilder builder = new FixedSetBuilder();

                builder.addAll(objects);
                builder.removeAll(taggedObjects);

                // Caching strategy: set changes, create new set
                m_tagToObjects.put(tag, builder.buildFixedSet());
            }
            
            Iterator j = taggedObjects.iterator();
            while (j.hasNext()) {
                URI object = (URI) j.next();

                Set<URI> tags = (Set) m_objectToTags.get(object);
                if (tags != null) {
                    tags.remove(tag);

                    // Caching strategy: set changes, create new set
                    m_objectToTags.put(tag, new HashSet<URI>(tags));
                }
                m_objectToTagLabels.remove(tag);
            }
        }
    }

    protected void enterLabelForTag(String label, URI tag) {
        Set<URI> labelTags = m_labelToTags.get(label);
        if (null != labelTags) {
            labelTags.add(tag);
        } else {
            Set<URI> tags = new HashSet<URI>();
            tags.add(tag);
            m_labelToTags.put(label, tags);
            m_cachedTagLabels.add(label);
        }
    }

    protected void removeLabelForTag(String label, URI tag) {
        Set<URI> tags = m_labelToTags.get(label);
        tags.remove(tag);
        if (tags.size() == 0) {
        	m_labelToTags.remove(label);
        	m_cachedTagLabels.remove(label);
        }
        
        Set<String> labels = m_tagToLabels.get(tag);
        labels.remove(label);
        if (labels.size() == 0) {
        	m_tagToLabels.remove(tag);
        }
    }

    protected String deriveTagLabel(URI tag) {
        String uri = tag.toString();
        if (uri.startsWith(s_tagNamespace)) {
            String suffix = uri.substring(s_tagNamespace.length());
            int separator = suffix.indexOf(SEPARATOR);

            if (separator < 0) {
                return base64Decode(suffix);
            }

            return base64Decode(suffix.substring(0, separator));
        }

        return uri;
    }

    protected Set<URI> internalGetTags(URI object) throws Exception {

        Set<URI> tags = new HashSet<URI>();
        
        RepositoryConnection c = null;

        try {
            c = m_profile.getRepository().getConnection();
            Iterator i = m_queryManager.listObjectsOfProperty(c, object, new URIImpl(s_tag)).iterator();
            while (i.hasNext()) {
                Object o = i.next();
                if (!(o instanceof URI)) {
                    continue;
                }
                tags.add((URI) o);
            }
        } finally {
            if (c != null) c.close();
        }

        return tags;
    }

    protected Set<String> internalGetTagLabels(URI object) throws Exception {
        Set tags = getObjectTags(object);
        Set<String> labels = new HashSet<String>();

        Iterator i = tags.iterator();
        while (i.hasNext()) {
            URI tag = (URI) i.next();
            tagToLabels(tag, labels);
        }

        return labels;
    }

    protected Set internalGetObjects(URI tag) throws Exception {
        FixedSetBuilder builder = new FixedSetBuilder();

        RepositoryConnection c = null;
        try {
            c = m_profile.getRepository().getConnection();
            Iterator i = m_queryManager.listSubjectsOfProperty(c, new URIImpl(s_tag), tag).iterator();
            while (i.hasNext()) {
                builder.add(i.next());
            }
        } finally {
            if (c != null) c.close();
        }

        return builder.buildFixedSet();
    }

    protected void tagToLabels(URI tag, Set<String> results) {
    	results.addAll(m_tagToLabels.get(tag));
    }

    protected void labelToTags(String label, Set<URI> results, boolean create) {
        Set<URI> labelTags = m_labelToTags.get(label);
        if (null != labelTags) {
            results.addAll(labelTags);
        } else if (create) {
            if (label.length() > 0) {
                String uri = s_tagNamespace + base64Encode(label) + SEPARATOR + getTagSuffixHash();

                URI tag = new URIImpl(uri);

                Set<URI> tags = new HashSet<URI>();
                Set<String> labels = new HashSet<String>();
                
                tags.add(tag);
                labels.add(label);
                
                m_labelToTags.put(label, tags);
                m_tagToLabels.put(tag, labels);

                m_cachedTagLabels.add(label);

                results.add(tag);
            }
        }
    }

    static public String md5hash(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(s.getBytes());
            return toHexString(md.digest());
        } catch (NoSuchAlgorithmException e) {
            return s;
        }
    }

    static protected String base64Encode(String s) {
        try {
            return new BASE64Encoder().encode(s.getBytes("UTF-8"));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    static protected String base64Decode(String s) {
        try {
            return new String(new BASE64Decoder().decodeBuffer(s), "UTF-8");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static String toHexString(byte[] b) {
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < b.length; ++i) {
            buf.append(Integer.toHexString((b[i] >> 4) & 0x0f));
            buf.append(Integer.toHexString(b[i] & 0x0f));
        }
        return buf.toString();
    }
    
    static public class TagComparator implements Comparator {
        public int compare(Object o1, Object o2) {
            String s1 = (String) o1;
            String s2 = (String) o2;
            return s1.compareTo(s2);
        }
    }
}
