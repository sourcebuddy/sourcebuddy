package com.javax0.sourcebuddy;

import java.io.IOException;
import java.nio.file.Path;
import java.util.stream.Stream;

public interface Fluent {

    interface AddSource {
        CanCompile from(Path path) throws IOException;

        CanCompile from(String name, String source);
    }

    interface CanCompile extends AddSource {

        Compiled compile() throws Compiler.CompileException;
    }

    interface Compiled {

        Stream<byte[]> stream();

        Compiler.Loaded load() throws ClassNotFoundException;

        Compiler.Loaded load(ClassLoader cl) throws ClassNotFoundException;

        void saveTo(Path path);

        AddSource reset();
    }
}
