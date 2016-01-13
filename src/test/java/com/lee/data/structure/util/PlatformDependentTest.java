package com.lee.data.structure.util;

import static com.lee.data.structure.util.PlatformDependent.*;

import java.util.Arrays;

public class PlatformDependentTest {

	public static void main(String[] args) {
		char[] src = new char[] {'1','2','3'};
		long dest = allocateMemory(3);
		
		writeChar(dest, 0, 'a');
		writeChar(dest, 1, 'a');
		writeChar(dest, 2, 'a');
		
		writeChars(dest, 1, src, 1, 3);
		System.out.println(readChar(dest, 1));
		System.out.println(readChar(dest, 2));
		
		writeChar(dest, 1, 'b');
		writeChar(dest, 2, 'c');
		
		readChars(dest, 0, src);
		System.out.println(Arrays.toString(src));
		
		freeMemory(dest);
	}

}
