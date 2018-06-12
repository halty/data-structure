package com.lee.data.structure;

public class MainTest {

    public static void main(String[] args) {
        int i = Integer.MIN_VALUE;
        int v = Integer.lowestOneBit(i);
        int c = Integer.numberOfTrailingZeros(i);
        System.out.println(v);
        System.out.println(c);
    }
}
