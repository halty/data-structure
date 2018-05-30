package com.lee.data.structure.buffer;

import static com.lee.data.structure.util.PlatformDependent.*;
import sun.misc.Cleaner;

/**
 * A rolling char buffer based OS's memory
 * @NotThreadSafe
 */
class RollingDirectCharBuffer extends RollingCharBuffer {

	private MemoryBlock block;	// memory block to store characters
	private int capacity;		// capacity, in chars
	
	RollingDirectCharBuffer(int capacity) {
		super();
		block = MemoryBlock.allocate(capacity);
		this.capacity = capacity;
	}

	@Override
	protected int retCapacity() { return capacity; }

	@Override
	protected void ensureCapacity(int incCap) {
		if(incCap < remained()) { return; }
		
		int newCapacity = 0;
		if(2*incCap <= capacity) {
			newCapacity = capacity * 3 / 2 + 1;
		}else {
			newCapacity = capacity * 2;
		}
		
		MemoryBlock newBlock = MemoryBlock.allocate(newCapacity);
		if(size > 0) {
			long oldAddress = block.address;
			long newAddress = newBlock.address;
			if(putIndex > takeIndex) {
				copyMemory(oldAddress, takeIndex, newAddress, 0, size);
			}else {
				int len = capacity - takeIndex;
				copyMemory(oldAddress, takeIndex, newAddress, 0, len);
				if(size > len) {
					copyMemory(oldAddress, 0, newAddress, len, putIndex);
				}
			}
		}
		
		MemoryBlock.deallocate(block);	// clean old memory
		block = newBlock;
		capacity = newCapacity;
		takeIndex = 0;
		putIndex = size;
	}

	@Override
	protected void write(char ch) {
		writeChar(block.address, putIndex, ch);
	}

	@Override
	protected void write(char[] arr, int offset, int size) {
		int tailLen = capacity - putIndex;
		long address = block.address;
		if(size <= tailLen) {
			writeChars(address, putIndex, arr, offset, offset+size);
		}else {
			writeChars(address, putIndex, arr, offset, offset+tailLen);
			writeChars(address, 0, arr, offset+tailLen, offset+size);
		}
	}

	@Override
	protected char read() {
		return readChar(block.address, takeIndex);
	}

	@Override
	protected void read(char[] arr, int offset, int size) {
		long address = block.address;
		if(putIndex <= takeIndex) {
			int tailLen = capacity - takeIndex;
			if(size <= tailLen) {
				readChars(address, takeIndex, arr, offset, offset+size);
			}else {
				readChars(address, takeIndex, arr, offset, offset+tailLen);
				readChars(address, 0, arr, offset+tailLen, offset+size);
			}
		}else {
			readChars(address, takeIndex, arr, offset, offset+size);
		}
	}

	@Override
	protected boolean isArrayBacked() { return false; }

	@Override
	protected char[] backendArray() { throw new UnsupportedOperationException("rolling direct char buffer"); }

	private static class MemoryBlock {
		private final Cleaner cleaner;	// finalize Cleaner clean the memory to prevent memory leak
		private final long address;		// start address of memory block

		MemoryBlock(int capacity) {
			address = allocateMemory(capacity);
			cleaner = Cleaner.create(this, new Deallocator(address));
		}
		
		static MemoryBlock allocate(int capacity) {
			return new MemoryBlock(capacity);
		}
		
		static void deallocate(MemoryBlock block) {
			if(block != null) {
				block.cleaner.clean();
			}
		}
	}
	
	private static class Deallocator implements Runnable {
		private long address;
		private Deallocator(long address) { this.address = address; }
		public void run() {
			if(address == 0) { return; }
			freeMemory(address);
			address = 0;
		}
	}

	@Override
	protected void clean() {
		resetIndex();
		capacity = 0;
		if(block != null) {
			MemoryBlock.deallocate(block);
			block = null;
		}
	}

}
