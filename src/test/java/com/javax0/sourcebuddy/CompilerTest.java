package com.javax0.sourcebuddy;

import com.javax0.sourcebuddytest.OuterClass;
import com.javax0.sourcebuddytest.OuterClass2;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import static com.javax0.sourcebuddy.Compiler.args;
import static com.javax0.sourcebuddy.Compiler.classes;


@DisplayName("Test the simple one source compilation call")
public class CompilerTest {

    private String loadJavaSource(String name) throws IOException {
        try (final var is = this.getClass().getResourceAsStream(name)) {
            return new String(Objects.requireNonNull(is).readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private Path getTest1ResourcePath() {
        return Paths.get(this.getClass().getResource("Test1.java").getPath());
    }

    @Test
    @DisplayName("source code compiles")
    public void goodSimpleCode() throws Exception {
        final String source = loadJavaSource("Test3.java");
        Class<?> newClass = Compiler.compile("com.javax0.sourcebuddy.Test3", source);
        Object object = newClass.getConstructor().newInstance();
        Method f = newClass.getMethod("a");
        String s = (String) f.invoke(object);
        Assertions.assertEquals("x", s);
    }

    @Test
    @DisplayName("source code compiles providing the path to the file")
    public void goodSimpleCodeWithPathToFile() throws Exception {
        Class<?> newClass = Compiler.java().from(getTest1ResourcePath()).compile().load().get();
        Object object = newClass.getConstructor().newInstance();
        Method f = newClass.getMethod("a");
        String s = (String) f.invoke(object);
        Assertions.assertEquals("x", s);
    }

    @Test
    @DisplayName("source code compiles providing the path to the file with explicit name")
    public void goodSimpleCodeWithPathToFileAndName() throws Exception {
        Class<?> newClass = Compiler.java().from("com.javax0.sourcebuddy.Test1", getTest1ResourcePath()).compile().load().get();
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
        final String source = loadJavaSource("Test4.java");
        Class<?> newClass = Compiler.compile("com.javax0.sourcebuddy.Test4", source);
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
    @DisplayName("There is an IO exception if the source code file cannot be found")
    void testLoadFailure() {
        Assertions.assertThrows(NoSuchFileException.class, () ->
                Compiler.java().from(Paths.get("nonexistent_java_directory")));
    }

    @Test
    @DisplayName("Cannot find the name when there is no package")
    void noPackageNoNameFail() {
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
        final var sut = loadTestSources();
        final var target = "./target/test-classes";
        sut.compile().saveTo(Paths.get(target));
        for (int i = 1; i <= N; i++) {
            final var file = Paths.get(target, "com/javax0/sourcebuddy/Test%d.class".formatted(i));
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
        final var sut = loadTestSources();
        sut.compile().load().stream().forEach(klass -> {
            // all that finds were expected
            final var cn = klass.getName();
            Assertions.assertTrue(classes.remove(cn), "class '%s' was not expected".formatted(cn));
        });
        // finds all
        Assertions.assertEquals(0, classes.size());
    }

    @Test
    @DisplayName("get the stream of hidden classes")
    public void getStreamOfHiddenClasses() throws Exception {
        Compiler.java()
                // an example where a comment before the 'class ...' line messes up the automatic class name detection
                .from("com.javax0.sourcebuddy.Test1", loadJavaSource("Test1.java"))
                .hidden(MethodHandles.lookup())
                .compile()
                .load()
                .stream()
                .forEach(klass -> Assertions.assertNull(klass.getCanonicalName()));
    }

    private Fluent.CanCompile loadTestSources() throws IOException {
        final var source = new ArrayList<String>();
        for (int i = 1; i <= CompilerTest.N; i++) {
            source.add(loadJavaSource("Test%d.java".formatted(i)));
        }
        var sut = (Fluent.AddSource) Compiler.java();
        for (int i = 1; i <= CompilerTest.N; i++) {
            sut = sut.from("com.javax0.sourcebuddy.Test%d".formatted(i), source.get(i - 1));
        }
        return (Fluent.CanCompile) sut;
    }

    @Test
    @DisplayName("Compile all classes from a directory")
    void compileAllFromFile() throws Exception {
        final var classes = Compiler.java().from(Paths.get("./src/test/resources/source_tree")).compile().load();
        Class<?> newClass = classes.get("com.javax0.sourcebuddy.Test1");
        Object object = classes.newInstance("com.javax0.sourcebuddy.Test1");
        Method f = newClass.getMethod("a");
        String s = (String) f.invoke(object);
        Assertions.assertEquals("x", s);
    }

    @Test
    @DisplayName("Compile one class and load the object")
    void loadOne() throws Exception {
        final Object o = Compiler.java().from("package A;public class A{}").compile().load().newInstance();
        Assertions.assertNotNull(o);
    }

    @Test
    @DisplayName("Redefine class is not possible")
    void redefineAsHidden() throws Exception {
        final var compiler = Compiler.java();
        compiler.from("A", "class A { void hi(){} } ").compile().load().get();
        compiler.reset();
        Assertions.assertThrows(Compiler.CompileException.class,
                () -> compiler.from("A", "class A { void lo(){} } ").compile().load().get());
    }

    final static private String PACKAGE_PROTECTED_CLASS = """
            package com.javax0.sourcebuddy;
             
            class PackageClass {
              void hi(){
                System.out.println("hi");
              }
            }
            """;

    @Test
    @DisplayName("Can create class in the same module, not hidden, using lookup object")
    void createdWithLookup() throws Exception {
        final var lookup = MethodHandles.lookup();
        final var loaded = Compiler.java().from(PACKAGE_PROTECTED_CLASS).named(lookup).compile().load();
        final var hi = loaded.newInstance();
        final var m = hi.getClass().getDeclaredMethod("hi");
        System.out.println("createdWithLookup");
        m.invoke(hi);
    }

    @Test
    @DisplayName("Can create class in the same module, not hidden, using lookup object")
    void createdWithLookup1() throws Exception {
        final var lookup = MethodHandles.lookup();
        var loaded = Compiler.java().from(PACKAGE_PROTECTED_CLASS).named(lookup).compile().load();
        var hi = loaded.newInstance();
        final var m = hi.getClass().getDeclaredMethod("hi");
        System.out.println("createdWithLookup");
        m.invoke(hi);
        // now the same class is compiled and loaded again, but it was already loaded by the application class loader
        // and class loading follows the standard strategy: parent first
        final var loaded1 = Compiler.java().from(PACKAGE_PROTECTED_CLASS).named().compile().load();
        final var hi1 = loaded1.newInstance();
        final var m1 = hi1.getClass().getDeclaredMethod("hi");
        System.out.println("createdWithoutLookup");
        // although they are in the same package, but they are loaded by different class loaders, and
        // therefore they are in different modules, cannot call package protected methods
        m1.invoke(hi1);
        final var loaded2 = Compiler.java().from(PACKAGE_PROTECTED_CLASS).named().compile().load(Compiler.LoaderOption.REVERSE);
        final var hi2 = loaded2.newInstance();
        final var m2 = hi2.getClass().getDeclaredMethod("hi");
        System.out.println("createdWithoutLookup REVERSE");
        // although they are in the same package, but they are loaded by different class loaders, and
        // therefore they are in different modules, cannot call package protected methods
        Assertions.assertThrows(IllegalAccessException.class, () -> m2.invoke(hi2));
    }

    private static void printClass(final Class<?> c) {
        System.out.printf("%s - %s/%s#%s%n", c.getClassLoader(), c.getModule(), c.getPackageName(), c.getSimpleName());
    }

    @Test
    @DisplayName("Create inner class for already existing class, access private field")
    void testInnerClassCreation() throws Exception {
        // snippet InnerClass
        final var outer = new OuterClass();
        final var lookup = outer.getLookup();
        final var inner = Compiler.java().from("""
                        package com.javax0.sourcebuddytest;
                                                
                        public class OuterClass {
                            private int z=33;
                                        
                            public class Inner {
                               public void a(){
                                 z++;
                               }
                            }
                                        
                        }""").nest(lookup, MethodHandles.Lookup.ClassOption.NESTMATE).compile().load()
                .newInstance("Inner", classes(OuterClass.class), args(outer));
        final var m = inner.getClass().getDeclaredMethod("a");
        m.invoke(inner);
        Assertions.assertEquals(56, outer.getZ());
        // end snippet
    }
    @Test
    @DisplayName("Create inner class for already existing class, call private method")
    void testInnerClassCreation2() throws Exception {
        final var outer = new OuterClass();
        final var lookup = outer.getLookup();
        final var inner = Compiler.java().from("""
                        package com.javax0.sourcebuddytest;
                                                
                        public class OuterClass {
                            private int z=33;
                            private void inc(){}                   
                            public class Inner {
                               public void a(){
                                 z++;
                                 inc();
                               }
                            }
                                        
                        }""").nest(lookup, MethodHandles.Lookup.ClassOption.NESTMATE).compile().load()
                .newInstance("Inner", classes(OuterClass.class), args(outer));
        final var m = inner.getClass().getDeclaredMethod("a");
        m.invoke(inner);
        Assertions.assertEquals(57, outer.getZ());
        // end snippet
    }

    @Test
    @DisplayName("Inner class creation works with fetched lookup object")
    void testInnerClassCreationFetchLookup() throws Exception {
        final var outer = new OuterClass2();
        final var inner = Compiler.java().from("""
                        package com.javax0.sourcebuddytest;
                                                
                        public class OuterClass2 {
                            private int z;
                                        
                            public class Inner {
                               public void a(){
                                 z++;
                               }
                            }
                                        
                        }""").nest(MethodHandles.Lookup.ClassOption.NESTMATE).compile().load()
                .newInstance("Inner", classes(OuterClass2.class), args(outer));
        final var m = inner.getClass().getDeclaredMethod("a");
        m.invoke(inner);
        Assertions.assertEquals(56, outer.getZ());
    }

    @Test
    @DisplayName("Inner class creation does not work without lookup object")
    void testInnerClassCreationFailure() throws Exception {
        final var outer = new OuterClass();
        final var inner = Compiler.java().from("""
                        package com.javax0.sourcebuddytest;
                                                
                        public class OuterClass {
                            private int z;
                                        
                            public class Inner {
                               public void a(){
                                 z++;
                               }
                            }
                                        
                        }""").nest(MethodHandles.Lookup.ClassOption.NESTMATE).compile().load()
                .newInstance("Inner", classes(OuterClass.class), args(outer));
        final var m = inner.getClass().getDeclaredMethod("a");
        try {
            m.invoke(inner);
            Assertions.fail("Did not throw exception");
        }catch (InvocationTargetException ite){
            Assertions.assertEquals(IllegalAccessError.class, ite.getCause().getClass());
        }
    }

    @Test
    @DisplayName("Inner class creation fails when field has different type")
    void testInnerClassUseFailure() throws Exception {
        final var outer = new OuterClass();
        final var lookup = outer.getLookup();
        final var inner = Compiler.java().from("""
                        package com.javax0.sourcebuddytest;
                                                
                        public class OuterClass {
                            private long z; // the type of the field does not match the one in the code
                                        
                            public class Inner {
                               public void a(){
                                 z++;
                               }
                            }
                                        
                        }""").nest(lookup,MethodHandles.Lookup.ClassOption.NESTMATE).compile().load()
                .newInstance("Inner", classes(OuterClass.class), args(outer));
        final var m = inner.getClass().getDeclaredMethod("a");
        try {
            m.invoke(inner);
            Assertions.fail("Did not throw exception");
        }catch (InvocationTargetException ite){
            Assertions.assertEquals(NoSuchFieldError.class, ite.getCause().getClass());
        }
    }

}