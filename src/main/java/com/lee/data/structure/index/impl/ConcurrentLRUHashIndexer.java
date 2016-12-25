package com.lee.data.structure.index.impl;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
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
 * providing LRU strategy with capacity constraints which is thread safe.
 */
public class ConcurrentLRUHashIndexer<K, V> extends AbstractIndexer<K, V> {
	
	private static final float LOAD_FACTOR = 0.75f;
	private static final int CONCURRENCY_LEVEL = 16;
	
	private AtomicInteger size;
	
	private final ConcurrentHashMap<IndexKey<K>, ValueNode<K, V>> map;
	
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
		
		this.size = new AtomicInteger();
		this.map = new ConcurrentHashMap<IndexKey<K>, ValueNode<K, V>>(initCapacity, loadFactor, concurrencyLevel);
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
		ValueNode<K, V> node = map.get(key);
		if(node == null) { return null; }
		shiftNode(node);
		return node.value;
	}
	
	private void shiftNode(ValueNode<K, V> node) {
		boolean needFlush = readBuffer.write(node);
		FlushState state = flushState.get();
		if(state.needFlush(needFlush)) {
			tryFlushBuffers();
		}
	}
	
	private void tryFlushBuffers() {
		if(lruLock.tryLock()) {
			try {
				flushState.lazySet(FlushState.No_Flush);
				readBuffer.flush();
				writeBuffer.flush();
			}finally {
				flushState.compareAndSet(FlushState.No_Flush, FlushState.Condition_Flush);
				lruLock.unlock();
			}
		}
	}

	private void flushRead(ValueNode<K, V> node) {
		
	}

	@Override
	protected V internalPut(IndexKey<K> key, V value) {
		
	}
	
	@Override
	protected V internalPutIfAbsent(IndexKey<K> key, V value) {
		
	}
	
	@Override
	protected V internalReplaceIfPresent(IndexKey<K> key, V value) {
		
	}
	
	@Override
	protected boolean internalReplaceIfMatched(IndexKey<K> key, V oldValue, V newValue) {
		
	}

	@Override
	public V remove(IndexKey<K> key) {
		
	}

	@Override
	protected boolean internalRemoveIfMatched(IndexKey<K> key, V value) {
		
	}

	@Override
	public void clear() {
		
	}

	@Override
	public Iterator<IndexKey<K>> keyIterator() {
		
	}

	@Override
	public Iterator<ImmutableEntry<IndexKey<K>, V>> entryIterator() {
		
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
		
		final K key;
		final V value;
		final AtomicInteger state;
		ValueNode<K, V> prev;
		ValueNode<K, V> next;
		
		ValueNode(K key, V value, int state) {
			this.key = key;
			this.value = value;
			this.state = new AtomicInteger(state);
		}
		
		public boolean isAlive() { return state.get() < LINKED_REACH; }
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
	
	final class ReadBuffer {
		static final int MIN_FLUSH_BATCH_SIZE = 16;
		static final int MAX_FLUSH_BATCH_SIZE = MIN_FLUSH_BATCH_SIZE << 1;
		static final int BUFFER_CAPACITY = MAX_FLUSH_BATCH_SIZE << 1;
		static final int BUFFER_INDEX_MASK = BUFFER_CAPACITY - 1;
		
		final int bufferCount;
		final int bufferMask;
		final long[] readIndexes;
		final AtomicLong[] writeIndexes;
		final AtomicLong[] flushIndexes;
		final AtomicReference<ValueNode<K, V>>[][] buffers;
		
		ReadBuffer(int concurrencyLevel) {
			this.bufferCount = roundUpToPowerOf2(concurrencyLevel);
			this.bufferMask = bufferCount - 1;
			this.readIndexes = new long[bufferCount];
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
			@SuppressWarnings("unchecked")
			AtomicReference<ValueNode<K, V>>[][] buffers = new AtomicReference[bufferCount][BUFFER_CAPACITY];
			for(int i=0; i<bufferCount; i++) {
				AtomicReference<ValueNode<K, V>>[] buffer = buffers[i];
				for(int j=0; j<BUFFER_CAPACITY; j++) {
					buffer[j] = new AtomicReference<ValueNode<K,V>>();
				}
			}
			this.buffers = buffers;
		}
		
		/** buffered write the read node and return need flush read buffer or not **/
		boolean write(ValueNode<K, V> node) {
			int bufferIndex = bufferIndex();
			AtomicReference<ValueNode<K, V>>[] buffer = buffers[bufferIndex];
			long flushIndex = flushIndexes[bufferIndex].get();
			long writeIndex = writeIndexes[bufferIndex].getAndIncrement();
			long pending = writeIndex - flushIndex;
			for(;;) {
				if(pending < BUFFER_CAPACITY) {
					buffer[slotIndex(writeIndex)].lazySet(node);
					return pending >= MIN_FLUSH_BATCH_SIZE;
				}else {
					flushIndex = flushIndexes[bufferIndex].get();
				}
			}
		}
		
		void flush() {
			for(long start=Thread.currentThread().getId(), end=start+bufferCount; start<end; start++) {
				int bufferIndex = bufferIndex(start);
				AtomicReference<ValueNode<K, V>>[] buffer = buffers[bufferIndex];
				final long writeIndex = writeIndexes[bufferIndex].get();
				for(int i=0; i<MAX_FLUSH_BATCH_SIZE; readIndexes[bufferIndex]++, i++) {
					int index = slotIndex(readIndexes[bufferIndex]);
					ValueNode<K, V> node = buffer[index].get();
					if(node == null) { break; }
					buffer[index].lazySet(null);
					flushRead(node);
				}
				flushIndexes[bufferIndex].lazySet(writeIndex);
			}
		}
		
		int bufferIndex() { return bufferIndex(Thread.currentThread().getId()); }
		int bufferIndex(long threadId) { return (int) threadId & bufferMask; }
		int slotIndex(long index) { return (int) index & BUFFER_INDEX_MASK; }
	}
	
    private static int roundUpToPowerOf2(int number) {
    	final int MaxNumber = 1 << 30;
        // assert number >= 0 : "number must be non-negative";
        return number >= MaxNumber
                ? MaxNumber
                : (number > 1) ? Integer.highestOneBit((number - 1) << 1) : 1;
    }
	
	final class WriteBuffer {
		static final int FLUSH_BATCH_SIZE = 16;
		
		final int bufferCount;
		final ConcurrentLinkedQueue<WriteTask>[] buffers;
		
		WriteBuffer(int concurrencyLevel) {
			this.bufferCount = roundUpToPowerOf2(concurrencyLevel);
			@SuppressWarnings("unchecked")
			ConcurrentLinkedQueue<WriteTask>[] buffers = new ConcurrentLinkedQueue[bufferCount];
			for(int i=0; i<bufferCount; i++) {
				buffers[i] = new ConcurrentLinkedQueue<WriteTask>();
			}
			this.buffers = buffers;
		}
		
		void flush() {
			
		}
	}
	
	abstract class WriteTask {
		final ValueNode<K, V> node;
		WriteTask(ValueNode<K, V> node) {
			this.node = node;
		}
	}
	
	final class LRUQueue {
		ValueNode<K, V> head;
		ValueNode<K, V> tail;
		
		LRUQueue() {
			head = tail = null;
		}
	}
}
