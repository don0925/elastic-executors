package com.don.elastic.executors.util;

/**
 * 断言工具
 * @author Don Du
 */
public abstract class Asserts {

    public static void notNull(Object object, String message) {
        if (object == null) {
            throw new IllegalArgumentException(message);
        }
    }

    public static void isFalse(boolean value, String message) {
        if (value) {
            throw new IllegalArgumentException(message);
        }
    }

    public static void notEmpty(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(message);
        }
    }

}
