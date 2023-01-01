package com.javax0.sourcebuddy;

import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.JavaFileObject.Kind;
import javax.tools.StandardJavaFileManager;
import java.util.HashMap;
import java.util.Map;

public class InMemoryJavaFileManager extends
        ForwardingJavaFileManager<StandardJavaFileManager> {
    private final Map<String, MemoryFileObject> classFilesMap;

    protected InMemoryJavaFileManager(final StandardJavaFileManager fileManager) {
        super(fileManager);
        classFilesMap = new HashMap<>();
    }

    public Map<String, MemoryFileObject> getClassFileObjectsMap() {
        return classFilesMap;
    }

    @Override
    public JavaFileObject getJavaFileForOutput(final Location location,
                                               final String className,
                                               final Kind kind,
                                               final FileObject sibling) {
        MemoryFileObject fileObject = new MemoryFileObject(className);
        classFilesMap.put(className, fileObject);
        return fileObject;
    }

}
