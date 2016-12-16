package com.lee.data.structure;

public class MainTest {

	public static void main(String[] args) {
		int i = -1;
		long e = (((long)i) << 32) + i;
		System.out.println(Long.toBinaryString(e));
	}

}
