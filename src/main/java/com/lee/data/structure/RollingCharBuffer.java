package com.lee.data.structure;

import static com.lee.data.structure.util.PlatformDependent.hasUnsafe;

/** Non thread safe buffer **/
public abstract class RollingCharBuffer {

	private boolean isReleased;	// mark this buffer whether released by caller or not
	protected int putIndex;
	protected int takeIndex;
	protected int size;

	/**
	 * Allocate a rolling char buffer with fixed <code>capacity</code> size from JVM Heap.
	 * @param capacity	buffer's capacity, in chars
	 * @return	a rolling char buffer
	 * @throws	IllegalArgumentException
     *          If the <tt>capacity</tt> isn't a positive integer
	 */
	public final static RollingCharBuffer allocate(int capacity) {
		return allocate(capacity, false);
	}

	/**
	 * Allocate a new rolling char buffer with fixed <code>capacity</code> size.
	 * @param capacity	buffer's capacity, in chars
	 * @param isDirect	if true, allocate the buffer from OS's direct memory, otherwise,
	 * 					allocate it from JVM Heap.
	 * @return	a new rolling char buffer
	 * @throws	IllegalArgumentException
     *          If the <tt>capacity</tt> isn't a positive integer
	 */
	public final static RollingCharBuffer allocate(int capacity, boolean isDirect) {
		if(capacity < 1) {
			throw new IllegalArgumentException("buffer's capacity must be a positive integer.");
		}
		return isDirect && hasUnsafe() ? new RollingDirectCharBuffer(capacity) : new RollingHeapCharBuffer(capacity);
	}

	/**
     * Wraps a char array into a rolling buffer.
     *
     * <p> The new rolling buffer will be backed by the given character array;
     * that is, modifications to the buffer will cause the array to be modified
     * and vice versa.  The new buffer's capacity will be <tt>array.length</tt>.
     * Its {@link #array </code>backing array<code>} will be the
     * given array.  </p>
     *
     * @param  array
     *         The array that will back this buffer
     *
     * @return  The new rolling char buffer
     */
	public final static RollingCharBuffer wrap(char[] array) {
		if(array == null || array.length < 1) {
			throw new IllegalArgumentException("buffer's capacity must be a positive integer.");
		}
		return new RollingHeapCharBuffer(array);
	}

	protected RollingCharBuffer() { 
		isReleased = false;
		resetIndex();
	}

	/** don't change the internal buffer, just reset the position **/
	public final void reset() {
		checkReleased();
		resetIndex();
	}
	
	/** internal reset the read and write index **/
	protected final void resetIndex() {
		takeIndex = putIndex = size = 0;
	}

	/** next take index **/
	public final int takeIndex() {
		checkReleased();
		return takeIndex;
	}

	/** next put index **/
	public final int putIndex() {
		checkReleased();
		return putIndex;
	}

	/** capacity of buffer **/
	public final int capacity() {
		checkReleased();
		return retCapacity();
	}
	
	/** return the capacity of buffer **/
	protected abstract int retCapacity();

	public final int size() {
		checkReleased();
		return size;
	}

	public final boolean isEmpty() {
		checkReleased();
		return size == 0;
	}

	public final boolean isFull() {
		checkReleased();
		return size == capacity();
	}

	/** reserved free size **/
	public final int remained() {
		checkReleased();
		return capacity() - size;
	}

	/** 
	 * expand the capacity to satisfy the new added <code>incCap</code> chars.<br/>
	 * <code> new capacity >= old capacity + incCap </code>
	 */
	public final void expandCapacity(int incCap) {
		checkReleased();
		ensureCapacity(incCap);
	}
	
	/** ensure the buffer to satisfy the new added <code>incCap</code> chars, if not, expand itself **/
	protected abstract void ensureCapacity(int incCap);

	/** write one char, if remained capacity isn't enough, expand then write **/
	public final void put(char ch) {
		checkReleased();
		if(remained() <= 1) {
			ensureCapacity(1);
		}
		write(ch);
		shiftPutIndex(1);
	}

	/** write a char to buffer **/
	protected abstract void write(char ch);

	/** write char array, if remained capacity isn't enough, expand then write **/
	public final void put(char[] arr) {
		checkReleased();
		int len = arr.length;
		if(remained() <= len) {
			ensureCapacity(len);
		}
		write(arr, 0, len);
		shiftPutIndex(len);
	}
	
	/**
	 * write <code>size</code> chars from <code>arr</code> started with <code>offset</code>.
	 * if <code>arr</code> started with <code>offset</code> has no specified <code>size</code>
	 * characters, throw a {@link IllegalStateException}
	 */
	public final void put(char[] arr, int offset, int size) {
		checkReleased();
		if(arr == null || offset < 0 || (arr.length - offset) < size) {
			throw new IllegalStateException("array started with offset has no specified size characters");
		}
		
		if(remained() <= size) {
			ensureCapacity(size);
		}
		write(arr, offset, size);
		shiftPutIndex(size);
	}

	/** write <code>size</code> chars from <code>arr</code> started with <code>offset</code> **/
	protected abstract void write(char[] arr, int offset, int size);

	/** read one char.
	 * if buffer's reserved char size &lt; <code>1</code>,
	 * throw a {@link IllegalStateException}
	 **/
	public final char take() {
		checkReleased();
		if(size < 1) {
			throw new IllegalStateException("no character can be taken");
		}

		char ch = read();
		shiftTakeIndex(1);

		return ch;
	}

	/** read a char from buffer **/
	protected abstract char read();

	/** read <code>size</code> chars.
	 * if buffer's reserved char size &lt; <code>size</code>,
	 * throw a {@link IllegalStateException}
	 **/
	public final char[] take(int size) {
		checkReleased();
		if(size < 1 || size > this.size) {
			throw new IllegalStateException("taken size less than 1 or more characters taken than the buffer size");
		}

		char[] tmp = new char[size];
		read(tmp, 0, size);
		shiftTakeIndex(size);

		return tmp;
	}
	
	/**
	 * read <code>size</code> chars to <code>arr</code> started with <code>offset</code>.
	 * if buffer's reserved char size &lt; <code>size</code> or <code>arr</code>
	 * started with <code>offset</code> has no enough space to place <code>size</code> characters,
	 * throw a {@link IllegalStateException}
	 */
	public final void take(char[] arr, int offset, int size) {
		checkReleased();
		if(arr == null || offset < 0 || (arr.length - offset) < size) {
			throw new IllegalStateException("no enough array space to place the required characters");
		}
		if(size < 1 || size > this.size) {
			throw new IllegalStateException("taken size less than 1 or more characters taken than the buffer size");
		}
		
		read(arr, offset, size);
		shiftTakeIndex(size);
	}

	/** read <code>size</code> chars to <code>arr</code> started with <code>offset</code> **/
	protected abstract void read(char[] arr, int offset, int size);

	/** read all reserved chars **/
	public final char[] takeAll() {
		checkReleased();
		return take(size);
	}
	
	/** read all reserved chars to <code>buf</code> started with <code>offset</code> **/
	public final void takeAll(char[] buf, int offset) {
		checkReleased();
		take(buf, offset, size);
	}

	/** Tells whether or not this buffer is backed by an accessible character array. **/
	public final boolean hasArray() {
		checkReleased();
		return isArrayBacked();
	}
	
	/** Tells whether or not this buffer is backed by an accessible character array. **/
	protected abstract boolean isArrayBacked();

	/** Returns the internal character array that backs this buffer.
	 * <p> Modifications to this buffer's content will cause the returned
     * array's content to be modified, and vice versa.
     *
     * <p> Invoke the {@link #hasArray hasArray} method before invoking this
     * method in order to ensure that this buffer has an accessible backing
     * array.
     *
     * @throws  UnsupportedOperationException
     *          If this buffer is not backed by an accessible array
     */
	public final char[] array() {
		checkReleased();
		return backendArray();
	}
	
	/** Returns the backend character array **/
	protected abstract char[] backendArray();

	/** if you don't read the chars or external read by the {@link #array()} returned array, so just shift the take index **/
	public final void shiftTakeIndex(int step) {
		checkReleased();
		if(step > size) {
			throw new IllegalStateException("step exceed the max taken shifted steps");
		}
		takeIndex = (takeIndex + step) % capacity();
		size -= step;
	}

	/** if you external write by the {@link #array()} returned array, provided a interface to shift the put index.
	 * you must confirm this operation after the external write operation, and the <code>step</code>
	 * equal the written size of chars.
	 **/
	public final void shiftPutIndex(int step) {
		checkReleased();
		if(remained() < step) {
			throw new IllegalStateException("step exceed the max put shifted steps");
		}
		putIndex = (putIndex + step) % capacity();
		size += step;
	}

	/** 
	 * help GC to release the internal resources. 
	 * if you call this method, mark the release sign, then subsequent operation on this buffer
	 * will throw a {@link IllegalStateException}
	 */
	public final void release() {
		if(!isReleased) {
			clean();
			isReleased = true;
		}
	}
	
	/** if {@link #isReleased} mark was set, throw a {@link IllegalStateException} **/
	protected void checkReleased() {
		if(isReleased) {
			throw new IllegalStateException("buffer has been released.");
		}
	}
	
	/** subclass implement this method to clean their own resources **/
	protected abstract void clean();
}
