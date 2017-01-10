package edu.mit.simile.longwell;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.collections.set.UnmodifiableSet;

public class FixedSetBuilder {

    final protected Set m_set;

    public FixedSetBuilder() {
        m_set = new HashSet();
    }

    public FixedSetBuilder(Comparator comparator) {
        m_set = new TreeSet(comparator);
    }

    public void add(Object elmt) {
        m_set.add(elmt);
    }

    public void addAll(Collection elmts) {
        m_set.addAll(elmts);
    }

    public void remove(Object elmt) {
        m_set.remove(elmt);
    }

    public void removeAll(Collection elmts) {
        m_set.removeAll(elmts);
    }

    public void retainAll(Collection elmts) {
        m_set.retainAll(elmts);
    }

    public boolean contains(Object elmt) {
        return m_set.contains(elmt);
    }

    public Set buildFixedSet() {
        if (m_set instanceof TreeSet) {
            // With order imposed on the objects, we can just turn them into
            // an array to minimize memory size.
            int count = m_set.size();

            Object[] elmts = new Object[count];
            int index = 0;

            Iterator i = m_set.iterator();
            while (i.hasNext()) {
                elmts[index++] = i.next();
            }

            return createFixedSet(elmts, m_set instanceof TreeSet ? ((TreeSet) m_set).comparator() : null);
        } else {
            // We can't implement .contains() ourselves since there is no order
            // to do a binary search. So, we have to use a HashSet.
            return UnmodifiableSet.decorate(m_set);
        }
    }

    protected FixedSet createFixedSet(Object[] elmts, Comparator c) {
        return new FixedSet(elmts, c);
    }

    public static class FixedSet implements Set {
        final protected Object[] m_elmts;

        final protected Comparator m_comparator;

        protected FixedSet(Object[] elmts, Comparator comparator) {
            m_elmts = elmts;
            m_comparator = comparator;
        }

        public int size() {
            return m_elmts.length;
        }

        public void clear() {
            throw new InternalError("Not implemented");
        }

        public boolean isEmpty() {
            return size() == 0;
        }

        public Object[] toArray() {
            Object[] elmts = new Object[m_elmts.length];
            for (int i = 0; i < m_elmts.length; i++) {
                elmts[i] = m_elmts[i];
            }

            return elmts;
        }

        public boolean add(Object o) {
            throw new InternalError("Not implemented");
        }

        public boolean contains(Object o) {
            if (m_elmts.length == 0) {
                return false;
            }

            int min = 0;
            int max = m_elmts.length;
            while (min < max - 1) {
                int mid = (min + max) / 2;
                int c = m_comparator.compare(o, m_elmts[mid]);

                if (c == 0) {
                    return true;
                } else if (c < 0) {
                    max = mid;
                } else {
                    min = mid;
                }
            }
            return m_comparator.compare(o, m_elmts[min]) == 0;
        }

        public boolean remove(Object o) {
            throw new InternalError("Not implemented");
        }

        public boolean addAll(Collection c) {
            throw new InternalError("Not implemented");
        }

        public boolean containsAll(Collection c) {
            throw new InternalError("Not implemented");
        }

        public boolean removeAll(Collection c) {
            throw new InternalError("Not implemented");
        }

        public boolean retainAll(Collection c) {
            throw new InternalError("Not implemented");
        }

        public Iterator iterator() {
            return new FixedSetIterator();
        }

        public Object[] toArray(Object[] a) {
            if (a.length != m_elmts.length) {
                throw new IllegalArgumentException("Wrong length");
            }

            for (int i = 0; i < m_elmts.length; i++) {
                a[i] = m_elmts[i];
            }
            return a;
        }

        class FixedSetIterator implements Iterator {
            int m_index;

            public void remove() {
                throw new InternalError("Not implemented");
            }

            public boolean hasNext() {
                return m_index < m_elmts.length;
            }

            public Object next() {
                return m_elmts[m_index++];
            }
        }
    }
}
