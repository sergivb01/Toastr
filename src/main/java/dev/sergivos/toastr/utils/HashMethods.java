package dev.sergivos.toastr.utils;

import com.google.common.hash.Hashing;

import java.nio.charset.StandardCharsets;

public class HashMethods {

    public static String SHA512H(String password, String salt) {
        String str = Hashing.sha512().hashString(password, StandardCharsets.UTF_8).toString();
        return Hashing.sha512().hashString(str + salt, StandardCharsets.UTF_8).toString();
    }

}
