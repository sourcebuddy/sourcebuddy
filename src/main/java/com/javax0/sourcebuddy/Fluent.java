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

        CanCompile options(String... options);

        CanCompile annotatedClasses(String... options);
    }

    interface CanCompile extends AddSource {
        CanCompile annotatedClasses(String... options);

        CanCompile options(String... options);

        Compiled compile(String... options) throws Compiler.CompileException;

    }

    interface SpecifyNestHiddenNamed extends CanCompile {
        CanCompile hidden(MethodHandles.Lookup.ClassOption... classOptions);

        CanCompile hidden(MethodHandles.Lookup lookup, MethodHandles.Lookup.ClassOption... classOptions);

        CanCompile nest(MethodHandles.Lookup.ClassOption... classOptions);

        CanCompile nest(MethodHandles.Lookup lookup, MethodHandles.Lookup.ClassOption... classOptions);

        CanCompile named();

        CanCompile named(MethodHandles.Lookup lookup);
    }

    interface Compiled {

        Stream<byte[]> stream();

        byte[] get() throws ClassNotFoundException;

        Compiler.Loaded load(Compiler.LoaderOption... options) throws ClassNotFoundException;

        void saveTo(Path path);

        AddSource reset();
    }
}
