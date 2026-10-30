package io.github.itech_framework.core.utils;

public class ObjectUtils {

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static Object convertValue(String value, Class<?> type) {
        if (value == null) {
            if (type.isPrimitive() || type.isEnum()) {
                throw new IllegalArgumentException(
                        "Cannot convert null to type " + type.getSimpleName());
            }
            return null;
        }

        if (type == String.class) return value;

        if (type == char.class || type == Character.class) {
            if (value.length() != 1) {
                throw new IllegalArgumentException(
                        "Cannot convert value '" + value + "' to type " + type.getSimpleName()
                                + ": char value must be exactly 1 character");
            }
            return value.charAt(0);
        }

        String text = value.trim();
        try {
            if (type == int.class || type == Integer.class) return Integer.parseInt(text);
            if (type == double.class || type == Double.class) return Double.parseDouble(text);
            if (type == long.class || type == Long.class) return Long.parseLong(text);
            if (type == boolean.class || type == Boolean.class) return parseBoolean(text);
            if (type == float.class || type == Float.class) return Float.parseFloat(text);
            if (type == short.class || type == Short.class) return Short.parseShort(text);
            if (type == byte.class || type == Byte.class) return Byte.parseByte(text);
            if (type.isEnum()) return Enum.valueOf((Class<Enum>) type, text);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Cannot convert value '" + value + "' to type " + type.getSimpleName(), e);
        }

        throw new UnsupportedOperationException(
                "Unsupported conversion to type: " + type.getName());
    }

    private static boolean parseBoolean(String text) {
        if ("true".equalsIgnoreCase(text)) return true;
        if ("false".equalsIgnoreCase(text)) return false;
        throw new IllegalArgumentException("Boolean value must be 'true' or 'false'");
    }
}