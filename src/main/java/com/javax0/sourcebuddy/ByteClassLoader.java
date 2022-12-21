package com.javax0.sourcebuddy;

import java.lang.invoke.MethodHandles;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Class loader that loads a class from a byte array. This loader can load
 * classes that are passed to the constructor in a map in their compiled binary
 * form. The main use of this class loader is to load the class or classes that
 * were generated during the compilation of a single Java source class that may
 * contain inner classes, anonymous classes and thus need the load of more than
 * one class to have the {@code Class} object usable in your Java code.
 *
 * @author Peter Verhas
 */
public class ByteClassLoader extends URLClassLoader {
    public static class ByteCode {
        private final byte[] code;
        boolean isHidden;
        MethodHandles.Lookup.ClassOption[] classOptions;
        MethodHandles.Lookup lookup;

        public ByteCode(final byte[] code) {
            this.code = code;
        }
    }

    protected final Map<String, ByteCode> classFilesMap;
    private static final System.Logger LOG = System.getLogger(ByteClassLoader.class.getName());

    /**
     * @param parent        passed to the super constructor. For more information see
     *                      {@link URLClassLoader#URLClassLoader(URL[], ClassLoader)}.
     * @param classFilesMap a map that contains the binary code of the classes that may be
     *                      loaded by this class loader. The key of the map is the name of
     *                      the class that is used when the class is loaded calling the
     *                      method {@link #findClass(String)}. The value in a map element
     *                      is the byte array of the class as generated by the compiler.
     */
    public ByteClassLoader(ClassLoader parent, final Map<String, byte[]> classFilesMap, List<StringJavaSource> sources) {
        super(new URL[0], parent);
        this.classFilesMap = new HashMap<>();
        addByteCodes(classFilesMap, sources);
    }

    public void addByteCodes(final Map<String, byte[]> classFilesMap, List<StringJavaSource> sources) {
        Map<String, ByteCode> map =
                classFilesMap.entrySet().stream()
                        .map(e -> new AbstractMap.SimpleEntry<>(e.getKey(), new ByteCode(e.getValue())))
                        .collect(Collectors.toMap(AbstractMap.SimpleEntry::getKey, AbstractMap.SimpleEntry::getValue));

        sources.forEach(source -> {
            final var byteCode = map.get(source.binaryName);
            byteCode.isHidden = source.isHidden;
            byteCode.classOptions = source.classOptions;
            byteCode.lookup = source.lookup;

        });
        this.classFilesMap.putAll(map);
    }

    private static final Map<String, MethodHandles.Lookup> lookups = new ConcurrentHashMap<>();
    private static final AtomicLong count = new AtomicLong(0);

    @Override
    public Class<?> loadClass(String binaryName) throws ClassNotFoundException {
        final var byteCodeRecord = classFilesMap.get(binaryName);
        if (byteCodeRecord == null || !byteCodeRecord.isHidden) {
            return super.loadClass(binaryName);
        }
        final byte[] byteCode = byteCodeRecord.code;
        MethodHandles.Lookup lookup = byteCodeRecord.lookup;
        final MethodHandles.Lookup.ClassOption[] classOptions = byteCodeRecord.classOptions;
        if (byteCode == null) {
            throw new ClassNotFoundException("Class %s cannot be found.".formatted(binaryName));
        }
        if (lookup == null) {
            final var className = Compiler.getBinaryName(byteCode);
            int lastDot = className.lastIndexOf('.');
            final String packageLine;
            final String packageName;
            if (lastDot == -1) {
                packageName = "";
                packageLine = ""; // default package
            } else {
                packageName = className.substring(0, lastDot);
                packageLine = "package %s;".formatted(packageName);
            }
            if (lookups.containsKey(packageName)) {
                lookup = lookups.get(packageName);
            } else {
                final var packageDot = packageName.length() == 0 ? "" : packageName + ".";
                final var lookupHelperBinaryName = "A%d".formatted(count.addAndGet(1));
                try {
                    // snippet lookup_creation
                    lookup = (MethodHandles.Lookup) Compiler.java().from(packageDot + lookupHelperBinaryName, """
                                    %s
                                       
                                    import java.util.function.Supplier;
                                    import java.lang.invoke.MethodHandles;
                                                                
                                    public class %s implements Supplier<MethodHandles.Lookup> {
                                        public %s(){}
                                        @Override
                                        public MethodHandles.Lookup get() {
                                            return MethodHandles.lookup();
                                        }
                                    }
                                    """.formatted(packageLine, lookupHelperBinaryName, lookupHelperBinaryName)).compile().load()
                            .newInstance(packageDot + lookupHelperBinaryName, Supplier.class).get();
                    // end snippet
                        /*this goes into the documentation, update if the code above changes
                        // snippet lookup_creation_describe
In the code above the variable `packageLine` contains the `package` keyword and the name of the package and a `;`.
When the generated class is in the default package then this variable is empty string.

`lookupHelperBinaryName` is the name of the class. This is just the letter `A` and a counter to have a unique name every time.
It could be a constant.
This variable is used twice, one for the name of the class and once to create a public constructor.
                        // end snippet
                         */
                    synchronized (lookups) {
                        if (!lookups.containsKey(packageName)) {
                            lookups.put(packageName, lookup);
                        }
                    }
                } catch (Exception e) {
                    throw new ClassNotFoundException("%s cannot be loaded".formatted(lookupHelperBinaryName), e);
                }
            }
        }
        try {
            return lookup.defineHiddenClass(byteCode, true, classOptions)
                    .lookupClass();
        } catch (IllegalAccessException e) {
            throw new ClassNotFoundException("Class '%s' cannot be found".formatted(binaryName));
        }
    }

    @Override
    protected Class<?> findClass(final String name)
            throws ClassNotFoundException {
        LOG.log(System.Logger.Level.DEBUG, String.format("findClass(%s)", name));
        if (classFilesMap.containsKey(name)) {
            byte[] classFile = classFilesMap.get(name).code;
            Class<?> klass = defineClass(name, classFile, 0, classFile.length);
            releaseClassByteArray(name);
            return klass;
        }
        return super.findClass(name);
    }

    /**
     * The class loader remains in memory so long as long the loaded class
     * remains in memory but the source byte array that was used to load the
     * code of the class is not needed anymore. This method removes the element
     * from the map that contained the byte array so that the gc can reclaim the
     * memory.
     *
     * @param name is the name of the class that was recently loaded. The map
     *             should contain an element with this key.
     */
    private void releaseClassByteArray(String name) {
        classFilesMap.remove(name);
    }

}
