package com.javax0.sourcebuddy;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.file.Path;
import java.util.stream.Stream;

@SuppressWarnings("unused")
public interface Fluent {

    interface AddSource {
        SpecifyNestHiddenNamed from(Path path) throws IOException, ClassNotFoundException;

        SpecifyNestHiddenNamed from(String binary, Path path);

        SpecifyNestHiddenNamed from(String name, String source);

        SpecifyNestHiddenNamed from(String source) throws ClassNotFoundException;

        AddSource reset();

        CanIsolate options(String... options);

        CanIsolate annotatedClasses(String... options);

        AddSource modules(String ... modules);
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

        Stream<byte[]> stream();

        byte[] get() throws ClassNotFoundException;

        Compiler.Loaded load(Compiler.LoaderOption... options) throws ClassNotFoundException;

        void saveTo(Path path);

        AddSource reset();
    }
}
