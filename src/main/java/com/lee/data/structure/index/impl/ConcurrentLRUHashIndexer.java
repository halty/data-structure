package com.lee.data.structure.index.impl;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.lee.data.structure.ImmutableEntry;
import com.lee.data.structure.index.IndexKey;
import com.lee.data.structure.index.Indexer;

/**
 * {@link ConcurrentHashMap} based implementation of {@link Indexer} interface
 * providing non-strictly LRU strategy with capacity constraints which is thread safe.
 */
public class ConcurrentLRUHashIndexer<K, V> extends AbstractIndexer<K, V> {
	
	private static final float LOAD_FACTOR = 0.75f;
	private static final int CONCURRENCY_LEVEL = 16;
	
	private final int maxCapacity;
	private final AtomicInteger size;
	
	private final ConcurrentHashMap<IndexKey<K>, ValueNode<IndexKey<K>, V>> map;
	
	private final ReadBuffer readBuffer;
	private final WriteBuffer writeBuffer;
	private final AtomicReference<FlushState> flushState;
	
	private final Lock lruLock;
	private final LRUQueue lruQueue;
	
	/**
	 * Constructs an empty ConcurrentHashIndexer with specified max capacity (need >= 0),
	 * and default load factor (0.75), concurrencyLevel (16).
	 */
	public ConcurrentLRUHashIndexer(int maxCapacity) {
		this(maxCapacity, LOAD_FACTOR, CONCURRENCY_LEVEL);
	}
	
	/**
	 * Constructs an empty ConcurrentHashIndexer with specified max capacity (need >= 0),
	 * load factor (need > 0) and default concurrencyLevel (16).
	 */
	public ConcurrentLRUHashIndexer(int maxCapacity, float loadFactor) {
		this(maxCapacity, loadFactor, CONCURRENCY_LEVEL);
	}
	
	/**
	 * Constructs an empty ConcurrentHashIndexer with specified max capacity (need >= 0),
	 * concurrencyLevel (need > 0) and default load factor (0.75.
	 */
	public ConcurrentLRUHashIndexer(int maxCapacity, int concurrencyLevel) {
		this(maxCapacity, LOAD_FACTOR, concurrencyLevel);
	}
	
	/**
	 * Constructs an empty ConcurrentHashIndexer with specified max capacity (need >= 0),
	 * load factor (need > 0) and concurrencyLevel (need > 0).
	 */
	public ConcurrentLRUHashIndexer(int maxCapacity, float loadFactor, int concurrencyLevel) {
		if(maxCapacity <= 0) {
			throw new IllegalArgumentException("Illegal max capacity: " + maxCapacity);
		}
		if(loadFactor <= 0 || loadFactor > 1 || Float.isNaN(loadFactor)) {
            throw new IllegalArgumentException("Illegal load factor: " + loadFactor);
		}
		if(concurrencyLevel <= 0) {
			throw new IllegalArgumentException("Illegal concurrency level: " + concurrencyLevel);
		}
		int initCapacity = (int)(maxCapacity / loadFactor);
		if(initCapacity < Integer.MAX_VALUE) { initCapacity += 1; }
		
		this.maxCapacity = maxCapacity;
		this.size = new AtomicInteger();
		this.map = new ConcurrentHashMap<IndexKey<K>, ValueNode<IndexKey<K>, V>>(initCapacity, loadFactor, concurrencyLevel);
		this.readBuffer = new ReadBuffer(concurrencyLevel);
		this.writeBuffer = new WriteBuffer(concurrencyLevel);
		this.flushState = new AtomicReference<FlushState>(FlushState.Condition_Flush);
		this.lruLock = new ReentrantLock();
		this.lruQueue = new LRUQueue();
	}

	@Override
	protected Map<IndexKey<K>, V> backedMap() { throw new UnsupportedOperationException(); }

	@Override
	public int size() { return size.get(); }

	@Override
	public boolean isEmpty() { return size.get() == 0; }

	@Override
	public boolean containsKey(IndexKey<K> key) { return map.containsKey(key); }

	@Override
	public V get(IndexKey<K> key) {
		ValueNode<IndexKey<K>, V> node = map.get(key);
		if(node == null || !node.isAlive()) { return null; }
		shiftNode(node);
		return node.value;
	}
	
	private void shiftNode(ValueNode<IndexKey<K>, V> node) {
		boolean needFlush = readBuffer.append(node);
		FlushState state = flushState.get();
		if(state.needFlush(needFlush)) {
			tryFlushBuffers();
		}
	}
	
	private void tryFlushBuffers() {
		if(lruLock.tryLock()) {
			try {
				flushState.lazySet(FlushState.No_Flush);
				writeBuffer.flush();
				readBuffer.flush();
			}finally {
				flushState.compareAndSet(FlushState.No_Flush, FlushState.Condition_Flush);
				lruLock.unlock();
			}
		}
	}

	/** guard by {@link ConcurrentLRUHashIndexer#lruLock} **/
	private void flushRead(ValueNode<IndexKey<K>, V> node) {
		int state = node.getState();
		// expect HASH_LINKED_REACH or HASH_REACH
		if(state == ValueNode.HASH_REACH) {	// help append
			if(node.compareAndSetState(state, ValueNode.HASH_LINKED_REACH)) {	
				lruQueue.offer(node);
			}	// maybe removing concurrently 
		}else if(state == ValueNode.HASH_LINKED_REACH) {
			if(lruQueue.contains(node)) {
				lruQueue.moveToTail(node);
			}
		}
	}

	@Override
	protected V internalPut(IndexKey<K> key, V value) {
		ValueNode<IndexKey<K>, V> newNode = new ValueNode<IndexKey<K>, V>(key, value, ValueNode.HASH_REACH);
		ValueNode<IndexKey<K>, V> oldNode = map.put(key, newNode);
		if(oldNode != null) {
			removeNode(oldNode);
		}else {
			int currentSize = size.incrementAndGet();
			if(currentSize > maxCapacity) {
				evict();
			}
		}
		appendNode(newNode);
		
		return oldNode == null ? null : oldNode.value;
	}

	private void removeNode(ValueNode<IndexKey<K>, V> node) {
		for(;;) {
			int state = node.getState();
			if(state == ValueNode.OUT_OF_REACH) {	// concurrently evict
				return;
			}else {
				if(node.compareAndSetState(state, ValueNode.LINKED_REACH)) {
					boolean needFlush = writeBuffer.append(new RemoveTask(node));
					if(flushState.get().needFlush(needFlush)) {
						tryFlushBuffers();
					}
					return;
				}
			}
		}
	}
	
	private void evict() {
		lruLock.lock();
		try {
			while(size.get() > maxCapacity) {
				ValueNode<IndexKey<K>, V> node = lruQueue.poll();
				/* maybe exceed the max capacity without flushing write buffer */
				if(node == null) { return; }
				node.lazySetState(ValueNode.OUT_OF_REACH);
				if(map.remove(node.key, node)) {
					size.decrementAndGet();
				}
			}
		}finally {
			lruLock.unlock();
		}
	}

	private void appendNode(ValueNode<IndexKey<K>, V> newNode) {
		boolean needFlush = writeBuffer.append(new AppendTask(newNode));
		FlushState state = FlushState.Forced_Flush;
		flushState.lazySet(state);
		if(state.needFlush(needFlush)) {
			tryFlushBuffers();
		}
	}
	
	@Override
	protected V internalPutIfAbsent(IndexKey<K> key, V value) {
		ValueNode<IndexKey<K>, V> newNode = new ValueNode<IndexKey<K>, V>(key, value, ValueNode.HASH_REACH);
		ValueNode<IndexKey<K>, V> oldNode = map.putIfAbsent(key, newNode);
		if(oldNode != null) {
			if(oldNode.isAlive()) { shiftNode(oldNode); }
		}else {
			int currentSize = size.incrementAndGet();
			if(currentSize > maxCapacity) {
				evict();
			}
			appendNode(newNode);
		}
		
		return oldNode == null ? null : oldNode.value;
	}
	
	@Override
	protected V internalReplaceIfPresent(IndexKey<K> key, V value) {
		ValueNode<IndexKey<K>, V> newNode = new ValueNode<IndexKey<K>, V>(key, value, ValueNode.HASH_REACH);
		ValueNode<IndexKey<K>, V> oldNode = map.replace(key, newNode);
		if(oldNode != null) {
			removeNode(oldNode);
			appendNode(newNode);
		}
		return oldNode == null ? null : oldNode.value;
	}
	
	@Override
	protected boolean internalReplaceIfMatched(IndexKey<K> key, V oldValue, V newValue) {
		ValueNode<IndexKey<K>, V> oldNode = map.get(key);
		if(oldNode == null || !oldNode.isAlive()) { return false; }
		if(oldValue.equals(oldNode.value)) {
			ValueNode<IndexKey<K>, V> newNode = new ValueNode<IndexKey<K>, V>(key, newValue, ValueNode.HASH_REACH);
			if(map.replace(key, oldNode, newNode)) {
				removeNode(oldNode);
				appendNode(newNode);
				return true;
			}
		}else {
			shiftNode(oldNode);
		}
		return false;
	}

	@Override
	public V remove(IndexKey<K> key) {
		ValueNode<IndexKey<K>, V> oldNode = map.remove(key);
		if(oldNode != null) {
			size.decrementAndGet();
			removeNode(oldNode);
			return oldNode.value;
		}else {
			return null;
		}
	}

	@Override
	protected boolean internalRemoveIfMatched(IndexKey<K> key, V value) {
		ValueNode<IndexKey<K>, V> oldNode = map.get(key);
		if(oldNode == null || !oldNode.isAlive()) { return false; }
		if(value.equals(oldNode.value)) {
			return remove(key, oldNode); 
		}else {
			shiftNode(oldNode);
			return false;
		}
	}
	
	private boolean remove(IndexKey<K> key, ValueNode<IndexKey<K>, V> oldNode) {
		if(map.remove(key, oldNode)) {
			size.decrementAndGet();
			removeNode(oldNode);
			return true;
		}else {
			return false;
		}
	}

	@Override
	public void clear() {
		lruLock.lock();
		try {
			readBuffer.clearAll();
			writeBuffer.flushAll();
			for(ValueNode<IndexKey<K>, V> node = lruQueue.poll(); node != null; node = lruQueue.poll()) {
				node.lazySetState(ValueNode.OUT_OF_REACH);
				if(map.remove(node.key, node)) { size.decrementAndGet(); }
			}
		}finally {
			lruLock.unlock();
		}
	}

	@Override
	public Iterator<IndexKey<K>> keyIterator() { return new KeyIterator(); }

	@Override
	public Iterator<ImmutableEntry<IndexKey<K>, V>> entryIterator() { return new EntryIterator(); }
	
	final class KeyIterator implements Iterator<IndexKey<K>> {
		private final Iterator<IndexKey<K>> iter = map.keySet().iterator();
		private IndexKey<K> current;
		
		@Override
		public boolean hasNext() { return iter.hasNext(); }

		@Override
		public IndexKey<K> next() { return current = iter.next(); }

		@Override
		public void remove() {
			if(current == null) { throw new IllegalStateException(); }
			ConcurrentLRUHashIndexer.this.remove(current);
			current = null;
		}
	}
	
	final class EntryIterator implements Iterator<ImmutableEntry<IndexKey<K>, V>> {
		private final Iterator<Entry<IndexKey<K>, ValueNode<IndexKey<K>, V>>> iter = map.entrySet().iterator();
		private Entry<IndexKey<K>, ValueNode<IndexKey<K>, V>> current;
		
		@Override
		public boolean hasNext() { return iter.hasNext(); }

		@Override
		public ImmutableEntry<IndexKey<K>, V> next() {
			current = iter.next();
			return new ImmutableEntry<IndexKey<K>, V>(current.getKey(),
					ConcurrentLRUHashIndexer.this.unmask(current.getValue().value));
		}

		@Override
		public void remove() {
			if(current == null) { throw new IllegalStateException(); }
			ConcurrentLRUHashIndexer.this.remove(current.getKey(), current.getValue());
			current = null;
		}
	}
	
	static final class ValueNode<K, V> {
		
		/** this node can be reached only by hash index **/
		static final int HASH_REACH = 1;
		/** this node can be reached by hash and linked index **/
		static final int HASH_LINKED_REACH = 2;
		/** this node can be reached only by linked index **/
		static final int LINKED_REACH = 3;
		/** this node can not be reached **/
		static final int OUT_OF_REACH = 4;
		
		/*
		 * node state transition:
		 *                       -----------------                  ----------
		 *                       |(get and flush)|                  | (get)  |
		 *                       v               |                  v        |
		 *            ---------- HASH_LINKED_REACH <----(flush)---- HASH_REACH <---(put)---
		 *            |                       |                         /
		 *     (clear or evict)      (remove or replace)       (remove or replace)
		 *            |                       |                     /
		 *            v                       v                   /
		 *    OUT_OF_REACH <-- (flush)-- LINKED_REACH <-----------
		 */
		
		final K key;
		final V value;
		final AtomicInteger state;
		/** guard by {@link ConcurrentLRUHashIndexer#lruLock} **/
		ValueNode<K, V> prev;
		/** guard by {@link ConcurrentLRUHashIndexer#lruLock} **/
		ValueNode<K, V> next;
		
		ValueNode(K key, V value, int state) {
			this.key = key;
			this.value = value;
			this.state = new AtomicInteger(state);
		}
		
		boolean isAlive() {
			int s = state.get();
			return s == HASH_REACH || s == HASH_LINKED_REACH;
		}
		
		int getState() { return state.get(); }
		
		boolean compareAndSetState(int expect, int update) {
			return state.compareAndSet(expect, update);
		}
		
		void lazySetState(int newState) { state.lazySet(newState); }
	}
	
	static enum FlushState {
		No_Flush {
			@Override
			public boolean needFlush(boolean condition) { return false; }
		},
		
		Condition_Flush {
			@Override
			public boolean needFlush(boolean condition) { return condition; }
		},
		
		Forced_Flush {
			@Override
			public boolean needFlush(boolean condition) { return true; }
		}
		;
		
		public abstract boolean needFlush(boolean condition);
	}
	
	static abstract class Buffer<T> {
		final int bufferCount;
		final int bufferMask;
		/** the next write index of buffer **/
		final AtomicLong[] writeIndexes;
		/** the next flush index of buffer **/
		final AtomicLong[] flushIndexes;
		
		final int minFlushBatchSize;
		final int maxFlushBatchSize;
		final int bufferCapacity;
		final int bufferIndexMask;
		final AtomicReference<T>[][] buffers;
		
		Buffer(int concurrencyLevel, int minFlushBatchSize) {
			this.bufferCount = roundUpToPowerOf2(concurrencyLevel);
			this.bufferMask = bufferCount - 1;
			AtomicLong[] writeIndexes = new AtomicLong[bufferCount];
			for(int i=0; i<bufferCount; i++) {
				writeIndexes[i] = new AtomicLong();
			}
			this.writeIndexes = writeIndexes;
			AtomicLong[] flushIndexes = new AtomicLong[bufferCount];
			for(int i=0; i<bufferCount; i++) {
				flushIndexes[i] = new AtomicLong();
			}
			this.flushIndexes = flushIndexes;
			this.minFlushBatchSize = roundUpToPowerOf2(minFlushBatchSize);
			this.maxFlushBatchSize = minFlushBatchSize << 1;
			this.bufferCapacity = maxFlushBatchSize << 1;
			this.bufferIndexMask = bufferCapacity - 1;
			@SuppressWarnings("unchecked")
			AtomicReference<T>[][] buffers = new AtomicReference[bufferCount][bufferCapacity];
			for(int i=0; i<bufferCount; i++) {
				@SuppressWarnings("unchecked")
				AtomicReference<T>[] buffer = new AtomicReference[bufferCapacity];
				for(int j=0; j<bufferCapacity; j++) {
					buffer[j] = new AtomicReference<T>();
				}
				buffers[i] = buffer;
			}
			this.buffers = buffers;
		}
		
	    private static int roundUpToPowerOf2(int number) {
	    	final int MaxNumber = 1 << 30;
	        // assert number >= 0 : "number must be non-negative";
	        return number >= MaxNumber
	                ? MaxNumber
	                : (number > 1) ? Integer.highestOneBit((number - 1) << 1) : 1;
	    }
		
		/** buffered append the element and return need flush buffer or not **/
		boolean append(T node) {
			int bufferIndex = bufferIndex();
			AtomicReference<T>[] buffer = buffers[bufferIndex];
			AtomicLong flushIndex = flushIndexes[bufferIndex];
			long writeIndex = writeIndexes[bufferIndex].getAndIncrement();
			for(;;) {
				long pending = writeIndex - flushIndex.get();
				if(pending < bufferCapacity) {
					doAppend(buffer[slotIndex(writeIndex)], node);
					return pending >= minFlushBatchSize;
				}
			}
		}
		
		abstract void doAppend(AtomicReference<T> slot, T node);
		
		/** guard by {@link ConcurrentLRUHashIndexer#lruLock} **/
		void flush() {
			for(long start=Thread.currentThread().getId(), end=start+bufferCount; start<end; start++) {
				int bufferIndex = bufferIndex(start);
				AtomicReference<T>[] buffer = buffers[bufferIndex];
				long flushIndex = flushIndexes[bufferIndex].get();
				long writeIndex = writeIndexes[bufferIndex].get();
				long pending = Math.min(writeIndex - flushIndex, maxFlushBatchSize);
				for(int i=0; i<pending; i++) {
					int index = slotIndex(flushIndex);
					T element = buffer[index].get();
					if(element == null) { break; }		// due to eventually sets, the new element maybe isn't visible at the moment
					buffer[index].lazySet(null);
					doFlush(element);
					flushIndex++;
				}
				flushIndexes[bufferIndex].lazySet(flushIndex);
			}
		}
		
		/** guard by {@link ConcurrentLRUHashIndexer#lruLock} **/
		abstract void doFlush(T element);
		
		/** guard by {@link ConcurrentLRUHashIndexer#lruLock} **/
		void flushAll() {
			for(int i=0; i<bufferCount; i++) {
				AtomicReference<T>[] buffer = buffers[i];
				long flushIndex = flushIndexes[i].get();
				long writeIndex = writeIndexes[i].get();
				long pending = Math.min(writeIndex - flushIndex, bufferCapacity);
				for(int j=0; j<pending; j++) {
					int index = slotIndex(flushIndex);
					T element = buffer[index].get();
					if(element != null) { doFlush(element); }	// maybe miss the element due to eventually sets
					buffer[index].lazySet(null);
					flushIndex++;
				}
				flushIndexes[i].lazySet(flushIndex);
			}
		}
		
		/** guard by {@link ConcurrentLRUHashIndexer#lruLock} **/
		void clearAll() {
			for(int i=0; i<bufferCount; i++) {
				AtomicReference<T>[] buffer = buffers[i];
				long flushIndex = flushIndexes[i].get();
				long writeIndex = writeIndexes[i].get();
				long pending = Math.min(writeIndex - flushIndex, bufferCapacity);
				for(int j=0; j<pending; j++) {
					int index = slotIndex(flushIndex);
					buffer[index].lazySet(null);
					flushIndex++;
				}
				flushIndexes[i].lazySet(flushIndex);
			}
		}
		
		int bufferIndex() { return bufferIndex(Thread.currentThread().getId()); }
		int bufferIndex(long threadId) { return (int) threadId & bufferMask; }
		int slotIndex(long index) { return (int) index & bufferIndexMask; }
	}
	
	final class ReadBuffer extends Buffer<ValueNode<IndexKey<K>, V>> {
		static final int MIN_FLUSH_READ_SIZE = 32;
		
		ReadBuffer(int concurrencyLevel) { super(concurrencyLevel, MIN_FLUSH_READ_SIZE); }
		
		@Override
		void doAppend(AtomicReference<ValueNode<IndexKey<K>, V>> slot, ValueNode<IndexKey<K>, V> node) {
			slot.lazySet(node);
		}

		@Override
		void doFlush(ValueNode<IndexKey<K>, V> node) { flushRead(node); }
	}
	
	final class WriteBuffer extends Buffer<WriteTask> {
		static final int MIN_FLUSH_WRITE_SIZE = 16;
		
		WriteBuffer(int concurrencyLevel) { super(concurrencyLevel, MIN_FLUSH_WRITE_SIZE); }
		
		@Override
		void doAppend(AtomicReference<WriteTask> slot, WriteTask task) {
			slot.set(task);
		}

		@Override
		void doFlush(WriteTask task) { task.execute(); }
	}
	
	abstract class WriteTask {
		final ValueNode<IndexKey<K>, V> node;
		
		WriteTask(ValueNode<IndexKey<K>, V> node) { this.node = node; }
		
		/** guard by {@link ConcurrentLRUHashIndexer#lruLock} **/
		abstract void execute();
	}
	
	final class RemoveTask extends WriteTask {

		RemoveTask(ValueNode<IndexKey<K>, V> node) { super(node); }
		
		@Override
		void execute() {
			int state = node.getState();
			if(state == ValueNode.OUT_OF_REACH) { return; }
			node.lazySetState(ValueNode.OUT_OF_REACH);
			lruQueue.remove(node);
		}
	}
	
	final class AppendTask extends WriteTask {

		AppendTask(ValueNode<IndexKey<K>, V> node) { super(node); }
		
		@Override
		void execute() {
			int state = node.getState();
			if(state == ValueNode.HASH_REACH) {
				if(node.compareAndSetState(state, ValueNode.HASH_LINKED_REACH)) {
					lruQueue.offer(node);
				}
			}
		}
	}
	
	/** guard by {@link ConcurrentLRUHashIndexer#lruLock} **/
	final class LRUQueue {
		ValueNode<IndexKey<K>, V> head;
		ValueNode<IndexKey<K>, V> tail;
		
		LRUQueue() { head = tail = null; }
		
		boolean contains(ValueNode<IndexKey<K>, V> node) {
			return node.next != null || node.prev != null || node == head;
		}
		
		boolean offer(ValueNode<IndexKey<K>, V> node) {
			if(contains(node)) { return false; }
			linkToTail(node);
			return true;
		}
		
		private void linkToTail(ValueNode<IndexKey<K>, V> node) {
			ValueNode<IndexKey<K>, V> t = tail;
			tail = node;
			if(t == null) {		// first node
				head = node;
			}else {
				t.next = node;
				node.prev = t;
			}
		}
		
		void moveToTail(ValueNode<IndexKey<K>, V> node) {
			if(node != tail) {
				unlink(node);
				linkToTail(node);
			}
		}
		
		private void unlink(ValueNode<IndexKey<K>, V> node) {
			ValueNode<IndexKey<K>, V> prev = node.prev;
			ValueNode<IndexKey<K>, V> next = node.next;
			if(prev != null) {
				prev.next = next;
				node.prev = null;
			}else {
				head = next;
			}
			if(next != null) {
				next.prev = prev;
				node.next = null;
			}else {
				tail = prev;
			}
		}
		
		ValueNode<IndexKey<K>, V> poll() {
			if(head == null) {
				return null;
			}else {
				ValueNode<IndexKey<K>, V> node = head;
				ValueNode<IndexKey<K>, V> next = node.next;
				if(next != null) {
					next.prev = null;
				}else {
					tail = null;
				}
				head = next;
				node.next = null;
				return node;
			}
		}
		
		boolean remove(ValueNode<IndexKey<K>, V> node) {
			if(contains(node)) {
				unlink(node);
				return true;
			}else {
				return false;
			}
		}
	}
}
