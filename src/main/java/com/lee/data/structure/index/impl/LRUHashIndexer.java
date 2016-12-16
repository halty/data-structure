package com.lee.data.structure.index.impl;

import java.util.LinkedHashMap;
import java.util.Map;

import com.lee.data.structure.index.IndexKey;
import com.lee.data.structure.index.Indexer;

/** {@link LinkedHashMap} based implementation of {@link Indexer} interface which is not thread safe. **/
public class LRUHashIndexer<K, V> extends AbstractIndexer<K, V> {
	
	private static final float LOAD_FACTOR = 0.75f;
	
	private final LRUMap map;
	private final int maxCapacity;
	
	/** Constructs an empty LRUHashIndexer with the specified max capacity (need > 0) and the default load factor (0.75) **/
	public LRUHashIndexer(int maxCapacity) {
		this(maxCapacity, LOAD_FACTOR);
	}
	
	/** Constructs an empty LRUHashIndexer with the specified max capacity (need > 0) and load factor (need > 0) **/
	public LRUHashIndexer(int maxCapacity, float loadFactor) {
		if(maxCapacity <= 0) {
			throw new IllegalArgumentException("Illegal max capacity: " + maxCapacity);
		}
		if (loadFactor <= 0 || Float.isNaN(loadFactor)) {
            throw new IllegalArgumentException("Illegal load factor: " + loadFactor);
		}
		int initCapacity = (int)(maxCapacity / loadFactor);
		if(initCapacity < Integer.MAX_VALUE) { initCapacity += 1; }
		this.map = new LRUMap(initCapacity, loadFactor);
		this.maxCapacity = maxCapacity;
	}

	@Override
	protected Map<IndexKey<K>, V> backendMap() { return map; }

	private final class LRUMap extends LinkedHashMap<IndexKey<K>, V> {

		private static final long serialVersionUID = 1L;

		LRUMap(int initCapacity, float loadFactor) {
			super(initCapacity, loadFactor, true);
		}
		
		@Override
		protected boolean removeEldestEntry(Map.Entry<IndexKey<K>, V> eldest) {
			return size() > maxCapacity;
		}
	}
}
