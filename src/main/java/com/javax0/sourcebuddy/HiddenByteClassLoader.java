package com.javax0.sourcebuddy;

import java.lang.invoke.MethodHandles;
import java.util.Map;

/**
 * This is an extension of the {@link ByteClassLoader} using the byte array map of the compiled classes defined in the
 * parent class. The difference is that this class directly overrides the {@link ClassLoader#loadClass(String)} method
 * instead of the conventional {@link ClassLoader#findClass(String)} loading the class directly using the
 * {@link MethodHandles#lookup()} handler to load the class as a hidden class.
 */
public class HiddenByteClassLoader extends ByteClassLoader {
    MethodHandles.Lookup.ClassOption[] classOptions;

    /**
     * @param parent        see the documentation in the parent class, {@link ByteClassLoader}
     * @param classFilesMap see the documentation in the parent class, {@link ByteClassLoader}
     * @param classOptions  the class options to use for the hidden loading
     */
    public HiddenByteClassLoader(final ClassLoader parent, final Map<String, byte[]> classFilesMap, final MethodHandles.Lookup.ClassOption[] classOptions) {
        super(parent, classFilesMap);
        this.classOptions = classOptions;
    }

    @Override
    public Class<?> loadClass(String binaryName) throws ClassNotFoundException {
        final var lookup = MethodHandles.lookup();
        try {
            return lookup.defineHiddenClass(classFilesMap.get(binaryName), true, classOptions)
                    .lookupClass();
        } catch (IllegalAccessException e) {
            throw new ClassNotFoundException("Class '%s' cannot be found".formatted(binaryName));
        }
    }

}
