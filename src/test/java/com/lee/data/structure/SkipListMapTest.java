package com.lee.data.structure;

import java.util.Iterator;

public class SkipListMapTest {

	public static void main(String[] args) {
		normalTest();
		iteratorTest();
		sortTest();
	}

	private static void normalTest() {
		SkipListMap<String, String> map = new SkipListMap<String, String>(4);
		map.put("1", "one");
		map.put("3", "three");
		map.put("4", "four");
		map.put("2", "two");
		map.put("5", "five");
		
		System.out.println(map.size());
		System.out.println(map.put("1", "yi"));
		System.out.println(map.get("2"));
		System.out.println(map.put("3", null));
		System.out.println(map.get("3"));
		System.out.println(map.remove("4"));
		System.out.println(map.remove("5"));
		System.out.println(map.put("0", "zero"));
		System.out.println(map.size());
		System.out.println(map);
	}

	private static void iteratorTest() {
		SkipListMap<String, String> map = new SkipListMap<String, String>(4);
		map.put("1", "one");
		map.put("3", "three");
		map.put("4", "four");
		map.put("2", "two");
		map.put("5", "five");
		
		System.out.println(map.size());
		Iterator<ImmutableEntry<String, String>> iterator = map.iterator();
		while(iterator.hasNext()) {
			ImmutableEntry<String, String> e = iterator.next();
			System.out.println(e.key + " = " + e.value);
			iterator.remove();
		}
		System.out.println(map.size());
	}
	
	private static void sortTest() {
		SkipListMap<String, String> map = new SkipListMap<String, String>(4);
		map.put("1", "one");
		map.put("3", "three");
		map.put("4", "four");
		map.put("2", "two");
		map.put("5", "five");
		
		System.out.println(map.size());
		System.out.println(map.lowerEntry("1"));
		System.out.println(map.floorEntry("1"));
		System.out.println(map.floorEntry("3"));
		System.out.println(map.ceilingEntry("3"));
		System.out.println(map.ceilingEntry("5"));
		System.out.println(map.higherEntry("5"));
		System.out.println(map.firstEntry());
		System.out.println(map.lastEntry());
	}
}
