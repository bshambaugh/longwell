package edu.mit.simile.longwell;

import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;

import com.whirlycott.cache.Cacheable;

public class Cache {

    final static private Logger s_logger = Logger.getLogger(Cache.class);

    final private CacheFactory m_factory;
    final private com.whirlycott.cache.Cache m_cache;
    final protected int m_hash;
    final protected String m_name;
    final protected boolean m_clearable;

    protected Set m_keys;

    public class CompositeKey implements Cacheable {
        final private Object _a;

        final private Object _b;

        final private Object _c;

        public CompositeKey(Object a) {
            this._a = a;
            this._b = null;
            this._c = null;
        }

        public CompositeKey(Object a, Object b) {
            this._a = a;
            this._b = b;
            this._c = null;
        }

        public CompositeKey(Object a, Object b, Object c) {
            this._a = a;
            this._b = b;
            this._c = c;
        }

        final public int hashCode() {
            int hash = m_hash ^ this._a.hashCode();
            if (this._b != null) hash ^= this._b.hashCode();
            if (this._c != null) hash ^= this._c.hashCode();
            return hash;
        }

        final public boolean equals(Object o) {
            return this.hashCode() == o.hashCode();
        }

        final public void onRetrieve(Object o) {
            // do nothing
        }

        final public void onStore(Object o) {
            if (m_clearable) {
                synchronized (m_keys) {
                    m_keys.add(CompositeKey.this);
                }
            }
        }

        final public void onRemove(Object o) {
            if (m_clearable) {
                synchronized (m_keys) {
                    m_keys.remove(CompositeKey.this);
                }
            }
        }
    }

    public Cache(CacheFactory factory, com.whirlycott.cache.Cache cache, String name, boolean clearable) {
        this.m_factory = factory;
        this.m_cache = cache;
        this.m_name = name;
        this.m_clearable = clearable;
        this.m_hash = this.hashCode();
        if (clearable) {
            m_keys = new HashSet();
        }
    }

    public Object get(Object key) {
        return internalRetrieve(new CompositeKey(key));
    }

    public Object get(Object a, Object b) {
        return internalRetrieve(new CompositeKey(a, b));
    }

    public void put(Object key, Object obj) {
        internalStore(new CompositeKey(key), obj);
    }

    public void put(Object a, Object b, Object obj) {
        internalStore(new CompositeKey(a, b), obj);
    }

    public Object remove(Object key) {
        return internalRemove(new CompositeKey(key));
    }

    public void clear() {
        if (m_clearable) {
            Object[] keys = null;
            synchronized (m_keys) {
                keys = m_keys.toArray();
            }
            for (int i = 0; i < keys.length; i++) {
                internalRemove((CompositeKey) keys[i]);
            }
            if (s_logger.isDebugEnabled()) s_logger.debug("Clearing " + this + " " + m_name + " " + m_hash +
                    " " + this.getClass().getClassLoader() +
                    " from " + keys.length + " " + m_keys.size());           
        } else {
            throw new InternalError("This cache (" + m_name + "," + m_hash + ") is NOT constructed to be clearable");
        }
    }

    public void collectValues(Set values) {
        if (m_clearable) {
            Object[] keys = null;
            synchronized (m_keys) {
                keys = m_keys.toArray();
            }
            for (int i = 0; i < keys.length; i++) {
                Object o = internalRetrieve((CompositeKey) keys[i]);
                if (o != null) {
                    values.add(o);
                }
            }
        } else {
            throw new InternalError("This cache (" + m_name + "," + m_hash + ") is NOT constructed to be numerable");
        }
    }

    final private Object internalRetrieve(CompositeKey k) {
        return m_cache.retrieve(k);
    }

    final private void internalStore(CompositeKey k, Object o) {
        m_cache.store(k, o);
        internalModified();
    }

    final private Object internalRemove(CompositeKey k) {
        Object removed = m_cache.remove(k);
        internalModified();
        return removed; 
    }
    
    final private void internalModified() {
        m_factory.setLastModified(System.currentTimeMillis());
    }
}
