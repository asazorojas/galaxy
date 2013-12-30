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
package co.paralleluniverse.io.serialization;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

/**
 *
 * @author pron
 */
public final class JDKSerializer implements ByteArraySerializer, IOStreamSerializer {
    @Override
    public byte[] write(Object object) {
        try {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            final ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(object);
            oos.flush();
            baos.close();
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Object read(byte[] buf) {
        return read(buf, 0);
    }

    @Override
    public Object read(byte[] buf, int offset) {
        try {
            final ByteArrayInputStream bais = new ByteArrayInputStream(buf, offset, buf.length - offset);
            final ObjectInputStream ois = new ObjectInputStream(bais);
            Object obj = ois.readObject();
            return obj;
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void write(OutputStream os, Object object) throws IOException {
        final ObjectOutput oo = toObjectOutput(os);
        oo.writeObject(object);
    }

    @Override
    public Object read(InputStream is) throws IOException {
        final ObjectInput oi = toObjectInput(is);
        return oi.read();
    }

    public static DataOutput toDataOutput(OutputStream os) {
        if (os instanceof DataOutput)
            return (DataOutput) os;
        return new DataOutputStream(os);
    }

    public static ObjectOutput toObjectOutput(OutputStream os) throws IOException {
        if (os instanceof ObjectOutput)
            return (ObjectOutput) os;
        return new ObjectOutputStream(os);
    }

    public static DataInput toDataInput(InputStream is) {
        if (is instanceof DataInput)
            return (DataInput) is;
        return new DataInputStream(is);
    }

    public static ObjectInput toObjectInput(InputStream is) throws IOException {
        if (is instanceof DataInput)
            return (ObjectInput) is;
        return new ObjectInputStream(is);
    }
}
