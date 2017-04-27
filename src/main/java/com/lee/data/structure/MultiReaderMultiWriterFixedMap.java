package com.lee.data.structure;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Map.Entry;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * /** A Simple Thread-safe, Multi-Reader-Multi-Writer, Random-Evict Map with fixed capacity,
 * neither <code>null</code> key nor <code>null</code> value permit.
 * @ThreadSafe
 */
public class MultiReaderMultiWriterFixedMap<K, V> {

	private static final float DEFAULT_LOAD_FACTOR = 0.75f;
	
	private final Map<K, V> backMap;
	private final K[] keys;
	/** guard by {@link #readWriteLock}.writeLock **/
	private int keyCount;
	private final ReadWriteLock readWriteLock;
	private final Random rand;
	
	/**
	 * Creates an empty <tt>MultiReaderMultiWriterFixedMap</tt> with the fixed capacity
	 * and default load factor(0.75).
	 * @param fixedCapacity		the fixed capacity
	 */
	public MultiReaderMultiWriterFixedMap(int fixedCapacity) {
		this(fixedCapacity, DEFAULT_LOAD_FACTOR);
	}
	
	/**
	 * Creates an empty <tt>MultiReaderMultiWriterFixedMap</tt> with the fixed capacity
	 * and load factor.
	 * @param fixedCapacity		the fixed capacity which must > 0
	 * @param loadFactor	the load factor which must > 0 and < 1
	 */
	public MultiReaderMultiWriterFixedMap(int fixedCapacity, float loadFactor) {
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
		this.readWriteLock = new ReentrantReadWriteLock();
		this.rand = new Random();
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
	 * Associates the specified value with the specified key in this map.
	 * If the map previously contained a mapping for the key, the old value 
	 * is replaced by the specified value.
	 * @return the previous value associated with <tt>key</tt>, or <tt>null</tt> 
     *          if there was no mapping for <tt>key</tt>.
	 */
	public V put(K key, V value) {
		nullCheck(key);
		nullCheck(value);
		Lock lock = readWriteLock.writeLock();
		lock.lock();
		try {
			return innerPut(key, value);
		}finally {
			lock.unlock();
		}
	}
	
	/** guard by {@link #readWriteLock}.writeLock **/
	private V innerPut(K key, V value) {
		V oldValue = backMap.put(key, value);
		int size = backMap.size();
		if(size > keyCount) {	// new key-value mapping
			if(keyCount == keys.length) {
				randomEvict(key);
			}else {
				keys[keyCount] = key;
				keyCount++;
			}
		}	// replace old value
		return oldValue;
	}
	
	private void randomEvict(K newKey) {
		int index = rand.nextInt(keyCount);
		K oldKey = keys[index];
		backMap.remove(oldKey);
		keys[index] = newKey;
	}
	
	/**
	 * bulk version for {@link #put(K, V)}. ignore the <code>null</code> key or <code>null</code> value entry.
	 */
	public void bulkPut(Map<K, V> map) {
		Lock lock = readWriteLock.writeLock();
		lock.lock();
		try {
			for(Entry<K, V> entry : map.entrySet()) {
				K key = entry.getKey();
				V value = entry.getValue();
				if(key != null && value != null) {
					innerPut(key, value);
				}
			}
		}finally {
			lock.unlock();
		}
	}
}
