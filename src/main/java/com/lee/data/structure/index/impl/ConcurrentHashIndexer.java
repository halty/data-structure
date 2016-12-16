package com.lee.data.structure.index.impl;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.lee.data.structure.ImmutableEntry;
import com.lee.data.structure.index.IndexKey;
import com.lee.data.structure.index.Indexer;

/** {@link ConcurrentHashMap} based implementation of {@link Indexer} interface which is thread safe. **/
public class ConcurrentHashIndexer<K, V> extends AbstractIndexer<K, V> {

	private final ConcurrentHashMap<IndexKey<K>, V> map;
	
	/**
	 * Constructs an empty ConcurrentHashIndexer with default initial capacity (16),
	 * load factor (0.75) and concurrencyLevel (16).
	 */
	public ConcurrentHashIndexer() {
		this(new ConcurrentHashMap<IndexKey<K>, V>());
	}
	
	/**
	 * Constructs an empty ConcurrentHashIndexer with specified initial capacity (need >= 0),
	 * and default load factor (0.75), concurrencyLevel (16).
	 */
	public ConcurrentHashIndexer(int initialCapacity) {
		this(new ConcurrentHashMap<IndexKey<K>, V>(initialCapacity));
	}
	
	/**
	 * Constructs an empty ConcurrentHashIndexer with specified initial capacity (need >= 0),
	 * load factor (need > 0) and default concurrencyLevel (16).
	 */
	public ConcurrentHashIndexer(int initialCapacity, float loadFactor) {
		this(new ConcurrentHashMap<IndexKey<K>, V>(initialCapacity, loadFactor));
	}
	
	/**
	 * Constructs an empty ConcurrentHashIndexer with specified initial capacity (need >= 0),
	 * load factor (need > 0) and concurrencyLevel (need > 0).
	 */
	public ConcurrentHashIndexer(int initialCapacity, float loadFactor, int concurrencyLevel) {
		this(new ConcurrentHashMap<IndexKey<K>, V>(initialCapacity, loadFactor, concurrencyLevel));
	}
	
	protected ConcurrentHashIndexer(ConcurrentHashMap<IndexKey<K>, V> map) {
		this.map = map;
	}
	
	@Override
	protected Map<IndexKey<K>, V> backendMap() { return map; }

	@Override
	protected V internalPutIfAbsent(IndexKey<K> key, V value) {
		return map.putIfAbsent(key, value);
	}
	
	@Override
	protected V internalReplaceIfPresent(IndexKey<K> key, V value) {
		return map.replace(key, value);
	}
	
	@Override
	protected boolean internalReplaceIfMatched(IndexKey<K> key, V oldValue, V newValue) {
		return map.replace(key, oldValue, newValue);
	}
	
	@Override
	protected boolean internalRemoveIfMatched(IndexKey<K> key, V value) {
		return map.remove(key, value);
	}
	
	/**
	 * <p>The view's <tt>iterator</tt> is a "weakly consistent" iterator
     * that will never throw {@link ConcurrentModificationException},
     * and guarantees to traverse elements as they existed upon
     * construction of the iterator, and may (but is not guaranteed to)
     * reflect any modifications subsequent to construction.
	 */
	@Override
	public Iterator<IndexKey<K>> keyIterator() { return map.keySet().iterator(); }

	/**
	 * <p>The view's <tt>iterator</tt> is a "weakly consistent" iterator
     * that will never throw {@link ConcurrentModificationException},
     * and guarantees to traverse elements as they existed upon
     * construction of the iterator, and may (but is not guaranteed to)
     * reflect any modifications subsequent to construction.
	 */
	@Override
	public Iterator<ImmutableEntry<IndexKey<K>, V>> entryIterator() {
		return super.entryIterator();
	}
}
