package com.lee.data.structure.index;

import java.util.Iterator;

import com.lee.data.structure.ImmutableEntry;

public interface SortedIndexer<K, V> extends Indexer<K, V> {

	/**
	 * return the greatest key strictly less than the given key, or
     * {@code null} if there is no such key.
	 */
	IndexKey<K> lowerKey(IndexKey<K> key);
	
	/**
	 * return a key-value mapping associated with the greatest key
     * strictly less than the given key, or {@code null} if there is
     * no such key.
	 */
	ImmutableEntry<IndexKey<K>, V> lowerEntry(IndexKey<K> key);
	
	/**
	 * return the greatest key less than or equal to the given key,
     * or {@code null} if there is no such key.
	 */
	IndexKey<K> floorKey(IndexKey<K> key);
	
	/**
	 * return a key-value mapping associated with the greatest key
     * less than or equal to the given key, or {@code null} if there
     * is no such key.
	 */
	ImmutableEntry<IndexKey<K>, V> floorEntry(IndexKey<K> key);
	
	/**
	 * return the least key greater than or equal to the given key,
     * or {@code null} if there is no such key.
	 */
	IndexKey<K> ceilingKey(IndexKey<K> key);
	
	/**
	 * return a key-value mapping associated with the least key
     * greater than or equal to the given key, or {@code null} if there
     * is no such key.
	 */
	ImmutableEntry<IndexKey<K>, V> ceilingEntry(IndexKey<K> key);
	
	/**
	 * return the least key strictly greater than the given key,
     * or {@code null} if there is no such key.
	 */
	IndexKey<K> higherKey(IndexKey<K> key);
	
	/**
	 * return a key-value mapping associated with the least key strictly
     * greater than the given key, or {@code null} if there
     * is no such key.
	 */
	ImmutableEntry<IndexKey<K>, V> higherEntry(IndexKey<K> key);
	
	/**
	 * return the least key in this sorted indexer, or {@code null}
	 * if this sorted indexer is empty.
	 */
	IndexKey<K> firstKey();
	
	/**
	 * return a key-value mapping associated with the least key in
	 * this sorted indexer, or {@code null} if this sorted indexer
     * is empty.
	 */
	ImmutableEntry<IndexKey<K>, V> firstEntry();
	
	/**
	 * return the greatest key in this sorted indexer, or {@code null}
	 * if this sorted indexer is empty.
	 */
	IndexKey<K> lastKey();
	
	/**
	 * return a key-value mapping associated with the greatest key in
	 * this sorted indexer, or {@code null} if this sorted indexer
     * is empty.
	 */
	ImmutableEntry<IndexKey<K>, V> lastEntry();
	
	/**
	 * return a view of the portion of this indexer whose keys prefix equals to
     * {@code key}.  The returned indexer is backed by this indexer, so changes
     * in the returned indexer are reflected in this indexer, and vice-versa.
	 */
	SortedIndexer<K, V> prefix(IndexKey<K> key);
	
	/**
	 * return a view of the portion of this indexer whose keys are less than (or
     * equal to, if {@code inclusive} is true) {@code toKey}.  The returned
     * indexer is backed by this indexer, so changes in the returned indexer
     * are reflected in this indexer, and vice-versa.
	 */
	SortedIndexer<K, V> head(IndexKey<K> toKey, boolean inclusive);
	
	/**
	 * return a view of the portion of this indexer whose keys are greater than (or
     * equal to, if {@code inclusive} is true) {@code fromKey}.  The returned
     * indexer is backed by this indexer, so changes in the returned indexer
     * are reflected in this indexer, and vice-versa.
	 */
	SortedIndexer<K, V> tail(IndexKey<K> fromKey, boolean inclusive);
	
	/**
	 * returns a view of the portion of this indexer whose keys sorted from
     * {@code fromKey} to {@code toKey}.  If {@code fromKey} and
     * {@code toKey} are equal, the returned indexer is empty unless
     * {@code fromInclusive} and {@code toInclusive} are both true.  The
     * returned indexer is backed by this indexer, so changes in the returned indexer
     * are reflected in this indexer, and vice-versa.
	 */
	SortedIndexer<K, V> between(IndexKey<K> fromKey, boolean fromInclusive,
			IndexKey<K> toKey, boolean toInclusive);
	
	/** return an iterator over a set of key of this indexer in natural key order **/
	Iterator<IndexKey<K>> keyIterator();
	
	/** return an iterator over a set of mapping from key to value of this indexer in natural key order **/
	Iterator<ImmutableEntry<IndexKey<K>, V>> entryIterator();
	
	/** return an iterator over a set of key of this indexer in reversed key order **/
	Iterator<IndexKey<K>> reversedKeyIterator();
	
	/** return an iterator over a set of mapping from key to value of this indexer in reversed key order **/
	Iterator<ImmutableEntry<IndexKey<K>, V>> reversedEntryIterator();
}
