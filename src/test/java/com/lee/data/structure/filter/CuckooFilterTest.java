package com.lee.data.structure.filter;

import com.lee.data.structure.filter.CuckooFilter;

public class CuckooFilterTest {

	public static void main(String[] args) {
		// smallFingerprintTest();
		largeFingerprintTest();
	}

	private static void smallFingerprintTest() {
		int expectedCapacity = 10;
		double fpp = 0.001;	// 0.1%
		CuckooFilter<String> filter = CuckooFilter.create(expectedCapacity, fpp);
		printPropertiesOf(filter);
		
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
		
		// remove
		element = "";
		println("remove {"+element+"}: "+filter.remove(element));
		element = "1234567890";
		println("remove {"+element+"}: "+filter.remove(element));
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
		
		printStatisticsOf(filter);
	}
	
	private static void largeFingerprintTest() {
		int expectedCapacity = 100;
		double fpp = 6.776263578034403E-21;
		CuckooFilter<String> filter = CuckooFilter.create(expectedCapacity, fpp);
		printPropertiesOf(filter);
		
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
		
		// remove
		element = "";
		println("remove {"+element+"}: "+filter.remove(element));
		element = "1234567890";
		println("remove {"+element+"}: "+filter.remove(element));
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
		
		printStatisticsOf(filter);
	}
	
	private static void printPropertiesOf(CuckooFilter filter) {
		println("CuckooFilter properties: ");
		println("numOfBuckets: "+filter.numOfBuckets());
		println("numOfEntries: "+filter.numOfEntries());
		println("fingerprintLength: "+filter.fingerprintLength());
		println();
	}
	
	private static void printStatisticsOf(CuckooFilter filter) {
		println("CuckooFilter properties: ");
		println("numOfExistedElements: "+filter.numOfExistedElements());
		println("numOfKickoutElements: "+filter.numOfKickoutElements());
		println();
	}
	
	private static void println() { System.out.println(); }
	
	private static <T> void println(T obj) { System.out.println(obj); }
}
