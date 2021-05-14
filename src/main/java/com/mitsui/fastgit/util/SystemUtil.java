package com.mitsui.fastgit.util;

public class SystemUtil {
    public static String getApplicationRunPath() {
        String path = System.getProperty("user.dir");
        return path;
    }
}
