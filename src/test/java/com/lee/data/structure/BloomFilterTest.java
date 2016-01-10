package com.lee.data.structure;

public class BloomFilterTest {

	public static void main(String[] args) {
		int expectedCapacity = 10;
		double fpp = 0.001;	// 0.1%
		BloomFilter<String> filter = BloomFilter.create(expectedCapacity, fpp);
		
		// put
		String element = "";
		println("put {"+element+"}: "+filter.put(element));
		element = "abcdefg";
		println("put {"+element+"}: "+filter.put(element));
		element = "bcdefgh";
		println("put {"+element+"}: "+filter.put(element));
		element = "1234567890";
		println("put {"+element+"}: "+filter.put(element));
		println();
		
		// contain
		element = "";
		println("contain {"+element+"}: "+filter.mightContain(element));
		element = "abcdefg";
		println("contain {"+element+"}: "+filter.mightContain(element));
		element = "bcdefgh";
		println("contain {"+element+"}: "+filter.mightContain(element));
		element = "1234567890";
		println("contain {"+element+"}: "+filter.mightContain(element));
		println();
	}
	
	private static void println() { System.out.println(); }
	
	private static <T> void println(T obj) { System.out.println(obj); }

}
