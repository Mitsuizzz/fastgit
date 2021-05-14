package com.mitsui.fastgit.util;

import java.util.Collection;
import java.util.Hashtable;

public class CollectionUtil {
    public CollectionUtil() {
    }

    public static boolean isEmpty(Collection<?> collection) {
        return collection == null || collection.isEmpty();
    }

    public static boolean isEmpty(Hashtable hashtable) {
        return hashtable == null || hashtable.isEmpty();
    }

}
