package io.github.itech_framework.core.utils;

public class ObjectUtils {
    @SuppressWarnings({ "unchecked", "rawtypes" })
	public static Object convertValue(String value, Class<?> type) {
        // Handle null input with type-specific defaults
        if (value == null) {
            if (type == String.class) return "";
            if (type == int.class || type == Integer.class) return 0;
            if (type == double.class || type == Double.class) return 0.0;
            if (type == long.class || type == Long.class) return 0L;
            if (type == boolean.class || type == Boolean.class) return false;
            if (type == float.class || type == Float.class) return 0.0f;
            if (type == short.class || type == Short.class) return (short) 0;
            if (type == byte.class || type == Byte.class) return (byte) 0;
            if (type == char.class || type == Character.class) return '\0';
            if (type.isEnum()) return type.getEnumConstants()[0];  // First enum value
            return null;  // For other object types
        }

        // Existing conversion logic for non-null values
        try {
            if (type == String.class) return value;
            if (type == int.class || type == Integer.class) return Integer.parseInt(value);
            if (type == double.class || type == Double.class) return Double.parseDouble(value);
            if (type == long.class || type == Long.class) return Long.parseLong(value);
            if (type == boolean.class || type == Boolean.class) return Boolean.parseBoolean(value);
            if (type == float.class || type == Float.class) return Float.parseFloat(value);
            if (type == short.class || type == Short.class) return Short.parseShort(value);
            if (type == byte.class || type == Byte.class) return Byte.parseByte(value);
            if (type == char.class || type == Character.class) {
                if (value.length() != 1) {
                    throw new IllegalArgumentException("Char value must be exactly 1 character");
                }
                return value.charAt(0);
            }
            if (type.isEnum()) return Enum.valueOf((Class<Enum>) type, value);
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Cannot convert value '" + value + "' to type " + type.getSimpleName(), e
            );
        }

        throw new UnsupportedOperationException(
                "Unsupported conversion to type: " + type.getName()
        );
    }
}
