package com.javax0.sourcebuddy;

import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.nio.file.Path;
import java.util.stream.Stream;

@SuppressWarnings("unused")
public interface Fluent {

    interface AddSource {
        boolean canCompile();

        Compiled byteCode(byte[] code) throws IOException;

        Compiled byteCode(InputStream is) throws IOException;

        Compiled byteCode(Path classpath) throws IOException;

        SpecifyNestHiddenNamed from(Path path) throws IOException, ClassNotFoundException;

        SpecifyNestHiddenNamed from(String binary, Path path);

        SpecifyNestHiddenNamed from(String name, String source);

        SpecifyNestHiddenNamed from(String source) throws ClassNotFoundException;

        AddSource reset();

        CanIsolate options(String... options);

        CanIsolate classPath(final String cp);

        CanIsolate inheritClassPath();

        CanIsolate systemClassPath();

        CanIsolate annotatedClasses(String... options);

        AddSource modules(String... modules);
    }

    interface CanIsolate extends AddSource {

        Compiled compile(String... options) throws Compiler.CompileException;

        CanCompile isolate();
    }

    interface CanCompile extends CanIsolate {
        Compiled compile(String... options) throws Compiler.CompileException;
    }

    interface SpecifyNestHiddenNamed extends CanIsolate {
        CanIsolate hidden(MethodHandles.Lookup.ClassOption... classOptions);

        CanIsolate hidden(MethodHandles.Lookup lookup, MethodHandles.Lookup.ClassOption... classOptions);

        CanIsolate nest(MethodHandles.Lookup.ClassOption... classOptions);

        CanIsolate nest(MethodHandles.Lookup lookup, MethodHandles.Lookup.ClassOption... classOptions);

        CanIsolate named();

        CanIsolate named(MethodHandles.Lookup lookup);
    }

    interface Compiled {

        Compiled byteCode(byte[] code) throws IOException;

        Compiled byteCode(InputStream is) throws IOException;

        Compiled byteCode(Path classpath) throws IOException;

        Stream<byte[]> stream();

        byte[] get() throws ClassNotFoundException;

        Compiler.Loaded load(Compiler.LoaderOption... options) throws ClassNotFoundException;

        void saveTo(Path path);

        AddSource reset();
    }
}
