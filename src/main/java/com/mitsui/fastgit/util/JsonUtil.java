package com.mitsui.fastgit.util;

import com.alibaba.fastjson.JSONArray;

import java.util.List;

public class JsonUtil {
    public static String listToJSONString(Object object) {
        return JSONArray.toJSONString(object);
    }

    public static <T> List<T> parseArray(String text, Class<T> clazz) {
        return JSONArray.parseArray(text, clazz);
    }

}
