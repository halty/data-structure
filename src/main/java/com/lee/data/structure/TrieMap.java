package com.lee.data.structure;

/** 允许<code>null</code> key or <code>null</code> value **/
public class TrieMap<V> {
	
	private static final int INIT_CHILD_NODE_NUM = 1;

	private static class TrieNode<V> {
		boolean occupied;	// 标识该节点是否已存储value
		V value;
		TrieNode<V> childNodeForZeroChar;
		int numOfChildNodes;
		char[] chars;
		TrieNode<V>[] childNodes;
		
		@SuppressWarnings("unchecked")
		TrieNode(int expectedNum) {
			occupied = false;
			value = null;
			numOfChildNodes = 0;
			childNodeForZeroChar = null;
			chars = new char[expectedNum];
			childNodes = new TrieNode[expectedNum];
		}
	}
	
	private int size;
	private boolean hasNullKey;
	private V valueForNullKey;
	private TrieNode<V> root;
	
	public TrieMap() { clear(); }
	
	public int size() { return size; }
	
	public boolean isEmpty() { return size == 0; }
	
	public void clear() {
		size = 0;
		hasNullKey = false;
		valueForNullKey = null;
		root = new TrieNode<V>(INIT_CHILD_NODE_NUM);
	}
	
	public V put(String key, V value) {
		V oldValue = null;
		if(key == null) {
			oldValue = valueForNullKey;
			valueForNullKey = value;
			if(!hasNullKey) {
				hasNullKey = true;
				size++;
			}
		}else if(key.isEmpty()) {
			oldValue = root.value;
			root.value = value;
			if(!root.occupied) {
				root.occupied = true;
				size++;
			}
		}else {
			oldValue = put(key, 0, value, root);
		}
		return oldValue;
	}
	
	private V put(String key, int index, V value, TrieNode<V> parent) {
		if(index == key.length()) {
			V oldValue = parent.value;
			parent.value = value;
			if(!parent.occupied) {
				parent.occupied = true;
				size++;
			}
			return oldValue;
		}else {
			char ch = key.charAt(index);
			if(ch == 0) {
				if(parent.childNodeForZeroChar == null) {
					parent.childNodeForZeroChar = new TrieNode<V>(INIT_CHILD_NODE_NUM);
					putForCreate(key, index+1, value, parent.childNodeForZeroChar);
					return null;
				}else {
					return put(key, index+1, value, parent.childNodeForZeroChar);
				}
			}else {
				if(parent.numOfChildNodes == parent.chars.length) {
					resize(parent);
				}
				char[] chars = parent.chars;
				int first = ch % chars.length;
				int i = first;
				boolean found = false;
				do {
					if(chars[i] == 0) {
						break;
					}else if(chars[i] == ch) {
						found = true;
						break;
					}else {
						i = (i+1) % chars.length;
					}
				}while(i != first);
				if(found) {
					return put(key, index+1, value, parent.childNodes[i]);
				}else {
					if(chars[i] != 0) {
						throw new IllegalStateException("child node array is not full, so exists vacancy");
					}
					chars[i] = ch;
					parent.childNodes[i] = new TrieNode<V>(INIT_CHILD_NODE_NUM);
					parent.numOfChildNodes++;
					putForCreate(key, index+1, value, parent.childNodes[i]);
					return null;
				}
			}
		}
	}
	
	private void putForCreate(String key, int index, V value, TrieNode<V> parent) {
		if(index == key.length()) {
			parent.value = value;
			parent.occupied = true;
			size++;
		}else {
			char ch = key.charAt(index);
			if(ch == 0) {
				TrieNode<V> node = new TrieNode<V>(INIT_CHILD_NODE_NUM);
				parent.childNodeForZeroChar = node;
				putForCreate(key, index+1, value, node);
			}else {
				TrieNode<V> node = new TrieNode<V>(INIT_CHILD_NODE_NUM);
				int i = ch % parent.chars.length;
				parent.chars[i] = ch;
				parent.childNodes[i] = node;
				parent.numOfChildNodes++;
				putForCreate(key, index+1, value, node);
			}
		}
	}
	
	private void resize(TrieNode<V> parent) {
		char[] oldChars = parent.chars;
		TrieNode<V>[] oldChildNodes = parent.childNodes;
		int newCapacity = oldChars.length << 1;
		char[] newChars = new char[newCapacity];
		@SuppressWarnings("unchecked")
		TrieNode<V>[] newChildNodes = new TrieNode[newCapacity];
		for(int i=0; i<oldChars.length; i++) {
			if(oldChars[i] == 0) { continue; }
			int newIndex = oldChars[i] % newCapacity;
			while(newChars[newIndex] != 0) {
				newIndex = (newIndex+1) % newCapacity;
			}
			newChars[newIndex] = oldChars[i];
			newChildNodes[newIndex] = oldChildNodes[i];
		}
		parent.childNodes = newChildNodes;
		parent.chars = newChars;
	}
}