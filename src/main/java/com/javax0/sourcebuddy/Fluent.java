package com.javax0.sourcebuddy;

import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.nio.charset.Charset;
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


        // snippet CompilerOptions
        CanIsolate release(int version);
        // * `release(int)` sets the release version of the Java compiler.

        CanIsolate source(int version);
        // * `source(int)` sets the source version of the Java compiler.

        CanIsolate target(int version);
        // * `target(int)` sets the target version of the Java compiler.

        CanIsolate encoding(Charset charset);
        // * `encoding(Charset)` sets the encoding of the source files.

        CanIsolate verbose();
        // * `verbose()` sets the compiler to be verbose.

        CanIsolate debugInfo(DebugInfo debugInfo);
        // * `debugInfo(DebugInfo)` sets the debug information level of the compiler.
        // The possible values are `NONE`, `LINES`, `SOURCE`, `VARS`, and `ALL` as listed in the enumeration.

        CanIsolate noDebugInfo();
        // * `noDebugInfo()` sets the compiler to suppress debug information.

        CanIsolate nowarn();
        // * `nowarn()` sets the compiler to suppress warnings.

        CanIsolate showDeprecation();
        // * `showDeprecation()` sets the compiler to show deprecation warnings.

        CanIsolate parameters();
        // * `parameters()` sets the compiler to store formal parameter names of constructors and methods in the generated class files.

        class Export {
            final String module;
            final String pckg;
            final String otherModule;

            public Export(String module, String pckg, String otherModule) {
                this.module = module;
                this.pckg = pckg;
                this.otherModule = otherModule;
            }

            public static Export from(String module) {
                return new Export(module, "*", "ALL-UNNAMED");
            }

            public Export thePackage(String pckg) {
                return new Export(module, pckg, otherModule);
            }

            public Export to(String otherModule) {
                return new Export(module, pckg, otherModule);
            }

            public String toString() {
                return module + "/" + pckg + "=" + otherModule;
            }
        }

        CanIsolate addExports(Export... exports);
        // * `addExports(Export...)` adds export directives to the module declaration.
        // To create an `Export` object, use the methods of the class `Export`.
        // A typical usage is
        // +
        // [source,java]
        // ----
        //             addExports(Export.from("module").thePackage("package").to("otherModule"))
        // ----
        // +
        // You can make a static import for the method `from` to make the code more readable.

        CanIsolate addModules(String... modules);
        // * `addModules(String...)` adds required modules to the module declaration.

        CanIsolate limitModules(String... modules);
        // * `limitModules(String...)` limits the modules that are visible during compilation.

        CanIsolate module(String module);
        // * `module(String)` sets the module name of the compiled classes.

        // end snippet
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
