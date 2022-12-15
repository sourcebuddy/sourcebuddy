package com.javax0.sourcebuddy;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.String.format;

/**
 * A very simple Java source compiler code based on the javac provided by the
 * JDK. This class can be used to compile sources that do not depend on any
 * other source code and thus can be compiled alone. The class compiled from the
 * code given in a String, generates the bytecode in memory and finally loads
 * the generated class, so that it can directly be instantiated and invoked
 * after the compiler returns.
 * <p>
 * Note that starting with version 1.0.1 classes that contain inner classes are
 * also supported.
 *
 * @author Peter Verhas
 */
public class Compiler implements Fluent.AddSource, Fluent.CanCompile, Fluent.Compiled {

    /**
     * Exception type that the compilation process throws if the source code cannot be compiled. The message of the
     * exception is the compiler error message.
     */
    public static class CompileException extends Exception {

        public CompileException(final String compilerErrorOutput) {
            super(compilerErrorOutput);
        }
    }

    /**
     * Inner class supporting the fluent API. This class contains the methods invoked after {@link #load()}.
     */
    public class Loaded {
        Loaded() throws ClassNotFoundException {
            if (state != CompilationState.SUCCESS) {
                throw new RuntimeException("Loading a class is only possible after successful compilation.");
            }
            for (final var source : sources) {
                classLoader.loadClass(source.binaryName);
            }
        }

        public Fluent.AddSource reset() {
            return Compiler.this.reset();
        }

        /**
         * Get the class for the name. The class name is usually one of the class names, which were recently compiled.
         *
         * @param binaryName the binary name of the class.
         * @return the loaded class
         * @throws ClassNotFoundException if the class cannot be found.
         */
        public Class<?> get(final String binaryName) throws ClassNotFoundException {
            return classLoader.loadClass(binaryName);
        }

        /**
         * Create and return an instance of the class. The class must have an argument less accessible constructor.
         *
         * @param binaryName the binary name of the class
         * @param ignored    used to casting, usually an interface implemented, or a class extended by the dynamically created class
         * @param <T>        the type of the object used for casting
         * @return the loaded object
         * @throws ClassNotFoundException    if there is no such class
         * @throws NoSuchMethodException     if the class does not have argumentless constructor
         * @throws InvocationTargetException if the constructor throws an exception
         * @throws InstantiationException    if the object cannot be instantiated
         * @throws IllegalAccessException    if the constructor is not accessible (for example, private)
         * @throws ClassCastException        if the class is of a different type and cannot be cast to {@code T}
         */
        public <T> T newInstance(final String binaryName, Class<T> ignored)
                throws ClassNotFoundException,
                NoSuchMethodException,
                InvocationTargetException,
                InstantiationException,
                IllegalAccessException,
                ClassCastException {
            return (T) get(binaryName).getConstructor().newInstance();
        }

        public Stream<Class<?>> stream() {
            return manager.getClassFileObjectsMap().keySet().stream().map(binaryName -> {
                try {
                    return get(binaryName);
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException(e);
                }
            });
        }

    }

    private final List<StringJavaSource> sources = new ArrayList<>();
    private final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
    private final InMemoryJavaFileManager manager = new InMemoryJavaFileManager(compiler.getStandardFileManager(null, null, StandardCharsets.UTF_8));
    private ClassLoader classLoader = null;

    private enum CompilationState {
        ADD_SOURCE,
        SUCCESS,
        FAILURE,
    }

    private CompilationState state = CompilationState.ADD_SOURCE;

    /**
     * This method provides the simple API for the compilation. It can be used to compile one single Java source file
     * specified as a {@link String}.
     * <p>
     * Compile the Java code provided as a string and if the compilation was
     * successful then load the class.
     *
     * @param binaryName the fully qualified name of the class
     * @param sourceCode the Java source code of the class
     * @return the loaded class or null if the compilation was not successful
     */
    public static Class<?> compile(final String binaryName, final String sourceCode) throws CompileException, ClassNotFoundException {
        final var compiler = Compiler.java();
        final var loaded = compiler.from(binaryName, sourceCode).compile().load();
        return loaded.get(binaryName);
    }

    public static Fluent.AddSource java() {
        return new Compiler();
    }

    @Override
    public Fluent.AddSource reset() {
        if (classLoader instanceof HiddenByteClassLoader) {
            throw new RuntimeException("Cannot reset hidden class loading compiler.");
        }
        state = CompilationState.ADD_SOURCE;
        return this;
    }

    /**
     * The constructor is not to be used. Use the {@link #java()} factory method.
     */
    private Compiler() {
    }

    /**
     * Add a Java source to the compilation task. You can call this method intermixed with the {@link #from(Path)}
     * intermixed as many times as you like.
     *
     * @param binaryName the binary name of the class, e.g.: {@code com.javax0.sourcebuddy.Test2$Hallo}
     * @param sourceCode the Java source code as a string.
     * @return the fluent object for the further call chaining
     */
    public Fluent.CanCompile from(final String binaryName, final String sourceCode) {
        if (state != CompilationState.ADD_SOURCE) {
            throw new RuntimeException("Cannot add source after compilation");
        }
        sources.add(new StringJavaSource(binaryName, sourceCode));
        return this;
    }

    /**
     * Add a Java source to the compilation task. You can call this method intermixed with the {@link #from(String, String)}
     * intermixed as many times as you like.
     *
     * @param directory the path to the source directory. This is the directory where your source root is. For example,
     *                  when your class file(s) are in the package {@code com.javax0.sourcebuddy} package then this is
     *                  the directory that contains the {@code com} directory.
     * @return the fluent object for the further call chaining
     */
    public Fluent.CanCompile from(final Path directory) throws IOException {
        try (final var fileStream = Files.walk(directory)) {
            sources.addAll(
                    fileStream
                            .filter(file -> file.toString().endsWith(".java"))
                            .map((Path file) -> new StringJavaSource(directory.relativize(file).toString()
                                    .replaceAll("/", ".")
                                    .replaceAll("\\.java$", "")
                                    , getFileContent(file))).toList());
        } catch (RuntimeException re) {
            throwCause(re);
        }
        return this;
    }

    private static void throwCause(final RuntimeException re) throws IOException {
        if (re.getCause() instanceof IOException) {
            throw (IOException) re.getCause();
        } else {
            throw re;
        }
    }

    private static String getFileContent(final Path file) {
        try {
            return String.join("\n", Files.readAllLines(file, StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Compile the collected sources.
     *
     * @return the fluent object for the further call chaining
     * @throws CompileException if there was an error during the compilation
     */
    public Compiler compile() throws CompileException {

        final var sw = new StringWriter();
        final var task = compiler.getTask(sw, manager, null, null, null, sources);
        final var compileOK = task.call();
        if (compileOK) {
            state = CompilationState.SUCCESS;
        } else {
            state = CompilationState.FAILURE;
            throw new CompileException(sw.toString());
        }
        return this;
    }


    /**
     * Get the byte code as a stream of byte arrays. It will return the byte code of not only the classes added as
     * source but also the classes, which are inner classes, anonymous classes created automatically by the compiler.
     * <p>
     * You only get binary code arrays in a stream, you may need the name of the class that belongs to the individual
     * elements. To get the binary name of a class from the byte code represented as a byte array this class provides
     * a utility method {@link #getBinaryName(byte[])}.
     *
     * @return the stream of byte arrays each containing the byte code of one of the compiled classes
     */
    public Stream<byte[]> stream() {
        return classesByteArraysMap().values().stream();
    }


    /**
     * Load the compiled classes using the library provided class loader.
     *
     * @return the fluent api object
     * @throws ClassNotFoundException if some classes cannot be loaded for whatever reason
     */
    public Loaded load() throws ClassNotFoundException {
        if (classLoader == null) {
            classLoader = new ByteClassLoader(this.getClass().getClassLoader(), classesByteArraysMap());
        } else {
            if (classLoader instanceof ByteClassLoader) {
                ((ByteClassLoader) classLoader).addByteCodes(classesByteArraysMap());
            }
        }
        return new Loaded();
    }

    @Override
    public Loaded loadHidden(MethodHandles.Lookup.ClassOption... classOptions) throws ClassNotFoundException {
        return loadHidden(null, classOptions);
    }

    @Override
    public Loaded loadHidden(MethodHandles.Lookup lookup, MethodHandles.Lookup.ClassOption... classOptions) throws ClassNotFoundException {
        if (classLoader == null) {
            classLoader = new HiddenByteClassLoader(this.getClass().getClassLoader(), classesByteArraysMap(), lookup, classOptions);
        } else {
            throw new RuntimeException("Should not reuse hidden loading class loader.");
        }
        return new Loaded();
    }

    /**
     * Save the byte codes to {@code .class} files.
     *
     * @param target the directory where the class files will be saved. The saving process will overwrite already
     *               existing class files that have the same name. The saving process automatically creates the
     *               needed directory structure.
     */
    public void saveTo(final Path target) {
        stream().forEach(byteCode -> {
            try {
                final var fileName = getBinaryName(byteCode).replaceAll("\\.", "/") + ".class";
                final var targetFile = Paths.get(target.toString(), fileName);
                Files.createDirectories(targetFile.getParent());
                Files.write(targetFile, byteCode, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    // snipline JVM_VERSION
    private final static int JVM_VERSION = 63;
    // snipline JAVA_VERSION
    private final static int JAVA_VERSION = 19;

    /**
     * Get the binary name of the class from the compiled byte code array.
     *
     * @param byteCode the byte array of the compiled code.
     * @return the name of the class
     */
    public static String getBinaryName(byte[] byteCode) {
        try (final var is = new DataInputStream(new ByteArrayInputStream(byteCode))) {
            final var magic = is.readInt();
            if (magic != 0xCAFEBABE) {
                throw new RuntimeException("Class file header is missing.");
            }
            final var minor = is.readUnsignedShort();
            final var major = is.readUnsignedShort();
            if (major > JVM_VERSION) {
                throw new RuntimeException(String.format("This version support Java up to version %d.", JAVA_VERSION));
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
                    default -> throw new RuntimeException(format("Invalid constant pool tag %d at position %d", t, i));
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

    /**
     * Get the map of class name / class byte array from the file manager.
     *
     * @return the map containing the byte arrays associated with the names of the classes
     */
    private Map<String, byte[]> classesByteArraysMap() {
        return manager.getClassFileObjectsMap().entrySet().stream()
                .map(e -> Map.entry(e.getKey(), e.getValue().getByteArray()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}
