package com.mitsui.fastgit.util;

import java.util.List;

public class StringUtil {
    public static boolean isNullOrEmpty(String str) {
        return str == null || str.isEmpty();
    }

    public static String listToString(List list, String separator) {
        StringBuilder sb = new StringBuilder();

        for(int i = 0; i < list.size(); ++i) {
            sb.append(list.get(i)).append(separator);
        }

        return sb.toString().substring(0, sb.toString().length() - 1);
    }
}
