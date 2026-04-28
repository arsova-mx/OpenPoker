package com.openpoker.domain;

import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class FibonacciDeck implements VotingDeck {
    public static final Set<String > values = Set.of("0", "1", "2", "3", "5", "8", "13", "21", "34", "55", "89", "?");

    @Override
    public boolean isValid(String value) {
        return values.contains(value);
    }
}
