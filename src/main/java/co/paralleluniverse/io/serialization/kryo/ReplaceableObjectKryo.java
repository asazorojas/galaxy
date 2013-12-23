/*
 * Quasar: lightweight threads and actors for the JVM.
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
package co.paralleluniverse.io.serialization.kryo;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Registration;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A subclass of {@link Kryo} that respects {@link Serializable java.io.Serializable}'s {@code writeReplace} and {@code readResolve}.
 * @author pron
 */
public class ReplaceableObjectKryo extends Kryo {
    private final static Map<Class, ReplaceMethods> replaceMethodsCache = new ConcurrentHashMap<>();
    private static final String WRITE_REPLACE = "writeReplace";
    private static final String READ_RESOLVE = "readResolve";

    @Override
    public void writeClassAndObject(Output output, Object object) {
        if (output == null)
            throw new IllegalArgumentException("output cannot be null.");
        if (object == null) {
            super.writeClass(output, null);
            return;
        }
        Object newObj = getReplacement(getMethods(object.getClass()).writeReplace,object);
        setAutoReset(false);
        Registration registration = super.writeClass(output, newObj.getClass());
        setAutoReset(true);
        super.writeObject(output, newObj, registration.getSerializer());
//        System.out.println("wrote an object "+newObj+" id "+registration.getId());
//        reset();
    }

    public ReplaceableObjectKryo() {
    }

    @Override
    public Registration writeClass(Output output, Class type) {
        if (type==null || getMethods(type).writeReplace == null)
            return super.writeClass(output, type);
        return super.getRegistration(type); // do nothing. write object will write the class too
    }

    @Override
    public void writeObject(Output output, Object object, Serializer serializer) {
        Method m = getMethods(object.getClass()).writeReplace;
        if (m != null) {
            object = getReplacement(m, object);
            Registration reg = super.writeClass(output, object.getClass());
            serializer = reg.getSerializer();
        }
        super.writeObject(output, object, serializer);
//        System.out.println("wrote2 an object "+object+" id "+getRegistration(object.getClass()).getId());
        
    }

    @Override
    public <T> T readObject(Input input, Class<T> type, Serializer serializer) {
        return (T) getReplacement(getMethods(type).readResolve,super.readObject(input, type, serializer));
    }

    private static Object getReplacement(Method m, Object object) {
        if (m == null)
            return object;
        try {
            return m.invoke(object);
        } catch (IllegalAccessException e) {
            throw new AssertionError(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e.getCause()); // Exceptions.rethrow(e.getCause());
        }
    }

    private static ReplaceMethods getMethods(Class clazz) {
        if (replaceMethodsCache.containsKey(clazz))
            return replaceMethodsCache.get(clazz);
        final ReplaceMethods replaceMethods = new ReplaceMethods(getMethodByReflection(clazz, WRITE_REPLACE),
                getMethodByReflection(clazz, READ_RESOLVE));
        replaceMethodsCache.put(clazz, replaceMethods);
        return replaceMethods;
    }

    private static Method getMethodByReflection(Class clazz, final String methodName) throws SecurityException {
        if (!Serializable.class.isAssignableFrom(clazz))
            return null;

        Method m = null;
        try {
            m = clazz.getDeclaredMethod(methodName);
        } catch (NoSuchMethodException ex) {
            Class ancestor = clazz.getSuperclass();
            while (ancestor != null) {
                if (!Serializable.class.isAssignableFrom(ancestor))
                    return null;
                try {
                    m = ancestor.getDeclaredMethod(methodName);
                    if (!Modifier.isPublic(m.getModifiers()) && !Modifier.isProtected(m.getModifiers()))
                        return null;
                    break;
                } catch (NoSuchMethodException ex1) {
                    ancestor = ancestor.getSuperclass();
                }
            }
        }
        if (m != null)
            m.setAccessible(true);
        return m;
    }

    private static class ReplaceMethods {
        Method writeReplace;
        Method readResolve;

        public ReplaceMethods(Method writeReplace, Method readResolve) {
            this.writeReplace = writeReplace;
            this.readResolve = readResolve;
        }
    }
}
