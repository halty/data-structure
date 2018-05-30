package com.lee.data.structure.buffer;

/**
 * A rolling char buffer based JVM Heap
 * @NotThreadSafe
 */
class RollingHeapCharBuffer extends RollingCharBuffer {

	private char[] buffer;

	RollingHeapCharBuffer(int capacity) {
		super();
		buffer = new char[capacity];
	}

	RollingHeapCharBuffer(char[] array) {
		super();
		buffer = array;
	}

	@Override
	protected int retCapacity() {
		return buffer.length;
	}

	@Override
	protected void ensureCapacity(int incCap) {
		if(incCap < remained()) { return; }
		
		int capacity = capacity();
		int newCapacity = 0;
		if(2*incCap <= capacity) {
			newCapacity = capacity * 3 / 2 + 1;
		}else {
			newCapacity = capacity * 2;
		}
		char[] newBuf = new char[newCapacity];

		if(size > 0) {
			if(putIndex > takeIndex) {
				System.arraycopy(buffer, takeIndex, newBuf, 0, size);
			}else {
				int len = buffer.length - takeIndex;
				System.arraycopy(buffer, takeIndex, newBuf, 0, len);
				if(size > len) { System.arraycopy(buffer, 0, newBuf, len, putIndex); }
			}
		}
		buffer = newBuf;
		takeIndex = 0;
		putIndex = size;
	}

	@Override
	protected void write(char ch) {
		buffer[putIndex] = ch;
	}

	@Override
	protected void write(char[] arr, int offset, int size) {
		int tailLen = buffer.length - putIndex;
		if(tailLen >= size) {
			System.arraycopy(arr, offset, buffer, putIndex, size);
		}else {
			System.arraycopy(arr, offset, buffer, putIndex, tailLen);
			System.arraycopy(arr, offset+tailLen, buffer, 0, size-tailLen);
		}
	}

	@Override
	protected char read() {
		return buffer[takeIndex];
	}

	@Override
	protected void read(char[] arr, int offset, int size) {
		if(putIndex <= takeIndex) {
			int len = buffer.length - takeIndex;
			if(size <= len) {
				System.arraycopy(buffer, takeIndex, arr, offset, size);
			}else {
				System.arraycopy(buffer, takeIndex, arr, offset, len);
				System.arraycopy(buffer, 0, arr, offset+len, size-len);
			}
		}else {
			System.arraycopy(buffer, takeIndex, arr, offset, size);
		}
	}

	@Override
	protected boolean isArrayBacked() { return true; }

	@Override
	protected char[] backendArray() { return buffer; }

	@Override
	protected void clean() {
		resetIndex();
		if(buffer != null) { buffer = null; }
	}

}
