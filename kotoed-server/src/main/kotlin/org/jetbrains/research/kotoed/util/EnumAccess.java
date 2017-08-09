package org.jetbrains.research.kotoed.util;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

public class EnumAccess {

    static <E> E enumValueOf(String s, Class<E> eClass)
            throws Throwable {
        return (E)
                MethodHandles.lookup().findStatic(eClass, "valueOf",
                MethodType.methodType(eClass, String.class))
                .invoke(s);
    }

}
