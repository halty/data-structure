package com.lee.data.structure;

import java.util.Iterator;

public class HashTreeMapTest {

	public static void main(String[] args) {
		normalTest();
		iteratorTest();
	}
	
	private static void normalTest() {
		HashTreeMap<String, String> map = new HashTreeMap<String, String>();
		map.put("1", "one");
		map.put("2", "two");
		map.put("3", "three");
		map.put("4", "four");
		map.put("5", "five");
		
		System.out.println(map.size());
		System.out.println(map.put("1", "yi"));
		System.out.println(map.get("2"));
		System.out.println(map.put("3", null));
		System.out.println(map.get("3"));
		System.out.println(map.remove("4"));
		System.out.println(map.put(null, "null"));
		System.out.println(map.get(null));
		System.out.println(map.remove("5"));
		System.out.println(map.size());
		System.out.println(map);
	}

	private static void iteratorTest() {
		HashTreeMap<String, String> map = new HashTreeMap<String, String>();
		map.put("0", "zero");
		map.put("1", "one");
		map.put("2", "two");
		map.put("3", "three");
		map.put("4", "four");
		map.put("5", "five");
		map.put("6", "six");
		map.put("7", "seven");
		map.put("8", "eight");
		map.put("9", "nine");
		
		System.out.println(map.size());
		Iterator<ImmutableEntry<String, String>> iterator = map.iterator();
		while(iterator.hasNext()) {
			ImmutableEntry<String, String> e = iterator.next();
			System.out.println(e.key + " = " + e.value);
			iterator.remove();
		}
		System.out.println(map.size());
	}
}
