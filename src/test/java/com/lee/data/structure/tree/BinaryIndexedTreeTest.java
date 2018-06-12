package com.lee.data.structure.tree;

import java.util.Arrays;
import java.util.Random;

public class BinaryIndexedTreeTest {

    public static void main(String[] args) {
        int capacity = 1024 * 1024;
        int iteratorTimes = 1000 * 50;
        testSingleUpdateRangeSum(capacity, iteratorTimes);
        testRangeUpdateSingleGet(capacity, iteratorTimes);
    }

    private static void testSingleUpdateRangeSum(int capacity, int iteratorTimes) {
        BinaryIndexedTree.SingleUpdateRangeSum tree = BinaryIndexedTree.createWithSURSMode(capacity);
        RawArray array = new RawArray(capacity);
        Random r = new Random();
        boolean isOk = true;
        int half = iteratorTimes / 2;
        for(int i=0; i<iteratorTimes; i++) {
            // execute
            int index = r.nextInt(capacity);
            int delta = r.nextInt();
            tree.increment(index, delta);
            array.increment(index, delta);

            // check
            int begin = r.nextInt(capacity);
            int end = begin + 1 + r.nextInt(capacity-begin);
            long sum1 = tree.sum(begin, end);
            long sum2 = array.sum(begin, end);
            if(sum1 != sum2) {
                System.out.printf("while %d times iteration, BinaryIndexedTree's sum(%d, %d)=%d, RawArray's sum(%d, %d)=%d, not equal\n",
                 i+1, begin, end, sum1, begin, end, sum2);
                isOk = false;
                break;
            }

            // expand capacity
            if(i == half) {
                capacity = capacity * 3 / 2;
                tree.ensureCapacity(capacity);
                array.ensureCapacity(capacity);
            }
        }

        if(isOk) {
            System.out.printf("after %d times iteration, BinaryIndexedTree's each sum(begin, end) also equals to RawArray\n", iteratorTimes);
        }
    }

    private static void testRangeUpdateSingleGet(int capacity, int iteratorTimes) {
        BinaryIndexedTree.RangeUpdateSingleGet tree = BinaryIndexedTree.createWithRUSGMode(capacity);
        RawArray array = new RawArray(capacity);
        Random r = new Random();
        boolean isOk = true;
        int half = iteratorTimes / 2;
        for(int i=0; i<iteratorTimes; i++) {
            // execute
            int begin = r.nextInt(capacity);
            int end = begin + 1 + r.nextInt(capacity-begin);
            int delta = r.nextInt();
            tree.increment(begin, end, delta);
            array.increment(begin, end, delta);

            // check
            int index = r.nextInt(capacity);
            long value1 = tree.get(index);
            long value2 = array.get(index);
            if(value1 != value2) {
                System.out.printf("while %d times iteration, BinaryIndexedTree's get(%d)=%d, RawArray's get(%d)=%d, not equal\n",
                        i+1, index, value1, index, value2);
                isOk = false;
                break;
            }

            // expand capacity
            if(i == half) {
                capacity = capacity * 3 / 2;
                tree.ensureCapacity(capacity);
                array.ensureCapacity(capacity);
            }
        }
        if(isOk) {
            System.out.printf("after %d times iteration, BinaryIndexedTree's each get(index) also equals to RawArray\n", iteratorTimes);
        }
    }

    public static class RawArray {

        public static final int MAX_CAPACITY = Integer.MAX_VALUE - 8;

        private long[] elements;

        public RawArray(int capacity) {
            checkCapacity(capacity);
            elements = new long[capacity];
        }

        private static void checkCapacity(int capacity) {
            if(capacity <= 0) {
                throw new IllegalArgumentException("capacity must be a positive number");
            }
            if(capacity > MAX_CAPACITY) {
                throw new IllegalArgumentException("capacity exceed the max limit: "+MAX_CAPACITY);
            }
        }

        protected void checkIndex(int index) {
            if(index < 0 || index >= elements.length) {
                throw new IndexOutOfBoundsException("index must be >=0 and <"+elements.length);
            }
        }

        /** [begin, end)为左闭右开区间 **/
        protected void checkInterval(int begin, int end) {
            if(begin < 0) {
                throw new IndexOutOfBoundsException("begin index must be >=0");
            }
            if(end > elements.length) {
                throw new IndexOutOfBoundsException("end index must be less than or equals capacity="+elements.length);
            }
            if(begin >= end) {
                throw new IllegalArgumentException("begin index must be less than end index");
            }
        }

        /** 指定{@code index}位置元素值增加{@code delta}，时间复杂度O(1) **/
        public void increment(int index, int delta) {
            checkIndex(index);
            elements[index] += delta;
        }

        /** 返回指定区间[{@code begin}, {@code end})元素累加的和，区间为左闭右开，时间复杂度O(N) **/
        public long sum(int begin, int end) {
            checkInterval(begin, end);
            long sum = 0;
            for(int i=begin; i<end; i++) {
                sum += elements[i];
            }
            return sum;
        }

        /** 指定区间[{@code begin}, {@code end})元素值均增加{@code delta}，区间为左闭右开，时间复杂度O(N) **/
        public void increment(int begin, int end, int delta) {
            checkInterval(begin, end);
            for(int i=begin; i<end; i++) {
                elements[i] += delta;
            }
         }

        /** 返回指定{@code index}位置元素值，时间复杂度O(1) **/
        public long get(int index) {
            checkIndex(index);
            return elements[index];
        }

        /** 扩容树状数组，如果当前数组容量{@code <minCapacity}，则进行扩容操作 **/
        public void ensureCapacity(int minCapacity) {
            int oldCapacity = elements.length;
            if(oldCapacity >= minCapacity) { return; }
            checkCapacity(minCapacity);
            long[] newElements = Arrays.copyOf(elements, minCapacity);
            elements = newElements;
        }
    }
}
