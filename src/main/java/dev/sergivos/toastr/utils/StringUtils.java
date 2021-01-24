package dev.sergivos.toastr.utils;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class StringUtils {

    public static String joinList(List<String> list, String delimiter, int start) {
        if(list.size() < start) return "";

        return String.join(delimiter, list.subList(start - 1, list.size()));
    }

    public static String joinArray(CharSequence... array) {
        return String.join(", ", array);
    }

    public static String joinArray(Collection<String> array) {
        return String.join(", ", array);
    }

    public static String joinArray(String[] array, String delimiter, int start) {
        if(array.length < start) return "";

        return String.join(delimiter, Arrays.copyOfRange(array, start - 1, array.length));
    }

    public static boolean isNullOrEmpty(String value) {
        return value == null || value.isEmpty() || value.trim().isEmpty();
    }

    public static String capitalize(String string) {
        return String.valueOf(string.charAt(0)).toUpperCase() + string.substring(1);
    }

}
