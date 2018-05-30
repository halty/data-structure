package com.lee.data.structure.map;

import com.lee.data.structure.ImmutableEntry;
import com.lee.data.structure.Pair;

import java.util.Arrays;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.NoSuchElementException;

/**
 * don't permit <code>null</code> key, permits <code>null</code> value<br/>
 * The map is ordered according to the {@linkplain Comparable natural
 * ordering} of its {@linkplain String} keys.
 * @NotThreadSafe
 */
public class SortedTrieMap<V> implements Iterable<ImmutableEntry<String, V>> {

	private static final int INIT_CHILD_NODE_NUM = 1;
	
	private static class SortedTrieNode<V> {
		boolean occupied;	// 标识该节点是否已存储value
		V value;
		int numOfChildNodes;
		char[] chars;	// sorted chars
		SortedTrieNode<V>[] childNodes;
		
		@SuppressWarnings("unchecked")
		SortedTrieNode(int expectedNumOfChildNodes) {
			occupied = false;
			value = null;
			numOfChildNodes = 0;
			chars = new char[expectedNumOfChildNodes];
			childNodes = new SortedTrieNode[expectedNumOfChildNodes];
		}
	}
	
	private int size;
	private SortedTrieNode<V> root;
	
	/** Using the natural {@link String} ordering **/
	public SortedTrieMap() { clear(); }
	
	public int size() { return size; }
	
	public boolean isEmpty() { return size == 0; }
	
	public void clear() {
		size = 0;
		root = new SortedTrieNode<V>(INIT_CHILD_NODE_NUM);
	}
	
	public V put(String key, V value) {
		V oldValue = null;
		if(key.isEmpty()) {
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
	
	private V put(String key, int index, V value, SortedTrieNode<V> parent) {
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
			Pair<Boolean, Integer> found = find(ch, parent.chars, parent.numOfChildNodes);
			if(found.getLeft()) {
				return put(key, index+1, value, parent.childNodes[found.getRight()]);
			}else {
				if(parent.numOfChildNodes == parent.chars.length) {
					expand(parent);
				}
				int insertIndex = found.getRight();
				if(insertIndex < parent.numOfChildNodes) {
					int newIndex = insertIndex + 1;
					int shiftLength = parent.numOfChildNodes - insertIndex;
					System.arraycopy(parent.chars, insertIndex, parent.chars, newIndex, shiftLength);
					System.arraycopy(parent.childNodes, insertIndex, parent.childNodes, newIndex, shiftLength);
				}
				parent.chars[insertIndex] = ch;
				parent.childNodes[insertIndex] = new SortedTrieNode<V>(INIT_CHILD_NODE_NUM);
				parent.numOfChildNodes++;
				putForCreate(key, index+1, value, parent.childNodes[insertIndex]);
				return null;
			}
		}
	}
	
	private void expand(SortedTrieNode<V> parent) {
		int oldCapacity = parent.chars.length;
		int maxCapacity = Character.MAX_VALUE + 1;
		if(oldCapacity == maxCapacity) { return; }
		int newCapacity = oldCapacity << 1;
		if(newCapacity > maxCapacity) {
			newCapacity = maxCapacity;
		}
		char[] newChars = new char[newCapacity];
		System.arraycopy(parent.chars, 0, newChars, 0, parent.numOfChildNodes);
		parent.chars = newChars;
		@SuppressWarnings("unchecked")
		SortedTrieNode<V>[] newChildNodes = new SortedTrieNode[newCapacity];
		System.arraycopy(parent.childNodes, 0, newChildNodes, 0, parent.numOfChildNodes);
		parent.childNodes = newChildNodes;
	}
	
	/**
	 * (binary search)
	 * return &lt; true, matched index &gt; or &lt; false, inserting index &gt;
	 */
	private Pair<Boolean, Integer> find(char ch, char[] array, int length) {
		int left = 0;
		int right = length - 1;
		while(left <= right) {
			int middle = (left + right) / 2;
			char c = array[middle];
			if(ch == c) {
				return Pair.of(true, middle);
			}else if(ch < c) {
				right = middle - 1;
			}else {
				left = middle + 1;
			}
		}
		return Pair.of(false, left);
	}
	
	private void putForCreate(String key, int index, V value, SortedTrieNode<V> parent) {
		if(index == key.length()) {
			parent.value = value;
			parent.occupied = true;
			size++;
		}else {
			char ch = key.charAt(index);
			SortedTrieNode<V> node = new SortedTrieNode<V>(INIT_CHILD_NODE_NUM);
			parent.chars[0] = ch;
			parent.childNodes[0] = node;
			parent.numOfChildNodes++;
			putForCreate(key, index+1, value, node);
		}
	}
	
	public V get(String key) {
		if(key.isEmpty()) {
			return root.value;
		}else {
			SortedTrieNode<V> node = get(key, 0, root);
			return node == null ? null : node.value;
		}
	}
	
	private SortedTrieNode<V> get(String key, int index, SortedTrieNode<V> parent) {
		if(index == key.length()) {
			return parent;
		}else {
			char ch = key.charAt(index);
			Pair<Boolean, Integer> found = find(ch, parent.chars, parent.numOfChildNodes);
			if(found.getLeft()) {
				return get(key, index+1, parent.childNodes[found.getRight()]);
			}else {
				return null;
			}
		}
	}
	
	public boolean containKey(String key) {
		if(key.isEmpty()) {
			return root.occupied;
		}else {
			SortedTrieNode<V> node = get(key, 0, root);
			return node == null ? false : node.occupied;
		}
	}
	
	public V remove(String key) {
		V oldValue = null;
		SortedTrieNode<V> node = key.isEmpty() ? root : get(key, 0, root);
		if(node != null && node.occupied) {
			oldValue = node.value;
			node.value = null;
			node.occupied = false;
			size--;
		}
		return oldValue;
	}
	
	public void compact() { compact(root); }
	
	private void compact(SortedTrieNode<V> parent) {
		if(parent.numOfChildNodes > 0) {
			int nextIndex = 0;
			int length = parent.numOfChildNodes;
			for(int i=nextIndex; i<length; i++) {
				char ch = parent.chars[i];
				SortedTrieNode<V> node = parent.childNodes[i];
				compact(node);
				if(isEmptyNode(node)) {
					parent.chars[i] = 0;
					parent.childNodes[i] = null;
				}else {
					if(i != nextIndex) {
						parent.chars[nextIndex] = ch;
						parent.childNodes[nextIndex] = node;
						parent.chars[i] = 0;
						parent.childNodes[i] = null;
					}
					nextIndex++;
				}
			}
			parent.numOfChildNodes = nextIndex;
			if(nextIndex <= (parent.chars.length >>> 1)) { shrink(parent); }
		}
	}
	
	private boolean isEmptyNode(SortedTrieNode<V> node) {
		return !node.occupied && node.numOfChildNodes == 0;
	}
	
	private void shrink(SortedTrieNode<V> parent) {
		int newLength = parent.numOfChildNodes + 1;
		parent.chars = Arrays.copyOf(parent.chars, newLength);
		parent.childNodes = Arrays.copyOf(parent.childNodes, newLength);
	}
	
	@Override
	public String toString() {
		StringBuilder buf = new StringBuilder(16*size+2);	// 预估长度
		if(size == 0) {
			return buf.append("{}").toString();
		}
		buf.append("{");
		StringBuilder keyStack = new StringBuilder();
		append(keyStack, root, buf);
		if(buf.length() > 1) {
			buf.setLength(buf.length()-2);
			buf.append("}");
		}
		return buf.toString();
	}
	
	private void append(StringBuilder keyStack, SortedTrieNode<V> parent, StringBuilder buf) {
		if(parent.occupied) {
			buf.append(keyStack.toString())
			.append(" = ")
			.append(parent.value == this ? "(this map)" : parent.value)
			.append(", ");
		}
		if(parent.numOfChildNodes > 0) {
			for(int i=0; i<parent.numOfChildNodes; i++) {
				char ch = parent.chars[i];
				keyStack.append(ch);
				append(keyStack, parent.childNodes[i], buf);
				keyStack.setLength(keyStack.length()-1);
			}
		}
	}
	
	@Override
	public Iterator<ImmutableEntry<String, V>> iterator() {
		return new SortedTrieIterator();
	}
	
	private final class SortedTrieIterator implements Iterator<ImmutableEntry<String, V>> {
		
		private Deque<Pair<SortedTrieNode<V>, Integer>> stack;
		private StringBuilder keyStack;
		private SortedTrieNode<V> next;
		private SortedTrieNode<V> current;
		
		SortedTrieIterator() {
			stack = new LinkedList<Pair<SortedTrieNode<V>, Integer>>();
			keyStack = new StringBuilder();
			next = size == 0 ? null : SortedTrieMap.this.firstNode(stack, keyStack);
			current = null;
		}
		
		@Override
		public boolean hasNext() {
			return next != null;
		}

		@Override
		public ImmutableEntry<String, V> next() {
			SortedTrieNode<V> node = next;
			if(node == null) { throw new NoSuchElementException(); }
			String key = keyStack.toString();
			next = SortedTrieMap.this.nextNode(stack, keyStack);
			current = node;
			return new ImmutableEntry<String, V>(key, node.value);
		}
		
		@Override
		public void remove() {
			if(current != null && current.occupied) {
				current.value = null;
				current.occupied = false;
				size--;
			}
		}
	}

	/**
     * Returns a key-value mapping associated with the greatest key
     * strictly less than the given key, or {@code null} if there is
     * no such key.
     */
	public ImmutableEntry<String, V> lowerEntry(String key) {
		if(size == 0 || key.length() == 0) {
			return null;
		}else {
			Deque<Pair<SortedTrieNode<V>, Integer>> stack = new LinkedList<Pair<SortedTrieNode<V>, Integer>>();
			StringBuilder keyStack = new StringBuilder();
			matchForDesc(key, 0, root, stack, keyStack);
			if(keyStack.length() == key.length()) {	// all matches
				keyStack.setLength(keyStack.length() - 1);
			}
			Pair<SortedTrieNode<V>, Integer> pair = stack.peek();
			pair.setRight(pair.getRight() - 1);
			SortedTrieNode<V> node = prevNode(stack, keyStack);
			return node == null ? null : new ImmutableEntry<String, V>(keyStack.toString(), node.value);
		}
	}
	
	private void matchForDesc(String key, int index, SortedTrieNode<V> parent,
			Deque<Pair<SortedTrieNode<V>, Integer>> stack, StringBuilder keyStack) {
		if(index < key.length()) {
			char ch = key.charAt(index);
			Pair<Boolean, Integer> found = find(ch, parent.chars, parent.numOfChildNodes);
			if(found.getLeft()) {
				stack.push(Pair.of(parent, found.getRight()));
				keyStack.append(ch);
				matchForDesc(key, index+1, parent.childNodes[found.getRight()], stack, keyStack);
			}else {
				stack.push(Pair.of(parent, found.getRight()));
			}
		}
	}
	
	private SortedTrieNode<V> prevNode(Deque<Pair<SortedTrieNode<V>, Integer>> stack, StringBuilder keyStack) {
		while(!stack.isEmpty()) {
			Pair<SortedTrieNode<V>, Integer> pair = stack.peek();
			SortedTrieNode<V> node = pair.getLeft();
			int childIndex = pair.getRight();
			if(childIndex >= 0) {
				pair.setRight(childIndex - 1);
				SortedTrieNode<V> child = node.childNodes[childIndex];
				stack.push(Pair.of(child, child.numOfChildNodes-1));
				keyStack.append(node.chars[childIndex]);
				continue;
			}else {
				if(node.occupied) {
					return node;
				}else {
					int length = keyStack.length();
					if(length > 0) { keyStack.setLength(length - 1); }
					stack.pop();
				}
			}
		}
		return null;
	}
	
	/**
     * Returns a key-value mapping associated with the greatest key
     * less than or equal to the given key, or {@code null} if there
     * is no such key.
     */
	public ImmutableEntry<String, V> floorEntry(String key) {
		if(size == 0) { return null; }
		if(key.length() == 0) {
			return root.occupied ? new ImmutableEntry<String, V>(key, root.value) : null;
		}else {
			Deque<Pair<SortedTrieNode<V>, Integer>> stack = new LinkedList<Pair<SortedTrieNode<V>, Integer>>();
			StringBuilder keyStack = new StringBuilder();
			matchForDesc(key, 0, root, stack, keyStack);
			if(keyStack.length() == key.length()) {	// all matches
				Pair<SortedTrieNode<V>, Integer> pair = stack.peek();
				SortedTrieNode<V> node = pair.getLeft().childNodes[pair.getRight()];
				if(node.occupied) {
					return new ImmutableEntry<String, V>(key, node.value);
				}
				keyStack.setLength(keyStack.length() - 1);
			}
			Pair<SortedTrieNode<V>, Integer> pair = stack.peek();
			pair.setRight(pair.getRight() - 1);
			SortedTrieNode<V> node = prevNode(stack, keyStack);
			return node == null ? null : new ImmutableEntry<String, V>(keyStack.toString(), node.value);
		}
	}
	
	/**
     * Returns a key-value mapping associated with the least key
     * greater than or equal to the given key, or {@code null} if
     * there is no such key.
     */
	public ImmutableEntry<String, V> ceilingEntry(String key) {
		if(size == 0) { return null; }
		if(key.length() == 0 && root.occupied) {
			return new ImmutableEntry<String, V>(key, root.value);
		}
		Deque<Pair<SortedTrieNode<V>, Integer>> stack = new LinkedList<Pair<SortedTrieNode<V>, Integer>>();
		StringBuilder keyStack = new StringBuilder();
		matchForAsc(key, 0, root, stack, keyStack);
		if(keyStack.length() == key.length()) {	// all matches
			if(key.length() == 0) {
				stack.push(Pair.of(root, 0));	// sub nodes
			}else {
				Pair<SortedTrieNode<V>, Integer> pair = stack.peek();
				SortedTrieNode<V> node = pair.getLeft().childNodes[pair.getRight()-1];
				if(node.occupied) {
					return new ImmutableEntry<String, V>(key, node.value);
				}
				stack.push(Pair.of(node, 0));	// sub nodes
			}
		}
		SortedTrieNode<V> node = nextNode(stack, keyStack);
		return node == null ? null : new ImmutableEntry<String, V>(keyStack.toString(), node.value);
	}
	
	private void matchForAsc(String key, int index, SortedTrieNode<V> parent,
			Deque<Pair<SortedTrieNode<V>, Integer>> stack, StringBuilder keyStack) {
		if(index < key.length()) {
			char ch = key.charAt(index);
			Pair<Boolean, Integer> found = find(ch, parent.chars, parent.numOfChildNodes);
			if(found.getLeft()) {
				stack.push(Pair.of(parent, found.getRight() + 1));
				keyStack.append(ch);
				matchForAsc(key, index+1, parent.childNodes[found.getRight()], stack, keyStack);
			}else {
				stack.push(Pair.of(parent, found.getRight()));
			}
		}
	}
	
	private SortedTrieNode<V> nextNode(Deque<Pair<SortedTrieNode<V>, Integer>> stack, StringBuilder keyStack) {
		while(!stack.isEmpty()) {
			Pair<SortedTrieNode<V>, Integer> pair = stack.peek();
			SortedTrieNode<V> node = pair.getLeft();
			int childIndex = pair.getRight();
			if(childIndex == -1) {
				if(node.occupied) {
					pair.setRight(childIndex + 1);
					return node;
				}
				childIndex += 1;
			}
			if(childIndex < node.numOfChildNodes) {
				pair.setRight(childIndex + 1);
				stack.push(Pair.of(node.childNodes[childIndex], -1));
				keyStack.append(node.chars[childIndex]);
				continue;
			}
			// childIndex == node.numOfChildNodes
			int length = keyStack.length();
			if(length > 0) { keyStack.setLength(length - 1); }
			stack.pop();
		}
		return null;
	}
	
	/**
     * Returns a key-value mapping associated with the least key
     * strictly greater than the given key, or {@code null} if there
     * is no such key.
     */
	public ImmutableEntry<String, V> higherEntry(String key) {
		if(size == 0) { return null; }
		Deque<Pair<SortedTrieNode<V>, Integer>> stack = new LinkedList<Pair<SortedTrieNode<V>, Integer>>();
		StringBuilder keyStack = new StringBuilder();
		matchForAsc(key, 0, root, stack, keyStack);
		if(keyStack.length() == key.length()) {	// all matches
			if(key.length() == 0) {
				stack.push(Pair.of(root, 0));	// sub nodes
			}else {
				Pair<SortedTrieNode<V>, Integer> pair = stack.peek();
				SortedTrieNode<V> node = pair.getLeft().childNodes[pair.getRight()-1];
				stack.push(Pair.of(node, 0));	// sub nodes
			}
		}
		SortedTrieNode<V> node = nextNode(stack, keyStack);
		return node == null ? null : new ImmutableEntry<String, V>(keyStack.toString(), node.value);
	}
	
	/**
     * Returns a key-value mapping associated with the least
     * key in this map, or {@code null} if the map is empty.
     */
	public ImmutableEntry<String, V> firstEntry() {
		if(size == 0) { return null; }
		Deque<Pair<SortedTrieNode<V>, Integer>> stack = new LinkedList<Pair<SortedTrieNode<V>, Integer>>();
		StringBuilder keyStack = new StringBuilder();
		SortedTrieNode<V> node = firstNode(stack, keyStack);
		return node == null ? null : new ImmutableEntry<String, V>(keyStack.toString(), node.value);
	}
	
	private SortedTrieNode<V> firstNode(Deque<Pair<SortedTrieNode<V>, Integer>> stack, StringBuilder keyStack) {
		stack.push(Pair.of(root, -1));
		return nextNode(stack, keyStack);
	}
	
	/**
     * Returns a key-value mapping associated with the greatest
     * key in this map, or {@code null} if the map is empty.
     */
	public ImmutableEntry<String, V> lastEntry() {
		if(size == 0) { return null; }
		Deque<Pair<SortedTrieNode<V>, Integer>> stack = new LinkedList<Pair<SortedTrieNode<V>, Integer>>();
		StringBuilder keyStack = new StringBuilder();
		SortedTrieNode<V> node = lastNode(stack, keyStack);
		return node == null ? null : new ImmutableEntry<String, V>(keyStack.toString(), node.value);
	}
	
	private SortedTrieNode<V> lastNode(Deque<Pair<SortedTrieNode<V>, Integer>> stack, StringBuilder keyStack) {
		stack.push(Pair.of(root, root.numOfChildNodes-1));
		return prevNode(stack, keyStack);
	}
}
