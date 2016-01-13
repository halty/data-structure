package com.lee.data.structure.util;

import java.lang.reflect.Field;

import sun.misc.Unsafe;

/** Unsafe usage Can refer to AtomicXXX class. e.g AtomicInteger **/
public class UnsafeTest {

	public static void main(String[] args) throws Exception {
		// testAccessJavaCharArray();
		// testReallocateMemory();
		// testCopyJavaMemory();
		testPageSize();
	}

	public static void testAccessJavaCharArray() throws Exception {
		char[] chars = new char[] {'1', '2', '3'};
		Unsafe unsafe = getUnsafe();
		long address = unsafe.arrayBaseOffset(char[].class);
		int scale = unsafe.arrayIndexScale(char[].class);
		println("scale = "+scale);
		println(unsafe.getChar(chars, address));
		println(unsafe.getChar(chars, address+scale));
		println(unsafe.getChar(chars, address+2*scale));
	}

	public static void testReallocateMemory() throws Exception {
		Unsafe unsafe = getUnsafe();
		long addr = unsafe.allocateMemory(2);
		unsafe.putByte(addr, (byte)1);
		unsafe.putByte(addr+1, (byte)2);
		println(unsafe.getByte(addr));
		println(unsafe.getByte(addr+1));
		long newAddr = unsafe.reallocateMemory(addr, 4);
		println(unsafe.getByte(newAddr));
		println(unsafe.getByte(newAddr+1));
		unsafe.freeMemory(newAddr);
	}

	public static void testCopyJavaMemory() throws Exception {
		Unsafe unsafe = getUnsafe();
		
		char[] src = new char[] {'1', '2', '3'};
		long offset = unsafe.arrayBaseOffset(char[].class);
		long scale = unsafe.arrayIndexScale(char[].class);
		long dest = unsafe.allocateMemory(3);
		unsafe.putChar(dest, '4');
		unsafe.putChar(dest+scale, '5');
		unsafe.putChar(dest+scale*2, '6');
		
		unsafe.copyMemory(src, offset+scale, null, dest+scale, 2*scale);
		println(unsafe.getChar(dest));
		println(unsafe.getChar(dest+scale));
		println(unsafe.getChar(dest+scale*2));
		
		unsafe.putChar(dest, 'a');
		unsafe.putChar(dest+scale, 'b');
		unsafe.putChar(dest+scale*2, 'c');
		
		unsafe.copyMemory(null, dest+scale, src, offset+scale, 2*scale);
		println(src[0]);
		println(src[1]);
		println(src[2]);
		
		unsafe.freeMemory(dest);
	}
	
	private static void testPageSize() throws Exception {
		Unsafe unsafe = getUnsafe();
		
		println(unsafe.pageSize());
	}

	private static Unsafe getUnsafe() throws Exception {
		Field f = Unsafe.class.getDeclaredField("theUnsafe");
		f.setAccessible(true);
		return (Unsafe) f.get(null);
	}

	private static <T> void println(T obj) {
		System.out.println(obj);
	}
}
