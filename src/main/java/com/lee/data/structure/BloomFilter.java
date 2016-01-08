package com.lee.data.structure;

import com.lee.util.HashCode;
import com.lee.util.Hashing;

/**
 * see more detail of <a href="https://en.wikipedia.org/wiki/Bloom_filter">Bloom Filter</a>
 * @NotThreadSafe
 **/
public final class BloomFilter<T> {
	
	/** the max permitted number of hashes per element **/
	private static final int MAX_NUM_OF_HASH_FUNCTION = 256;
	
	/** the number of bits per hash code of element **/
	private static final int NUM_OF_BIT_PER_HASH_CODE = Long.SIZE * 2;

	/** the bit set for BloomFilter **/
	private final Bits bits;
	
	/** number of hashes per element **/
	private final int numOfHashFunctions;
	
	private BloomFilter(Bits bits, int numOfHashFunctions) {
		if(bits == null) {
			throw new NullPointerException("underlying bit set is null");
		}
		if(numOfHashFunctions <= 0) {
			throw new IllegalArgumentException(String.format("numOfHashFunctions (%d) must be > 0", numOfHashFunctions));
		}
		if(numOfHashFunctions > MAX_NUM_OF_HASH_FUNCTION) {
			throw new IllegalArgumentException(String.format("numOfHashFunctions (%d) must be <= %d", numOfHashFunctions, MAX_NUM_OF_HASH_FUNCTION));
		}
		this.bits = bits;
		this.numOfHashFunctions = numOfHashFunctions;
	}
	
	/**
	 * put an element into this {@link BloomFilter}, ensure that subsequent invocations of
     * {@link #mightContain(element)} with the same element will always return {@code true}.
	 * @return true if the {@link #bits} back of bloom filter changed after this operation.
	 * If the bits changed, this is the first time {@code element} which has been added to the filter.
	 * <p>note that although return {@code false}, subsequent invocations of {@link #mightContain(element)}
	 * with the same element might return {@code true}.</p>
	 */
	public boolean put(T element) {
		HashCode hashCode = Hashing.hash(element, NUM_OF_BIT_PER_HASH_CODE);
		boolean bitsChanged = false;
		long hash = hashCode.asNthLong(1), hash2 = hashCode.asNthLong(2);
		for(int i=0; i<numOfHashFunctions; i++) {
			bitsChanged |= bits.set(hash);
			hash += hash2;
		}
		return bitsChanged;
	}
	
	/**
	 * return {@code true} if the element <i>might</i> have been put in this Bloom filter,
     * {@code false} if this is <i>definitely</i> not the case.
	 */
	public boolean mightContain(T element) {
		HashCode hashCode = Hashing.hash(element, NUM_OF_BIT_PER_HASH_CODE);
		long hash = hashCode.asNthLong(1), hash2 = hashCode.asNthLong(2);
		for(int i=0; i<numOfHashFunctions; i++) {
			if(!bits.get(hash)) { return false; }
			hash += hash2;
		}
		return true;
	}
	
	/**
	 * return current probability that {@link #mightContain(element)} will erroneously return {@code true}
     * for an element that has not actually been put in the {@link BloomFilter}.
	 * <p>Ideally, this number should be close to the {@code fpp} parameter passed in
     * {@link #create(int, double)}, or smaller. If it is significantly higher, it is usually
     * the case that too many elements (more than expected) have been put in the
     * {@link BloomFilter}. preventing the {@code fpp} degeneration, maybe you need recreate
     * an new {@link BloomFilter} with bigger {@code expectedCapacity}.</P
	 */
	public double currentFpp() {
		return Math.pow(((double)bits.bitCount) / bits.bitSize, numOfHashFunctions);
	}
	
	/**
	 * create a {@link BloomFilter BloomFilter<T>} with the expected number of element and
     * expected false positive probability.
     * <p>note that the saturation of {@link BloomFilter} with significantly more elements
     * than expected, will result in a sharp degeneration of its false positive probability</p>
	 * @param expectedCapacity 	expected number of elements
	 * @param fpp	expected false positive probability
	 * @return	a {@link BloomFilter}
	 */
	public static <T> BloomFilter<T> create(int expectedCapacity, double fpp) {
		if(expectedCapacity <= 0) {
			throw new IllegalArgumentException(String.format("expectedCapacity (%d) must be > 0", expectedCapacity));
		}
		if(fpp <= 0) {
			throw new IllegalArgumentException(String.format("fpp (%d) must be > 0", fpp));
		}
		if(fpp >= 1) {
			throw new IllegalArgumentException(String.format("fpp (%d) must be < 1", fpp));
		}
		int numOfHashFunctions = optimalNumOfHashFunctions(fpp);
		long optimalTotalNumOfBits = optimalTotalNumOfBits(expectedCapacity, fpp);
		
		return new BloomFilter<T>(new Bits(optimalTotalNumOfBits), numOfHashFunctions);
	}
	
	/*
	 * optimal computation sheet for bloom filter
	 * e - exponential factor
	 * m - total bits
	 * n expected number of elements
	 * b=m/n - bits per elements
	 * p - expected false positive probability
	 * 
	 * conclusion:
	 * 1) optimal k = ln2 * b (number of hashes per element)
	 * 2) p = (1 - e ^ (-k/b)) ^ k
	 * 3) for optimal k, p = 2 ^ (-k)
	 * 4) for optimal k, m = -n * lnp / ((ln2) ^ 2)
	 * 
	 * derivation process see https://en.wikipedia.org/wiki/Bloom_filter
	 */
	
	/** compute the optimal k (number of hashes per element), given the expected false positive probability **/
	private static int optimalNumOfHashFunctions(double p) {
		/* for optimal k, p = 2 ^ (-k) => k = -lnp / ln2 */
		return Math.max(1, (int) Math.round(-Math.log(p) / Math.log(2)));
	}
	
	/** compute the m (total bits of bloom filter), given the expected number of elements and expected
	 * false positive probability
	 */
	private static long optimalTotalNumOfBits(long n, double p) {
		return Math.max(1, Math.round(-n * Math.log(p) / (Math.log(2) * Math.log(2))));
	}
	
	/** use this instead of {@link java.util.BitSet} because BitSet just support most {@link Integer#MAX_VALUE} bits **/
	private static final class Bits {
		static final long MAX_BIT_CAPACITY = ((long)Integer.MAX_VALUE) * Long.SIZE;
		final long[] words;
		final long bitSize;
		long bitCount;	// total number of set bits (binary flag = 1)
		
		Bits(long capacity) {
			if(capacity > MAX_BIT_CAPACITY) { capacity = MAX_BIT_CAPACITY; }
			words = new long[(int)((capacity-1) / Long.SIZE)+1];
			bitSize = words.length * Long.SIZE;
		}
		
		boolean set(long index) {
			index = ensureIndex(index);
			if(!bGet(index)) {
				bSet(index);
				bitCount++;
				return true;
			}
			return false;
		}
		
		private long ensureIndex(long index) { return (index & Long.MAX_VALUE) % bitSize; }
		
		private boolean bGet(long index) { return (words[(int)(index >>> 6)] & (1L << index)) != 0; }
		
		private void bSet(long index) { words[(int)(index >>> 6)] |= (1L << index); }
		
		boolean get(long index) {
			return bGet(ensureIndex(index));
		}
	}
}
