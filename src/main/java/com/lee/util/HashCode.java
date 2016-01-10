package com.lee.util;

public final class HashCode {

	private final byte[] hashCode;
	
	public HashCode(byte[] hashCode) {
		this.hashCode = hashCode;
	}
	
	/** returns the number of bits in this hash code; a positive multiple of 8 **/
	public int bits() { return hashCode.length * 8; }
	
	/** return the first byte of this hash code, return 0 if {@code bits() < 8} */
	public byte asByte() { return hashCode.length > 0 ? hashCode[0] : 0; }
	
	/** return the <i>n</i>th byte of this hash code, return 0 if {@code n <= 0} or {@code bits() < 8*n} */
	public byte asNthByte(int n) {
		if(n < 1 || n > hashCode.length) { return 0; }
		return hashCode[n-1];
	}
	
	/** return all the bytes of this hash code **/
	public byte[] asBytes() { return hashCode; }
	
	/**
     * returns the first four bytes of {@linkplain #asBytes() this hash code's bytes},
     * converted to an {@code int} value in little-endian order. if it has not enough bits,
     * padding {@code 0x00} as the remaining most-significant bytes
     */
	public int asInt() { return asInt(hashCode); }
	
	/**
     * returns the first four bytes of {@code bytes}, converted to an {@code int} value
     * in little-endian order. if it has not enough bits, padding {@code 0x00} as the
     * remaining most-significant bytes.
     */
	public static int asInt(byte[] bytes) {
		int size = Math.min(4, bytes.length);
		int value = 0;
		switch(size) {
		case 4: value |= ((0xff & bytes[3]) << 24);
		case 3: value |= ((0xff & bytes[2]) << 16);
		case 2: value |= ((0xff & bytes[1]) << 8);
		case 1: value |= (0xff & bytes[0]);
		}
		return value;
	}
	
	/**
     * returns the <i>n</i>th integer in little-endian order consist of four bytes of
     * {@linkplain #asBytes() this hash code's bytes}, if it has not enough bits,
     * padding {@code 0x00} as the remaining most-significant bytes
     */
	public int asNthInt(int n) { return asNthInt(hashCode, n); }
	
	/**
     * returns the <i>n</i>th integer in little-endian order consist of four bytes of
     * {@lcode bytes}, if it has not enough bits, padding {@code 0x00} as the remaining
     * most-significant bytes.
     */
	public static int asNthInt(byte[] bytes, int n) { return asInt(bytes, (n-1)*4); }
	
	/**
     * returns four bytes of {@code bytes} began with byte index {@code fromIndex},
     * converted to an {@code int} value in little-endian order. if it has not enough bits,
     * padding {@code 0x00} as the remaining most-significant bytes
     */
	public static int asInt(byte[] bytes, int fromIndex) {
		if(fromIndex < 0 || fromIndex >= bytes.length) { return 0; }
		int size = Math.min(4, bytes.length - fromIndex);
		int value = 0;
		switch(size) {
		case 4: value |= ((0xff & bytes[fromIndex+3]) << 24);
		case 3: value |= ((0xff & bytes[fromIndex+2]) << 16);
		case 2: value |= ((0xff & bytes[fromIndex+1]) << 8);
		case 1: value |= (0xff & bytes[fromIndex]);
		}
		return value;
	}
	
	/**
     * returns four bytes of {@code bytes} began with bit index {@code bitIndex},
     * converted to an {@code int} value in little-endian order. if it has not enough bits,
     * padding {@code 0x00} as the remaining most-significant bytes
     */
	public static int asIntFrom(byte[] bytes, long bitIndex) {
		if(bitIndex < 0 || bitIndex >= bytes.length*8) { return 0; }

		int index = (int) (bitIndex / 8);
		int offset = (int) (bitIndex % 8);
		int value = (0xff & bytes[index]) >>> offset;
		
		for(int i=1, shift=8-offset; i<4; i++, shift+=8) {
			index += 1;
			if(index >= bytes.length) { break; }
			value |= (0xff & bytes[index]) << shift;
		}
		
		if(offset > 0 && (index+=1) < bytes.length) {
			value |= ((int)bytes[index]) << (-offset);
		}
		
		return value;
	}
	
	/**
     * returns all the bytes of {@linkplain #asBytes() this hash code's bytes},
     * converted to an {@code int} array in little-endian order. if it has not enough bits,
     * padding {@code 0x00} as the remaining most-significant bytes
     */
	public int[] asInts() { return asInts(hashCode); }
	
	/**
     * returns all the bytes of {code bytes}, converted to an {@code int} array in 
     * little-endian order. if it has not enough bits, padding {@code 0x00} as the
     * remaining most-significant bytes.
     */
	public static int[] asInts(byte[] bytes) {
		int size = bytes.length / 4;
		int remain = bytes.length % 4;
		int index = 0;
		int[] array = new int[size + (remain == 0 ? 0 : 1)];
		for(int i=0; i<size; i++) {
			array[i] = ((0xff & bytes[index+3]) << 24)
					   | ((0xff & bytes[index+2]) << 16)
					   | ((0xff & bytes[index+1]) << 8)
					   | (0xff & bytes[index]);
			index += 4;
		}
		if(remain > 0) {
			int value = 0;
			switch(remain) {
			case 3: value |= ((0xff & bytes[index+2]) << 16);
			case 2: value |= ((0xff & bytes[index+1]) << 8);
			case 1: value |= (0xff & bytes[index]);
			}
			array[size] = value;
		}
		return array;
	}
	
	/**
     * returns the first eight bytes of {@linkplain #asBytes() this hash code's bytes},
     * converted to an {@code long} value in little-endian order. if it has not enough bits,
     * padding {@code 0x00} as the remaining most-significant bytes
     */
	public long asLong() { return asLong(hashCode); }
	
	/**
     * returns the first eight bytes of {code bytes}, converted to an {@code long} value
     * in little-endian order. if it has not enough bits, padding {@code 0x00} as the
     * remaining most-significant bytes.
     */
	public static long asLong(byte[] bytes) {
		int size = Math.min(8, bytes.length);
		long value = 0;
		switch(size) {
		case 8: value |= ((0xffL & bytes[7]) << 56);
		case 7: value |= ((0xffL & bytes[6]) << 48);
		case 6: value |= ((0xffL & bytes[5]) << 40);
		case 5: value |= ((0xffL & bytes[4]) << 32);
		case 4: value |= ((0xffL & bytes[3]) << 24);
		case 3: value |= ((0xffL & bytes[2]) << 16);
		case 2: value |= ((0xffL & bytes[1]) << 8);
		case 1: value |= (0xffL & bytes[0]);
		}
		return value;
	}
	
	/**
     * returns the <i>n</i>th long integer in little-endian order consist of eight bytes of
     * {@linkplain #asBytes() this hash code's bytes}, if it has not enough bits,
     * padding {@code 0x00} as the remaining most-significant bytes
     */
	public long asNthLong(int n) { return asNthLong(hashCode, n); }
	
	/**
     * returns the <i>n</i>th long integer in little-endian order consist of eight bytes of
     * {@code bytes}, if it has not enough bits, padding {@code 0x00} as the remaining
     * most-significant bytes.
     */
	public static long asNthLong(byte[] bytes, int n) { return asLong(bytes, (n-1) * 8); }
	
	/**
     * returns eight bytes of {@code bytes} began with byte index {@code fromIndex},
     * converted to an {@code long} value in little-endian order. if it has not enough bits,
     * padding {@code 0x00} as the remaining most-significant bytes
     */
	public static long asLong(byte[] bytes, int fromIndex) {
		if(fromIndex < 0 || fromIndex >= bytes.length) { return 0; }
		int size = Math.min(8, bytes.length-fromIndex);
		long value = 0;
		switch(size) {
		case 8: value |= ((0xffL & bytes[7]) << 56);
		case 7: value |= ((0xffL & bytes[6]) << 48);
		case 6: value |= ((0xffL & bytes[5]) << 40);
		case 5: value |= ((0xffL & bytes[4]) << 32);
		case 4: value |= ((0xffL & bytes[3]) << 24);
		case 3: value |= ((0xffL & bytes[2]) << 16);
		case 2: value |= ((0xffL & bytes[1]) << 8);
		case 1: value |= (0xffL & bytes[0]);
		}
		return value;
	}
	
	/**
     * returns eight bytes of {@code bytes} began with bit index {@code bitIndex},
     * converted to an {@code long} value in little-endian order. if it has not enough bits,
     * padding {@code 0x00} as the remaining most-significant bytes
     */
	public static long asLongFrom(byte[] bytes, long bitIndex) {
		if(bitIndex < 0 || bitIndex >= bytes.length*8) { return 0; }
		
		int index = (int) (bitIndex / 8);
		int offset = (int) (bitIndex % 8);
		long value = (0xffL & bytes[index]) >>> offset;
		
		for(int i=1, shift=8-offset; i<8; i++, shift+=8) {
			index += 1;
			if(index >= bytes.length) { break; }
			value |= (0xffL & bytes[index]) << shift;
		}
		
		if(offset > 0 && (index+=1) < bytes.length) {
			value |= ((long)bytes[index]) << (-offset);
		}
		
		return value;
	}
	
	/**
     * returns all the bytes of {@linkplain #asBytes() this hash code's bytes},
     * converted to an {@code long} array in little-endian order. if it has not enough bits,
     * padding {@code 0x00} as the remaining most-significant bytes
     */
	public long[] asLongs() { return asLongs(hashCode); }
	
	/**
     * returns all the bytes of {@code bytes}, converted to an {@code long} array
     * in little-endian order. if it has not enough bits, padding {@code 0x00} as
     * the remaining most-significant bytes.
     */
	public static long[] asLongs(byte[] bytes) {
		int size = bytes.length / 8;
		int remain = bytes.length % 8;
		int index = 0;
		long[] array = new long[size + (remain == 0 ? 0 : 1)];
		for(int i=0; i<size; i++) {
			array[i] =   ((0xffL & bytes[index+7]) << 56)
					   | ((0xffL & bytes[index+6]) << 48)
					   | ((0xffL & bytes[index+5]) << 40)
					   | ((0xffL & bytes[index+4]) << 32)
					   | ((0xffL & bytes[index+3]) << 24)
					   | ((0xffL & bytes[index+2]) << 16)
					   | ((0xffL & bytes[index+1]) << 8)
					   | (0xffL & bytes[index]);
			index += 8;
		}
		if(remain > 0) {
			long value = 0;
			switch(remain) {
			case 7: value |= ((0xffL & bytes[index+6]) << 48);
			case 6: value |= ((0xffL & bytes[index+5]) << 40);
			case 5: value |= ((0xffL & bytes[index+4]) << 32);
			case 4: value |= ((0xffL & bytes[index+3]) << 24);
			case 3: value |= ((0xffL & bytes[index+2]) << 16);
			case 2: value |= ((0xffL & bytes[index+1]) << 8);
			case 1: value |= (0xffL & bytes[index]);
			}
			array[size] = value;
		}
		return array;
	}
}
