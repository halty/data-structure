package com.lee.data.structure.index.impl;

import java.util.HashMap;
import java.util.Map;
import com.lee.data.structure.index.IndexKey;
import com.lee.data.structure.index.Indexer;

/** {@link HashMap} based implementation of {@link Indexer} interface which is not thread safe. **/
public class HashIndexer<K, V> extends AbstractIndexer<K, V> {
	
	private final HashMap<IndexKey<K>, V> map;
	
	/** Constructs an empty HashIndexer with the default initial capacity (16) and the default load factor (0.75) **/
	public HashIndexer() {
		this(new HashMap<IndexKey<K>, V>());
	}
	
	/** Constructs an empty HashIndexer with the specified initial capacity (need >= 0) and the default load factor (0.75) **/
	public HashIndexer(int initialCapacity) {
		this(new HashMap<IndexKey<K>, V>(initialCapacity));
	}
	
	/** Constructs an empty HashIndexer with the specified initial capacity (need >= 0) and load factor (need > 0) **/
	public HashIndexer(int initialCapacity, float loadFactor) {
		this(new HashMap<IndexKey<K>, V>(initialCapacity, loadFactor));
	}
	
	protected HashIndexer(HashMap<IndexKey<K>, V> map) {
		this.map = map;
	}

	@Override
	protected Map<IndexKey<K>, V> backedMap() { return map; }
	
}
