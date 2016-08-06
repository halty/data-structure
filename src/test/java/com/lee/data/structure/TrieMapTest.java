package com.lee.data.structure;

public class TrieMapTest {

	public static void main(String[] args) {
		TrieMap<Integer> map = new TrieMap<Integer>();
		map.put("one", 1);
		map.put("two", 2);
		map.put("three", 3);
		map.put("four", 4);
		map.put("five", 5);
		map.put("", 0);
		map.put(null, -1);
		
		System.out.println(map.size());
		System.out.println();
		
		System.out.println(map.get("one"));
		System.out.println(map.put("one", 10));
		System.out.println(map.get("one"));
		System.out.println();
		
		System.out.println(map.get(""));
		System.out.println(map.put("", -2));
		System.out.println(map.get(""));
		System.out.println();
		
		System.out.println(map.get(null));
		System.out.println(map.put(null, 0));
		System.out.println(map.get(null));
		System.out.println();
		
		System.out.println(map.get("null"));
		System.out.println();
		
		System.out.println(map.remove("two"));
		System.out.println(map.containKey("two"));
		System.out.println();
		
		System.out.println(map.containKey(null));
		System.out.println(map.containKey(""));
		System.out.println(map.containKey("five"));
		System.out.println();
		
		System.out.println(map.size());
		System.out.println();
		
		System.out.println(map);
	}

}
