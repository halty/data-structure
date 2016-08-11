package com.lee.data.structure;

import java.util.Comparator;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Random;

/** （Key有序）不允许<code>null</code> key, 但允许<code>null</code> value **/
public class SkipListMap<K, V> implements Iterable<ImmutableEntry<K, V>> {

	private static class SkipListNode<K, V> {
		K key;
		V value;
		SkipListNode<K, V>[] nexts;
		
		@SuppressWarnings("unchecked")
		SkipListNode(K key, V value, int level) {
			this.key = key;
			this.value = value;
			this.nexts = new SkipListNode[level+1];
		}
	}
	
	private final Random RAND = new Random();
	private final int maxLevel;	// 层数从0开始计数
	private final Comparator<? super K> comparator;
	private SkipListNode<K, V> head;
	private int size;
	private int level;
	
	public SkipListMap(int maxLevel) {
		this(maxLevel, null);
	}
	
	public SkipListMap(int maxLevel, Comparator<? super K> comparator) {
		this.maxLevel = maxLevel;
		this.comparator = comparator;
		clear();
	}
	
	public int size() { return size; }
	
	public boolean isEmpty() { return size == 0; }
	
	public void clear() {
		size = 0;
		level = 0;
		head = new SkipListNode<K, V>(null, null, maxLevel);
	}
	
	public V put(K key, V value) {
		SkipListNode<K, V> prev = prevFor(key);
		SkipListNode<K, V> cur = prev.nexts[0];
		if(cur != null && compare(key, cur.key) == 0) {
			V oldValue = cur.value;
			cur.value = value;
			return oldValue;
		}
		
		int nodeLevel = randomLevel();
		SkipListNode<K, V> node = new SkipListNode<K, V>(key, value, nodeLevel);
		int minLevel = Math.min(nodeLevel, prev.nexts.length-1);
		for(int i=0; i<=minLevel; i++) {
			SkipListNode<K, V> tmp = prev.nexts[i];
			prev.nexts[i] = node;
			node.nexts[i] = tmp;
		}
		if(nodeLevel > minLevel) {
			for(int i=minLevel+1; i<=nodeLevel; i++) {
				prev = head;
				while(prev.nexts[i] != null && compare(key, prev.nexts[i].key) > 0) {
					prev = prev.nexts[i];
				}
				SkipListNode<K, V> tmp = prev.nexts[i];
				prev.nexts[i] = node;
				node.nexts[i] = tmp;
			}
		}
		size++;
		return null;
	}
	
	private SkipListNode<K, V> prevFor(K key) {
		SkipListNode<K, V> prev = head;
		for(int i=level; i >= 0; i--) {
			while(prev.nexts[i] != null && compare(key, prev.nexts[i].key) > 0) {
				prev = prev.nexts[i];
			}
		}
		return prev;
	}

	@SuppressWarnings("unchecked")
	private int compare(K key1, K key2) {
		return comparator == null ? ((Comparable<? super K>)key1).compareTo(key2)
					: comparator.compare(key1, key2);
	}

	private int randomLevel() {
		int curLevel = RAND.nextInt(maxLevel+1);
		if(curLevel > level) { level = curLevel; }
		return curLevel;
	}
	
	public V get(K key) {
		SkipListNode<K, V> node = matchFor(key);
		return node == null ? null : node.value;
	}
	
	private SkipListNode<K, V> matchFor(K key) {
		SkipListNode<K, V> prev = prevFor(key);
		SkipListNode<K, V> cur = prev.nexts[0];
		return (cur != null && compare(key, cur.key) == 0) ? cur : null;
	}
	
	public boolean containKey(K key) { return matchFor(key) != null; }
	
	public V remove(K key) {
		SkipListNode<K, V> prev = prevFor(key);
		SkipListNode<K, V> cur = prev.nexts[0];
		if(cur == null || compare(key, cur.key) != 0) {
			return null;
		}
		
		return removeNode(prev, cur);
	}
	
	private V removeNode(SkipListNode<K, V> prev, SkipListNode<K, V> current) {
		int minLevel = Math.min(prev.nexts.length, current.nexts.length);
		for(int i=0; i<minLevel; i++) {
			prev.nexts[i] = current.nexts[i];
		}
		if(current.nexts.length > prev.nexts.length) {
			for(int i=minLevel; i<current.nexts.length; i++) {
				prev = head;
				while(prev.nexts[i] != null && compare(current.key, prev.nexts[i].key) > 0) {
					prev = prev.nexts[i];
				}
				prev.nexts[i] = current.nexts[i];
			}
		}
		size--;
		return current.value;
	}
	
	@Override
	public String toString() {
		StringBuilder buf = new StringBuilder(size * (5 + 1 + 8 + 1) + 2);	// '{' 预估keyLen=5, '=', valueLen=8, ',' '}'
		if(size == 0) {
			return buf.append("{}").toString();
		}
		buf.append("{");
		append(head.nexts[0], buf);
		if(buf.length() > 1) {
			buf.setLength(buf.length()-2);
			buf.append("}");
		}
		return buf.toString();
	}
	
	private void append(SkipListNode<K, V> node, StringBuilder buf) {
		while(node != null) {
			if(buf.length() > 0) { buf.append(", "); }
			buf.append(node.key == this ? "(this map)" : node.key)
			   .append("=")
			   .append(node.value == this ? "(this map)" : node.value);
			node = node.nexts[0];
		}
	}

	@Override
	public Iterator<ImmutableEntry<K, V>> iterator() {
		return new SkipListIterator();
	}

	private final class SkipListIterator implements Iterator<ImmutableEntry<K, V>> {
		
		private SkipListNode<K, V> next;
		private SkipListNode<K, V> current;
		private SkipListNode<K, V> prev;
		
		SkipListIterator() {
			next = head.nexts[0];
			current = null;
			prev = null;
		}
		
		@Override
		public boolean hasNext() {
			return next != null;
		}

		@Override
		public ImmutableEntry<K, V> next() {
			SkipListNode<K, V> node = next;
			if(node == null) { throw new NoSuchElementException(); }
			next = next.nexts[0];
			if(prev == null) {
				prev = head;
			}else {
				prev = prev.nexts[0];
			}
			current = node;
			return new ImmutableEntry<K, V>(node.key, node.value);
		}
		
		@Override
		public void remove() {
			SkipListNode<K, V> node = current;
			if(node == null) { throw new IllegalStateException(); }
			removeNode(prev, node);
			current = null;
		}
	}
}
