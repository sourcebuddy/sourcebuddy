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
    }

    interface CanCompile extends AddSource {

        Compiled compile() throws Compiler.CompileException;
    }

    interface Compiled {

        Stream<byte[]> stream();

        Compiler.Loaded load() throws ClassNotFoundException;

        Compiler.Loaded loadHidden(MethodHandles.Lookup.ClassOption... classOptions) throws ClassNotFoundException;

        Compiler.Loaded loadHidden(MethodHandles.Lookup lookup, MethodHandles.Lookup.ClassOption... classOptions) throws ClassNotFoundException;

        void saveTo(Path path);

        AddSource reset();
    }
}
