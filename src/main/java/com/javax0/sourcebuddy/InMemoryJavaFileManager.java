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
