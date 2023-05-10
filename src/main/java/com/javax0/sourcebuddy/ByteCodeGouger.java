package com.javax0.sourcebuddy;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;

class ByteCodeGouger {
    // snipline JVM_VERSION
    private final static int JVM_VERSION = 64;
    // snipline JAVA_VERSION
    private final static int JAVA_VERSION = 20;

    static boolean magicOk(byte[] byteCode) {
        try (final var is = new DataInputStream(new ByteArrayInputStream(byteCode))) {
            final var magic = is.readInt();
            return magic == 0xCAFEBABE;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    static int major(byte[] byteCode) {
        try (final var is = new DataInputStream(new ByteArrayInputStream(byteCode))) {
            final var magic = is.readInt();
            if (magic != 0xCAFEBABE) {
                throw new RuntimeException("Class file header is missing.");
            }
            @SuppressWarnings("unused") final var minor = is.readUnsignedShort();
            return is.readUnsignedShort();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    static int minor(byte[] byteCode) {
        try (final var is = new DataInputStream(new ByteArrayInputStream(byteCode))) {
            final var magic = is.readInt();
            if (magic != 0xCAFEBABE) {
                throw new RuntimeException("Class file header is missing.");
            }
            return is.readUnsignedShort();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Get the binary name of the class from the compiled byte code array.
     *
     * @param byteCode the byte array of the compiled code.
     * @return the name of the class
     */
    static String getBinaryName(byte[] byteCode) {
        try (final var is = new DataInputStream(new ByteArrayInputStream(byteCode))) {
            final var magic = is.readInt();
            if (magic != 0xCAFEBABE) {
                throw new RuntimeException("Class file header is missing.");
            }
            @SuppressWarnings("unused") final var minor = is.readUnsignedShort();
            final var major = is.readUnsignedShort();
            if (major > JVM_VERSION) {
                throw new RuntimeException("This version support Java up to version %d.".formatted(JAVA_VERSION));
            }
            final int constantPoolCount = is.readShort();
            final var classes = new int[constantPoolCount - 1];
            final var strings = new String[constantPoolCount - 1];
            for (int i = 0; i < constantPoolCount - 1; i++) {
                int t = is.read();
                switch (t) {
                    case 1 ->//utf-8
                            strings[i] = is.readUTF();
                    // Long
                    case 5, 6 -> { // Double
                        read8(is);
                        i++;
                    }
                    case 7 -> // Class index
                            classes[i] = is.readUnsignedShort();
                    // method type
                    case 16, 8 -> // string index
                            read2(is);
                    //Integer
                    // float
                    // field ref
                    // method ref
                    // interface method ref
                    // name and type
                    case 3, 4, 9, 10, 11, 12, 18 -> // invoke dynamic
                            read4(is);
                    case 15 -> { // method handle
                        read1(is);
                        read2(is);
                    }
                    case 19 -> { // module
                        read2(is);
                    }
                    case 20 -> { // package
                        read2(is);
                    }
                    default ->
                            throw new RuntimeException("Invalid constant pool tag %d at position %d".formatted(t, i));
                }
            }
            is.readShort(); // skip access flags
            final var classNameIndex = is.readUnsignedShort();
            if (classNameIndex >= constantPoolCount - 1) {
                throw new RuntimeException("The binary class file seems to be corrupt.");
            }
            final var classNameStringIndex = classes[classNameIndex - 1] - 1;
            if (classNameStringIndex >= constantPoolCount - 1) {
                throw new RuntimeException("The binary class file seems to be corrupt.");
            }
            return strings[classNameStringIndex].replace('/', '.');
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * Read one byte form the data input stream.
     *
     * @param dis the input stream to read from
     * @throws IOException when some error happens
     */
    private static void read1(DataInputStream dis) throws IOException {
        //noinspection ResultOfMethodCallIgnored
        dis.read();
    }

    /**
     * Read two bytes form the data input stream.
     *
     * @param dis the input stream to read from
     * @throws IOException when some error happens
     */
    private static void read2(DataInputStream dis) throws IOException {
        dis.readShort();
    }

    /**
     * Read four bytes form the data input stream.
     *
     * @param dis the input stream to read from
     * @throws IOException when some error happens
     */
    private static void read4(DataInputStream dis) throws IOException {
        dis.readInt();
    }

    /**
     * Read eight bytes form the data input stream.
     *
     * @param dis the input stream to read from
     * @throws IOException when some error happens
     */
    private static void read8(DataInputStream dis) throws IOException {
        dis.readLong();
    }

}
