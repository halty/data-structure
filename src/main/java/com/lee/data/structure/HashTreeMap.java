package com.lee.data.structure;

import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.NoSuchElementException;

/** 允许<code>null</code> key or <code>null</code> value **/
public class HashTreeMap<K, V> implements Iterable<ImmutableEntry<K, V>> {

	private static class HashTreeNode<K, V> {
		K key;
		V value;
		boolean occupied;	// 标识该节点是否已存储key-value
		HashTreeNode<K, V>[] childs;
		
		@SuppressWarnings("unchecked")
		HashTreeNode(int primeNumOfChild) {
			childs = new HashTreeNode[primeNumOfChild];
		}
	}
	
	private int size;
	private HashTreeNode<K, V> root;
	
	public HashTreeMap() { clear(); }
	
	private int nextPrime(int currentPrime) {
		int i = currentPrime + 1;
		while(true) {
			if(isPrime(i)) { return i; }
			i++;
		}
	}
	
	/** 无需很大的素数，所以普通的因子素数检测法已经足够 **/
	private static boolean isPrime(int number) {
		int max = (int) Math.sqrt(number);
		if(max < 2) { return true; }
		for(int i=2; i<=max; i++) {
			if(number % i == 0) { return false; }
		}
		return true;
	}
	
	public int size() { return size; }
	
	public boolean isEmpty() { return size == 0; }
	
	public void clear() {
		size = 0;
		root = null;
	}
	
	public V put(K key, V value) {
		HashTreeNode<K, V> p = root();
		HashTreeNode<K, V> node = null;
		if(key == null) {
			node = putForNullKey(p);
		}else {
			node = putFor(p, key);
		}
		return addTo(node, key, value);
	}
	
	private HashTreeNode<K, V> root() {
		if(root == null) {
			root = new HashTreeNode<K, V>(nextPrime(1));	// 根节点第1层
		}
		return root;
	}

	private HashTreeNode<K, V> putForNullKey(HashTreeNode<K, V> p) {
		if(!p.occupied) { return p; }
		if(p.key == null) { return p; }
		if(p.childs[0] == null) {
			return addChild(p, 0);
		}else {
			return putForNullKey(p.childs[0]);
		}
	}
	
	private HashTreeNode<K, V> addChild(HashTreeNode<K, V> p, int index){
		HashTreeNode<K, V> node = new HashTreeNode<K, V>(nextPrime(p.childs.length));
		p.childs[index] = node;
		return node;
	}

	private HashTreeNode<K, V> putFor(HashTreeNode<K, V> p, K key) {
		if(!p.occupied) { return p; }
		if(key.equals(p.key)) { return p; }
		int index = hash(key) % p.childs.length;
		if(p.childs[index] == null) {
			return addChild(p, index);
		}else {
			return putFor(p.childs[index], key);
		}
	}
	
	private V addTo(HashTreeNode<K, V> node, K key, V value) {
		V oldValue = node.value;
		if(!node.occupied) { size++; }
		node.key = key;
		node.value = value;
		node.occupied = true;
		return oldValue;
	}
		
	public V get(K key) {
		if(key == null) {
			return getForNullKey();
		}
		HashTreeNode<K, V> node = matchFor(root, key);
		return node == null ? null : node.value;
	}

	private V getForNullKey() {
		V value = null;
		HashTreeNode<K, V> node = matchForNullKey(root);
		if(node != null) { value = node.value; }
		return value;
	}
	
	private HashTreeNode<K, V> matchForNullKey(HashTreeNode<K, V> p) {
		if(p == null || !p.occupied) { return null; }
		if(p.key == null) { return p; }
		return matchForNullKey(p.childs[0]);
	}
	
	private HashTreeNode<K, V> matchFor(HashTreeNode<K, V> p, K key) {
		if(p == null || !p.occupied) { return null; }
		if(key.equals(p.key)) { return p; }
		int index = hash(key) % p.childs.length;
		return matchFor(p.childs[index], key);
	}
	
	/** 防止POOR的hash函数碰撞攻击，导致OOM **/
	private final int hashSeed = (int)System.currentTimeMillis();
	
	private int hash(K key) {
		int h = hashSeed ^ key.hashCode();

        // This function ensures that hashCodes that differ only by
        // constant multiples at each bit position have a bounded
        // number of collisions (approximately 8 at default load factor).
        h ^= (h >>> 20) ^ (h >>> 12);
        return h ^ (h >>> 7) ^ (h >>> 4);
	}
	
	public boolean containKey(K key) {
		if(key == null) { return matchForNullKey(root) != null; }
		return matchFor(root, key) != null;
	}

	public V remove(K key) {
		HashTreeNode<K, V> node = null;
		if(key == null) {
			node = matchForNullKey(root);
		}else {
			node = matchFor(root(), key);
		}
		return removeFrom(node, key);
	}
	
	private V removeFrom(HashTreeNode<K, V> node, K key) {
		V oldValue = null;
		if(node != null) {
			node.occupied = false;
			node.key = null;
			oldValue = node.value;
			node.value = null;
			size--;
		}
		return oldValue;
	}
	
	@Override
	public String toString() {
		StringBuilder buf = new StringBuilder(size * (5 + 1 + 8 + 1) + 2);	// 预估keyLen=5, '=', valueLen=8, ','
		if(size == 0) {
			return buf.append("{}").toString();
		}
		buf.append("{");
		append(root, buf);
		if(buf.length() > 1) {
			buf.setLength(buf.length()-2);
			buf.append("}");
		}
		return buf.toString();
	}
	
	private void append(HashTreeNode<K, V> node, StringBuilder buf) {
		if(node.occupied) {
			if(buf.length() > 0) { buf.append(", "); }
			buf.append(node.key == this ? "(this map)" : node.key)
			   .append("=")
			   .append(node.value == this ? "(this map)" : node.value);
		}
		for(HashTreeNode<K, V> child : node.childs) {
			if(child != null) { append(child, buf); }
		}
	}

	@Override
	public Iterator<ImmutableEntry<K, V>> iterator() {
		return new HashTreeIterator();
	}
	
	private final class HashTreeIterator implements Iterator<ImmutableEntry<K, V>> {
		
		private Deque<Pair<HashTreeNode<K, V>, Integer>> stack;
		private HashTreeNode<K, V> next;
		private HashTreeNode<K, V> current;
		
		HashTreeIterator() {
			stack = new LinkedList<Pair<HashTreeNode<K, V>, Integer>>();
			next = firstNode();
			current = null;
		}
		
		HashTreeNode<K, V> firstNode() {
			if(size == 0) { return null; }
			pushStack(root);
			return nextNode();
		}
		
		void pushStack(HashTreeNode<K,V> node) {
			stack.push(Pair.of(node, -1));
		}
		
		HashTreeNode<K, V> nextNode() {
			while(!stack.isEmpty()) {
				Pair<HashTreeNode<K,V>, Integer> pair = peekStack();
				HashTreeNode<K,V> node = pair.getLeft();
				int childIndex = pair.getRight();
				if(childIndex == -1) {
					if(node.occupied) {
						pair.setRight(childIndex + 1);
						return node;
					}
					childIndex += 1;
				}
				
				for(;childIndex < node.childs.length && node.childs[childIndex] == null; childIndex++);
				if(childIndex < node.childs.length) {
					pair.setRight(childIndex+1);
					pushStack(node.childs[childIndex]);
					continue;
				}
				// childIndex == node.childs.length
				popStack();
			}
			return null;
		}
		
		Pair<HashTreeNode<K,V>, Integer> peekStack() {
			return stack.peek();
		}
		
		Pair<HashTreeNode<K,V>, Integer> popStack() {
			return stack.pop();
		}
		
		@Override
		public boolean hasNext() {
			return next != null;
		}

		@Override
		public ImmutableEntry<K, V> next() {
			HashTreeNode<K, V> node = next;
			if(node == null) { throw new NoSuchElementException(); }
			next = nextNode();
			current = node;
			return new ImmutableEntry<K, V>(node.key, node.value);
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

}
