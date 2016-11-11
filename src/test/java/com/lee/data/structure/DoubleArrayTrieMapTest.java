package com.lee.data.structure;

import java.util.Iterator;

public class DoubleArrayTrieMapTest {

	public static void main(String[] args) {
		normalTest();
		compactTest();
		iteratorTest();
		sortTest();
	}

	private static void normalTest() {
		DoubleArrayTrieMap<Integer> map = new DoubleArrayTrieMap<Integer>(6, 5);
		map.put("one", 1);
		map.put("two", 2);
		map.put("three", 3);
		map.put("four", 4);
		map.put("five", 5);
		map.put("", 0);
		
		System.out.println(map.size());
		System.out.println(map);
		System.out.println();
		
		System.out.println(map.get("one"));
		System.out.println(map.put("one", 10));
		System.out.println(map.get("one"));
		System.out.println();
		
		System.out.println(map.get(""));
		System.out.println(map.put("", -2));
		System.out.println(map.get(""));
		System.out.println();
		
		System.out.println(map.get("null"));
		System.out.println();
		
		System.out.println(map.remove("two"));
		System.out.println(map.containKey("two"));
		System.out.println();
		
		System.out.println(map.containKey(""));
		System.out.println(map.containKey("five"));
		System.out.println();
		
		System.out.println(map.size());
		System.out.println();
		
		System.out.println(map);
	}
	
	private static void compactTest() {
		DoubleArrayTrieMap<String> map = new DoubleArrayTrieMap<String>(16, 8);
		map.put("aaaaaaaaaa", "10a");
		map.put("aaaaaaaaab", "9a1b");
		map.put("aaaaaaaaa", "9a");
		map.put("aaaaaaaab", "8a1b");
		map.put("aaaaaaaabb", "8a2b");
		map.put("aaaaaaa", "7a");
		map.put("aaaaaaab", "7a1b");
		map.put("aaaaaa", "6a");
		map.put("aaaaa", "5a");
		map.put("aaaaab", "5a1b");
		map.put("aaaa", "4a");
		map.put("aaa", "3a");
		map.put("aa", "2a");
		map.put("ab", "1a1b");
		map.put("a", "1a");
		map.put("", "0a");
		
		System.out.println(map.size());
		System.out.println(map);
		System.out.println();
		
		System.out.println(map.remove("aaaaaaaaaa"));
		System.out.println(map.containKey("aaaaaaaaaa"));
		System.out.println();
		
		System.out.println(map.remove("aaaaaaaaaa"));
		System.out.println(map.remove("aaaaaaaaab"));
		System.out.println(map.remove("aaaaaaaaa"));
		System.out.println(map.remove("aaaaaaab"));
		System.out.println(map.remove("aaaaaaaaab"));
		System.out.println();
		
		System.out.println(map.size());
		System.out.println();
		
		System.out.println(map.remove("aaaaaaaab"));
		System.out.println(map.remove("aaaaaaaabb"));
		System.out.println(map.remove("aaaaaaa"));
		System.out.println(map.remove("aaaa"));
		System.out.println(map.remove("a"));
		System.out.println(map.remove("ab"));
		System.out.println(map.remove("aaaaaa"));
		System.out.println(map.remove("aaaaa"));
		System.out.println(map.remove("aaaaab"));
		System.out.println(map.remove("aaa"));
		System.out.println(map.remove("aa"));
		System.out.println(map.size());
		System.out.println();
		
		System.out.println(map.size());
		System.out.println(map.get(""));
		System.out.println(map.containKey("aaaaaaaaaa"));
		System.out.println();
	}
	
	private static void iteratorTest() {
		DoubleArrayTrieMap<Integer> map = new DoubleArrayTrieMap<Integer>(6, 5);
		map.put("one", 1);
		map.put("two", 2);
		map.put("three", 3);
		map.put("four", 4);
		map.put("five", 5);
		map.put("", 0);
		
		System.out.println(map);
		System.out.println(map.size());
		Iterator<ImmutableEntry<String, Integer>> iterator = map.iterator();
		while(iterator.hasNext()) {
			ImmutableEntry<String, Integer> e = iterator.next();
			System.out.println(e.key + " = " + e.value);
			iterator.remove();
		}

		System.out.println();
		System.out.println(map);
		System.out.println(map.size());
	}
	
	private static void sortTest() {
		DoubleArrayTrieMap<String> map = new DoubleArrayTrieMap<String>();
		map.put("1", "1");
		map.put("13", "13");
		map.put("124", "124");
		map.put("23", "23");
		map.put("35", "35");
		map.put("0", "zero");
		map.put("", "empty");
		
		System.out.println("stage 1:");
		System.out.println(map.size());
		System.out.println(map.lowerEntry(""));
		System.out.println(map.floorEntry(""));
		System.out.println(map.lowerEntry("1"));
		System.out.println(map.floorEntry("1"));
		System.out.println(map.ceilingEntry("124"));
		System.out.println(map.higherEntry("124"));
		System.out.println(map.ceilingEntry("125"));
		System.out.println(map.higherEntry("125"));
		System.out.println(map.ceilingEntry("35"));
		System.out.println(map.higherEntry("35"));
		System.out.println(map.firstEntry());
		System.out.println(map.lastEntry());
		
		System.out.println();
		map = new DoubleArrayTrieMap<String>();
		
		System.out.println("stage 2:");
		System.out.println(map.size());
		System.out.println(map.lowerEntry(""));
		System.out.println(map.floorEntry(""));
		System.out.println(map.ceilingEntry("35"));
		System.out.println(map.higherEntry("35"));
		System.out.println(map.firstEntry());
		System.out.println(map.lastEntry());
		
		System.out.println();
		map = new DoubleArrayTrieMap<String>();
		map.put("", "empty");
		
		System.out.println("stage 3:");
		System.out.println(map.size());
		System.out.println(map.lowerEntry(""));
		System.out.println(map.floorEntry(""));
		System.out.println(map.ceilingEntry("35"));
		System.out.println(map.higherEntry("35"));
		System.out.println(map.firstEntry());
		System.out.println(map.lastEntry());
	}

}
