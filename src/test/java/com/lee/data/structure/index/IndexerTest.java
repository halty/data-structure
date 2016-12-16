package com.lee.data.structure.index;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.lee.data.structure.ImmutableEntry;
import com.lee.data.structure.index.impl.ConcurrentHashIndexer;
import com.lee.data.structure.index.impl.HashIndexer;
import com.lee.data.structure.index.impl.LRUHashIndexer;

public class IndexerTest {

	public static void main(String[] args) {
		// testHashIndexer();
		// testLRUHashIndexer();
		testConcurrentHashIndexer();
	}
	
	private static void testHashIndexer() {
		Indexer<Integer, String> indexer = new HashIndexer<Integer, String>();
		runTestCaseOn(indexer);
	}
	
	private static void testLRUHashIndexer() {
		Indexer<Integer, String> indexer = new LRUHashIndexer<Integer, String>(10);
		runTestCaseOn(indexer);
	}
	
	private static void testConcurrentHashIndexer() {
		Indexer<Integer, String> indexer = new ConcurrentHashIndexer<Integer, String>();
		runTestCaseOn(indexer);
	}
	
	private static void runTestCaseOn(Indexer<Integer, String> indexer) {
		indexer.put(IndexKey.of(1), "1");
		indexer.put(IndexKey.of(1, 2), "12");
		indexer.put(IndexKey.of(1, 2, 3), "123");
		indexer.put(IndexKey.of(1, 2, 3, 4), "1234");
		
		System.out.println(indexer.size());		// 4
		System.out.println(indexer.isEmpty());	// false
		System.out.println();
		
		System.out.println(indexer.containsKey(IndexKey.of(0)));	// false
		System.out.println(indexer.containsKey(IndexKey.of(1)));	// true
		System.out.println();
		
		System.out.println(indexer.get(IndexKey.of(1, 2)));		// "12"
		System.out.println(indexer.get(IndexKey.of(1, 3)));		// null
		System.out.println();
		
		System.out.println(indexer.putIfAbsent(IndexKey.of(1, 2, 3, 4, 5), "12345"));	// null
		System.out.println(indexer.putIfAbsent(IndexKey.of(1, 2), "一二"));		// "12"
		System.out.println();
		
		System.out.println(indexer.replaceIfPresent(IndexKey.of(1, 2, 3), "一二三"));	// "123"
		System.out.println(indexer.replaceIfPresent(IndexKey.of(1, 2, 4), "一二四"));	// null
		System.out.println();
		
		System.out.println(indexer.replaceIfMatched(IndexKey.of(0), "0", "零"));	// false
		System.out.println(indexer.replaceIfMatched(IndexKey.of(1), "one", "一"));	// false
		System.out.println(indexer.replaceIfMatched(IndexKey.of(1), "1", "first"));	// true
		System.out.println(indexer.size());	// 5
		System.out.println();
		
		Map<IndexKey<Integer>, String> map = new HashMap<IndexKey<Integer>, String>();
		map.put(IndexKey.of((Integer)null), "");
		map.put(IndexKey.of(2, 3), "23");
		indexer.putAll(map);
		System.out.println(indexer.size());	// 7
		System.out.println();
		
		Indexer<Integer, String> other = new HashIndexer<Integer, String>();
		other.put(IndexKey.of(2), "2");
		other.put(IndexKey.of(3), "3");
		indexer.putAll(other);
		System.out.println(indexer.size());	// 9
		System.out.println();
		
		map = new HashMap<IndexKey<Integer>, String>();
		map.put(IndexKey.of(9), "9");
		map.put(IndexKey.of(2, 3), "二三");
		System.out.println(indexer.putAllIfAbsent(map));	// 1
		System.out.println(indexer.size());	// 10
		System.out.println();
		
		other = new HashIndexer<Integer, String>();
		other.put(IndexKey.of(2), "二");
		other.put(IndexKey.of(3, 4), "34");
		System.out.println(indexer.putAllIfAbsent(other));	// 1
		System.out.println(indexer.size());	// 11
		System.out.println();
		
		map = new HashMap<IndexKey<Integer>, String>();
		map.put(IndexKey.of(5), "5");
		map.put(IndexKey.of(3, 4), "三四");
		System.out.println(indexer.replaceAllIfPresent(map));	// 1
		System.out.println(indexer.size());	// 11
		System.out.println();
		
		other = new HashIndexer<Integer, String>();
		other.put(IndexKey.of(2), "two");
		other.put(IndexKey.of(3, 4), "three, four");
		System.out.println(indexer.replaceAllIfPresent(other));	// 2
		System.out.println(indexer.size());	// 11
		System.out.println();
		
		System.out.println(indexer.remove(IndexKey.of(0)));		// null
		System.out.println(indexer.remove(IndexKey.of(1, 2)));	// "12"
		System.out.println(indexer.size());	// 10
		System.out.println();
		
		System.out.println(indexer.removeIfMatched(IndexKey.of(1, 2, 3, 4), "一二三四"));	// false
		System.out.println(indexer.removeIfMatched(IndexKey.of(1, 2, 3, 4), "1234"));	// true
		System.out.println(indexer.size());	// 9
		System.out.println();
		
		Iterator<ImmutableEntry<IndexKey<Integer>, String>> iter = indexer.entryIterator();
		/*
		 * <[1], "first">, <[1,2,3], "一二三">,
		 * <[1,2,3,4,5], "12345">, <[null], "">,
		 * <[2,3], "23">, <[2], "two">, <[3], "3">,
		 * <[9], "9">, <[3,4], "three, four">
		 */
		while(iter.hasNext()) { System.out.println(iter.next()); }
		System.out.println();
		
		indexer.clear();
		System.out.println(indexer.size());		// 0
		System.out.println(indexer.isEmpty());	// true
	}

}
