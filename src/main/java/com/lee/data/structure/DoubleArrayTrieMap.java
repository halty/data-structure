package com.lee.data.structure;

import java.util.Iterator;

/**
 * don't permit <code>null</code> key, permits <code>null</code> value<br/>
 * see more detail of <a href="https://linux.thai.net/~thep/datrie/datrie.html">
 * An Implementation of Double-Array Trie</a>
 * @NotThreadSafe
 */
public class DoubleArrayTrieMap<V> implements Iterable<ImmutableEntry<String, V>> {

	/** initial capacity of state nodes **/
	private static final int INIT_CAPACITY_OF_NODES = 32;
	/** the index of root node **/
	private static final int ROOT_STATE_INDEX = 0;
	/** the mark of root node for check **/
	private static final int ROOT_STATE_MARK = -1;
	
	/** the size of key-value pair **/
	private int size;
	/** the number of state nodes of trie **/
	private int numOfNodes;
	private int[] base;
	private int[] check;
	/** the outgoing characters of one state node **/
	private char[][] childs;
	/** the values attached with state nodes **/
	private Object[] values;
	/** the max depth of trie **/
	private int maxDepth;
	
	public DoubleArrayTrieMap() {
		init(INIT_CAPACITY_OF_NODES);
	}
	
	public DoubleArrayTrieMap(int expectedNumOfNodes) {
		if(expectedNumOfNodes <= 0) {
			throw new IllegalArgumentException("expectedNumOfNodes need > 0");
		}
		init(expectedNumOfNodes);
	}
	
	private void init(int capacityOfNodes) {
		this.size = 0;
		this.numOfNodes = 0;
		this.base = new int[capacityOfNodes];
		this.check = new int[capacityOfNodes];
		this.childs = new char[capacityOfNodes][];
		this.values = new Object[capacityOfNodes];
		this.maxDepth = 0;
	}
	
	public int size() { return size; }
	
	public boolean isEmpty() { return size == 0; }
	
	public void clear() { init(INIT_CAPACITY_OF_NODES); }
	
	public V put(String key, V value) {
		int length = key.length();
		if(length == 0) {
			V oldValue = null;
			if(check[ROOT_STATE_INDEX] == ROOT_STATE_MARK) {
				oldValue = (V) values[ROOT_STATE_INDEX];
			}else {
				base[ROOT_STATE_INDEX] = 0;
				check[ROOT_STATE_INDEX] = ROOT_STATE_MARK;
				size++;
				numOfNodes++;
			}
			values[ROOT_STATE_INDEX] = value;
			return oldValue;
		}
		
		
	}
	
	public V get(String key) {
		
	}
	
	public boolean containKey(String key) {
		
	}
	
	public V remove(String key) {
		
	}
	
	public void compact() {
		
	}
	
	@Override
	public String toString() {
		
	}
	
	@Override
	public Iterator<ImmutableEntry<String, V>> iterator() {
		return new DoubleArrayTrieIterator();
	}

	private final class DoubleArrayTrieIterator implements Iterator<ImmutableEntry<String, V>> {
		@Override
		public boolean hasNext() {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public ImmutableEntry<String, V> next() {
			// TODO Auto-generated method stub
			return null;
		}
	}
}
