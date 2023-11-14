package com.javax0.sourcebuddy;

import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.JavaFileObject.Kind;
import javax.tools.StandardJavaFileManager;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * A file manager that stores the compiled bytes in {@link HashMap}.
 * It is used when the Java source code is compiled, but it is also used when the class files are read from the disk.
 * In the latter case it may happen that the compiler is not available in the environment, which also implies that
 * the compiler, being non-existent, cannot provide a {@link StandardJavaFileManager}.
 *
 * In that case the constructor argument will be {@code null} and the {@link InMemoryJavaFileManager} will use a
 * {@link FakeFileManager} that does not do anything. The "does nothing" in this case is not a problem, because there
 * is no compiler. The object is needed to call the super constructor.
 *
 * In this case, the use of this class is that it can hold the class binaries in memory.
 */
public class InMemoryJavaFileManager extends
        ForwardingJavaFileManager<StandardJavaFileManager> {

    final static StandardJavaFileManager fake = new FakeFileManager();

    private final Map<String, MemoryFileObject> classFilesMap = new HashMap<>();

    protected InMemoryJavaFileManager(final StandardJavaFileManager fileManager) {
        super(fileManager == null ? fake : fileManager);
    }

    public Map<String, MemoryFileObject> getClassFileObjectsMap() {
        return classFilesMap;
    }

    @Override
    public JavaFileObject getJavaFileForOutput(final Location location,
                                               final String className,
                                               final Kind kind,
                                               final FileObject sibling) {
        final var fileObject = new MemoryFileObject(className);
        classFilesMap.put(className, fileObject);
        return fileObject;
    }

}
