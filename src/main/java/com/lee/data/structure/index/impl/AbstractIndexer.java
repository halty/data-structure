package com.lee.data.structure.index.impl;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import com.lee.data.structure.ImmutableEntry;
import com.lee.data.structure.index.IndexKey;
import com.lee.data.structure.index.Indexer;

/**
 * skeletal implementation of {@link Indexer} interface,
 * to minimize the effort required to implement this interface.
 */
public abstract class AbstractIndexer<K, V> implements Indexer<K, V> {

	protected static final Object NULL_VALUE = new Object();
	
	/** return the map backed by subclass implementation  **/
	protected abstract Map<IndexKey<K>, V> backedMap();
	
	@Override
	public int size() { return backedMap().size(); }

	@Override
	public boolean isEmpty() { return backedMap().isEmpty(); }

	@Override
	public boolean containsKey(IndexKey<K> key) { return backedMap().containsKey(key); }

	@Override
	public V get(IndexKey<K> key) { return unmask(backedMap().get(key)); }
	
	protected V unmask(V value) { return value == NULL_VALUE ? null : value; }

	@Override
	public V put(IndexKey<K> key, V value) { return unmask(internalPut(key, value)); }
	
	protected V internalPut(IndexKey<K> key, V value) { return backedMap().put(key, mask(value)); }
	
	@SuppressWarnings("unchecked")
	protected V mask(V value) { return value == null ? (V) NULL_VALUE : value; }

	@Override
	public V putIfAbsent(IndexKey<K> key, V value) { return unmask(internalPutIfAbsent(key, mask(value))); }
	
	protected V internalPutIfAbsent(IndexKey<K> key, V value) {
		Map<IndexKey<K>, V> map = backedMap();
		V oldValue = map.get(key);
		if(oldValue != null) {
			return oldValue;
		}else {
			map.put(key, value);
			return null;
		}
	}

	@Override
	public V replaceIfPresent(IndexKey<K> key, V value) { return unmask(internalReplaceIfPresent(key, mask(value))); }
	
	protected V internalReplaceIfPresent(IndexKey<K> key, V value) {
		Map<IndexKey<K>, V> map = backedMap();
		V oldValue = map.get(key);
		if(oldValue != null) {
			map.put(key, value);
			return oldValue;
		}else {
			return null;
		}
	}
	
	@Override
	public boolean replaceIfMatched(IndexKey<K> key, V oldValue, V newValue) {
		return internalReplaceIfMatched(key, mask(oldValue), mask(newValue));
	}

	protected boolean internalReplaceIfMatched(IndexKey<K> key, V oldValue, V newValue) {
		Map<IndexKey<K>, V> map = backedMap();
		V prevValue = map.get(key);
		if(prevValue != null) {
			if(prevValue.equals(oldValue)) {
				map.put(key, newValue);
				return true;
			}else {
				return false;
			}
		}else {
			return false;
		}
	}

	@Override
	public void putAll(Map<? extends IndexKey<K>, ? extends V> m) {
		if(m.size() == 0) { return; }
		/*
		 * If (m.size() + size) >= map.threshold, and this indexer doesn't
		 * contain the keys to be added, maybe result in the backed map
		 * of this indexer resize multiple times.
		 */
		for(Entry<? extends IndexKey<K>, ? extends V> entry : m.entrySet()) {
			internalPut(entry.getKey(), mask(entry.getValue()));
		}
	}

	@Override
	public void putAll(Indexer<K, V> idx) {
		if(idx.size() == 0) { return; }
		/*
		 * If (idx.size() + size) >= map.threshold, and this indexer doesn't
		 * contain the keys to be added, maybe result in the backed map
		 * of this indexer resize multiple times.
		 */
		Iterator<ImmutableEntry<IndexKey<K>, V>> iter = idx.entryIterator();
		while(iter.hasNext()) {
			ImmutableEntry<IndexKey<K>, V> entry = iter.next();
			internalPut(entry.key, mask(entry.value));
		}
	}

	@Override
	public int putAllIfAbsent(Map<? extends IndexKey<K>, ? extends V> m) {
		if(m.size() == 0) { return 0; }
		int putCount = 0;
		for(Entry<? extends IndexKey<K>, ? extends V> entry : m.entrySet()) {
			if(internalPutIfAbsent(entry.getKey(), mask(entry.getValue())) == null) {
				putCount++;
			}
		}
		return putCount;
	}

	@Override
	public int putAllIfAbsent(Indexer<K, V> idx) {
		if(idx.size() == 0) { return 0; }
		int putCount = 0;
		Iterator<ImmutableEntry<IndexKey<K>, V>> iter = idx.entryIterator();
		while(iter.hasNext()) {
			ImmutableEntry<IndexKey<K>, V> entry = iter.next();
			if(internalPutIfAbsent(entry.key, mask(entry.value)) == null) {
				putCount++;
			};
		}
		return putCount;
	}

	@Override
	public int replaceAllIfPresent(Map<? extends IndexKey<K>, ? extends V> m) {
		if(m.size() == 0) { return 0; }
		int replacedCount = 0;
		for(Entry<? extends IndexKey<K>, ? extends V> entry : m.entrySet()) {
			if(internalReplaceIfPresent(entry.getKey(), mask(entry.getValue())) != null) {
				replacedCount++;
			}
		}
		return replacedCount;
	}

	@Override
	public int replaceAllIfPresent(Indexer<K, V> idx) {
		if(idx.size() == 0) { return 0; }
		int replacedCount = 0;
		Iterator<ImmutableEntry<IndexKey<K>, V>> iter = idx.entryIterator();
		while(iter.hasNext()) {
			ImmutableEntry<IndexKey<K>, V> entry = iter.next();
			if(internalReplaceIfPresent(entry.key, mask(entry.value)) != null) {
				replacedCount++;
			};
		}
		return replacedCount;
	}

	@Override
	public V remove(IndexKey<K> key) { return unmask(backedMap().remove(key)); }
	
	@Override
	public boolean removeIfMatched(IndexKey<K> key, V value) {
		return internalRemoveIfMatched(key, mask(value));
	}

	protected boolean internalRemoveIfMatched(IndexKey<K> key, V value) {
		Map<IndexKey<K>, V> map = backedMap();
		V oldValue = map.get(key);
		if(oldValue != null) {
			if(oldValue.equals(value)) {
				map.remove(key);
				return true;
			}else {
				return false;
			}
		}else {
			return false;
		}
	}

	@Override
	public void clear() { backedMap().clear(); }

	@Override
	public Iterator<IndexKey<K>> keyIterator() { return backedMap().keySet().iterator(); }

	@Override
	public Iterator<ImmutableEntry<IndexKey<K>, V>> entryIterator() {
		return new EntryIterator(backedMap().entrySet().iterator());
	}

	private final class EntryIterator implements Iterator<ImmutableEntry<IndexKey<K>, V>> {
		private final Iterator<Entry<IndexKey<K>, V>> iter;
		
		public EntryIterator(Iterator<Entry<IndexKey<K>, V>> iter) { this.iter = iter; }
		
		@Override
		public boolean hasNext() { return iter.hasNext(); }

		@Override
		public ImmutableEntry<IndexKey<K>, V> next() {
			Entry<IndexKey<K>, V> entry = iter.next();
			return new ImmutableEntry<IndexKey<K>, V>(entry.getKey(), unmask(entry.getValue()));
		}

		@Override
		public void remove() { iter.remove(); }
		
	}
}
