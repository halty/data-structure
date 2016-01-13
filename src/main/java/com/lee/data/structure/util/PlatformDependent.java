package com.lee.data.structure.util;

import java.lang.reflect.Field;

import sun.misc.Unsafe;

/** Platform dependent Unsafe operations **/
public class PlatformDependent {

	private static final Unsafe UNSAFE;
	private static final long CHAR_ARRAY_OFFSET;
	private static final long CHAR_ARRAY_SCALE;
	
	static {
		Unsafe unsafe = null;
		long offset = 0;
		long scale = 0;
		try {
			// whether support sum.misc.Cleaner or not
			Class.forName("sun.misc.Cleaner");
			
			// whether support sun.misc.Unsafe or not
			Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
			unsafeField.setAccessible(true);
			unsafe = (Unsafe) unsafeField.get(null);
			
			// whether support sun.misc.Unsafe copyMemory method for java object memory copy or not. 
			unsafe.getClass().getDeclaredMethod(
					"copyMemory",
					new Class[] {Object.class, long.class, Object.class, long.class, long.class});
			
			// report the char array base offset and index scale
			offset = unsafe.arrayBaseOffset(char[].class);
			scale = unsafe.arrayIndexScale(char[].class);
		}catch(Throwable t) {	// not support Unsafe operations
			unsafe = null;
			offset = scale = 0;
		}
		
		UNSAFE = unsafe;
		CHAR_ARRAY_OFFSET = offset;
		CHAR_ARRAY_SCALE = scale;
	}
	
	public static boolean hasUnsafe() {
		return UNSAFE != null;
	}
	
	/** return the char size of os's memory page **/
	public static int memoryPageSize() {
		if(!hasUnsafe()) {
			throw new UnsupportedOperationException("unsupported opertaion: memoryPageSize()");
		}
		return UNSAFE.pageSize() / (int)CHAR_ARRAY_SCALE;
	}
	
	/** allocate <code>chars</code> characters memory block **/
	public static long allocateMemory(long chars) {
		if(chars < 0) {
			throw new IllegalArgumentException("illegal argument for allocate memory operaion");
		}
		if(!hasUnsafe()) {
			throw new UnsupportedOperationException("unsupported opertaion: allcateMemory(long)");
		}
		return UNSAFE.allocateMemory(CHAR_ARRAY_SCALE * chars);
	}
	
	/** deallocate the memory block allocated by {@link #allcateMemory(long)} **/
	public static void freeMemory(long address) {
		if(!hasUnsafe()) {
			throw new UnsupportedOperationException("unsupported opertaion: freeMemory(long)");
		}
		UNSAFE.freeMemory(address);
	}
	
	/** read a character from the memory address <code>address</code> with <code>offset</code> **/
	public static char readChar(long address, int offset) {
		if(address == 0 || offset < 0) {
			throw new IllegalArgumentException("illegal argument for read char operaion");
		}
		if(!hasUnsafe()) {
			throw new UnsupportedOperationException("unsupported opertaion: readChar(long)");
		}
		return UNSAFE.getChar(address+offset*CHAR_ARRAY_SCALE);
	}
	
	/** read characters to <code>arr</code> from the memory address <code>address</code> with <code>offset</code> **/
	public static void readChars(long address, int offset, char[] arr) {
		if(address == 0 || offset < 0 || arr == null) {
			throw new IllegalArgumentException("illegal argument for read chars opertaion");
		}
		if(!hasUnsafe()) {
			throw new UnsupportedOperationException("unsupported opertaion: readChars(long, char[])");
		}
		
		if(arr.length == 0) { return; }
		copyMemory(address, offset, arr, 0, arr.length);
	}
	
	/** read characters to <code>arr</code> from the memory address <code>address</code> with <code>offset</code>.
	 * the read characters size equals <code>endIndex - beginIndex</code>.<br/>
	 * if <code>arr.length == 0</code> or <code>beginIndex >= endIndex</code> do nothing.
	 */
	public static void readChars(long address, int offset, char[] arr, int beginIndex, int endIndex) {
		if(address == 0 || offset < 0 || arr == null) {
			throw new IllegalArgumentException("illegal argument for read chars opertaion");
		}
		if(beginIndex < 0 || endIndex > arr.length) {
			throw new ArrayIndexOutOfBoundsException("out of index for read chars operation");
		}
		if(!hasUnsafe()) {
			throw new UnsupportedOperationException("unsupported opertaion: readChars(long, char[])");
		}
		
		if(arr.length == 0 || beginIndex >= endIndex) { return; }
		copyMemory(address, offset, arr, beginIndex, endIndex - beginIndex);
	}
	
	/** write a character to the memory address <code>address</code> with <code>offset</code> **/
	public static void writeChar(long address, int offset, char ch) {
		if(address == 0 || offset < 0) {
			throw new IllegalArgumentException("illegal argument for write char operaion");
		}
		if(!hasUnsafe()) {
			throw new UnsupportedOperationException("unsupported opertaion: writeChar(long, char)");
		}
		UNSAFE.putChar(address+offset*CHAR_ARRAY_SCALE, ch);
	}
	
	/** write characters from <code>arr</code> to the memory address <code>address</code> with <code>offset</code> **/
	public static void writeChars(long address, int offset, char[] arr) {
		if(address == 0 || offset < 0 || arr == null) {
			throw new IllegalArgumentException("illegal argument for write chars opertaion");
		}
		if(!hasUnsafe()) {
			throw new UnsupportedOperationException("unsupported opertaion: writeChars(long, char[])");
		}
		
		if(arr.length == 0) { return; }
		copyMemory(arr, 0, address, offset, arr.length);
	}
	
	/** write characters from <code>arr</code> to the memory address <code>address</code> with <code>offset</code>.
	 * the written characters size equals <code>endIndex - beginIndex</code>.<br/>
	 * if <code>arr.length == 0</code> or <code>beginIndex >= endIndex</code> do nothing.
	 */
	public static void writeChars(long address, int offset, char[] arr, int beginIndex, int endIndex) {
		if(address == 0 || offset < 0 || arr == null) {
			throw new IllegalArgumentException("illegal argument for write chars opertaion");
		}
		if(beginIndex < 0 || endIndex > arr.length) {
			throw new ArrayIndexOutOfBoundsException("out of index for write chars operation");
		}
		if(!hasUnsafe()) {
			throw new UnsupportedOperationException("unsupported opertaion: writeChars(long, char[])");
		}
		
		if(arr.length == 0 || beginIndex >= endIndex) { return; }
		copyMemory(arr, beginIndex, address, offset, endIndex - beginIndex);
	}

	/**
	 * copy <code>chars</code> characters 
	 * from <code>srcMemAddr</code> memory address with <code>srcOffset</code>
	 * to <code>destMemAddr</code> memory address with <code>destOffset</code>
	 * @param srcMemAddr	source memory address
	 * @param srcOffset		source offset, begin at 0
	 * @param destMemAddr	destination memory address
	 * @param destOffset	destination offset, begin at 0
	 * @param chars		copy length based on character
	 */
	public static void copyMemory(long srcMemAddr, int srcOffset, long destMemAddr, int destOffset, long chars) {
		if(!hasUnsafe()) {
			throw new UnsupportedOperationException("unsupported opertaion: copyMemory(long, long, long)");
		}
		UNSAFE.copyMemory(srcMemAddr+srcOffset*CHAR_ARRAY_SCALE,
						  destMemAddr+destOffset*CHAR_ARRAY_SCALE,
						  chars*CHAR_ARRAY_SCALE);
	}
	
	/**
	 * copy <code>chars</code> characters 
	 * from <code>srcArray</code> character array begin at <code>offset</code> 
	 * to <code>destMemAddr</code> memory address
	 * @param srcArray	source java character array
	 * @param srcOffset	java array index, begin at 0.
	 * @param destMemAddr	destination memory address
	 * @param destOffset	destination offset, begin at 0
	 * @param chars		copy length, unit based on character
	 */
	private static void copyMemory(char[] srcArray, int srcOffset, long destMemAddr, int destOffset, long chars) {
		UNSAFE.copyMemory(srcArray, CHAR_ARRAY_OFFSET+srcOffset*CHAR_ARRAY_SCALE,
						  null, destMemAddr+destOffset*CHAR_ARRAY_SCALE,
						  chars*CHAR_ARRAY_SCALE);
	}
	
	/**
	 * copy <code>chars</code> characters 
	 * from <code>srcMemAddr</code> memory address 
	 * to <code>destArray</code> character array begin at <code>offset</code>.
	 * @param srcMemAddr	source memory address
	 * @param srcOffset		source offset, begin at 0
	 * @param destArray		destination java character array
	 * @param destOffset	java array index, begin at 0
	 * @param chars			copy length, unit based on character
	 */
	private static void copyMemory(long srcMemAddr, int srcOffset, char[] destArray, int destOffset, long chars) {
		UNSAFE.copyMemory(null, srcMemAddr+srcOffset*CHAR_ARRAY_SCALE,
						  destArray, CHAR_ARRAY_OFFSET+destOffset*CHAR_ARRAY_SCALE,
						  chars*CHAR_ARRAY_SCALE);
	}
}
