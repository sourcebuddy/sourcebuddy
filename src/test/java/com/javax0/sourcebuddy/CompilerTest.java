package com.javax0.sourcebuddy;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import static java.lang.String.format;

@DisplayName("Test the simple one source compilation call")
public class CompilerTest {

    private String loadJavaSource(String name) throws IOException {
        try (final var is = this.getClass().getResourceAsStream(name)) {
            return new String(Objects.requireNonNull(is).readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private Path getResourcePath(String name) {
        return Paths.get(this.getClass().getResource(name).getPath());
    }

    @Test
    @DisplayName("source code compiles")
    public void goodSimpleCode() throws Exception {
        final String source = loadJavaSource("Test1.java");
        Class<?> newClass = Compiler.compile("com.javax0.sourcebuddy.Test1", source);
        Object object = newClass.getConstructor().newInstance();
        Method f = newClass.getMethod("a");
        String s = (String) f.invoke(object);
        Assertions.assertEquals("x", s);
    }

    @Test
    @DisplayName("source code compiles providing the path to the file")
    public void goodSimpleCodeWithPathToFile() throws Exception {
        Class<?> newClass = Compiler.java().from(getResourcePath("Test1.java")).compile().load().get();
        Object object = newClass.getConstructor().newInstance();
        Method f = newClass.getMethod("a");
        String s = (String) f.invoke(object);
        Assertions.assertEquals("x", s);
    }

    @Test
    @DisplayName("source code compiles providing the path to the file with explicit name")
    public void goodSimpleCodeWithPathToFileAndName() throws Exception {
        Class<?> newClass = Compiler.java().from("com.javax0.sourcebuddy.Test1",getResourcePath("Test1.java")).compile().load().get();
        Object object = newClass.getConstructor().newInstance();
        Method f = newClass.getMethod("a");
        String s = (String) f.invoke(object);
        Assertions.assertEquals("x", s);
    }

    @Test
    @DisplayName("source code compiles without explicitly specifying the name")
    public void goodSimpleCodeWithoutName()
            throws Exception {
        final String source = loadJavaSource("Test1.java");
        Class<?> newClass = Compiler.compile(source);
        Object object = newClass.getConstructor().newInstance();
        Method f = newClass.getMethod("a");
        String s = (String) f.invoke(object);
        Assertions.assertEquals("x", s);
    }

    @Test
    @DisplayName("erroneous source returns null class")
    public void erroneousSourceCode()
            throws Exception {
        final String source = loadJavaSource("TestError.java");
        Assertions.assertThrows(Compiler.CompileException.class, () -> Compiler.compile("com.javax0.sourcebuddy.TestError", source));
    }

    @Test
    @DisplayName("source code with subclasses works fine")
    public void sourceCodeWithSubclass()
            throws Exception {
        final String source = loadJavaSource("Test2.java");
        Class<?> newClass = Compiler.compile("com.javax0.sourcebuddy.Test2", source);
        Object object = newClass.getConstructor().newInstance();
        Method f = newClass.getMethod("method");
        int i = (int) f.invoke(object, (Object[]) null);
        Assertions.assertEquals(1, i);
    }

    @Test
    @DisplayName("source code with subclasses  without explicitly specifying the name works fine")
    public void sourceCodeWithSubclassWithoutName()
            throws Exception {
        final String source = loadJavaSource("Test2.java");
        Class<?> newClass = Compiler.compile(source);
        Object object = newClass.getConstructor().newInstance();
        Method f = newClass.getMethod("method");
        int i = (int) f.invoke(object, (Object[]) null);
        Assertions.assertEquals(1, i);
    }

    @Test
    @DisplayName("source code with subclasses works fine using fluent api")
    public void usingFluentApi()
            throws Exception {
        final String source = loadJavaSource("Test2.java");
        Class<?> newClass =
                Compiler.java().from("com.javax0.sourcebuddy.Test2", source).compile().load().get("com.javax0.sourcebuddy.Test2");
        Object object = newClass.getConstructor().newInstance();
        Method f = newClass.getMethod("method");
        int i = (int) f.invoke(object, (Object[]) null);
        Assertions.assertEquals(1, i);
    }

    @Test
    @DisplayName("source code with subclasses works fine using fluent api and get no arg provides top level class")
    public void usingFluentApiGetNoArg() throws Exception {
        final String source = loadJavaSource("Test2.java");
        Class<?> newClass =
                Compiler.java().from(source).compile().load().get();
        Object object = newClass.getConstructor().newInstance();
        Method f = newClass.getMethod("method");
        int i = (int) f.invoke(object, (Object[]) null);
        Assertions.assertEquals(1, i);
    }

    @Test
    @DisplayName("get w/o args will throw when there are multiple compiled classes")
    public void usingFluentApiGetNoArgThows() throws Exception {
        final String source1 = loadJavaSource("Test1.java");
        final String source2 = loadJavaSource("Test2.java");
        Assertions.assertThrows(ClassNotFoundException.class, () ->
                Compiler.java().from(source1).from(source2).compile().load().get());
    }

    @Test
    @DisplayName("More sources can be added after the compiled is reset")
    public void compilerCanReset() throws Exception {
        final String source1 = loadJavaSource("Test1.java");
        final String source2 = loadJavaSource("Test2.java");
        final var compiler = Compiler.java().from(source1).compile().load();
        final var classes = compiler.reset().from(source2).compile().load();
        final var newClass = classes.get("Test2");
        Object object = newClass.getConstructor().newInstance();
        Method f = newClass.getMethod("method");
        int i = (int) f.invoke(object, (Object[]) null);
        Assertions.assertEquals(1, i);
    }

    @Test
    @DisplayName("Compiler cannot reset when the classes are loaded hidden")
    public void compilerHiddenCannotReset() throws Exception {
        final String source1 = loadJavaSource("Test1.java");
        final String source2 = loadJavaSource("Test2.java");
        final var compiler = Compiler.java().from(source1).compile().loadHidden();
        Assertions.assertThrows(RuntimeException.class, () -> compiler.reset().from(source2));
    }

    @Test
    @DisplayName("There is an IO exception if the source code file cannot be found")
    void testLoadFailure() throws Exception {
        Assertions.assertThrows(NoSuchFileException.class, () ->
                Compiler.java().from(Paths.get("nonexistent_java_directory")));
    }

    @Test
    @DisplayName("Cannot find the name when there is no package")
    void noPackageNoNameFail() throws Exception {
        Assertions.assertThrows(ClassNotFoundException.class, () ->
                Compiler.java().from("class Z{}"));
    }

    @Test
    @DisplayName("Cannot add source file after compilation")
    void cannotAddSourceFileAfterCompilation() throws Exception {
        final var compiler = Compiler.java().from("Z", "class Z{}");
        compiler.compile();

        Assertions.assertThrows(RuntimeException.class, () ->
                compiler.from("X", "class X{}"));
    }


    @Test
    @DisplayName("after compilation the 'get' can give a \"normal\", not recently compiled class as well")
    public void loadsNormalClassesAsWell() throws Exception {
        final String source = loadJavaSource("Test2.java");
        final var loaded =
                Compiler.java().from("com.javax0.sourcebuddy.Test2", source).compile().load();
        final var stringClass = loaded.get("java.lang.String");
        Assertions.assertEquals(String.class, stringClass);
    }

    private static final int N = 2;

    @Test
    @DisplayName("generated class files are saved")
    public void saveClassFiles() throws Exception {
        final var sut = loadAll(N);
        final var target = "./target/test-classes";
        sut.compile().saveTo(Paths.get(target));
        for (int i = 1; i <= N; i++) {
            final var file = Paths.get(target, format("com/javax0/sourcebuddy/Test%d.class", i));
            Assertions.assertTrue(Files.exists(file));
            final var byteCode = Files.readAllBytes(file);
            Assertions.assertEquals(byteCode[0], (byte) 0xCA);
            Assertions.assertEquals(byteCode[1], (byte) 0xFE);
            Assertions.assertEquals(byteCode[2], (byte) 0xBA);
            Assertions.assertEquals(byteCode[3], (byte) 0xBE);
        }
    }

    @Test
    @DisplayName("get the stream of classes")
    public void getStreamOfClasses() throws Exception {
        final var classes = new HashSet<>(Set.of(
                "com.javax0.sourcebuddy.Test1",
                "com.javax0.sourcebuddy.Test2",
                "com.javax0.sourcebuddy.Test2$Hallo",
                "com.javax0.sourcebuddy.Test2$1"));
        final var sut = loadAll(N);
        sut.compile().load().stream().forEach(klass -> {
            // all that finds were expected
            final var cn = klass.getName();
            Assertions.assertTrue(classes.remove(cn), format("class '%s' was not expected", cn));
        });
        // finds all
        Assertions.assertEquals(0, classes.size());
    }

    @Test
    @DisplayName("get the stream of hidden classes")
    public void getStreamOfHiddenClasses() throws Exception {
        final var sut = loadAll(1);
        sut.compile().loadHidden(MethodHandles.lookup()).stream().forEach(klass -> {
            Assertions.assertNull(klass.getCanonicalName());
        });
    }

    private Fluent.CanCompile loadAll(int n) throws IOException {
        final var source = new ArrayList<String>();
        for (int i = 1; i <= n; i++) {
            source.add(loadJavaSource(format("Test%d.java", i)));
        }
        var sut = (Fluent.AddSource) Compiler.java();
        for (int i = 1; i <= n; i++) {
            sut = sut.from("com.javax0.sourcebuddy.Test%d".formatted(i), source.get(i - 1));
        }
        return (Fluent.CanCompile) sut;
    }

    @Test
    @DisplayName("Compile all classes from a directory")
    void compileAllFromFile() throws Exception {
        final var classes = Compiler.java().from(Paths.get("./src/test/resources/source_tree")).compile().load();
        Class<?> newClass = classes.get("com.javax0.sourcebuddy.Test1");
        Object object = classes.newInstance("com.javax0.sourcebuddy.Test1", Object.class);
        Method f = newClass.getMethod("a");
        String s = (String) f.invoke(object);
        Assertions.assertEquals("x", s);
    }

}