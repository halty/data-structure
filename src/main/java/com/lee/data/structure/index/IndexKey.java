package com.lee.data.structure.index;

import java.util.Arrays;

/** A wrapper class for multiple keys **/
public final class IndexKey<K> {

	private final Object[] keys;

	private int hash;
	
	public IndexKey(K key) {
		keys = new Object[]{ key };
	}
	
	public IndexKey(K key1, K key2) {
		keys = new Object[]{ key1, key2 };
	}
	
	public IndexKey(K key1, K key2, K key3) {
		keys = new Object[]{ key1, key2, key3 };
	}
	
	public IndexKey(K key1, K key2, K key3, K key4) {
		keys = new Object[]{ key1, key2, key3, key4 };
	}
	
	public IndexKey(K key1, K key2, K key3, K key4, K key5) {
		keys = new Object[]{ key1, key2, key3, key4, key5 };
	}
	
	public IndexKey(K[] keys) {
		this.keys = Arrays.copyOf(keys, keys.length);
	}
	
	@SuppressWarnings("unchecked")
	public K[] keys() {
		return (K[]) Arrays.copyOf(keys, keys.length);
	}
	
	public int keyCount() {
		return keys.length;
	}
	
	@SuppressWarnings("unchecked")
	public K keyAt(int index) {
		return (K) keys[index];
	}

	@Override
	public int hashCode() {
		int h = hash;
		if(h == 0 && keys.length > 0) {
			hash = h = Arrays.hashCode(keys);
		}
		return h;
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean equals(Object obj) {
		if(obj == this) { return true; }
		if(obj == null || obj.getClass() != IndexKey.class) { return false; }
		return Arrays.equals(keys, ((IndexKey<K>)obj).keys);
	}

	@Override
	public String toString() {
		return "IndexKey{keys=" + Arrays.toString(keys)+"}";
	}
	
}
