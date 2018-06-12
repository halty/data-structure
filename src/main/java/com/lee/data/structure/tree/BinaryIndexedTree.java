package com.lee.data.structure.tree;

import java.util.Arrays;

/**
 * <pre>
 * 树状数组
 *
 * 解决的抽象问题：
 *   对于指定大小(N)的数组，对某个或某些区间元素执行指定更新操作OP(+、-等)，然后查询某个或某些区间元素的累加和SUM，主要分以下2种模式：
 *   (1) 更新单个元素，查询某个区间[begin, end)内元素的累加和；
 *   (2) 批量更新某个区间内[begin, end)的元素，查询某个元素的值；
 *
 * 思路：
 *   (1) 朴素思想；数据存储在一个数组，循环更新或循环累加求和，空间复杂度O(N)
 *     更新单个元素，时间复杂度O(1)，查询区间内元素累加和，时间复杂度O(N)
 *     批量更新区间内元素，时间复杂度O(N)，查询单个元素值，时间复杂度O(1)
 *
 *   (2) 树状数组；数据存储在一个数组，但数组每个索引位置并非完全存储单个元素值，而是利用数组索引的二进制信息，构造一个树状结构
 *   详细的思想可参考<a href=https://en.wikipedia.org/wiki/Fenwick_tree>Wiki:Binary_Indexed_Tree</a>
 *     更新单个元素，时间复杂度O(logN)，查询区间内元素累加和，时间复杂度O(logN)
 *     批量更新区间内元素，时间复杂度O(logN)，查询单个元素值，时间复杂度O(logN)
 * </pre>
 * @NotThreadSafe
 *
 */
public abstract class BinaryIndexedTree {

    /** 树状数据的2种应用模式 **/
    public enum Mode {
        /** 单元素更新，区间元素累加和 **/
        SINGLE_UPDATE_RANGE_SUM,
        /** 区间元素批量更新，单元素查询 **/
        RANGE_UPDATE_SINGLE_GET
    }

    /** 单元素更新，区间元素累加和 **/
    public static class SingleUpdateRangeSum extends BinaryIndexedTree {
        /*
         * 该种模式下，树状数组元素存储原始数组区间元素的累加和
         * 例如：树状数组索引为i的元素存储原始数组区间[b, e]的累加和，其中：
         *   e = i；
         *   b由i的二进制表示可计算出
         *   假设i的二进制表示中，末尾0的个数为k，则区间[b，e]的长度为2^k，则b=i-2^k+1
         *
         *  详细的复杂度分析见：<a href="http://www.cppblog.com/menjitianya/archive/2015/11/02/212171.html">树状数组</a>
         */

        SingleUpdateRangeSum(int capacity) {
            super(capacity, Mode.SINGLE_UPDATE_RANGE_SUM);
        }

        /** 指定{@code index}位置元素值增加{@code delta}，时间复杂度O(logN) **/
        public void increment(int index, int delta) {
            checkIndex(index);
            int naturalIndex = index + 1;
            do {
                elements[naturalIndex] += delta;
                int lowestBit = Integer.lowestOneBit(naturalIndex);
                naturalIndex += lowestBit;
            }while(naturalIndex <= capacity && naturalIndex > 0);
        }

        /** 返回指定区间[{@code begin}, {@code end})元素累加的和，区间为左闭右开，时间复杂度O(logN) **/
        public long sum(int begin, int end) {
            checkInterval(begin, end);
            return sumWithNaturalRange(begin+1, end);
        }

        /** 返回自然序区间[{@code beginNaturalIndex}, {@code endNaturalIndex})元素累加的和，区间为左闭右闭，时间复杂度O(logN) **/
        private long sumWithNaturalRange(int beginNaturalIndex, int endNaturalIndex) {
            long sum = prefixSum(endNaturalIndex);
            if(beginNaturalIndex > 1) {
                sum -= prefixSum(beginNaturalIndex-1);
            }
            return sum;
        }

        /** 返回自1起始到{@code endNaturalIndex}结束的前缀区间累加和，时间复杂度O(logN) **/
        private long prefixSum(int endNaturalIndex) {
            long sum = 0;
            do {
                sum += elements[endNaturalIndex];
                int lowestBit = Integer.lowestOneBit(endNaturalIndex);
                endNaturalIndex -= lowestBit;
            }while(endNaturalIndex > 0);
            return sum;
        }

        /**
         * {@inheritDoc}
         * 由于新增长空间需要额外初始化操作，但对于每个需初始化的元素：
         *   其初始化值，可由其树状结构的直属子节点相加得到；
         *   某个索引下标对应的元素，其直属子节点个数等于其索引二进制表示中末尾0的个数k<=logN；
         * 故可认为计算初始化元素值时间复杂度为O(logN)，故整体复杂度为O(NlogN)
         */
        @Override
        protected void initGrowthPortion(int beginNaturalIndex, int endNaturalIndex) {
            for(int i=beginNaturalIndex; i<=endNaturalIndex; i++) {
                int lowestBit = Integer.lowestOneBit(i);
                switch(lowestBit) {
                    case 1: elements[i] = 0; break;
                    case 2: elements[i] = elements[i-1]; break;
                    default:
                        int b = i - lowestBit + 1;
                        elements[i] = sumWithNaturalRange(b, i);
                }
            }
        }
    }

    /** 区间元素批量更新，单元素查询 **/
    public static class RangeUpdateSingleGet extends BinaryIndexedTree {
        /*
         * 该种模式下，树状数组元素存储原始数组区间元素的增量delta
         * 例如：树状数组索引为i的元素存储元素数组区间[b, e]的增量delta，其中：
         *   e = i；
         *   b由i的二进制表示可计算出
         *   假设i的二进制表示种，末尾0的个数为k，则区间[b，e]的长度为2^k，则b=i-2^k+1
         */

        RangeUpdateSingleGet(int capacity) {
            super(capacity, Mode.RANGE_UPDATE_SINGLE_GET);
        }

        /** 指定区间[{@code begin}, {@code end})元素值均增加{@code delta}，区间为左闭右开，时间复杂度O(logN) **/
        public void increment(int begin, int end, int delta) {
            checkInterval(begin, end);
            int beginNaturalIndex = begin + 1;
            int endNaturalIndex = end;
            prefixIncrement(endNaturalIndex, delta);
            if(beginNaturalIndex > 1) {
                prefixIncrement(beginNaturalIndex-1, -delta);
            }
        }

        /** 自1起始到{@code endNaturalIndex}结束的前缀区间元素值增加{@code delta} **/
        private void prefixIncrement(int endNaturalIndex, int delta) {
            do {
                elements[endNaturalIndex] += delta;
                int lowestBit = Integer.lowestOneBit(endNaturalIndex);
                endNaturalIndex -= lowestBit;
            }while(endNaturalIndex > 0);
        }

        /** 返回指定{@code index}位置元素值，时间复杂度O(logN) **/
        public long get(int index) {
            checkIndex(index);
            int naturalIndex = index + 1;
            long sum = 0;
            do {
                sum += elements[naturalIndex];
                int lowestBit = Integer.lowestOneBit(naturalIndex);
                naturalIndex += lowestBit;
            }while(naturalIndex <= capacity && naturalIndex > 0);
            return sum;
        }

        /**
         * {@inheritDoc}
         * 时间复杂度O(N)
         */
        @Override
        protected void initGrowthPortion(int beginNaturalIndex, int endNaturalIndex) {
            /*
             * 当前模式下，树状数组元素存储原始数组区间元素的增量delta
             * 扩容新增长的空间无任何增量，故默认值都为0，无需做额外初始化处理
             */
        }
    }

    /**
     * 树状数组支持的最大容量
     * 具体选择这个数值的考量可参考ArrayList的MAX_ARRAY_SIZE
     */
    public static final int MAX_CAPACITY = Integer.MAX_VALUE - 8;

    /**
     * 数据的存储采用自然序(以1为起点，其中首个元素{@code elements[0]}空置)
     * 主要考量：
     *   (1) 树状数组的树状结构是通过数组索引的下标来维护(而树状数组的索引下标是基于自然序)
     *   (2) 浪费1个元素存储空间，在数组长度非常大的场景下，空间的损耗率基本可忽略
     */
    protected long[] elements;
    /** 实际数据存储的容量{@code =elements.length-1} **/
    protected int capacity;
    private final Mode mode;

    /** 创建{@link Mode#SINGLE_UPDATE_RANGE_SUM}模式的固定容量为{@code capacity}的树状数组 **/
    public static SingleUpdateRangeSum createWithSURSMode(int capacity) {
        return new SingleUpdateRangeSum(capacity);
    }

    /** 创建{@link Mode#RANGE_UPDATE_SINGLE_GET}模式的固定容量为{@code capacity}的树状数组 **/
    public static RangeUpdateSingleGet createWithRUSGMode(int capacity) {
        return new RangeUpdateSingleGet(capacity);
    }

    private BinaryIndexedTree(int capacity, Mode mode) {
        checkCapacity(capacity);
        this.elements = new long[capacity+1];
        this.capacity = capacity;
        this.mode = mode;
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
        if(index < 0 || index >= capacity) {
            throw new IndexOutOfBoundsException("index must be >=0 and <"+capacity);
        }
    }

    /** [begin, end)为左闭右开区间 **/
    protected void checkInterval(int begin, int end) {
        if(begin < 0) {
            throw new IndexOutOfBoundsException("begin index must be >=0");
        }
        if(end > capacity) {
            throw new IndexOutOfBoundsException("end index must be less than or equals capacity="+capacity);
        }
        if(begin >= end) {
            throw new IllegalArgumentException("begin index must be less than end index");
        }
    }

    /** 返回树状数组应用模式 **/
    public Mode mode() {
        return mode;
    }

    /** 返回树状数组容量大小 **/
    public int capacity() {
        return capacity;
    }

    /** 扩容树状数组，如果当前数组容量{@code <minCapacity}，则进行扩容操作 **/
    public void ensureCapacity(int minCapacity) {
        int oldCapacity = capacity;
        if(oldCapacity >= minCapacity) { return; }
        checkCapacity(minCapacity);
        long[] newElements = Arrays.copyOf(elements, minCapacity+1);
        elements = newElements;
        capacity = minCapacity;
        initGrowthPortion(oldCapacity+1, minCapacity);
    }

    /** 扩容后，初始化新增长部分的元素空间[{@code beginNaturalIndex}, {@code endNaturalIndex}]，区间左闭右闭 **/
    protected abstract void initGrowthPortion(int beginNaturalIndex, int endNaturalIndex);
}