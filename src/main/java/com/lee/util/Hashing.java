package com.lee.util;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Calendar;
import java.util.Date;

public final class Hashing {

	private static final long GOOD_HASH_SEED = System.currentTimeMillis();
	
	private static final long HASH_SEED_ADDER = 2147483647;		// max prime number within int range
	
	/**
	 * compute a hash code of {@code object} with {@code numOfHashBits} bits.
	 * if {@code numOfHashBits} isn't positive multiple of 8, round it, then
	 * the return {@link HashCode} contain {@code (numOfHashBits-1) / 8 + 1}
	 * bytes, and you can get the {@code numOfHashBits} number of least-significant bits.
	 * 
	 * 
	 * @param object	to be computed object
	 * @param numOfHashBits		expected number of bits in hash code
	 * @return	hash code with {@code (numOfHashBits-1) / 8 + 1} bytes
	 */
	public static <T> HashCode hash(T object, int numOfHashBits) {
		int byteCount = numOfHashBits <= 8 ? 1 : ((numOfHashBits - 1) / 8 + 1);
		if(byteCount <= Murmur3_32Hasher.CHUNK_SIZE) {
			return new HashCode(new Murmur3_32Hasher(GOOD_HASH_SEED).hashObject(object));
		}else if(byteCount <= Murmur3_128Hasher.CHUNK_SIZE) {
			return new HashCode(new Murmur3_128Hasher(GOOD_HASH_SEED).hashObject(object));
		}else {
			int _128Cnt = byteCount / Murmur3_128Hasher.CHUNK_SIZE;
			byteCount %= Murmur3_128Hasher.CHUNK_SIZE;
			int _32Cnt = (byteCount-1) / Murmur3_32Hasher.CHUNK_SIZE + 1;
			ByteBuffer buf = ByteBuffer.allocate(_128Cnt * Murmur3_128Hasher.CHUNK_SIZE + _32Cnt * Murmur3_32Hasher.CHUNK_SIZE)
					.order(ByteOrder.LITTLE_ENDIAN);
			long seed = GOOD_HASH_SEED;
			for(int i =0; i<_128Cnt; i++) {
				buf.put(new Murmur3_128Hasher(seed).hashObject(object));
				seed += HASH_SEED_ADDER;
			}
			for(int i =0; i<_32Cnt; i++) {
				buf.put(new Murmur3_32Hasher(seed).hashObject(object));
				seed += HASH_SEED_ADDER;
			}
			return new HashCode(buf.array());
		}
	}
	
	/** a modified variant from guava **/
	/*
	 * Copyright (C) 2011 The Guava Authors
	 *
	 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
	 * in compliance with the License. You may obtain a copy of the License at
	 *
	 * http://www.apache.org/licenses/LICENSE-2.0
	 *
	 * Unless required by applicable law or agreed to in writing, software distributed under the License
	 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
	 * or implied. See the License for the specific language governing permissions and limitations under
	 * the License.
	 */

	/*
	 * MurmurHash3 was written by Austin Appleby, and is placed in the public
	 * domain. The author hereby disclaims copyright to this source code.
	 */

	/*
	 * Source:
	 * http://code.google.com/p/smhasher/source/browse/trunk/MurmurHash3.cpp
	 * (Modified to adapt to Guava coding conventions and to use the HashFunction interface)
	 */
	
	private static abstract class StreamHasher {
		protected final ByteBuffer bb;
		private final int chunckSize;
		
		StreamHasher(int bufferSize, int chunckSize) {
			this.bb = ByteBuffer.allocate(bufferSize+7).order(ByteOrder.LITTLE_ENDIAN);	// allocate a space to put a max length primitive 
			this.chunckSize = chunckSize;
		}
		
		final byte[] hashObject(Object object) {
			flush(object);
			return hash();
		}
		
		private void flush(Object obj) {
			processIfFull(8);	// max size of bytes for single primitive
			if(obj == null) {
				bb.putInt(0);
				return;
			}
			Class<?> clazz = obj.getClass();
			if(clazz.isArray()) {
				Object[] array = (Object[]) obj;
				for(Object e : array) { flush(e); }
			}else if(clazz == boolean.class || clazz == Boolean.class) {
				bb.put((byte) ((boolean)obj ? 1 : 0));
			}else if(clazz == byte.class || clazz == Byte.class) {
				bb.put((byte) obj);
			}else if(clazz == char.class || clazz == Character.class) {
				bb.putChar((char) obj);
			}else if(clazz == short.class || clazz == Short.class) {
				bb.putShort((short) obj);
			}else if(clazz == int.class || clazz == Integer.class) {
				bb.putInt((int) obj);
			}else if(clazz == long.class || clazz == Long.class) {
				bb.putLong((long) obj);
			}else if(clazz == float.class || clazz == Float.class) {
				bb.putFloat((float) obj);
			}else if(clazz == double.class || clazz == Double.class) {
				bb.putDouble((double) obj);
			}else if(clazz == Date.class) {
				bb.putLong(((Date)obj).getTime());
			}else if(obj instanceof Calendar) {
				bb.putLong(((Calendar)obj).getTimeInMillis());
			}else if(obj instanceof CharSequence) {
				CharSequence chars = (CharSequence) obj;
				int length = chars.length();
				for(int i=0; i<length; i++) {
					bb.putChar(chars.charAt(i));
					processIfFull(2);
				}
			}else { bb.putInt(obj.hashCode()); }
		}
		
		final byte[] hash() {
			bb.flip();
	    	while(bb.remaining() > chunckSize) { process(bb); }
	    	bb.compact();
	    	if(bb.remaining() > 0) { processRemaining(bb); }
	    	return makeHash();
		}
		
		final void processIfFull(int requiredCapacity) {
			if(bb.remaining() <= requiredCapacity) {
				bb.flip();
		    	while(bb.remaining() > chunckSize) { process(bb); }
		    	bb.compact();
			}
		}
		
		abstract void process(ByteBuffer bb);
		
		abstract void processRemaining(ByteBuffer bb);
		
		abstract byte[] makeHash();
		
		static int toInt(byte value) { return value & 0xFF; }
	}
	
	private static final class Murmur3_32Hasher extends StreamHasher {
		private static final int CHUNK_SIZE = 4;
		private static final int C1 = 0xcc9e2d51;
		private static final int C2 = 0x1b873593;
	    private int h1;
	    private int length;
	    
	    Murmur3_32Hasher(long seed) {
	    	super(CHUNK_SIZE, CHUNK_SIZE);
	        this.h1 = (int) seed;
	        this.length = 0;
	    }
	    
	    void process(ByteBuffer bb) {
	        int k1 = mixK1(bb.getInt());
	        h1 = mixH1(h1, k1);
	        length += CHUNK_SIZE;
	    }
	    
	    void processRemaining(ByteBuffer bb) {
	    	length += bb.remaining();
	    	int k1 = 0;
	        for (int i = 0; bb.hasRemaining(); i += 8) {
	          k1 ^= toInt(bb.get()) << i;
	        }
	        h1 ^= mixK1(k1);
	    }
	    
	    byte[] makeHash() {
	    	return ByteBuffer.wrap(new byte[4])
	    			.order(ByteOrder.LITTLE_ENDIAN)
	    			.putInt(fmix(h1, length))
	    			.array();
	    }
	    
	    static int fmix(int h1, int length) {
	    	h1 ^= length;
	        h1 ^= h1 >>> 16;
	        h1 *= 0x85ebca6b;
	        h1 ^= h1 >>> 13;
	        h1 *= 0xc2b2ae35;
	        h1 ^= h1 >>> 16;
	        return h1;
	    }

	    static int mixK1(int k1) {
	        k1 *= C1;
	        k1 = Integer.rotateLeft(k1, 15);
	        k1 *= C2;
	        return k1;
	    }

	    static int mixH1(int h1, int k1) {
	    	h1 ^= k1;
	        h1 = Integer.rotateLeft(h1, 13);
	        h1 = h1 * 5 + 0xe6546b64;
	        return h1;
	    }
	}
	
	private static final class Murmur3_128Hasher extends StreamHasher {
	    private static final int CHUNK_SIZE = 16;
	    private static final long C1 = 0x87c37b91114253d5L;
	    private static final long C2 = 0x4cf5ad432745937fL;
	    private long h1;
	    private long h2;
	    private int length;

	    Murmur3_128Hasher(long seed) {
	    	super(CHUNK_SIZE, CHUNK_SIZE);
	        this.h1 = seed;
	        this.h2 = seed;
	        this.length = 0;
	    }
	    
	    void process(ByteBuffer bb) {
	        long k1 = bb.getLong();
	        long k2 = bb.getLong();
	        bmix64(k1, k2);
	        length += CHUNK_SIZE;
	    }

	    void bmix64(long k1, long k2) {
	        h1 ^= mixK1(k1);

	        h1 = Long.rotateLeft(h1, 27);
	        h1 += h2;
	        h1 = h1 * 5 + 0x52dce729;

	        h2 ^= mixK2(k2);

	        h2 = Long.rotateLeft(h2, 31);
	        h2 += h1;
	        h2 = h2 * 5 + 0x38495ab5;
	    }

	    void processRemaining(ByteBuffer bb) {
	        long k1 = 0;
	        long k2 = 0;
	        length += bb.remaining();
	        switch (bb.remaining()) {
	            case 15: k2 ^= (long) toInt(bb.get(14)) << 48; // fall through
	            case 14: k2 ^= (long) toInt(bb.get(13)) << 40; // fall through
	            case 13: k2 ^= (long) toInt(bb.get(12)) << 32; // fall through
	            case 12: k2 ^= (long) toInt(bb.get(11)) << 24; // fall through
	            case 11: k2 ^= (long) toInt(bb.get(10)) << 16; // fall through
	            case 10: k2 ^= (long) toInt(bb.get(9)) << 8; // fall through
	            case 9: k2 ^= (long) toInt(bb.get(8)); // fall through
	            case 8: k1 ^= bb.getLong();
	            break;
	            case 7: k1 ^= (long) toInt(bb.get(6)) << 48; // fall through
	            case 6: k1 ^= (long) toInt(bb.get(5)) << 40; // fall through
	            case 5: k1 ^= (long) toInt(bb.get(4)) << 32; // fall through
	            case 4: k1 ^= (long) toInt(bb.get(3)) << 24; // fall through
	            case 3: k1 ^= (long) toInt(bb.get(2)) << 16; // fall through
	            case 2: k1 ^= (long) toInt(bb.get(1)) << 8; // fall through
	            case 1: k1 ^= (long) toInt(bb.get(0));
	            break;
	            default:
	            throw new AssertionError("Should never get here.");
	        }
	        h1 ^= mixK1(k1);
	        h2 ^= mixK2(k2);
	    }

	    byte[] makeHash() {
	        h1 ^= length;
	        h2 ^= length;

	        h1 += h2;
	        h2 += h1;

	        h1 = fmix64(h1);
	        h2 = fmix64(h2);

	        h1 += h2;
	        h2 += h1;

	        return ByteBuffer.wrap(new byte[CHUNK_SIZE])
	            .order(ByteOrder.LITTLE_ENDIAN)
	            .putLong(h1)
	            .putLong(h2)
	            .array();
	    }

	    static long fmix64(long k) {
	        k ^= k >>> 33;
	        k *= 0xff51afd7ed558ccdL;
	        k ^= k >>> 33;
	        k *= 0xc4ceb9fe1a85ec53L;
	        k ^= k >>> 33;
	        return k;
	    }

	    static long mixK1(long k1) {
	        k1 *= C1;
	        k1 = Long.rotateLeft(k1, 31);
	        k1 *= C2;
	        return k1;
	    }

	    static long mixK2(long k2) {
	        k2 *= C2;
	        k2 = Long.rotateLeft(k2, 33);
	        k2 *= C1;
	        return k2;
	    }
    }
}
