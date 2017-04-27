package com.lee.data.structure;

import java.util.concurrent.TimeUnit;

public class MultiReaderSingleWriterFixedMapTest {

	public static void main(String[] args) throws Exception {
		MultiReaderSingleWriterFixedMap<Integer, String> map = new MultiReaderSingleWriterFixedMap<Integer, String>(20);
		for(int i=1; i<=100; i++) {
			String value = map.get(i);
			if(value == null) {
				map.append(i, String.valueOf(-i));
			}
		}
		TimeUnit.SECONDS.sleep(10);
		for(int i=1; i<=100; i++) {
			String value = map.get(i);
			if(value  != null) {
				System.out.println(i + " : " + value);
			}
		}
		map.destroy();
		System.out.println("exit");
	}

}
