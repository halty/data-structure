package com.lee.data.structure;

public class HashTreeMapTest {

	public static void main(String[] args) {
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

}
