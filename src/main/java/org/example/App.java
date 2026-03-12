package org.example;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class App {
    public static void main(String[] args) {
        log.info("Hello, JDK 25!");
    }

    public static int add(int a, int b) {
        return a + b;
    }
}
