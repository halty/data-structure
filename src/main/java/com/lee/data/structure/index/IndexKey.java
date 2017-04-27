package com.lee.data.structure.index;

import java.util.Arrays;

/** A wrapper class for multiple keys **/
public final class IndexKey<K> implements Comparable<IndexKey<K>> {

	private final Object[] keys;

	private int hash;
	
	public static <K> IndexKey<K> of(K key) {
		return of(new Object[]{ key });
	}
	
	public static <K> IndexKey<K> of(K key1, K key2) {
		return of(new Object[]{ key1, key2 });
	}
	
	public static <K> IndexKey<K> of(K key1, K key2, K key3) {
		return of(new Object[]{ key1, key2, key3 });
	}
	
	public static <K> IndexKey<K> of(K key1, K key2, K key3, K key4) {
		return of(new Object[]{ key1, key2, key3, key4 });
	}
	
	public static <K> IndexKey<K> of(K key1, K key2, K key3, K key4, K key5) {
		return of(new Object[]{ key1, key2, key3, key4, key5 });
	}
	
	public static <K> IndexKey<K> copyOf(K[] keys) {
		return of(Arrays.copyOf(keys, keys.length));
	}
	
	private static <K> IndexKey<K> of(Object[] keys) { return new IndexKey<K>(keys); }
	
	private IndexKey(Object[] keys) { this.keys = keys; }
	
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
			hash = h = (31 + Arrays.hashCode(keys));
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

	@SuppressWarnings("unchecked")
	@Override
	public int compareTo(IndexKey<K> o) {
		int length = Math.min(keys.length, o.keys.length);
		for(int i=0; i<length; i++) {
			Comparable<? super K> k1 = (Comparable<? super K>) keys[i];
			K k2 = (K) o.keys[i];
			if(k1 == null) {
				if(k2 != null) { return -1; }
			}else {
				if(k2 == null) {
					return 1;
				}else {
					int cmp = k1.compareTo(k2);
					if(cmp != 0) { return cmp; }
				}
			}
		}
		return keys.length - o.keys.length;
	}

	@Override
	public String toString() {
		return "IndexKey{keys=" + Arrays.toString(keys)+"}";
	}
	
}
