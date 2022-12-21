package com.sb.demo;

import com.javax0.sourcebuddy.Compiler;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Paths;

// snippet class_Demo_head
public class TestDemo {
    public final Double PI = 3.1415926;
    public final Long L = 13L;
    // end snippet

    @Test
    void simpleCompileOneLine() throws Compiler.CompileException, ClassNotFoundException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        String source = """
                package com.sb.demo;
                                
                public class MyClass implements Talker {
                    @Override
                    public void say() {
                        System.out.println("Hello, Buddy!");
                  }
                }""";
        var myO =
                // snipline simpleCompileOneLine
                Compiler.compile(source).getConstructor().newInstance();
        Talker myClass = (Talker) myO;
        myClass.say();
        //end snippet
    }

    @Test
    void oneFileCompile() throws Compiler.CompileException, ClassNotFoundException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        // snippet simple_compile
        String source = """
                package com.sb.demo;
                                
                public class MyClass implements Talker {
                    @Override
                    public void say() {
                        System.out.println("Hello, Buddy!");
                  }
                }""";
        Class<?> myClassClass = Compiler.compile(source);
        Talker myClass = (Talker) myClassClass.getConstructor().newInstance();
        myClass.say();
        //end snippet
    }


    @Test
    void compileFluentApi() throws Exception {
        String source = """
                package com.sb.demo;
                                
                public class MyClass implements Talker{
                    @Override
                    public void say() {
                        System.out.println("Hello, Buddy!");
                  }
                }""";
        Talker myClass =
                // snipline fluent_api_intro
                Compiler.java().from(source).compile().load().newInstance(Talker.class);
        myClass.say();

    }

    @Test
    void callBack() throws Compiler.CompileException, ClassNotFoundException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        // snippet callBack_compile
        String source = """
                package com.sb.demo;
                                
                public class MyClass {
                    public String a() {
                        TestDemo demo = new TestDemo();
                        return demo.getClass().getPackageName();
                  }
                }
                """;
        Class<?> myClassClass = Compiler.compile(source);
        Object myClass = myClassClass.getConstructor().newInstance();
        Method a = myClassClass.getDeclaredMethod("a");
        String s = (String) a.invoke(myClass);
        Assertions.assertEquals(this.getClass().getPackageName(), s);
        //end snippet
    }

    @Test
    @DisplayName("Compiler can be reset to add more files")
    void resetCompiler() throws Compiler.CompileException, ClassNotFoundException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        // snippet set_class_loader
        String sourceFirstClass = """
                package com.sb.demo;

                public class FirstClass {
                    public String a() {
                        return "x";
                  }
                }""";
        String sourceSecondClass = """
                package com.sb.demo;

                public class SecondClass {
                    public String a() {
                        return new FirstClass().a();
                  }
                }""";
        var compiler = Compiler.java()
                .from(sourceFirstClass)
                .compile().load();
        Class<?> firstClass = compiler.get("com.sb.demo.FirstClass");
        Assertions.assertNotNull(firstClass);
        var newCompiler = compiler.reset();
        Class<?> secondClass = newCompiler
                .from(sourceSecondClass)
                .compile()
                .load().get("SecondClass");
        Object second = secondClass.getConstructor().newInstance();
        Method a = secondClass.getDeclaredMethod("a");
        String s = (String) a.invoke(second);
        Assertions.assertEquals("x", s);
        //end snippet

    }

    @Test
    void fluentApiDoc() throws Exception {
        // snippet api_doc
        String sourceFirstClass = """
                package com.sb.demo;

                public class FirstClass {
                    public String a() {
                        return "x";
                  }
                }""";
        final var compiled = Compiler.java()
                .from("com.sb.demo.FirstClass", sourceFirstClass)
                .from(Paths.get("src/test/java"))
                .compile();
        compiled.saveTo(Paths.get("./target/generated_classes"));
        compiled.stream().forEach(bc -> System.out.println(Compiler.getBinaryName(bc)));
        final var loaded = compiled.load();
        Class<?> firstClassClass = loaded.get("com.sb.demo.FirstClass");
        Object firstClassInstance = loaded.newInstance("com.sb.demo.FirstClass");
        loaded.stream().forEach(klass -> System.out.println(klass.getSimpleName()));
        final var compiler = loaded.reset();
        final var sameCompiler = compiled.reset();
        // end snippet
    }

}
