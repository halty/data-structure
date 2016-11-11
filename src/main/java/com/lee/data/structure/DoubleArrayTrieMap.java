package com.lee.data.structure;

import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Random;

/**
 * don't permit <code>null</code> key, permits <code>null</code> value<br/>
 * see more detail of <a href="https://linux.thai.net/~thep/datrie/datrie.html">
 * An Implementation of Double-Array Trie</a>
 * @NotThreadSafe
 */
public class DoubleArrayTrieMap<V> implements Iterable<ImmutableEntry<String, V>> {

	/** max capacity of state nodes **/
	private static final int MAX_CAPACITY_OF_NODES = (1 << 30) + 1;
	/** initial capacity of state nodes **/
	private static final int INIT_CAPACITY_OF_NODES = 32 + 1;
	/** initial capacity of outgoing characters of each state node **/
	private static final int INIT_CAPACITY_OF_OUTGOING_CHARS = 2;
	/** state index for root node **/
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
	/** the outgoing character number of one state node **/
	private int[] childsCount;
	/** the outgoing characters of one state node **/
	private char[][] childs;
	/** the values attached with state nodes **/
	private Object[] values;
	
	/** recode history base while relocation **/
	private int[] historyBase = new int[MAX_RETRY_TIMES + 1];
	
	public DoubleArrayTrieMap() {
		init(INIT_CAPACITY_OF_NODES);
	}
	
	public DoubleArrayTrieMap(int numOfKeys, int averageKeyLength) {
		long numOfNodes = ((long)numOfKeys) * averageKeyLength;
		if(numOfNodes <= 0) {
			throw new IllegalArgumentException("numOfKeys or averageKeyLength need > 0");
		}
		int expectedNumOfNodes = numOfNodes >= MAX_CAPACITY_OF_NODES ?
				MAX_CAPACITY_OF_NODES : roundUpToPowerOf2((int)numOfNodes);
		expectedNumOfNodes += 1;	// add root state
		if(expectedNumOfNodes < INIT_CAPACITY_OF_NODES) {
			expectedNumOfNodes = INIT_CAPACITY_OF_NODES;
		}
		init(expectedNumOfNodes);
	}
	
	private static int roundUpToPowerOf2(int number) {
        return number >= MAX_CAPACITY_OF_NODES
                ? MAX_CAPACITY_OF_NODES
                : (number > 1) ? Integer.highestOneBit((number - 1) << 1) : 1;
    }
	
	private void init(int capacityOfNodes) {
		this.rand = new Random();
		this.size = 0;
		this.numOfNodes = 1;	// permanent root state
		this.base = new int[capacityOfNodes];
		this.check = new long[capacityOfNodes];
		this.childsCount = new int[capacityOfNodes];
		this.childs = new char[capacityOfNodes][];
		this.values = new Object[capacityOfNodes];
	}
	
	public int size() { return size; }
	
	public boolean isEmpty() { return size == 0; }
	
	public void clear() { init(INIT_CAPACITY_OF_NODES); }
	
	/**
	 * put {@code value} into this map mapping with string {@code key}.
	 * maybe put failed without enough capacity, you can choose
	 * {@link #expand()} operation manually and then retry put operation. 
	 * @param key
	 * @param value
	 * @return	true if put success, otherwise failed
	 */
	public boolean put(String key, V value) {
		int length = key.length();
		if(length == 0) {
			if(check[ROOT_STATE] != ROOT_STATE_MARK) {
				check[ROOT_STATE] = ROOT_STATE_MARK;
				size++;
			}
			values[ROOT_STATE] = value;
			return true;
		}
		
		long stateIndex = findFirstUnmatchedState(key, 0);
		return putForCreate(stateIndex, key, value);
	}
	
	/**
	 * return a long value which 
	 * high 32-bit reprsenting the first unmatched state,
	 * low 32-bit representing the unmatch character index of {@code key}.
	 */
	private long findFirstUnmatchedState(String key, int start) {
		int length = key.length();
		int parentState = ROOT_STATE;
		while(start < length) {
			if(!hasOutgoingTransition(parentState)) { break; }
			char ch = key.charAt(start);
			int targetState = targetState(parentState, ch);
			if(!isTransitionExisted(parentState, ch, targetState)) { break; }
			parentState = targetState;
			start += 1;
		}
		return stateIndex(parentState, start);
	}
	
	private boolean hasOutgoingTransition(int state) {
		return isValidBase(state) && hasChilds(state);
	}
	
	private boolean isValidBase(int state) { return base[state] != 0; }
	
	private boolean hasChilds(int state) { return childsCount[state] > 0; }
	
	private int targetState(int sourceState, char ch) {
		return calcTargetState(base[sourceState], ch);
	}
	
	private int calcTargetState(int baseIndex, char ch) {
		return calcTargetState(baseIndex, ch, base.length);
	}
	
	private int calcTargetState(int baseIndex, char ch, int baseLength) {
		// length must be a (2^n + 1)
		return ((baseIndex + ch) & (baseLength - 2)) + 1;
	}
	
	private boolean isTransitionExisted(int sourceState, char ch, int targetState) {
		long checkMark = check[targetState];
		return checkMark != 0 && checkMark == checkMark(sourceState, ch);
	}
	
	private static long checkMark(int sourceState, char ch) {
		long mark = Character.MAX_VALUE;
		mark = (mark << 16) + ch;
		mark = (mark << 32) | sourceState;
		return mark;
	}
	
	private static int sourceStateOf(long checkMark) { return (int) checkMark; }
	
	private static char charOf(long checkMark) { return (char)(checkMark >>> 32); }
	
	private static long stateIndex(int state, int index) {
		long stateIndex = state;
		stateIndex = (stateIndex << 32) | (0xffffffffL & index);
		return stateIndex;
	}
	
	private static int stateOf(long stateIndex) { return (int)(stateIndex >> 32); }
	
	private static int indexOf(long stateIndex) { return (int) stateIndex; };
	
	private static long incIndex(long stateIndex) { return ++stateIndex; }
	private static long decIndex(long stateIndex) {
		int index = indexOf(stateIndex);
		if(index > 0) {
			--stateIndex;
		}else {
			stateIndex |= 0xffffffffL;	// index = -1
		}
		return stateIndex;
	}
	
	private boolean putForCreate(long stateIndex, String key, V value) {
		int length = key.length();
		int index = indexOf(stateIndex);
		int state = stateOf(stateIndex);
		if(index == length) { return replaceIfExisted(state, value, length); }
		
		int vacancyStates = base.length - numOfNodes;
		int toBeAddStates = length - index;
		if(needExpand(vacancyStates, toBeAddStates)) {
			int oldLength = base.length;
			expand();
			int newLength = base.length;
			if(newLength != oldLength) {
				stateIndex = findFirstUnmatchedState(key, 0);
				index = indexOf(stateIndex);
				state = stateOf(stateIndex);
			}
		}
		
		int start = index;
		int parentState = state;
		while(start < length) {
			initBase(parentState);
			char ch = key.charAt(start);
			int targetState = targetState(parentState, ch);
			if(isValidCheckMark(check[targetState])) {
				int reserved = base.length - numOfNodes;
				targetState = relocate(parentState, ch, reserved);
				if(targetState < 0) { break; }
			}else {
				createTransition(parentState, ch, targetState);
			}
			parentState = targetState;
			start += 1;
		}
		if(start < length) {	// state node collision
			rollbackTransition(state, key, index, start);
			return false;
		}else {
			return replaceIfExisted(parentState, value, length);
		}
	}
	
	private boolean replaceIfExisted(int state, V newValue, int keyLength) {
		Object oldValue = values[state];
		if(oldValue == null) { size++; }
		values[state] = mask(newValue);
		return true;
	}
	
	private Object mask(V value) { return value == null ? NULL_VALUE : value; }
	
	private boolean needExpand(int vacancyStates, int toBeAddStates) {
		if(toBeAddStates >= vacancyStates) { return true; }
		int reserved = vacancyStates - toBeAddStates;
		return reserved <= (base.length / 4);
	}
	
	/** if put failed without enough capacity, you can expand capacity manually by this operation. **/
	public void expand() {
		int oldLength = base.length;
		if(oldLength >= MAX_CAPACITY_OF_NODES) { return; }
		expand((oldLength-1) * 2 + 1);
	}
	
	private void expand(int newCapacity) {
		int[] newBase = new int[newCapacity];
		long[] newCheck = new long[newCapacity];
		int[] newChildsCount = new int[newCapacity];
		char[][] newChilds = new char[newCapacity][];
		Object[] newValues = new Object[newCapacity];
		int length = check.length;
		for(int i=length-1; i>ROOT_STATE; i--) {
			long checkMark = check[i];
			if(isValidCheckMark(checkMark)) {	// valid state
				int sourceState = sourceStateOf(checkMark);
				char ch = charOf(checkMark);
				int newState = calcTargetState(base[sourceState], ch, newCapacity);
				newBase[newState] = base[i];
				newChildsCount[newState] = childsCount[i];
				newChilds[newState] = childs[i];
				newValues[newState] = values[i];
				if(hasOutgoingTransition(i)) {
					replaceCheckMark(base[i], childs[i], childsCount[i], newCapacity, newCheck, newState);
				}
			}
		}
		// move root state
		newBase[ROOT_STATE] = base[ROOT_STATE];
		newCheck[ROOT_STATE] = check[ROOT_STATE];
		newChildsCount[ROOT_STATE] = childsCount[ROOT_STATE];
		newChilds[ROOT_STATE] = childs[ROOT_STATE];
		newValues[ROOT_STATE] = values[ROOT_STATE];
		if(hasOutgoingTransition(ROOT_STATE)) {
			replaceCheckMark(base[ROOT_STATE], childs[ROOT_STATE], childsCount[ROOT_STATE], newCapacity, newCheck, ROOT_STATE);
		}
		
		base = newBase;
		check = newCheck;
		childs = newChilds;
		values = newValues;
	}
	
	private void replaceCheckMark(int baseIndex, char[] childChars, int childCount, int capacity,
			long[] newCheck, int newSourceState) {
		for(int i=0; i<childCount; i++) {
			char ch = childChars[i];
			int state = calcTargetState(baseIndex, ch, capacity);
			newCheck[state] = checkMark(newSourceState, ch);
		}
	}
	
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
	
	private static boolean isValidCheckMark(long checkMark) { return checkMark != 0; }
	
	private void createTransition(int sourceState, char ch, int targetState) {
		check[targetState] = checkMark(sourceState, ch);
		addChild(sourceState, ch);
		numOfNodes++;
	}
	
	private void addChild(int state, char ch) {
		char[] array = childs[state];
		int size = childsCount[state];
		if(array == null) {
			array = new char[INIT_CAPACITY_OF_OUTGOING_CHARS];
			childs[state] = array;
			array[0] = ch;
			childsCount[state] = 1;
		}else {
			if(size == array.length) {
				int newLength = array.length + INIT_CAPACITY_OF_OUTGOING_CHARS;
				array = Arrays.copyOf(array, newLength);
				childs[state] = array;
			}
			if(size == 0) {
				childsCount[state] = 1;
				array[0] = ch;
				return;
			}
			int foundIndex = find(ch, array, size-1);
			if(foundIndex < 0) {
				int position = (-foundIndex) - 1;
				System.arraycopy(array, position, array, position+1, size-position);
				array[position] = ch;
				childsCount[state] += 1;
			}else {
				// you never go to here
				throw new IllegalStateException("add outgoing character for state, exist duplicate in childs");
			}
		}
	}
	
	/**
	 * (binary search)
	 * return matched index if >= 0, or negative (inserting index + 1) if < 0;
	 */
	private int find(char ch, char[] array, int end) {
		int left = 0;
		int right = end;
		while(left <= right) {
			int middle = (left + right) / 2;
			char c = array[middle];
			if(ch == c) {
				return middle;
			}else if(ch < c) {
				right = middle - 1;
			}else {
				left = middle + 1;
			}
		}
		return -(left + 1);
	}
	
	/**
	 * relocate <code>parentState</code> all existed outgoing character for
	 * conflict character <code>conflictChar</code>, then create transition between
	 * <code>parentState</code> and <code>targetState</code> with
	 * <code>conflictChar</code>.
	 * @param parentState	need relocated state
	 * @param conflictChar	conflict character
	 * @param maxRetryTimes max retry times while confliction
	 * @return	target state, if relocate success, relocate targetState > 0, otherwise < 0
	 */
	private int relocate(int parentState, char conflictChar, int maxRetryTimes) {
		char[] childChars = childs[parentState];
		int childLength = childChars == null ? 0 : childChars[0];
		if(childLength == 0) {
			return relocateWithoutChilds(parentState, conflictChar, maxRetryTimes);
		}else {
			return relocateWithChilds(parentState, conflictChar, maxRetryTimes);
		}
	}
	
	private int relocateWithoutChilds(int parentState, char conflictChar, int maxRetryTimes) {
		for(int i=0; i<maxRetryTimes; i++) {
			int newBase = randomBase();
			int targetState = calcTargetState(newBase, conflictChar);
			if(!isValidCheckMark(check[targetState])) {
				createTransition(parentState, conflictChar, targetState);
				base[parentState] = newBase;
				return targetState;
			}
		}
		return -1;
	}
	
	private int relocateWithChilds(int parentState, char conflictChar, int maxRetryTimes) {
		char[] childChars = childs[parentState];
		int childCount = childsCount[parentState];
		int oldBase = base[parentState];
		fillHistoryBase(0, oldBase);
		if(maxRetryTimes > MAX_RETRY_TIMES) { maxRetryTimes = MAX_RETRY_TIMES; }
		for(int i=0; i<maxRetryTimes; i++) {
			int newBase = selectNonDuplicateBase(i);
			int targetState = calcTargetState(newBase, conflictChar);
			if(!isValidCheckMark(check[targetState])) {
				int j = 0;
				while(j<childCount) {
					char ch = childChars[j];
					int newState = calcTargetState(newBase, ch);
					if(isValidCheckMark(check[newState])) {
						break;
					}else {
						int oldState = calcTargetState(oldBase, ch);
						shift(oldState, newState);
						j++;
					}
				}
				if(j == childCount) {
					base[parentState] = newBase;
					if(!isValidCheckMark(check[targetState])) {	// double check for childs shift
						createTransition(parentState, conflictChar, targetState);
						return targetState;
					}
				}else {
					rollbackShift(newBase, childChars, j-1, oldBase);
				}
			}
			fillHistoryBase(i+1, newBase);
		}
		return -1;
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
	
	private void shift(int oldState, int newState) {
		copyStateData(oldState, newState);
		freeStateData(oldState);
	}
	
	private void copyStateData(int oldState, int newState) {
		int oldStateBase = base[oldState];
		base[newState] = oldStateBase;
		check[newState] = check[oldState];
		char[] childChars = childs[oldState];
		int childCount = childsCount[oldState];
		if(childCount > 0) {
			replaceCheckMark(oldStateBase, childChars, childCount, base.length, check, newState);
		}
		childsCount[newState] = childCount;
		childs[newState] = childs[oldState];
		values[newState] = values[oldState];
	}
	
	private void freeStateData(int oldState) {
		base[oldState] = 0;
		check[oldState] = 0L;
		childsCount[oldState] = 0;
		childs[oldState] = null;
		values[oldState] = null;
	}
	
	private void rollbackShift(int newBase, char[] childChars, int shiftEndIndex, int oldBase) {
		for(int j=0; j<=shiftEndIndex; j++) {
			char ch = childChars[j];
			int newState = calcTargetState(newBase, ch);
			int oldState = calcTargetState(oldBase, ch);
			shift(newState, oldState);
		}
	}
	
	private void rollbackTransition(int parentState, String key, int startIndex, int conflictIndex) {
		int baseIndex = base[parentState];
		for(int i=startIndex; i<conflictIndex; i++) {
			char ch = key.charAt(i);
			int targetState = calcTargetState(baseIndex, ch);
			baseIndex = base[targetState];
			freeStateData(targetState);
		}
		if(startIndex < conflictIndex) {
			numOfNodes -= (conflictIndex-startIndex);
			removeChild(parentState, key.charAt(startIndex));
		}
	}
	
	private boolean removeChild(int state, char ch) {
		char[] array = childs[state];
		if(array == null) { return false; }
		int size = childsCount[state];
		if(size == 0) {
			return false;
		}else if(size == 1) {
			if(array[0] == ch) {
				childsCount[state] = 0;
				return true;
			}else {
				return false;
			}
		}else {
			int foundIndex = find(ch, array, size-1);
			if(foundIndex < 0) {
				return false;
			}else {
				int position = foundIndex;
				System.arraycopy(array, position+1, array, position, size-position-1);
				childsCount[state] -= 1;
				return true;
			}
		}
	}
	
	public V get(String key) {
		if(size == 0) { return null; }
		int length = key.length();
		if(length == 0) {
			return check[ROOT_STATE] == ROOT_STATE_MARK ? unmask(values[ROOT_STATE]) : null;
		}
		
		long stateIndex = findFirstUnmatchedState(key, 0);
		if(indexOf(stateIndex) == length) {
			Object value = values[stateOf(stateIndex)];
			return value == null ? null : unmask(value);
		}else {
			return null;
		}
	}
	
	@SuppressWarnings("unchecked")
	private V unmask(Object value) { return value == NULL_VALUE ? null : (V)value; }
	
	public boolean containKey(String key) {
		if(size == 0) { return false; }
		int length = key.length();
		if(length == 0) {
			return check[ROOT_STATE] == ROOT_STATE_MARK && values[ROOT_STATE] != null;
		}else {
			long stateIndex = findFirstUnmatchedState(key, 0);
			return indexOf(stateIndex) == length && values[stateOf(stateIndex)] != null;
		}
	}
	
	public V remove(String key) {
		int length = key.length();
		if(length == 0) {
			V oldValue = null;
			if(check[ROOT_STATE] == ROOT_STATE_MARK) {
				oldValue = unmask(values[ROOT_STATE]);
				values[ROOT_STATE] = null;
				size--;
			}
			return oldValue;
		}
		
		long stateIndex = findFirstUnmatchedState(key, 0);
		return removeFrom(stateIndex, key);
	}
	
	private V removeFrom(long stateIndex, String key) {
		int length = key.length();
		int index = indexOf(stateIndex);
		int state = stateOf(stateIndex);
		return index != length ? null : removeAt(state);
	}
	
	private V removeAt(int state) {
		if(values[state] == null) { return null; }
		V oldValue = unmask(values[state]);
		values[state] = null;
		size--;
		freeFrom(state);
		return oldValue;
	}
	
	private long freeFrom(int state) {
		int count = 0;
		while(state != ROOT_STATE) {
			if(!hasChilds(state) && values[state] == null) {		// leaf node
				long checkMark = check[state];
				int sourceState = sourceStateOf(checkMark);
				char ch = charOf(checkMark);
				freeStateData(state);
				removeChild(sourceState, ch);
				numOfNodes--;
				state = sourceState;
				count++;
			}else {
				break;
			}
		}
		return stateIndex(state, count);
	}
	
	@Override
	public String toString() {
		if(size == 0) { return "{}"; }
		StringBuilder buf = new StringBuilder(16*size+2);	// 预估长度
		buf.append("{");
		StringBuilder keyStack = new StringBuilder();
		append(keyStack, ROOT_STATE, buf);
		if(buf.length() > 1) {
			buf.setLength(buf.length()-2);
			buf.append("}");
		}
		return buf.toString();
	}
	
	private void append(StringBuilder keyStack, int parentState, StringBuilder buf) {
		Object value = values[parentState];
		if(value != null) {
			buf.append(keyStack.toString())
			   .append(" = ")
			   .append(value == this ? "(this map)" : unmask(value))
			   .append(", ");
		}
		if(hasChilds(parentState)) {
			int childCount = childsCount[parentState];
			char[] childChars = childs[parentState];
			for(int i=0; i<childCount; i++) {
				char ch = childChars[i];
				keyStack.append(ch);
				int state = targetState(parentState, ch);
				append(keyStack, state, buf);
				keyStack.setLength(keyStack.length()-1);
			}
		}
	}
	
	@Override
	public Iterator<ImmutableEntry<String, V>> iterator() {
		return new DoubleArrayTrieIterator();
	}

	private final class DoubleArrayTrieIterator implements Iterator<ImmutableEntry<String, V>> {
		
		private Stack stateStack;
		private StringBuilder keyStack;
		private int nextState;
		private int currentState;
		private String currentKey;
		
		DoubleArrayTrieIterator() {
			stateStack = new Stack();
			keyStack = new StringBuilder();

			nextState = -1;
			if(size > 0) {
				if(check[ROOT_STATE] == ROOT_STATE_MARK) {
					nextState = ROOT_STATE;
				}else {
					stateStack.push(ROOT_STATE, 0);
					traverseToNextState(stateStack, keyStack);
					if(stateStack.isNotEmpty()) {
						nextState = stateStack.peekState();
					}
				}
			}
			currentState  = -1;
			currentKey = "";
		}
		
		@Override
		public boolean hasNext() { return nextState != -1; }

		@Override
		public ImmutableEntry<String, V> next() {
			if(!hasNext()) { throw new NoSuchElementException(); }
			String nextKey = keyStack.toString();
			ImmutableEntry<String, V> entry = new ImmutableEntry<String, V>(nextKey, unmask(values[nextState]));
			currentState = nextState;
			currentKey = nextKey;
			if(nextState == ROOT_STATE) {
				stateStack.push(ROOT_STATE, 0);
			}
			traverseToNextState(stateStack, keyStack);
			if(stateStack.isNotEmpty()) {
				nextState = stateStack.peekState();
			}else {
				nextState = -1;
			}
			return entry;
		}
		
		@Override
		public void remove() {
			if(currentState == -1) { throw new NoSuchElementException(); }
			values[currentState] = null;
			size--;
			if(currentState != ROOT_STATE) {
				int state = currentState;
				int depth = currentKey.length();
				long stateCount = freeFrom(state);
				state = stateOf(stateCount);
				if(currentState != state) {		// with free state
					int count = indexOf(stateCount);
					depth -= count;
					if(stateStack.top() > depth) {
						// currentState and nextState has common parent state
						assert state == stateStack.peekState(depth);
						stateStack.decChildIndex(depth);
					}
				}
			}
			currentState = -1;
			currentKey = "";
		}
	}
	
	private static final class Stack {
		private static final int SIZE = 16;

		private long[] elements;
		private int top;
		
		Stack() {
			this.elements = new long[SIZE];
			this.top = -1;
		}
		boolean isNotEmpty() { return top >= 0; }
		int top() { return top; }
		void push(int state, int childIndex) {
			long e = stateIndex(state, childIndex);
			if(top == elements.length - 1) {
				elements = Arrays.copyOf(elements, elements.length+SIZE);
			}
			elements[++top] = e;
		}
		void pop() { --top; }
		void decChildIndex() { decChildIndex(top); }
		void decChildIndex(int position) { elements[position] = decIndex(elements[position]); }
		void incChildIndex() { incChildIndex(top); }
		void incChildIndex(int position) { elements[position] = incIndex(elements[position]); }
		int peekState() { return peekState(top); }
		int peekState(int position) { return stateOf(elements[position]); }
		int peekChildIndex() { return peekChildIndex(top); }
		int peekChildIndex(int position) { return indexOf(elements[position]); }
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
			Stack stateStack = new Stack();
			StringBuilder keyStack = new StringBuilder();
			findTraversalPosition(key, stateStack, keyStack, true);
			if(keyStack.length() == key.length()) { // all key path matched
				stateStack.decChildIndex();
				keyStack.setLength(keyStack.length()-1);
			}
			
			traverseToPrevState(stateStack, keyStack);
			if(stateStack.isNotEmpty()) {
				int prevState = stateStack.peekState();
				String prevKey = keyStack.toString();
				return new ImmutableEntry<String, V>(prevKey, unmask(values[prevState]));
			}else {
				return null;
			}
		}
	}
	
	private void findTraversalPosition(String key, Stack stateStack, StringBuilder keyStack, boolean forPrevTraversal) {
		int parentState = ROOT_STATE;
		int length = key.length();
		int start = 0;
		while(start < length) {
			if(!hasOutgoingTransition(parentState)) {
				if(forPrevTraversal) {	// prev traversing
					stateStack.push(parentState, -1);
				}else {	// next traversing
					stateStack.push(parentState, childsCount[parentState]);
				}
				break;
			}
			char ch = key.charAt(start);
			int foundIndex = find(ch, childs[parentState], childsCount[parentState]-1);
			int targetState = targetState(parentState, ch);
			if(!isTransitionExisted(parentState, ch, targetState)) {
				if(foundIndex >= 0) {
					// you never go to here
					throw new IllegalStateException("while traversing trie, non existed transition find "
							+ "a matched character from parent state's childs");
				}
				int insertIndex = (-foundIndex) - 1;
				if(forPrevTraversal) {	// prev traversing
					stateStack.push(parentState, insertIndex - 1);
				}else {	// next traversing
					stateStack.push(parentState, insertIndex);
				}
				break;
			}
			if(foundIndex < 0) {
				// you never go to here
				throw new IllegalStateException("while traversing trie, existed transition can not find "
						+ "matched character from parent state's childs");
			}
			stateStack.push(parentState, foundIndex);
			keyStack.append(ch);
			parentState = targetState;
			start += 1;
		}
	}
	
	private void traverseToPrevState(Stack stateStack, StringBuilder keyStack) {
		boolean direction = true;	// true: (root -> leaf); false: (leaf -> root)
		while(stateStack.isNotEmpty()) {
			int state = stateStack.peekState();
			int childIndex = stateStack.peekChildIndex();
			if(direction) {
				if(childIndex < 0) {
					direction = false;
				}else {
					char ch = childs[state][childIndex];
					int targetState = targetState(state, ch);
					state = targetState;
					childIndex = childsCount[targetState] - 1;
					keyStack.append(ch);
					stateStack.push(state, childIndex);
				}
			}else {
				Object value = values[state];
				if(value != null) { break; }
				stateStack.pop();
				if(stateStack.isNotEmpty()) {
					keyStack.setLength(keyStack.length()-1);
					stateStack.decChildIndex();
					direction = true;
				}
			}
		}
	}
	
	/**
     * Returns a key-value mapping associated with the greatest key
     * less than or equal to the given key, or {@code null} if there
     * is no such key.
     */
	public ImmutableEntry<String, V> floorEntry(String key) {
		if(size == 0) {
			return null;
		}else if(key.length() == 0) {
			
			return check[ROOT_STATE] == ROOT_STATE_MARK ?
				new ImmutableEntry<String, V>(key, unmask(values[ROOT_STATE]))
			  : null;
		}else {
			Stack stateStack = new Stack();
			StringBuilder keyStack = new StringBuilder();
			findTraversalPosition(key, stateStack, keyStack, true);
			if(keyStack.length() == key.length()) { // all key path matched
				int parentState = stateStack.peekState();
				char ch = key.charAt(key.length()-1);
				int targetState = targetState(parentState, ch);
				Object value = values[targetState];
				if(value != null) {		// existed value matched
					return new ImmutableEntry<String, V>(key, unmask(value));
				}else {
					stateStack.decChildIndex();
					keyStack.setLength(keyStack.length()-1);
				}
			}
			
			traverseToPrevState(stateStack, keyStack);
			if(stateStack.isNotEmpty()) {
				int prevState = stateStack.peekState();
				String prevKey = keyStack.toString();
				return new ImmutableEntry<String, V>(prevKey, unmask(values[prevState]));
			}else {
				return null;
			}
		}
	}
	
	/**
     * Returns a key-value mapping associated with the least key
     * greater than or equal to the given key, or {@code null} if
     * there is no such key.
     */
	public ImmutableEntry<String, V> ceilingEntry(String key) {
		if(size == 0) { return null; }
		if(key.length() == 0 && check[ROOT_STATE] == ROOT_STATE_MARK) {
			return new ImmutableEntry<String, V>(key, unmask(values[ROOT_STATE]));
		}
		
		Stack stateStack = new Stack();
		StringBuilder keyStack = new StringBuilder();
		if(key.length() == 0) {
			stateStack.push(ROOT_STATE, 0);	// traverse from first child
		}else {
			findTraversalPosition(key, stateStack, keyStack, false);
			if(keyStack.length() == key.length()) { // all key path matched
				int parentState = stateStack.peekState();
				char ch = key.charAt(key.length()-1);
				int targetState = targetState(parentState, ch);
				Object value = values[targetState];
				if(value != null) {		// existed value matched
					return new ImmutableEntry<String, V>(key, unmask(value));
				}else {
					stateStack.push(targetState, 0);
				}
			}
		}
		
		traverseToNextState(stateStack, keyStack);
		if(stateStack.isNotEmpty()) {
			int nextState = stateStack.peekState();
			String nextKey = keyStack.toString();
			return new ImmutableEntry<String, V>(nextKey, unmask(values[nextState]));
		}else {
			return null;
		}
	}
	
	private void traverseToNextState(Stack stateStack, StringBuilder keyStack) {
		while(stateStack.isNotEmpty()) {
			int state = stateStack.peekState();
			int childIndex = stateStack.peekChildIndex();
			if(childIndex < childsCount[state]) {
				char ch = childs[state][childIndex];
				int targetState = targetState(state, ch);
				state = targetState;
				keyStack.append(ch);
				stateStack.push(state, 0);
				if(values[state] != null) { break; }
			}else {
				stateStack.pop();
				if(stateStack.isNotEmpty()) {
					keyStack.setLength(keyStack.length()-1);
					stateStack.incChildIndex();
				}
			}
		}
	}
	
	/**
     * Returns a key-value mapping associated with the least key
     * strictly greater than the given key, or {@code null} if there
     * is no such key.
     */
	public ImmutableEntry<String, V> higherEntry(String key) {
		if(size == 0) { return null; }

		Stack stateStack = new Stack();
		StringBuilder keyStack = new StringBuilder();
		if(key.length() == 0) {
			stateStack.push(ROOT_STATE, 0);	// traverse from first child
		}else {
			findTraversalPosition(key, stateStack, keyStack, false);
			if(keyStack.length() == key.length()) { // all key path matched
				int parentState = stateStack.peekState();
				char ch = key.charAt(key.length()-1);
				int targetState = targetState(parentState, ch);
				stateStack.push(targetState, 0);
			}
		}
		
		traverseToNextState(stateStack, keyStack);
		if(stateStack.isNotEmpty()) {
			int nextState = stateStack.peekState();
			String nextKey = keyStack.toString();
			return new ImmutableEntry<String, V>(nextKey, unmask(values[nextState]));
		}else {
			return null;
		}
	}
	
	/**
     * Returns a key-value mapping associated with the least
     * key in this map, or {@code null} if the map is empty.
     */
	public ImmutableEntry<String, V> firstEntry() {
		if(size == 0) { return null; }
		if(check[ROOT_STATE] == ROOT_STATE_MARK) {
			return new ImmutableEntry<String, V>("", unmask(values[ROOT_STATE]));
		}
		
		Stack stateStack = new Stack();
		StringBuilder keyStack = new StringBuilder();
		stateStack.push(ROOT_STATE, 0);	// traverse from first child
		traverseToNextState(stateStack, keyStack);
		if(stateStack.isNotEmpty()) {
			int nextState = stateStack.peekState();
			String nextKey = keyStack.toString();
			return new ImmutableEntry<String, V>(nextKey, unmask(values[nextState]));
		}else {
			return null;
		}
	}
	
	/**
     * Returns a key-value mapping associated with the greatest
     * key in this map, or {@code null} if the map is empty.
     */
	public ImmutableEntry<String, V> lastEntry() {
		if(size == 0) { return null; }
		Stack stateStack = new Stack();
		StringBuilder keyStack = new StringBuilder();
		stateStack.push(ROOT_STATE, childsCount[ROOT_STATE]-1);
		traverseToPrevState(stateStack, keyStack);
		if(stateStack.isNotEmpty()) {
			int prevState = stateStack.peekState();
			String prevKey = keyStack.toString();
			return new ImmutableEntry<String, V>(prevKey, unmask(values[prevState]));
		}else {
			return null;
		}
	}
}
