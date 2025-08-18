package org.acme.utils;

public class StringUtil {
    private StringUtil() {
        // constructor to prevent initialization
    }

    public static String captializeAllFirstLetters(String s) {
        if (s == null || s.isEmpty()) {
            return s;
        }

        char[] sArray = s.toCharArray();
        sArray[0] = Character.toUpperCase(sArray[0]);

        for (int i = 0; i < s.length(); i++) {
            if (Character.isWhitespace(sArray[i - 1])) {
                sArray[i] = Character.toUpperCase(sArray[i]);
            }
        }

        return new String(sArray);
    }
}