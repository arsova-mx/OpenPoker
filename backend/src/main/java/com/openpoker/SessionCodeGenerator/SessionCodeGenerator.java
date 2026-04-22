package com.openpoker.SessionCodeGenerator;

import java.util.Random;

import org.springframework.stereotype.Component;

@Component
public class SessionCodeGenerator {
    private static final String CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int LENGTH = 6;
    private final Random random = new Random();
    
    public String generate() {
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < LENGTH; i++) {
            code.append(CHARS.charAt(random.nextInt(CHARS.length())));
        }
        return code.toString();
    }
}
