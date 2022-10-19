package com.javax0.sourcebuddy;

import javax.tools.SimpleJavaFileObject;
import java.net.URI;

/**
 * A file object used to represent source coming from a string.
 */
public class StringJavaSource extends SimpleJavaFileObject {
    /**
     * The source code of this "file".
     */
    final String code;

    final String binaryName;


    private static String simpleNameFrom(String canonicalClassName) {
        return canonicalClassName
                .substring(canonicalClassName.lastIndexOf('.') + 1);
    }

    /**
     * Constructs a new JavaSourceFromString.
     *
     * @param binaryName the name of the compilation unit represented by this file object
     * @param code          the source code for the compilation unit represented by this file object
     */
    StringJavaSource(final String binaryName, final String code) {
        super(URI.create("string:///" + simpleNameFrom(binaryName).replace('.', '/') + Kind.SOURCE.extension), Kind.SOURCE);
        this.code = code;
        this.binaryName = binaryName;
    }

    @Override
    public CharSequence getCharContent(final boolean ignoreEncodingErrors) {
        return code;
    }

    @Override
    public String toString() {
        return binaryName;
    }
}

