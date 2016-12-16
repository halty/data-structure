package com.lee.data.structure.index.impl;

import com.lee.data.structure.index.Indexer;

/**
 * {@link ConcurrentHashMap} based implementation of {@link Indexer} interface
 * providing LRU strategy with capacity constraints which is thread safe.
 */
public class ConcurrentLRUHashIndexer<K, V> extends ConcurrentHashIndexer<K, V> {
	
	/** Constructs an empty ConcurrentLRUHashIndexer with the specified max capacity (need > 0) and the default load factor (0.75) **/
	public ConcurrentLRUHashIndexer(int maxCapacity) {
		super(maxCapacity);
	}

	/** Constructs an empty ConcurrentLRUHashIndexer with the specified max capacity (need > 0) and load factor (need > 0) **/
	public ConcurrentLRUHashIndexer(int maxCapacity, float loadFactor) {
		super(maxCapacity, loadFactor);
	}
}
