package com.lee.data.structure;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Random;

/**
 * don't permit <code>null</code> key, permits <code>null</code> value<br/>
 * see more detail of <a href="https://linux.thai.net/~thep/datrie/datrie.html">
 * An Implementation of Double-Array Trie</a>
 * @NotThreadSafe
 */
public class DoubleArrayTrieMap<V> implements Iterable<ImmutableEntry<String, V>> {

	/** initial capacity of state nodes **/
	private static final int INIT_CAPACITY_OF_NODES = 32;
	/** initial capacity of outgoing characters of each state node **/
	private static final int INIT_CAPACITY_OF_OUTGOING_CHARS = 2;
	/** the state of root node **/
	private static final int ROOT_STATE = 0;
	/** the mark of root node for check **/
	private static final int ROOT_STATE_MARK = -1;
	
	/** an random instance for generate base offset **/
	private Random rand;
	/** the size of key-value pair **/
	private int size;
	/** the number of state nodes of trie **/
	private int numOfNodes;
	private int[] base;
	private long[] check;
	/** the outgoing characters of one state node **/
	private char[][] childs;
	/** the values attached with state nodes **/
	private V[] values;
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
	
	@SuppressWarnings("unchecked")
	private void init(int capacityOfNodes) {
		this.rand = new Random();
		this.size = 0;
		this.numOfNodes = 0;
		this.base = new int[capacityOfNodes];
		this.check = new long[capacityOfNodes];
		this.childs = new char[capacityOfNodes][];
		this.values = (V[]) new Object[capacityOfNodes];
		this.maxDepth = 0;
	}
	
	public int size() { return size; }
	
	public boolean isEmpty() { return size == 0; }
	
	public void clear() { init(INIT_CAPACITY_OF_NODES); }
	
	public V put(String key, V value) {
		int length = key.length();
		if(length == 0) {
			V oldValue = null;
			if(check[ROOT_STATE] == ROOT_STATE_MARK) {
				oldValue = (V) values[ROOT_STATE];
			}else {
				base[ROOT_STATE] = 0;
				check[ROOT_STATE] = ROOT_STATE_MARK;
				size++;
				numOfNodes++;
			}
			values[ROOT_STATE] = value;
			return oldValue;
		}
		
		Pair<Integer, Integer> stateIndex = findFirstUnmatchedState(key, 0);
		return putForCreate(stateIndex, key, value);
	}
	
	private int randomBase() { return rand.nextInt(); }
	
	/** return < the first unmatched state, the unmatch character index of {@code key} > **/
	private Pair<Integer, Integer> findFirstUnmatchedState(String key, int start) {
		int parentState = ROOT_STATE;
		int length = key.length();
		while(start < length) {
			char ch = key.charAt(start);
			int targetState = targetState(parentState, ch);
			if(!isTransitionExisted(parentState, ch, targetState)) { break; }
			parentState = targetState;
			start += 1;
			if(!hasOutgoingTransition(targetState)) { break; }
		}
		return Pair.of(parentState, start);
	}
	
	private int targetState(int sourceState, char ch) {
		int r = (base[sourceState] + ch) % base.length;
		if(r < 0) { r += base.length; }
		return r + 1;
	}
	
	private boolean isTransitionExisted(int sourceState, char ch, int targetState) {
		long checkMark = check[targetState];
		return checkMark != 0 && checkMark == checkMark(sourceState, ch);
	}
	
	private static long checkMark(int sourceState, char ch) {
		long mark = Character.MAX_VALUE;
		mark = (mark << 16) + ch;
		mark = (mark << 32) + sourceState;
		return mark;
	}
	
	private boolean hasOutgoingTransition(int state) {
		if(base[state] == 0) { return false; }
		char[] outgoingChars = childs[state];
		return outgoingChars != null && outgoingChars.length > 0;
	}
	
	private V putForCreate(Pair<Integer, Integer> stateIndex, String key, V value) {
		int length = key.length();
		int index = stateIndex.getRight();
		int state = stateIndex.getLeft();
		if(index == length) {
			V oldValue = values[state];
			values[state] = value;
			return oldValue;
		}
		int start = index;
		int parentState = state;
		while(start < length) {
			char ch = key.charAt(start);
			int targetState = targetState(parentState, ch);
			if(isVacancyState(targetState)) {
				createTransition(parentState, ch, targetState);
			}
		}
	}
	
	private void initBaseIf
	
	private boolean isVacancyState(int state) { return check[state] == 0; }
	
	private void createTransition(int sourceState, char ch, int targetState) {
		check[targetState] = checkMark(sourceState, ch);
		addChild(sourceState, ch);
		numOfNodes++;
	}
	
	private void addChild(int state, char ch) {
		char[] array = childs[state];
		if(array == null) {
			array = new char[INIT_CAPACITY_OF_OUTGOING_CHARS];
			childs[state] = array;
			array[0] = 1;	// array size
			array[1] = ch;
		}else {
			int size = array[0];
			if(size == array.length - 1) {
				int newLength = array.length + INIT_CAPACITY_OF_OUTGOING_CHARS;
				array = Arrays.copyOf(array, newLength);
				childs[state] = array;
			}
			Pair<Boolean, Integer> found = find(ch, array, size);
			if(found.getLeft()) {
				// you are never go to here
				throw new IllegalStateException("add outgoing character for state, exist duplicate in childs");
			}else {
				int position = found.getRight();
				System.arraycopy(array, position, array, position+1, size-position+1);
				array[position] = ch;
				array[0] += 1;	// array size increase 1
			}
		}
	}
	
	/**
	 * (binary search)
	 * return &lt; true, matched index &gt; or &lt; false, inserting index &gt;
	 */
	private Pair<Boolean, Integer> find(char ch, char[] array, int end) {
		int left = 1;
		int right = end;
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
	
	public V get(String key) {
		if(size == 0) { return null; }
		int length = key.length();
		if(length == 0) {
			return check[ROOT_STATE] == ROOT_STATE_MARK ? values[ROOT_STATE] : null;
		}
		
		Pair<Integer, Integer> stateIndex = findFirstUnmatchedState(key, 0);
		if(stateIndex.getRight() == length) {
			return values[stateIndex.getLeft()];
		}else {
			return null;
		}
	}
	
	public boolean containKey(String key) {
		if(size == 0) { return false; }
		int length = key.length();
		return length == 0 ? check[ROOT_STATE] == ROOT_STATE_MARK :
				findFirstUnmatchedState(key, 0).getRight() == length;
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
