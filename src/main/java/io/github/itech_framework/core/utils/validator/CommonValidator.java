package io.github.itech_framework.core.utils.validator;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.regex.Pattern;

public class CommonValidator {
	
	private static final Pattern NUMERIC = Pattern.compile("[+-]?(\\d+(\\.\\d*)?|\\.\\d+)([eE][+-]?\\d+)?");
	
    public static boolean isValidObject(Object obj) {
        return obj != null;
    }

    public static boolean isValidDate(String dateStr, String dateFormat) {
        if (dateStr == null || dateFormat == null) {
            return false;
        }
        try {
            DateFormat df = new SimpleDateFormat(dateFormat);
            df.setLenient(false);
            ParsePosition position = new ParsePosition(0);
            Date parsed = df.parse(dateStr, position);
            return parsed != null && position.getIndex() == dateStr.length();
        } catch (IllegalArgumentException e) {
            return false;
        }
    }


    public static boolean validInteger(Integer value) {
        return value != null && value > 0;
    }

    public static boolean validNumber(Number value) {
        return value != null && value.floatValue() > 0;
    }

    public static boolean validLong(Long value) {
        return value != null && value > 0;
    }

    public static boolean validDouble(Double value) {
        return value != null && value > 0;
    }

    public static boolean validBigDecimal(BigDecimal value) {
        return value != null && value.compareTo(BigDecimal.ZERO) > 0;
    }

    public static boolean validString(String value) {
        return value != null && !value.trim().isEmpty();
    }

    public static boolean validList(Collection<?> value) {
        return value != null && !value.isEmpty();
    }

    public static boolean isNumeric(String strNum) {
        return !isEmpty(strNum) && NUMERIC.matcher(strNum.trim()).matches();
    }

    public static boolean isValidLongValue(String strLong) {
        if (isEmpty(strLong)) {
            return false;
        }
        try {
            Long.parseLong(strLong);
        } catch (NumberFormatException nfe) {
            return false;
        }
        return true;
    }

    public static boolean isEmpty(String str) {
        return str == null || str.trim().isEmpty() || str.isEmpty();
    }

}
