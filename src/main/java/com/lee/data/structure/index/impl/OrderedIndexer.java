package com.lee.data.structure.index.impl;

import java.util.Comparator;
import java.util.Iterator;
import java.util.SortedMap;
import java.util.TreeMap;

import com.lee.data.structure.ImmutableEntry;
import com.lee.data.structure.index.IndexKey;
import com.lee.data.structure.index.SortedIndexer;

public class OrderedIndexer<K, V> extends AbstractIndexer<K, V> implements SortedIndexer<K, V> {

	private final SortedMap<IndexKey<K>, V> sortedMap;
	
	public OrderedIndexer() {
		this(new TreeMap<IndexKey<K>, V>());
	}
	
	public OrderedIndexer(final Comparator<? super K> comparator) {
		this(new TreeMap<IndexKey<K>, V>(new Comparator<IndexKey<K>>() {
			@Override
			public int compare(IndexKey<K> key1, IndexKey<K> key2) {
				int count1 = key1.keyCount();
				int count2 = key2.keyCount();
				int count = Math.min(count1, count2);
				for(int i=0; i<count; i++) {
					K k1 = (K) key1.keyAt(i);
					K k2 = (K) key2.keyAt(i);
					if(k1 == null) {
						if(k2 != null) { return -1; }
					}else {
						if(k2 == null) {
							return 1;
						}else {
							int cmp = comparator.compare(k1, k2);
							if(cmp != 0) { return cmp; }
						}
					}
				}
				return count1 - count2;
			}
		}));
	}
	
	protected OrderedIndexer(SortedMap<IndexKey<K>, V> sortedMap) {
		this.sortedMap = sortedMap;
	}

	@Override
	protected SortedMap<IndexKey<K>, V> backedMap() { return sortedMap; }

	@Override
	public IndexKey<K> lowerKey(IndexKey<K> key) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ImmutableEntry<IndexKey<K>, V> lowerEntry(IndexKey<K> key) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IndexKey<K> floorKey(IndexKey<K> key) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ImmutableEntry<IndexKey<K>, V> floorEntry(IndexKey<K> key) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IndexKey<K> ceilingKey(IndexKey<K> key) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ImmutableEntry<IndexKey<K>, V> ceilingEntry(IndexKey<K> key) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IndexKey<K> higherKey(IndexKey<K> key) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ImmutableEntry<IndexKey<K>, V> higherEntry(IndexKey<K> key) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IndexKey<K> firstKey() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ImmutableEntry<IndexKey<K>, V> firstEntry() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IndexKey<K> lastKey() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ImmutableEntry<IndexKey<K>, V> lastEntry() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SortedIndexer<K, V> prefix(IndexKey<K> key) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SortedIndexer<K, V> head(IndexKey<K> toKey, boolean inclusive) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SortedIndexer<K, V> tail(IndexKey<K> fromKey, boolean inclusive) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SortedIndexer<K, V> between(IndexKey<K> fromKey, boolean fromInclusive,
			IndexKey<K> toKey, boolean toInclusive) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Iterator<IndexKey<K>> reversedKeyIterator() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Iterator<ImmutableEntry<IndexKey<K>, V>> reversedEntryIterator() {
		// TODO Auto-generated method stub
		return null;
	}
}
