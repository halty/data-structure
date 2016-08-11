package com.lee.data.structure;

/** 简单的不可变Entry **/
public final class ImmutableEntry<K, V> {
	
	public final K key;
	public final V value;
	
	public ImmutableEntry(K key, V value) {
		this.key = key;
		this.value = value;
	}
}
