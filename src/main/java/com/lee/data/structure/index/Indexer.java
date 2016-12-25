package com.lee.data.structure.index;

import java.util.Iterator;
import java.util.Map;
import com.lee.data.structure.ImmutableEntry;

/** do not permit <code>null</code> IndexKey, permits <code>null</code> value **/
public interface Indexer<K, V> {
	
	/** return the number of key-value mappings in this indexer **/
	int size();
	
	/** return <code>true</code> if this indexer contains no key-value mappings **/
	boolean isEmpty();
	
	/** return <code>true</code> if this indexer contains a mapping for the specified key **/
	boolean containsKey(IndexKey<K> key);
	
	/**
	 * return the value to which the specified key is mapped,
     * or {@code null} if this indexer contains no mapping for the key.
	 */
	V get(IndexKey<K> key);
	
	/**
	 * associate the specified value with the specified key in this indexer.
	 * return the previous value mapped the specified key, or <code>null</code>. 
	 */
	V put(IndexKey<K> key, V value);
	
	/**
	 * If the specified key is not already associated with a value,
	 * associate it with the given value in this indexer.
	 * return the previous value associated with the specified key, or
     * <code>null</code> if there was no mapping for the key.
     * (A <tt>null</tt> return can also indicate that the indexer
     * previously associated <tt>null</tt> with the key)
	 */
	V putIfAbsent(IndexKey<K> key, V value);
	
	/**
	 * replace the entry for a key only if currently mapped to some value.
	 * return the previous value associated with the specified key, or
     * <code>null</code> if there was no mapping for the key.
	 */
	V replaceIfPresent(IndexKey<K> key, V value);
	
	/**
	 * replace the entry with new value for a key only if currently mapped to a given old value.
	 * return <code>true</code> if the given old value was replaced.
	 */
	boolean replaceIfMatched(IndexKey<K> key, V oldValue, V newValue);
	
	/**
	 * copy all of the mappings from the specified map to this indexer.
	 * The effect of this call is equivalent to that of calling
	 * {@link #put(IndexKey, Object) put(IndexKey, V)} on this indexer
	 * once for each mapping from key to value in the specified map.
	 */
	void putAll(Map<? extends IndexKey<K>, ? extends V> map);
	
	/**
	 * copy all of the mappings from the specified indexer to this indexer.
	 * The effect of this call is equivalent to that of calling
	 * {@link #put(IndexKey, Object) put(IndexKey, V)} on this indexer
	 * once for each mapping from key to value in the specified indexer.
	 */
	void putAll(Indexer<K, V> indexer);
	
	/**
	 * copy all of the mappings from the specified map to this indexer.
	 * The effect of this call is equivalent to that of calling
	 * {@link #putIfAbsent(IndexKey, Object) putIfAbsent(IndexKey, V)} on this indexer
	 * once for each mapping from key to value in the specified map.
	 * return the added entry count from {@code map}.
	 */
	int putAllIfAbsent(Map<? extends IndexKey<K>, ? extends V> map);
	
	/**
	 * copy all of the mappings from the specified indexer to this indexer.
	 * The effect of this call is equivalent to that of calling
	 * {@link #putIfAbsent(IndexKey, Object) putIfAbsent(IndexKey, V)} on this indexer
	 * once for each mapping from key to value in the specified indexer.
	 * return the added entry count from {@code map}.
	 */
	int putAllIfAbsent(Indexer<K, V> indexer);
	
	/**
	 * copy all of the mappings from the specified map to this indexer.
	 * The effect of this call is equivalent to that of calling
	 * {@link #replaceIfPresent(IndexKey, Object) replaceIfPresent(IndexKey, V)} on this indexer
	 * once for each mapping from key to value in the specified map.
	 * return the replaced entry count from {@code map}.
	 */
	int replaceAllIfPresent(Map<? extends IndexKey<K>, ? extends V> map);
	
	/**
	 * copy all of the mappings from the specified indexer to this indexer.
	 * The effect of this call is equivalent to that of calling
	 * {@link #replaceIfPresent(IndexKey, Object) replaceIfPresent(IndexKey, V)} on this indexer
	 * once for each mapping from key to value in the specified indexer.
	 * return the replaced entry count from {@code map}.
	 */
	int replaceAllIfPresent(Indexer<K, V> indexer);
	
	/**
	 * remove the mapping for a key from this indexer if it is present.
	 * return the previous value mapped the specified key, or <code>null</code>.
	 */
	V remove(IndexKey<K> key);
	
	/**
	 * remove the entry for a key only if currently mapped to a given value.
	 * return <code>true</code> if the given value was removed.
	 */
	boolean removeIfMatched(IndexKey<K> key, V value);
	
	/**
	 * removes all of the mappings from this indexer.
     * The indexer will be empty after this call returns.
	 */
	void clear();
	
	/** return an iterator over a set of key of this indexer **/
	Iterator<IndexKey<K>> keyIterator();
	
	/** return an iterator over a set of mapping from key to value of this indexer **/
	Iterator<ImmutableEntry<IndexKey<K>, V>> entryIterator();
}
