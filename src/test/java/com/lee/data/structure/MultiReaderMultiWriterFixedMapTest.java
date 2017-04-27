package com.lee.data.structure;

import java.util.ArrayList;
import java.util.List;

public class MultiReaderMultiWriterFixedMapTest {

	public static void main(String[] args) {
		testSinglePut();
	}
	
	public static void testSinglePut() {
		MultiReaderMultiWriterFixedMap<Integer, String> map = new MultiReaderMultiWriterFixedMap<Integer, String>(20);
		List<Integer[]> list = new ArrayList<Integer[]>();
		list.add(fill(1, 21));
		list.add(fill(21, 41));
		list.add(fill(41, 61));
		list.add(fill(61, 81));
		list.add(fill(81, 101));
		for(Integer[] array : list) {
			Thread t = concurrentPut(map, array);
			t.start();
		}
		for(int i=1; i<=100; i++) {
			String value = map.get(i);
			if(value  != null) {
				System.out.println(i + " : " + value);
			}
		}
		System.out.println("end");
	}
	
	private static Integer[] fill(int begin, int end) {
		int length = end - begin;
		Integer[] array = new Integer[length];
		for(int i=begin, j=0; i<end; i++, j++) {
			array[j] = i;
		}
		return array;
	}
	
	private static Thread concurrentPut(final MultiReaderMultiWriterFixedMap<Integer, String> map, final Integer[] keys) {
		return new Thread(new Runnable() {
			@Override
			public void run() {
				for(Integer key : keys) {
					String value = map.get(key);
					if(value == null) {
						map.put(key, String.valueOf(-key));
					}
				}
			}
		});
	}
}
