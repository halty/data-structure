package com.lee.data.structure.map;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * /** A Simple Thread-safe, Multi-Reader-Single-Writer, Random-Evict Map with fixed capacity,
 * neither <code>null</code> key nor <code>null</code> value permit.
 * @ThreadSafe
 */
public class MultiReaderSingleWriterFixedMap<K, V> {
	
	private static final float DEFAULT_LOAD_FACTOR = 0.75f;
	
	private static final int DRAIN_INTERVAL_IN_MILLS = 1000 * 5;	// 5 seconds
	
	private static final int MIN_DRAIN_COUNT = 10;
	
	private static final AtomicInteger WRITER_NUMBER = new AtomicInteger(1);
	
	private final Map<K, V> backMap;
	private final K[] keys;
	/** guard by write lock **/
	private int keyCount;
	
	private final Queue<AppendTask> appendQueue;
	private final AtomicInteger appendCount;
	private final AtomicBoolean needDrain;
	private final ReadWriteLock readWriteLock;
	private final Random rand;
	
	private final AtomicBoolean destroyed;
	private final Thread writerThread;
	
	/**
	 * Creates an empty <tt>MultiReaderSingleWriterFixedMap</tt> with the fixed capacity
	 * and default load factor(0.75).
	 * @param fixedCapacity		the fixed capacity
	 */
	public MultiReaderSingleWriterFixedMap(int fixedCapacity) {
		this(fixedCapacity, DEFAULT_LOAD_FACTOR);
	}
	
	/**
	 * Creates an empty <tt>MultiReaderSingleWriterFixedMap</tt> with the fixed capacity
	 * and load factor.
	 * @param fixedCapacity		the fixed capacity which must > 0
	 * @param loadFactor	the load factor which must > 0 and < 1
	 */
	public MultiReaderSingleWriterFixedMap(int fixedCapacity, float loadFactor) {
		if(fixedCapacity <= 0) { throw new IllegalArgumentException("fixed capacity must > 0"); }
		if(loadFactor <= 0 || loadFactor >= 1) {
			throw new IllegalArgumentException("load factor must > 0 and < 1");
		}
		long initCapacity = fixedCapacity;
		initCapacity /= loadFactor;
		if(initCapacity > Integer.MAX_VALUE) { initCapacity = Integer.MAX_VALUE; }
		
		this.backMap = new HashMap<K, V>((int)initCapacity, loadFactor);
		this.keys = (K[]) new Object[fixedCapacity];
		this.keyCount = 0;
		this.appendQueue = new ConcurrentLinkedQueue<AppendTask>();
		this.appendCount = new AtomicInteger(0);
		this.needDrain = new AtomicBoolean(false);
		this.readWriteLock = new ReentrantReadWriteLock();
		this.rand = new Random();
		this.destroyed = new AtomicBoolean(false);
		this.writerThread = initWriterThread();
	}
	
	private Thread initWriterThread() {
		String threadName = getClass().getSimpleName() + "-writerThread-" + WRITER_NUMBER.getAndIncrement();
		Thread w = new Thread(new Runnable() {
			@Override
			public void run() {
				for(;;) {
					try {
						AppendTask task = appendQueue.poll();
						if(task == null) {	// queue is empty
							if(destroyed.get()) { break; }	// reponse close
							needDrain.set(false);
							waitForDrain();
						}else {
							appendCount.decrementAndGet();
							task.run();
						}
					}catch(Exception e) {
						if(destroyed.get()) {
							break;	// reponse close
						}	// ignore
					}
				}
			}
		}, threadName);
		w.setDaemon(true);
		w.start();
		return w;
	}
	
	private void waitForDrain() throws InterruptedException {
		synchronized(appendQueue) {
			appendQueue.wait(DRAIN_INTERVAL_IN_MILLS);
		}
	}
	
	/**
     * Returns the value to which the specified key is mapped,
     * or {@code null} if this map contains no mapping for the key.
     */
	public V get(K key) {
		nullCheck(key);
		Lock lock = readWriteLock.readLock();
		lock.lock();
		try {
			return backMap.get(key);
		}finally {
			lock.unlock();
		}
	}
	
	private static <T> void nullCheck(T t) {
        if(t == null) { throw new NullPointerException(); }
    }
	
	/** bulk version for {@link #get(K)}. ignore the <code>null</code> key with <code>keyList</code>. **/
	public Map<K, V> bulkGet(List<K> keys) {
		Map<K, V> result = new HashMap<K, V>(keys.size());
        Lock lock = readWriteLock.readLock();
        lock.lock();
        try {
            for(K key : keys) {
                if(key == null) { continue; }
                V value = backMap.get(key);
                if(value != null) { result.put(key, value); }
            }
            return result;
        }finally {
            lock.unlock();
        }
	}
	
	/**
     * async append the specified value with the specified key in this map.
     * delegate the association action to inner single-writer thread.
     * do nothing if {@link #close()}.
     * @throws IllegalStateException if the element cannot be appended at this
     *          time due to capacity restrictions
     */
	public void append(K key, V value) {
		nullCheck(key);
		nullCheck(value);
		if(destroyed.get()) { return; }
		int prevCount = appendCount.get();
		if(!appendQueue.offer(new AppendTask(key, value))) {
			throw new IllegalStateException();
		}
		signalDrainIfNeed(appendCount.incrementAndGet() - prevCount);
	}
	
	private void signalDrainIfNeed(int incrementCount) {
		if(incrementCount >= MIN_DRAIN_COUNT && !needDrain.get()) {
			if(needDrain.compareAndSet(false, true)) {
				synchronized(appendQueue) {
					appendQueue.notify();
				}
			}
		}
	}
	
	/**
	 * bulk version for {@link #append(K, V)}. ignore the <code>null</code> key or <code>null</code> value entry.
	 * do nothing if {@link #close()}.
	 * @throws IllegalStateException if one element cannot be added at this
     *          time due to capacity restrictions, and the succed element all append
	 */
	public void bulkAppend(Map<K, V> map) {
		if(destroyed.get()) { return; }
		int prevCount = appendCount.get();
		int delta = 0;
		for(Entry<K, V> entry : map.entrySet()) {
			K key = entry.getKey();
			V value = entry.getValue();
			if(key != null && value != null) {
				if(!appendQueue.offer(new AppendTask(key, value))) {
					throw new IllegalStateException();
				}
				delta++;
			}
		}
		signalDrainIfNeed(appendCount.addAndGet(delta) - prevCount);
	}
	
	/**
     * close the inner single-writer, then ignore all the append task
	 * @throws InterruptedException	if any thread has interrupted the current thread
     */
    public void destroy() throws InterruptedException {
        if(destroyed.compareAndSet(false, true)) {
        	writerThread.interrupt();
        }
    }
	
	private final class AppendTask implements Runnable {
		private final K key;
		private final V value;
		
		AppendTask(K key, V value) {
			this.key = key;
			this.value = value;
		}

		@Override
		public void run() {
			Lock lock = readWriteLock.writeLock();
			lock.lock();
			try {
				backMap.put(key, value);
				int size = backMap.size();
				if(size > keyCount) {	// new key-value mapping
					if(keyCount == keys.length) {
						randomEvict(key);
					}else {
						keys[keyCount] = key;
						keyCount++;
					}
				}	// replace old value
			}finally {
				lock.unlock();
			}
		}
		
		private void randomEvict(K newKey) {
			int index = rand.nextInt(keyCount);
			K oldKey = keys[index];
			backMap.remove(oldKey);
			keys[index] = newKey;
		}
	}
}
