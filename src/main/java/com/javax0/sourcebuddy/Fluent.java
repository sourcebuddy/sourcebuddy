package com.javax0.sourcebuddy;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.file.Path;
import java.util.stream.Stream;

@SuppressWarnings("unused")
public interface Fluent {

    interface AddSource {
        CanCompile from(Path path) throws IOException, ClassNotFoundException;

        CanCompile from(String binary, Path path);

        CanCompile from(String name, String source);

        CanCompile from(String source) throws ClassNotFoundException;

        AddSource reset();
    }

    interface CanCompile extends AddSource {

        Compiled compile() throws Compiler.CompileException;

        CanCompile hidden(MethodHandles.Lookup.ClassOption... classOptions);

        CanCompile hidden(MethodHandles.Lookup lookup, MethodHandles.Lookup.ClassOption... classOptions);
    }

    interface Compiled {

        Stream<byte[]> stream();

        byte[] get() throws ClassNotFoundException;

        Compiler.Loaded load() throws ClassNotFoundException;

        void saveTo(Path path);

        AddSource reset();
    }
}
