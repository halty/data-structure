package com.lee.data.structure;

import java.util.Random;

import com.lee.util.HashCode;
import com.lee.util.Hashing;

/**
 * see more detail of <a href="http://www.cs.cmu.edu/~binfan/papers/conext14_cuckoofilter.pdf">Cuckoo Filter</a>
 * @NotThreadSafe
 **/
public final class CuckooFilter<T> {
	
	/** a single insertion relocation times before finding an empty entry **/
	private static final int MAX_KICK_OUT_TIMES = 500;
	
	/** a random instance use to select kick out bucket **/
	private final Random rand = new Random();

	/** the buckets for CuckooFilter **/
	private final Buckets buckets;
	
	/** number of elements in this CuckooFilter **/
	private int numOfExistedElements;
	
	/** the number of kick out elements after {@link #MAX_KICK_OUT_TIMES} relocation times **/
	private int numOfKickoutElements;
	
	private CuckooFilter(int numOfBuckets, int numOfEntries, int fingerprintLength) {
		if(numOfBuckets <= 0) {
			throw new IllegalArgumentException(String.format("numOfBuckets (%d) must be > 0", numOfBuckets));
		}
		if(numOfEntries <= 0) {
			throw new IllegalArgumentException(String.format("numOfEntries (%d) must be > 0", numOfEntries));
		}
		if(fingerprintLength <= 0) {
			throw new IllegalArgumentException(String.format("fingerprintLength (%d) must be > 0", fingerprintLength));
		}
		this.buckets = Buckets.create(numOfBuckets, numOfEntries, fingerprintLength);
	}
	
	/**
	 * put an element into this {@link CuckooFilter}, ensure that subsequent invocations of
     * {@link #mightContain(element)} with the same element will always return {@code true}.
     * <p>note that this operation always ensure that {@code element} was put successful into
     * this {@link CuckooFilter}, so the return <code>boolean</code> value just represents
     * whether an existed elements kick out after {@link #MAX_KICK_OUT_TIMES} relocation times or not</p> 
	 * @return true if an existed elements kick out of cuckoo filter after {@link #MAX_KICK_OUT_TIMES}
	 * 			relocation times, otherwise false
	 */
	public boolean put(T element) {
		int fingerprintLength = buckets.fingerprintLength;
		byte[] fingerprint = flipIfAllZero(Hashing.hash(element, fingerprintLength).asBytes(), fingerprintLength);
		int index = Hashing.hash(element, 32).asInt();
		int alternate = index ^ hashFingerprint(fingerprint, fingerprintLength);
		if(tryPut(fingerprintLength, fingerprint, index, alternate)) {
			numOfExistedElements++;
			return false;
		}
		boolean hasBeenKickout = kickout(fingerprintLength, fingerprint, rand.nextBoolean() ? index : alternate);
		if(hasBeenKickout) { numOfKickoutElements++; }else { numOfExistedElements++; }
		return hasBeenKickout;
	}
	
	private static byte[] flipIfAllZero(byte[] fingerprint, int fingerprintLength) {
		return isAllZero(fingerprint, fingerprintLength) ? flipToOne(fingerprint) : fingerprint;
	}
	
	private static boolean isAllZero(byte[] fingerprint, int fingerprintLength) {
		int size = fingerprintLength >>> 3;		// fingerprintLength / 8
		int remain = fingerprintLength & 0x07;	// fingerprintLength % 8
		for(int i=0; i<size; i++) {
			if(fingerprint[i] != 0) { return false; }
		}
		if(remain != 0) {
			int mask = (1 << remain) - 1;
			return (fingerprint[size] & mask) == 0;
		}
		return true;
	}
	
	private static byte[] flipToOne(byte[] fingerprint) { fingerprint[0] = 1; return fingerprint; }
	
	private static int hashFingerprint(byte[] fingerprint, int fingerprintLength) {
		int h = 0;
		int size = (fingerprintLength - 1) / 32 + 1;
		switch(size) {
		case 0: break;
		case 1:
			int intVal = HashCode.asInt(fingerprint) & (0xffffffff >>> (32-fingerprintLength));
			/*
			 * MurmurHash3 was written by Austin Appleby, and is placed in the public domain. The author
			 * hereby disclaims copyright to this source code.
			 */
			h = 0x1b873593 * Integer.rotateLeft(intVal * 0xcc9e2d51, 15);
			break;
		default:
			int index = 0;
			for(int i=1; i<size; i++) {
				h ^= HashCode.asInt(fingerprint, index);
				index += 4;
				fingerprintLength -= 32;
			}
			h ^= (HashCode.asInt(fingerprint, index) & (0xffffffff >>> (32-fingerprintLength)));
		}
		return h == 0 ? 0xffffffff : h;		// index ^ h = index if h=0; index ^ h = ~index if h=0xffffffff
	}
	
	/** try to put fingerprint into primary or alternate bucket, fail if they all have no empty entry */
	private boolean tryPut(int fingerprintLength, byte[] fingerprint, int index, int alternate) {
		return buckets.put(fingerprint, index)
			|| buckets.put(fingerprint, alternate);
	}
	
	/**
	 * kick out an entry at {@code index} bucket. if the relocated bucket storing kick out fingerprint
	 * also has no empty entry, iterate the kick out operation until the kick out times over the
	 * {@link #MAX_KICK_OUT_TIMES}
	 * @return	true if an existed elements kick out of cuckoo filter after {@link #MAX_KICK_OUT_TIMES}
	 * 			relocation times, otherwise false
	 */
	private boolean kickout(int fingerprintLength, byte[] fingerprint, int index) {
		int i = 0;
		do {
			fingerprint = buckets.putOrReplace(fingerprint, index);
			if(fingerprint == null) { break; }
			i++;
			index = index ^ hashFingerprint(fingerprint, fingerprintLength);
		}while(i < MAX_KICK_OUT_TIMES);
		return i == MAX_KICK_OUT_TIMES;
	}
	
	/**
	 * return {@code true} if the element <i>might</i> have been put in this Cuckoo filter,
     * {@code false} if this is <i>definitely</i> not the case.
	 */
	public boolean mightContain(T element) {
		int fingerprintLength = buckets.fingerprintLength;
		byte[] fingerprint = flipIfAllZero(Hashing.hash(element, fingerprintLength).asBytes(), fingerprintLength);
		int index = Hashing.hash(element, 32).asInt();
		int alternate = index ^ hashFingerprint(fingerprint, fingerprintLength);
		return contain(fingerprintLength, fingerprint, index, alternate);
	}
	
	/** whether the {@code fingerprint} exists in primary and alternate bucket or not */
	private boolean contain(int fingerprintLength, byte[] fingerprint, int index, int alternate) {
		return (buckets.contain(fingerprint, index)
			 || buckets.contain(fingerprint, alternate));
	}
	
	/**
	 * remove the {@code element} from this {@link CuckooFilter}, ensure that subsequent invocations of
     * {@link #mightContain(element)} with the same element will always return {@code false}.
	 * @return true if the {@code element} definitely removed from this cuckoo filter after this operation.
	 * otherwise false, representing the {@code element} isn't exist into the filter.
	 * <p>note that although return {@code true}, subsequent invocations of {@link #mightContain(element)}
	 * with the same element might return {@code true}.</p>
	 */
	public boolean remove(T element) {
		int fingerprintLength = buckets.fingerprintLength;
		byte[] fingerprint = flipIfAllZero(Hashing.hash(element, fingerprintLength).asBytes(), fingerprintLength);
		int index = Hashing.hash(element, 32).asInt();
		int alternate = index ^ hashFingerprint(fingerprint, fingerprintLength);
		return remove(fingerprintLength, fingerprint, index, alternate);
	}
	
	/** remove the {@code fingerprint} if it exists in primary and alternate bucket, return true if removed, otherwise false  */
	private boolean remove(int fingerprintLength, byte[] fingerprint, int index, int alternate) {
		boolean hasBeenRemoved = (buckets.remove(fingerprint, index)
		      || buckets.remove(fingerprint, alternate));
		if(hasBeenRemoved) { numOfExistedElements--; }
		return hasBeenRemoved;
	}
	
	/** return number of existed elements in this CuckooFilter **/
	public int numOfExistedElements() { return numOfExistedElements; }
	
	/** return the number of kick out elements after {@link #MAX_KICK_OUT_TIMES} relocation times **/
	public int numOfKickoutElements() { return numOfKickoutElements; }
	
	/**
	 * create a {@link BloomFilter BloomFilter<T>} with the expected number of element and
     * expected false positive probability.
     * <p>note that the saturation of {@link BloomFilter} with significantly more elements
     * than expected, will result in a sharp degeneration of its false positive probability</p>
	 * @param expectedCapacity 	expected number of elements
	 * @param fpp	expected false positive probability
	 * @return	a {@link BloomFilter}
	 */
	public static <T> CuckooFilter<T> create(int expectedCapacity, double fpp) {
		if(expectedCapacity <= 0) {
			throw new IllegalArgumentException(String.format("expectedCapacity (%d) must be > 0", expectedCapacity));
		}
		if(fpp <= 0) {
			throw new IllegalArgumentException(String.format("fpp (%d) must be > 0", fpp));
		}
		if(fpp >= 1) {
			throw new IllegalArgumentException(String.format("fpp (%d) must be < 1", fpp));
		}
		
		int fingerprintLength = optimalFingerprintLength(EMPIRICAL_BUCKET_SIZE, fpp);
		int numOfBuckets = numOfBuckets(expectedCapacity, EMPIRICAL_BUCKET_SIZE);
		return new CuckooFilter<T>(numOfBuckets, EMPIRICAL_BUCKET_SIZE, fingerprintLength);
	}
	
	/*
	 * optimal computation sheet for cuckoo filter
	 * f - fingerprint length in bits
	 * a - load factor(0 < a < 1)
	 * b - number of entries per bucket
	 * m - number of buckets
	 * n - number of elements
	 * p - false positive probability
	 * C - bits per elements
	 * 
	 * conclusion:
	 * 1) empirical evaluation: larger bucket size improve load factor;
	 *    b = 1 -> a = 50%
	 *    b = 2 -> a = 84%
	 *    b = 4 -> a = 95%
	 *    b = 8 -> a = 98%
	 *    larger bucket size require longer fingerprints to retain the same false positive probability;
	 *    empirical evaluation suggests that the cuckoo filter which uses b = 4, will perform
	 *    well for a wide range of application
	 * 2) f >= ln(2b/p) / ln2
	 * 3) C = f / a
	 * 2) given b, the optimal f = ln(2b/p) / ln2
	 * 3) given b, n, m = round(n / b)
	 * 4) comparison with bloom filter, to retain the same p, while p < 0.6%, cuckoo filter need fewer bits elements
	 * 
	 * more detail of empirical evaluation and derivation process see http://www.cs.cmu.edu/~binfan/papers/conext14_cuckoofilter.pdf
	 */
	
	private static final int EMPIRICAL_BUCKET_SIZE = 4;
	
	/** compute the f (fingerprint length in bits), given the empirical number of entries in bucket and expected
	 * false positive probability
	 */
	private static final int optimalFingerprintLength(int b, double p) {
		return (int) Math.round(1 + Math.log(b / p) / Math.log(2));
	}
	
	/**
	 * compute the m (number of buckets), given the number of elements and the empirical number of entries in bucket
	 * <p>note that to ensure the kick out strategy the minimum number of buckets must be 2</p>
	 */
	private static final int numOfBuckets(int n, int b) {
		return Math.max(2, Math.round(n / b));
	}
	
	private static abstract class Buckets {
		
		/** a random instance use to select kick out entry **/
		protected static final Random RAND = new Random();
		
		/** used to shift left or right for a partial segment mask */
		protected static final long SEGMENT_MASK = 0xffffffffffffffffL;
		
		/** bit array **/
		protected final long[] bits;
		/** number of buckets **/
		protected final int numOfBuckets;
		/** number of entries **/
		protected final int numOfEntries;
		/** length of fingerprint in bits **/
		protected final int fingerprintLength;
		/** bit length of per bucket **/
		protected final int bitLengthOfBucket;
		
		static Buckets create(int numOfBuckets, int numOfEntries, int fingerprintLength) {
			if(exceedCapacity(numOfBuckets, numOfEntries, fingerprintLength)) {
				throw new IllegalArgumentException(String.format("too larger numOfBuckets (%d) or numOfEntries (%d) or fingerprintLength (%d)",
						numOfBuckets, numOfEntries, fingerprintLength));
			}
			return fingerprintLength <= 64 ? new SmallFingerprintBuckets(numOfBuckets, numOfEntries, fingerprintLength) :
				new LargerFingerprintBuckets(numOfBuckets, numOfEntries, fingerprintLength);
		}

		private static boolean exceedCapacity(int numOfBuckets, int numOfEntries, int fingerprintLength) {
			long bitLength = ((long)numOfBuckets) * numOfEntries * fingerprintLength;
			long size = (bitLength - 1) / 64 + 1;
			return size > (long)Integer.MAX_VALUE;
		}
		
		protected Buckets(int numOfBuckets, int numOfEntries, int fingerprintLength) {
			int bitLengthOfBucket = numOfEntries * fingerprintLength;
			long bitLength = ((long)numOfBuckets) * bitLengthOfBucket;
			int size = (int) ((bitLength - 1) / 64 + 1);
			this.bits = new long[size];
			this.numOfBuckets = numOfBuckets;
			this.numOfEntries = numOfEntries;
			this.fingerprintLength = fingerprintLength;
			this.bitLengthOfBucket = bitLengthOfBucket;
		}
		
		/** put the fingerprint into this bucket, return {@code true} if success, otherwise {@code false} */
		final boolean put(byte[] fingerprint, int startBucketIndex) {
			long startBitIndex = bitIndex(startBucketIndex);
			for(int i=0; i<numOfEntries; i++) {
				if(putAt(fingerprint, startBitIndex)) { return true; }
				startBitIndex += fingerprintLength;
			}
			return false;
		}
		
		private int bitIndex(int startBucketIndex) {
			startBucketIndex = Math.abs(startBucketIndex % numOfBuckets);
			return startBucketIndex * bitLengthOfBucket;
		}
		
		protected abstract boolean putAt(byte[] fingerprint, long startBitIndex);
		
		/** put the fingerprint into this bucket, return the replaced fingerprint if bucket is full, otherwise {@code null}  */
		final byte[] putOrReplace(byte[] fingerprint, int startBucketIndex) {
			long startBitIndex = bitIndex(startBucketIndex);
			long index = startBitIndex;
			for(int i=0; i<numOfEntries; i++) {
				if(putAt(fingerprint, index)) { return null; }
				index += fingerprintLength;
			}
			return replaceAt(fingerprint, startBitIndex + RAND.nextInt(numOfEntries)*fingerprintLength);
		}
		
		protected abstract byte[] replaceAt(byte[] fingerprint, long startBitIndex);
		
		boolean contain(byte[] fingerprint, int startBucketIndex) {
			long startBitIndex = bitIndex(startBucketIndex);
			for(int i=0; i<numOfEntries; i++) {
				if(existAt(fingerprint, startBitIndex)) { return true; }
				startBitIndex += fingerprintLength;
			}
			return false;
		}
		
		protected abstract boolean existAt(byte[] fingerprint, long startBitIndex);
		
		boolean remove(byte[] fingerprint, int startBucketIndex) {
			long startBitIndex = bitIndex(startBucketIndex);
			for(int i=0; i<numOfEntries; i++) {
				if(existAt(fingerprint, startBitIndex)) {
					clearAt(startBitIndex);
					return true;
				}
				startBitIndex += fingerprintLength;
			}
			return false;
		}
		
		protected abstract void clearAt(long startBitIndex);
	}
	
	/** fingerprintLength <= 64 **/
	private static final class SmallFingerprintBuckets extends Buckets {

		SmallFingerprintBuckets(int numOfBuckets, int numOfEntries, int fingerprintLength) {
			super(numOfBuckets, numOfEntries, fingerprintLength);
		}
		
		@Override
		protected boolean putAt(byte[] fingerprint, long startBitIndex) {
			int segmentIndex = segmentIndex(startBitIndex);
			int offset = offset(startBitIndex);
			long segment = bits[segmentIndex];
			
			int endOffset = offset + fingerprintLength;
			if(endOffset <= 64) {
				long mask = (SEGMENT_MASK >>> (-endOffset)) & (SEGMENT_MASK << offset);
				if((segment & mask) != 0) { return false; }
				segment |= ((HashCode.asLong(fingerprint) << offset) & mask);
				bits[segmentIndex] = segment;
				return true;
			}else {		// cross segment
				long firstMask = SEGMENT_MASK << offset;
				long secondMask = SEGMENT_MASK >>> (-endOffset);
				long secondSegment = bits[segmentIndex+1];
				if((segment & firstMask) != 0 || (secondSegment & secondMask) != 0) { return false; }
				long f = HashCode.asLong(fingerprint);
				segment |= (f << offset);
				secondSegment |= (f << (-fingerprintLength) >>> (-endOffset));
				bits[segmentIndex] = segment;
				bits[segmentIndex+1] = secondSegment;
				return true;
			}
		}
		
		private int segmentIndex(long bitIndex) { return (int) (bitIndex >>> 6);	}	// startBitIndex / 64
		
		private int offset(long bitIndex) { return (int) (bitIndex & 0x3fL); }	// startBitIndex % 64
		
		@Override
		protected byte[] replaceAt(byte[] fingerprint, long startBitIndex) {
			int segmentIndex = segmentIndex(startBitIndex);
			int offset = offset(startBitIndex);
			long segment = bits[segmentIndex];
			
			int endOffset = offset + fingerprintLength;
			if(endOffset <= 64) {
				long mask = (SEGMENT_MASK >>> (-endOffset)) & (SEGMENT_MASK << offset);
				long replaced = (segment & mask) >>> offset;
				segment &= (~mask);		// clear
				segment |= ((HashCode.asLong(fingerprint) << offset) & mask);	// set
				bits[segmentIndex] = segment;
				return toBytes(replaced, fingerprint);
			}else {		// cross segment
				long firstMask = SEGMENT_MASK << offset;
				long secondMask = SEGMENT_MASK >>> (-endOffset);
				long secondSegment = bits[segmentIndex+1];
				long f = HashCode.asLong(fingerprint);
				long replaced = segment >>> offset;
				segment &= (~firstMask);	// clear
				segment |= (f << offset);	// set
				replaced |= (secondSegment & secondMask) << (-offset);
				secondSegment &= (~secondMask);	// clear
				secondSegment |= (f << (-fingerprintLength) >>> (-endOffset));	// set
				bits[segmentIndex] = segment;
				bits[segmentIndex+1] = secondSegment;
				return toBytes(replaced, fingerprint);
			}
		}
		
		private byte[] toBytes(long replaced, byte[] fingerprint) {
			replaced &= (SEGMENT_MASK >>> (-fingerprintLength));
			int size = ((fingerprintLength-1) >>> 3) + 1; 
			size = Math.min(size, fingerprint.length);
			switch(size) {
			case 8: fingerprint[7] = (byte) ((replaced >>> 56) & 0xffL);
			case 7: fingerprint[6] = (byte) ((replaced >>> 48) & 0xffL);
			case 6: fingerprint[5] = (byte) ((replaced >>> 40) & 0xffL);
			case 5: fingerprint[4] = (byte) ((replaced >>> 32) & 0xffL);
			case 4: fingerprint[3] = (byte) ((replaced >>> 24) & 0xffL);
			case 3: fingerprint[2] = (byte) ((replaced >>> 16) & 0xffL);
			case 2: fingerprint[1] = (byte) ((replaced >>> 8) & 0xffL);
			case 1: fingerprint[0] = (byte) (replaced & 0xffL);
			}
			return fingerprint;
		}
		
		@Override
		protected boolean existAt(byte[] fingerprint, long startBitIndex) {
			int segmentIndex = segmentIndex(startBitIndex);
			int offset = offset(startBitIndex);
			long segment = bits[segmentIndex];
			
			int endOffset = offset + fingerprintLength;
			if(endOffset <= 64) {
				long mask = (SEGMENT_MASK >>> (-endOffset)) & (SEGMENT_MASK << offset);
				return (segment & mask) != 0;
			}else {		// cross segment
				long firstMask = SEGMENT_MASK << offset;
				long secondMask = SEGMENT_MASK >>> (-endOffset);
				long secondSegment = bits[segmentIndex+1];
				return (segment & firstMask) != 0 || (secondSegment & secondMask) != 0;
			}
		}
		
		@Override
		protected void clearAt(long startBitIndex) {
			int segmentIndex = segmentIndex(startBitIndex);
			int offset = offset(startBitIndex);
			long segment = bits[segmentIndex];
			
			int endOffset = offset + fingerprintLength;
			if(endOffset <= 64) {
				long mask = (SEGMENT_MASK >>> (-endOffset)) & (SEGMENT_MASK << offset);
				segment &= (~mask);
				bits[segmentIndex] = segment;
			}else {		// cross segment
				long firstMask = SEGMENT_MASK << offset;
				long secondMask = SEGMENT_MASK >>> (-endOffset);
				long secondSegment = bits[segmentIndex+1];
				segment &= (~firstMask);
				secondSegment &= (~secondMask);
				bits[segmentIndex] = segment;
				bits[segmentIndex+1] = secondSegment;
			}
		}
	}
	
	/** fingerprintLength > 64 **/
	private static final class LargerFingerprintBuckets extends Buckets {

		LargerFingerprintBuckets(int numOfBuckets, int numOfEntries, int fingerprintLength) {
			super(numOfBuckets, numOfEntries, fingerprintLength);
		}

		@Override
		protected boolean putAt(byte[] fingerprint, long startBitIndex) {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		protected byte[] replaceAt(byte[] fingerprint, long startBitIndex) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		protected boolean existAt(byte[] fingerprint, long startBitIndex) {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		protected void clearAt(long startBitIndex) {
			// TODO Auto-generated method stub
			
		}
		
	}
}
