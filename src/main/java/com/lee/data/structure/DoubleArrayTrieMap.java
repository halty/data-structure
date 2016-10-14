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
	/** default value for <code>null</code> **/
	private static final Object NULL_VALUE = new Object();
	/** max retry times for relocate base index or expand capacity **/
	private static final int MAX_RETRY_TIMES = 3;
	
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
	private Object[] values;
	/** the max depth of trie **/
	private int maxDepth;
	
	/** recode history base while relocation **/
	private int[] historyBase = new int[MAX_RETRY_TIMES + 1];
	
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
		this.rand = new Random();
		this.size = 0;
		this.numOfNodes = 1;	// permanent root node 
		this.base = new int[capacityOfNodes];
		this.check = new long[capacityOfNodes];
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
			Object oldValue = null;
			if(check[ROOT_STATE] == ROOT_STATE_MARK) {
				oldValue = values[ROOT_STATE];
			}else {
				check[ROOT_STATE] = ROOT_STATE_MARK;
				size++;
			}
			values[ROOT_STATE] = value;
			return unmask(oldValue);
		}
		
		Pair<Integer, Integer> stateIndex = findFirstUnmatchedState(key, 0);
		return putForCreate(stateIndex, key, value);
	}
	
	@SuppressWarnings("unchecked")
	private V unmask(Object value) { return value == NULL_VALUE ? null : (V)value; }
	
	/** return < the first unmatched state, the unmatch character index of {@code key} > **/
	private Pair<Integer, Integer> findFirstUnmatchedState(String key, int start) {
		int parentState = ROOT_STATE;
		int length = key.length();
		while(start < length) {
			if(!hasOutgoingTransition(parentState)) { break; }
			char ch = key.charAt(start);
			int targetState = targetState(parentState, ch);
			if(!isTransitionExisted(parentState, ch, targetState)) { break; }
			parentState = targetState;
			start += 1;
		}
		return Pair.of(parentState, start);
	}
	
	private boolean hasOutgoingTransition(int state) {
		if(base[state] == 0) { return false; }
		char[] outgoingChars = childs[state];
		return outgoingChars != null && outgoingChars.length > 0;
	}
	
	private int targetState(int sourceState, char ch) {
		return calcTargetState(base[sourceState], ch);
	}
	
	private int calcTargetState(int baseIndex, char ch) {
		int r = (baseIndex + ch) % base.length;
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
	
	private V putForCreate(Pair<Integer, Integer> stateIndex, String key, V value) {
		int length = key.length();
		int index = stateIndex.getRight();
		int state = stateIndex.getLeft();
		if(index == length) { return replaceIfExisted(state, value, length); }
		int start = index;
		int parentState = state;
		while(start < length) {
			initBase(parentState);
			char ch = key.charAt(start);
			int targetState = targetState(parentState, ch);
			if(isVacancyState(targetState)) {
				createTransition(parentState, ch, targetState);
			}else {
				targetState = relocate(parentState, ch);
			}
			parentState = targetState;
			start += 1;
		}
		return replaceIfExisted(parentState, value, length);
	}
	
	private V replaceIfExisted(int state, V newValue, int keyLength) {
		Object oldValue = values[state];
		if(oldValue == null) {
			size++;
			if(keyLength > maxDepth) { maxDepth = keyLength; }
		}
		values[state] = mask(newValue);
		return unmask(oldValue);
	}
	
	private Object mask(V value) { return value == null ? NULL_VALUE : value; }
	
	private void initBase(int state) {
		if(base[state] == 0) {
			base[state] = randomBase();
		}
	}
	
	private int randomBase() {
		int r = 0;
		do {
			r = rand.nextInt();
		}while(r == 0);
		return r;
	}
	
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
			if(size == 0) {
				array[0] = 1;
				array[1] = ch;
				return;
			}
			Pair<Boolean, Integer> found = find(ch, array, size);
			if(found.getLeft()) {
				// you never go to here
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
	
	/**
	 * relocate <code>parentState</code> all existed outgoing character for
	 * conflict character <code>conflictChar</code>, then create transition between
	 * <code>parentState</code> and <code>targetState</code> with
	 * <code>conflictChar</code>.
	 * @param parentState	need relocated state
	 * @param conflictChar	conflict character
	 * @param maxRetryTimes max retry times while confliction
	 * @return	< relocate success or not, target state >
	 */
	private Pair<Boolean, Integer> relocate(int parentState, char conflictChar, int maxRetryTimes) {
		char[] childChars = childs[parentState];
		int childLength = childChars == null ? 0 : childChars[0];
		if(childLength == 0) {
			return relocateWithoutChilds(parentState, conflictChar, maxRetryTimes);
		}else {
			return relocateWithChilds(parentState, conflictChar, maxRetryTimes);
		}
	}
	
	private Pair<Boolean, Integer> relocateWithoutChilds(int parentState, char conflictChar, int maxRetryTimes) {
		int oldBase = base[parentState];
		fillHistoryBase(0, oldBase);
		for(int i=0; i<maxRetryTimes; i++) {
			int newBase = selectNonDuplicateBase(i);
			int targetState = calcTargetState(newBase, conflictChar);
			if(isVacancyState(targetState)) {
				createTransition(parentState, conflictChar, targetState);
				base[parentState] = newBase;
				return Pair.of(true, targetState);
			}else {
				fillHistoryBase(i+1, newBase);
			}
		}
		return Pair.of(false, -1);
	}
	
	private void fillHistoryBase(int index, int oldBase) {
		historyBase[index] = oldBase;
	}
	
	private int selectNonDuplicateBase(int end) {
		int newBase = 0;
		do {
			newBase = randomBase();
		}while(isDuplicate(end, newBase));
		return newBase;
	}
	
	private boolean isDuplicate(int end, int newBase) {
		for(int i=0; i<=end; i++) {
			if(historyBase[i] == newBase) { return true; }
		}
		return false;
	}
	
	private Pair<Boolean, Integer> relocateWithChilds(int parentState, char conflictChar, int maxRetryTimes) {
		char[] childChars = childs[parentState];
		int childLength = childChars[0];
		int oldBase = base[parentState];
		fillHistoryBase(0, oldBase);
		if(maxRetryTimes > MAX_RETRY_TIMES) { maxRetryTimes = MAX_RETRY_TIMES; }
		for(int i=0; i<maxRetryTimes; i++) {
			int newBase = selectNonDuplicateBase(i);
			int targetState = calcTargetState(newBase, conflictChar);
			if(isVacancyState(targetState)) {
				int j = 1;
				while(j<=childLength) {
					char ch = childChars[j];
					int newState = calcTargetState(newBase, ch);
					if(isVacancyState(newState)) {
						shift(oldBase, ch, newBase);
						j++;
					}else {
						break;
					}
				}
				if(j > childLength) {
					
				}else {
					rollback(newBase, j-1, oldBase);
				}
			}else {
				fillHistoryBase(i+1, newBase);
			}
		}
		return Pair.of(false, -1);
	}
	
	private void shift(int oldBase, char ch, int newBase) {
		int oldState = calcTargetState(oldBase, ch);
		int newState = calcTargetState(newBase, ch);
		copyStateData(oldState, newState);
		freeStateData(oldState);
	}
	
	private void copyStateData(int oldState, int newState) {
		int oldStateBase = base[oldState];
		base[newState] = oldStateBase;
		check[newState] = check[oldState];
		char[] childChars = childs[oldState];
		int childLength = childChars == null ? 0 : childChars[0];
		if(childLength > 0) {
			for(int i=1; i<=childLength; i++) {
				char ch = childChars[i];
				int state = calcTargetState(oldStateBase, ch);
				check[state] = checkMark(newState, ch);
			}
		}
		childs[newState] = childs[oldState];
		values[newState] = values[oldState];
	}
	
	private void freeStateData(int oldState) {
		base[oldState] = 0;
		check[oldState] = 0L;
		childs[oldState] = null;
		values[oldState] = null;
	}
	
	private void rollback(int oldBase, int shiftEndIndex, int newBase) {
		
	}
	
	public V get(String key) {
		if(size == 0) { return null; }
		int length = key.length();
		if(length == 0) {
			return check[ROOT_STATE] == ROOT_STATE_MARK ? unmask(values[ROOT_STATE]) : null;
		}
		
		Pair<Integer, Integer> stateIndex = findFirstUnmatchedState(key, 0);
		if(stateIndex.getRight() == length) {
			Object value = values[stateIndex.getLeft()];
			return value == null ? null : unmask(value);
		}else {
			return null;
		}
	}
	
	public boolean containKey(String key) {
		if(size == 0) { return false; }
		int length = key.length();
		if(length == 0) {
			return check[ROOT_STATE] == ROOT_STATE_MARK && values[ROOT_STATE] != null;
		}else {
			Pair<Integer, Integer> stateIndex = findFirstUnmatchedState(key, 0);
			return stateIndex.getRight() == length && values[stateIndex.getLeft()] != null;
		}
	}
	
	public V remove(String key) {
		return null;
	}
	
	public void compact() {
		
	}
	
	@Override
	public String toString() {
		return "to be implement";
	}
	
	@Override
	public Iterator<ImmutableEntry<String, V>> iterator() {
		return new DoubleArrayTrieIterator();
	}

	private final class DoubleArrayTrieIterator implements Iterator<ImmutableEntry<String, V>> {
		@Override
		public boolean hasNext() {
			return false;
		}

		@Override
		public ImmutableEntry<String, V> next() {
			return null;
		}
	}
}
