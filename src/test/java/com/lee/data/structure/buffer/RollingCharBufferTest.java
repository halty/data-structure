package com.lee.data.structure.buffer;

import com.lee.data.structure.buffer.RollingCharBuffer;

import java.util.Arrays;

public class RollingCharBufferTest {

	public static void main(String[] args) throws Exception {
		testRollingHeapCharBuffer();
		testRollingDirectCharBuffer();
	}

	public static void testRollingHeapCharBuffer() {
		testOpSeq(RollingCharBuffer.allocate(6));
	}
	
	public static void testRollingDirectCharBuffer() {
		testOpSeq(RollingCharBuffer.allocate(6, true));
	}
	
	private static void testOpSeq(RollingCharBuffer buffer) {
		println("====================="+buffer.getClass().getSimpleName()+"=====================");
		
		boolean hasArray = buffer.hasArray();
		println(hasArray);
		
		buffer.put('1');
		buffer.put(new char[]{'2', '3'});
		buffer.put(new char[]{'4', '5'});
		println(buffer.take());
		println(Arrays.toString(buffer.take(2)));
		buffer.put(new char[]{'6', 'a', 'b'});
		
		if(hasArray) { println(Arrays.toString(buffer.array())); }
		buffer.put(new char[]{'c', 'd'});
		println(buffer.size());
		println(buffer.putIndex());
		println(buffer.takeIndex());
		
		if(hasArray) { println(Arrays.toString(buffer.array())); }
		println(Arrays.toString(buffer.takeAll()));
		println(buffer.size());
		println(buffer.putIndex());
		println(buffer.takeIndex());
		
		buffer.release();
		
		println("====================="+buffer.getClass().getSimpleName()+"=====================");
		println("");
	}

	private static <T> void println(T obj) {
		System.out.println(obj);
	}

}
