package com.javax0.sourcebuddy;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
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
import java.util.regex.Pattern;
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
         * @param name the name of the class. It can be the binary name of the class or the simple name in the case
         *             there are no two or more classes with the same simple name.
         * @return the loaded class
         * @throws ClassNotFoundException if the class cannot be found.
         */
        public Class<?> get(final String name) throws ClassNotFoundException {
            return classLoader.loadClass(getBinaryName(name));
        }

        /**
         * A simplified version of the {@link #get(String)}, which can be used when there is only one single class
         * compiled. The one class can contain inner class(es). In this case the top level class will be returned.
         *
         * @return the loaded class
         * @throws ClassNotFoundException if there was no class compiled or there were more than one classes compiled.
         */
        public Class<?> get() throws ClassNotFoundException {
            if (sources.size() == 0) {
                throw new ClassNotFoundException("There was no class compiled.");
            }
            if (sources.size() > 1) {
                throw new ClassNotFoundException("There were many classes compiled, you must specify the name which one you want to get.");
            }
            return classLoader.loadClass(sources.get(0).binaryName);
        }

        /**
         * Create and return an instance of the class. The class must have an argument less accessible constructor.
         *
         * @param name    the binary name of the class or the simple name in the case the simple name is unique.
         * @param ignored used to casting, usually an interface implemented, or a class extended by the dynamically created class
         * @param <T>     the type of the object used for casting
         * @return the loaded object
         * @throws ClassNotFoundException    if there is no such class
         * @throws NoSuchMethodException     if the class does not have argumentless constructor
         * @throws InvocationTargetException if the constructor throws an exception
         * @throws InstantiationException    if the object cannot be instantiated
         * @throws IllegalAccessException    if the constructor is not accessible (for example, private)
         * @throws ClassCastException        if the class is of a different type and cannot be cast to {@code T}
         */
        public <T> T newInstance(final String name, Class<T> ignored)
                throws ClassNotFoundException,
                NoSuchMethodException,
                InvocationTargetException,
                InstantiationException,
                IllegalAccessException,
                ClassCastException {
            return (T) get(name).getConstructor().newInstance();
        }

        /**
         * Create and return an instance of the class. The class must have an argument less accessible constructor.
         *
         * @param name the binary name of the class or the simple name in the case the simple name is unique.
         * @return the loaded object
         * @throws ClassNotFoundException    if there is no such class
         * @throws NoSuchMethodException     if the class does not have argumentless constructor
         * @throws InvocationTargetException if the constructor throws an exception
         * @throws InstantiationException    if the object cannot be instantiated
         * @throws IllegalAccessException    if the constructor is not accessible (for example, private)
         * @throws ClassCastException        if the class is of a different type and cannot be cast to {@code T}
         */
        public Object newInstance(final String name)
                throws ClassNotFoundException,
                NoSuchMethodException,
                InvocationTargetException,
                InstantiationException,
                IllegalAccessException,
                ClassCastException {
            return get(name).getConstructor().newInstance();
        }

        /**
         * This method is the same as {@link #newInstance(String, Class)} except it does not need a name.
         * It can be used if there is only one single class compiled. If there is only one class compiled you do not
         * need to specify the name.
         *
         * @param ignored used to casting, usually an interface implemented, or a class extended by the dynamically created class
         * @param <T>     the type of the object used for casting
         * @return the loaded object
         * @throws ClassNotFoundException    if there is no such class
         * @throws NoSuchMethodException     if the class does not have argumentless constructor
         * @throws InvocationTargetException if the constructor throws an exception
         * @throws InstantiationException    if the object cannot be instantiated
         * @throws IllegalAccessException    if the constructor is not accessible (for example, private)
         * @throws ClassCastException        if the class is of a different type and cannot be cast to {@code T}
         */
        public <T> T newInstance(Class<T> ignored)
                throws ClassNotFoundException,
                NoSuchMethodException,
                InvocationTargetException,
                InstantiationException,
                IllegalAccessException,
                ClassCastException {
            return (T) get().getConstructor().newInstance();
        }

        /**
         * Get the compiled and loaded class as a stream.
         *
         * @return the stream object.
         */
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

    /**
     * This method provides a simple API for the compilation.
     * The functionality is the same as in the case of {@link #compile(String, String)}, except here you do not need
     * to specify the name of the class. The code will try to figure out the name of the class form the source code.
     * It may not be possible for all the cases. About the restrictions read at {@link #from(String)}.
     *
     * @param sourceCode the Java source code of the class
     * @return the loaded class or null if the compilation was not successful
     * @throws CompileException       if the compilation throw an error
     * @throws ClassNotFoundException if the class name cannot be identified from the source code. See  {@link #from(String)}.
     */
    public static Class<?> compile(final String sourceCode) throws CompileException, ClassNotFoundException {
        final var compiler = Compiler.java();
        final var loaded = compiler.from(sourceCode).compile().load();
        return loaded.get();
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

    private static final Pattern PACKAGE_PATTERN = Pattern.compile("(?:\\W|^|\\s)package\\s+(.*?);");
    private static final Pattern CLASS_PATTERN = Pattern.compile("(?:\\W|\\s)class\\s+([\\w.]+)(?:\\W|\\s)");

    /**
     * Add Java source to the compilation task. In this version you do not need to specify the name of the class.
     * The implementation will try to figure it out in a very simple way.
     * The name of the package and the class is searched for using regular expressions.
     * <p>
     * The class should be in a package.
     * The class can not be an inner class.
     * The word 'package' should not appear (presumably in a comment) before the package declaration and similarly
     * the work 'class' should not appear (again presumably in a comment) before the class declaration.
     * <p>
     * In some special cases this may not work. In those cases the version with two arguments, specifying the class
     * name in the first argument is to be used.
     *
     * @param sourceCode the source code to be compiled
     * @return the fluent object for further calling
     * @throws ClassNotFoundException when the name of the class cannot be identified from the source
     */
    public Fluent.CanCompile from(final String sourceCode) throws ClassNotFoundException {
        final var binaryName = getBinaryNameFromSource(sourceCode);
        return from(binaryName, sourceCode);
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
     * <p>
     * When a directory is specified the directory should be the source root. The names of the classes will be
     * calculated using the path of the individual files relative to the given directory. For example, in a Maven
     * project this directory is the {@code src/main/java} directory.
     * <p>
     * When an individual file name is given the name of the class will be figured out from the source code. For the
     * details of the algorithm and the limits see {@link #from(String)}. If you cannot meet the limitations you
     * should use the method {@link #from(String, Path)}.
     *
     * @param fileOrDir the path to the source directory or to a source file.
     * @return the fluent object for the further call chaining
     */
    public Fluent.CanCompile from(final Path fileOrDir) throws IOException, ClassNotFoundException {
        if (new File(fileOrDir.toUri()).isDirectory()) {
            try (final var fileStream = Files.walk(fileOrDir)) {
                sources.addAll(
                        fileStream
                                .filter(file -> file.toString().endsWith(".java"))
                                .map((Path file) -> new StringJavaSource(fileOrDir.relativize(file).toString()
                                        .replaceAll("/", ".")
                                        .replaceAll("\\.java$", "")
                                        , getFileContent(file))).toList());
            } catch (RuntimeException re) {
                throwCause(re);
            }
        } else {
            try {
                final var source = getFileContent(fileOrDir);
                final var binaryName = getBinaryNameFromSource(source);
                sources.add(new StringJavaSource(binaryName, source));
            } catch (RuntimeException re) {
                throwCause(re);
            }
        }
        return this;
    }

    /**
     * Load a single Java source file with the given binary name. This method can be used when the binary name
     * of the Java class can not be identified from the source code. This can happen only in very rare cases.
     * For more details when it may happen see the {@link #from(String)}-
     *
     * @param binaryName the binary name of the class
     * @param file       the file that contains the source code
     * @return the fluent object for the further call chaining
     * @throws IOException when the file cannot be read
     */
    public Fluent.CanCompile from(final String binaryName, final Path file) throws IOException {
        final var source = getFileContent(file);
        sources.add(new StringJavaSource(binaryName, source));
        return this;
    }

    /**
     * During the stream operation when an IOException occurs it is put into a runtime exception.
     * This method throws the original exception if that is an IO exception.
     * If not then it throws the run time exception.
     *
     * @param re the runtime exception that may contain an IO exception
     * @throws IOException when there is an IO exception embedded into {@code re}.
     */
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

    /**
     * Convert the simple name to the binary name searching all the names of the compiled classes.
     * <p>
     * If there is a '.' dot in the simple name, then this is already the binary name, we just return it.
     * <p>
     * If there is only one class with the provided simple name it will be found.
     * If there are more than one classes with the simple name, and one of them is in the default package (there is
     * no package) then the simple name os the binary name.
     * If there are more than one classes with the simple name, and none of them is in the default package then a
     * {@link ClassNotFoundException} will throw.
     *
     * @param simpleName the simple name of the class
     * @return the found binary name
     * @throws ClassNotFoundException if the name cannot be found or ambiguous
     */
    private String getBinaryName(final String simpleName) throws ClassNotFoundException {
        boolean found = false;
        String binaryName = null;
        if (simpleName.contains(".")) {
            return simpleName;
        }
        for (final var source : sources) {
            int dotPos = source.binaryName.lastIndexOf('.');
            if (dotPos == -1) {
                if (simpleName.equals(source.binaryName)) {
                    return simpleName;
                }
            } else {
                if (simpleName.equals(source.binaryName.substring(dotPos + 1))) {
                    if (found) {
                        throw new ClassNotFoundException("The name of the class %s is ambiguous".formatted(simpleName));
                    } else {
                        found = true;
                        binaryName = source.binaryName;
                    }
                }
            }
        }
        if (!found) {
            throw new ClassNotFoundException("Class with the name %s cannot be found".formatted(simpleName));
        }
        return binaryName;
    }

    private String getBinaryNameFromSource(final String sourceCode) throws ClassNotFoundException {
        final var packageMatcher = PACKAGE_PATTERN.matcher(sourceCode);
        final var classMatcher = CLASS_PATTERN.matcher(sourceCode);
        if (packageMatcher.find() && classMatcher.find()) {
            return "%s.%s".formatted(packageMatcher.group(1), classMatcher.group(1));
        }
        throw new ClassNotFoundException("Cannot find the package and/or class declaration in the source code");
    }
}
