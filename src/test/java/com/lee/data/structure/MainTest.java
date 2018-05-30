package com.lee.data.structure;

public class MainTest {

    public static void main(String[] args) {
        Integer i = null;
        try {
            System.out.println(i.toString());
        }catch(Exception e) {
            System.err.println(e.getMessage());
        }
    }
}
