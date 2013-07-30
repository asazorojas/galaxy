/*
 * Copyright (C) 2013, Parallel Universe Software Co. All rights reserved.
 * 
 * This program and the accompanying materials are dual-licensed under
 * either the terms of the Eclipse Public License v1.0 as published by
 * the Eclipse Foundation
 *  
 *   or (per the licensee's choosing)
 *  
 * under the terms of the GNU Lesser General Public License version 3.0
 * as published by the Free Software Foundation.
 */
package co.paralleluniverse.common.util;

/**
 *
 * @author pron
 */
public final class Exceptions {
    public static RuntimeException rethrow(Throwable t) {
        if (t instanceof RuntimeException)
            throw ((RuntimeException) t);
        if (t instanceof Error)
            throw ((Error) t);
        else
            throw new RuntimeException(t);
    }

    /**
     * Unwraps several common wrapper exceptions and returns the underlying cause.
     *
     * @param t
     * @return
     */
    public static Throwable unwrap(Throwable t) {
        for (;;) {
            if (t == null)
                throw new NullPointerException();

            if (t instanceof java.util.concurrent.ExecutionException)
                t = t.getCause();
            else if (t instanceof java.lang.reflect.InvocationTargetException)
                t = t.getCause();
            else if (t.getClass().equals(RuntimeException.class) && t.getCause() != null)
                t = t.getCause();
            else
                return t;
        }
    }

    private Exceptions() {
    }
}
