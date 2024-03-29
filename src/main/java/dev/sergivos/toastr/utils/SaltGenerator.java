package dev.sergivos.toastr.utils;


import java.security.SecureRandom;
import java.util.Random;

public class SaltGenerator {
    private static final Random RANDOM = new SecureRandom();
    private static final char[] CHARS = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();
    private static final char[] INTS = "0123456789".toCharArray();

    public static String generateString() {
        return generateNumbers(CHARS.length);
    }

    private static String generateNumbers(int size) {
        StringBuilder stringBuilder = new StringBuilder(10);
        for(byte b = 0; b < 10; b++)
            stringBuilder.append(CHARS[RANDOM.nextInt(size)]);
        return stringBuilder.toString();
    }

    private static String generateNumb(int size) {
        StringBuilder stringBuilder = new StringBuilder(size);
        for(byte b = 0; b < size; b++)
            stringBuilder.append(INTS[RANDOM.nextInt(9)]);
        return stringBuilder.toString();
    }

}