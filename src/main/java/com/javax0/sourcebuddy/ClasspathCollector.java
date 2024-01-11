package com.javax0.sourcebuddy;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

/**
 * Collect the class path entries. The class path entries are the directories and JAR files that are listed in the
 * system property {@code java.class.path}. The class path entries are collected recursively. If a JAR file is listed
 * in the class path, then the manifest of the JAR file is read and the class path entries listed in the manifest are
 * also added to the list of class path entries.
 */
public class ClasspathCollector {

    private final Set<String> processedJars = new HashSet<>();
    private final Set<String> entries = new HashSet<>();

    /**
     * Get the class path entries.
     *
     * @return the set of class path entries
     */
    public static Set<String> getEntries() {
        final var collector = new ClasspathCollector();
        String classpath = System.getProperty("java.class.path");
        collector.processClassPathEntries(classpath);
        return collector.entries;
    }

    private void processClassPathEntries(final String classpath) {
        StringTokenizer tokenizer = new StringTokenizer(classpath, File.pathSeparator);

        while (tokenizer.hasMoreTokens()) {
            String path = tokenizer.nextToken();
            processPath(path, null);
        }
    }

    /**
     * Process the path. If the path is a directory, then it is added to the
     * entries. If the path is a jar file, then it is added to the entries and
     * the jar file is processed recursively.
     *
     * @param path    the path to process
     * @param baseDir the base directory to use when the path is relative, or {@code  null} when there is no possibility
     *                to use relative paths. The latter is the case when the path is listed in the system class path.
     *                When the path is listed in a JAR file Class-Path manifest, then the base directory is the directory
     *                where the JAR file is located.
     */
    private void processPath(final String path, final File baseDir) {
        final File file = baseDir != null ? new File(baseDir, path) : new File(path);
        try {
            final String canonicalPath = file.getCanonicalPath();

            if (!processedJars.contains(canonicalPath)) {
                processedJars.add(canonicalPath);
                entries.add(canonicalPath);
                if (file.isFile() && canonicalPath.endsWith(".jar")) {
                    processJarFile(file);
                }
            }
        } catch (IOException ignore) {
            // Ifa file is not accessible, or has a bad JAR format, then it will cause and issue
            // when running the compiler or the code. In that case, it is not a problem that
            // we ignore the exception here.
            //
            // If the IOException case is not a problem later, for example, it is a file not used by the compiler
            // and the execution for some magical reason (all classes are found sooner) then ignoring the exception
            // is also not a problem.
        }
    }

    private void processJarFile(final File jarFile) throws IOException {
        try (JarFile jar = new JarFile(jarFile)) {
            Manifest manifest = jar.getManifest();
            if (manifest != null) {
                String classpath = manifest.getMainAttributes().getValue("Class-Path");
                if (classpath != null) {
                    StringTokenizer manifestTokenizer = new StringTokenizer(classpath, " ");
                    while (manifestTokenizer.hasMoreTokens()) {
                        String manifestPath = manifestTokenizer.nextToken();
                        processPath(manifestPath, jarFile.getParentFile());
                    }
                }
            }
        }
    }

}

