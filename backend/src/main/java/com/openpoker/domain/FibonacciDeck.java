package com.openpoker.domain;

import java.util.Set;

public class FibonacciDeck {
    public static final Set<String > values = Set.of("0", "1", "2", "3", "5", "8", "13", "21", "34", "55", "89", "?");

    public static boolean isValid(String value) {
        return values.contains(value);
    }
}
