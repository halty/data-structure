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
					expand(parent);
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
	
	private void expand(TrieNode<V> parent) {
		int oldCapactiy = parent.chars.length;
		int newCapacity = oldCapactiy << 1;
		if(newCapacity > Character.MAX_VALUE) {
			newCapacity = Character.MAX_VALUE;
		}
		resize(parent, newCapacity);
	}
	
	private void resize(TrieNode<V> parent, int newCapacity) {
		char[] oldChars = parent.chars;
		TrieNode<V>[] oldChildNodes = parent.childNodes;
		char[] newChars = new char[newCapacity];
		@SuppressWarnings("unchecked")
		TrieNode<V>[] newChildNodes = new TrieNode[newCapacity];
		for(int i=0; i<oldChars.length; i++) {
			if(oldChars[i] == 0) { continue; }
			int first = oldChars[i] % newCapacity;
			int newIndex = first;
			boolean found = false;
			do {
				if(newChars[newIndex] == 0) {
					found = true;
					break;
				}
				newIndex = (newIndex+1) % newCapacity;
			}while(newIndex != first);
			if(!found) {
				throw new IllegalStateException("can not find vacancy for char '"
						+oldChars[i]+"' with newCapacity="+newCapacity);
			}
			newChars[newIndex] = oldChars[i];
			newChildNodes[newIndex] = oldChildNodes[i];
		}
		parent.childNodes = newChildNodes;
		parent.chars = newChars;
	}
	
	public V get(String key) {
		if(key == null) {
			return valueForNullKey;
		}else if(key.isEmpty()) {
			return root.value;
		}else {
			TrieNode<V> node = get(key, 0, root);
			if(node != null) {
				return node.value;
			}else {
				return null;
			}
		}
	}
	
	private TrieNode<V> get(String key, int index, TrieNode<V> parent) {
		if(index == key.length()) {
			return parent;
		}else {
			char ch = key.charAt(index);
			TrieNode<V> node = null;
			if(ch == 0) {
				node = parent.childNodeForZeroChar;
			}else {
				int first = ch % parent.chars.length;
				boolean found = false;
				int i = first;
				do {
					if(parent.chars[i] == 0) {
						break;
					}else if(parent.chars[i] == ch) {
						found = true;
						break;
					}else {
						i = (i+1) % parent.chars.length;
					}
				}while(i != first);
				if(found) {
					node = parent.childNodes[i];
				}
			}
			if(node != null) {
				return get(key, index+1, node);
			}else {
				return null;
			}
		}
	}
	
	public boolean containKey(String key) {
		if(key == null) {
			return hasNullKey;
		}else if(key.isEmpty()) {
			return root.occupied;
		}else {
			TrieNode<V> node = get(key, 0, root);
			if(node != null) {
				return node.occupied;
			}else {
				return false;
			}
		}
	}
	
	public V remove(String key) {
		V oldValue = null;
		if(key == null) {
			if(hasNullKey) {
				oldValue = valueForNullKey;
				valueForNullKey = null;
				hasNullKey = false;
				size--;
			}
		}else if(key.isEmpty()) {
			if(root.occupied) {
				oldValue = root.value;
				root.value = null;
				root.occupied = false;
				size--;
			}
		}else {
			TrieNode<V> node = get(key, 0, root);
			if(node != null) {
				if(node.occupied) {
					oldValue = node.value;
					node.value = null;
					node.occupied = false;
					size--;
				}
			}
		}
		return oldValue;
	}
	
	public void compact() { compact(root); }
	
	private void compact(TrieNode<V> parent) {
		if(parent.childNodeForZeroChar != null) {
			compact(parent.childNodeForZeroChar);
			if(isEmptyNode(parent.childNodeForZeroChar)) {
				parent.childNodeForZeroChar = null;
			}
		}
		if(parent.numOfChildNodes > 0) {
			for(int i=0; i<parent.chars.length; i++) {
				char ch = parent.chars[i];
				if(ch == 0) { continue; }
				compact(parent.childNodes[i]);
				if(isEmptyNode(parent.childNodes[i])) {
					parent.chars[i] = 0;
					parent.childNodes[i] = null;
					parent.numOfChildNodes--;
				}
			}
			
			if(parent.numOfChildNodes <= (parent.chars.length >>> 1)) {
				shrink(parent);
			}
		}
	}
	
	private boolean isEmptyNode(TrieNode<V> node) {
		return !node.occupied
			&& node.childNodeForZeroChar == null
			&& node.numOfChildNodes == 0;
	}
	
	private void shrink(TrieNode<V> parent) {
		int newCapacity = (parent.numOfChildNodes > 1) ?
				Integer.highestOneBit((parent.numOfChildNodes - 1) << 1) : 1;
		resize(parent, newCapacity);
	}
	
	@Override
	public String toString() {
		StringBuilder buf = new StringBuilder(16*size+2);	// 预估长度
		if(size == 0) {
			return buf.append("{}").toString();
		}
		buf.append("{");
		if(hasNullKey) {
			buf.append("null = ")
			.append(valueForNullKey == this ? "(this map)" : valueForNullKey)
			.append(", ");
		}
		StringBuilder keyStack = new StringBuilder();
		append(keyStack, root, buf);
		if(buf.length() > 1) {
			buf.setLength(buf.length()-2);
			buf.append("}");
		}
		return buf.toString();
	}
	
	private void append(StringBuilder keyStack, TrieNode<V> parent, StringBuilder buf) {
		if(parent.occupied) {
			buf.append(keyStack.toString())
			.append(" = ")
			.append(parent.value == this ? "(this map)" : parent.value)
			.append(", ");
		}
		if(parent.childNodeForZeroChar != null) {
			char ch = 0;
			keyStack.append(ch);
			append(keyStack, parent.childNodeForZeroChar, buf);
			keyStack.setLength(keyStack.length()-1);
		}
		if(parent.numOfChildNodes > 0) {
			for(int i=0; i<parent.chars.length; i++) {
				char ch = parent.chars[i];
				if(ch == 0) { continue; }
				keyStack.append(ch);
				append(keyStack, parent.childNodes[i], buf);
				keyStack.setLength(keyStack.length()-1);
			}
		}
	}
}
