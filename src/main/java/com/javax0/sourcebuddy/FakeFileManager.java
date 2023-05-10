package com.javax0.sourcebuddy;

import javax.tools.FileObject;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.Set;

/**
 * Fake file manager that does not do anything. It is used when the real file manager is not available.
 * In that case, the file manager is just storing byte code in the map, but it still needs a file manager
 * object to call the 'super' and pass this object to it.
 */
public class FakeFileManager implements StandardJavaFileManager {
    @Override
    public ClassLoader getClassLoader(Location location) {
        return null;
    }

    @Override
    public Iterable<JavaFileObject> list(Location location, String packageName, Set<JavaFileObject.Kind> kinds, boolean recurse) throws IOException {
        return null;
    }

    @Override
    public String inferBinaryName(Location location, JavaFileObject file) {
        return null;
    }

    @Override
    public boolean isSameFile(FileObject a, FileObject b) {
        return false;
    }

    @Override
    public boolean handleOption(String current, Iterator<String> remaining) {
        return false;
    }

    @Override
    public boolean hasLocation(Location location) {
        return false;
    }

    @Override
    public JavaFileObject getJavaFileForInput(Location location, String className, JavaFileObject.Kind kind) throws IOException {
        return null;
    }

    @Override
    public JavaFileObject getJavaFileForOutput(Location location, String className, JavaFileObject.Kind kind, FileObject sibling) throws IOException {
        return null;
    }

    @Override
    public FileObject getFileForInput(Location location, String packageName, String relativeName) throws IOException {
        return null;
    }

    @Override
    public FileObject getFileForOutput(Location location, String packageName, String relativeName, FileObject sibling) throws IOException {
        return null;
    }

    @Override
    public void flush() throws IOException {

    }

    @Override
    public void close() throws IOException {

    }

    @Override
    public Iterable<? extends JavaFileObject> getJavaFileObjectsFromFiles(Iterable<? extends File> files) {
        return null;
    }

    @Override
    public Iterable<? extends JavaFileObject> getJavaFileObjects(File... files) {
        return null;
    }

    @Override
    public Iterable<? extends JavaFileObject> getJavaFileObjectsFromStrings(Iterable<String> names) {
        return null;
    }

    @Override
    public Iterable<? extends JavaFileObject> getJavaFileObjects(String... names) {
        return null;
    }

    @Override
    public void setLocation(Location location, Iterable<? extends File> files) throws IOException {

    }

    @Override
    public Iterable<? extends File> getLocation(Location location) {
        return null;
    }

    @Override
    public int isSupportedOption(String option) {
        return 0;
    }
}
