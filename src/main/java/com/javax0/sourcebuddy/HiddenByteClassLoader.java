//snipline clPackage filter=package\s(.+);
package com.javax0.sourcebuddy;

import java.lang.invoke.MethodHandles;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

/**
 * This is an extension of the {@link ByteClassLoader} using the byte array map of the compiled classes defined in the
 * parent class. The difference is that this class directly overrides the {@link ClassLoader#loadClass(String)} method
 * instead of the conventional {@link ClassLoader#findClass(String)} loading the class directly using the
 * {@link MethodHandles#lookup()} handler to load the class as a hidden class.
 */
public class HiddenByteClassLoader extends ByteClassLoader {
    final MethodHandles.Lookup.ClassOption[] classOptions;
    final MethodHandles.Lookup lookup;

    /**
     * @param parent        see the documentation in the parent class, {@link ByteClassLoader}
     * @param classFilesMap see the documentation in the parent class, {@link ByteClassLoader}
     * @param classOptions  the class options to use for the hidden loading
     */
    public HiddenByteClassLoader(final ClassLoader parent, final Map<String, byte[]> classFilesMap, final MethodHandles.Lookup lookup, final MethodHandles.Lookup.ClassOption[] classOptions) {
        super(parent, classFilesMap);
        this.classOptions = classOptions;
        this.lookup = lookup;
    }

    private static final Map<String, MethodHandles.Lookup> lookups = new ConcurrentHashMap<>();
    private static final AtomicLong count = new AtomicLong(0);

    @Override
    public Class<?> loadClass(String binaryName) throws ClassNotFoundException {
        final MethodHandles.Lookup lookup;
        final byte[] byteCode = classFilesMap.get(binaryName);
        if (byteCode == null) {
            throw new ClassNotFoundException("Class %s cannot be found.".formatted(binaryName));
        }
        if (this.lookup == null) {
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
        } else {
            lookup = this.lookup;
        }
        try {
            return lookup.defineHiddenClass(byteCode, true, classOptions)
                    .lookupClass();
        } catch (IllegalAccessException e) {
            throw new ClassNotFoundException("Class '%s' cannot be found".formatted(binaryName));
        }
    }

}
