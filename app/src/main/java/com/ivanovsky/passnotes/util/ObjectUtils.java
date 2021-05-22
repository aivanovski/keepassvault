package com.ivanovsky.passnotes.util;

public class ObjectUtils {

    public static boolean isEquals(Object first, Object second) {
        return (first == second) || (first != null && first.equals(second));
    }

    public static boolean isNotEquals(Object first, Object second) {
        return !isEquals(first, second);
    }
}
