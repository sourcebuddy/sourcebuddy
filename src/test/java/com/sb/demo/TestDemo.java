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
    void oneFileCompile() throws Compiler.CompileException, ClassNotFoundException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        // snippet simple_compile
        String source = "package com.sb.demo;\n" +
                "\n" +
                "public class MyClass {\n" +
                "    public String a() {\n" +
                "        return \"x\";\n" +
                "  }\n" +
                "}";
        Class<?> myClassClass = Compiler.compile("com.sb.demo.MyClass", source);
        Object myClass = myClassClass.getConstructor().newInstance();
        Method a = myClassClass.getDeclaredMethod("a");
        String s = (String) a.invoke(myClass);
        //end snippet
        Assertions.assertEquals("x", s);
    }

    @Test
    void callBack() throws Compiler.CompileException, ClassNotFoundException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        // snippet callBack_compile
        String source = "package com.sb.demo;\n" +
                "\n" +
                "public class MyClass {\n" +
                "    public String a() {\n" +
                "        TestDemo demo = new TestDemo();\n" +
                "        return demo.getClass().getPackageName();\n" +
                "  }\n" +
                "}";
        Class<?> myClassClass = Compiler.compile("com.sb.demo.MyClass", source);
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
        String sourceFirstClass = "package com.sb.demo;\n" +
                "\n" +
                "public class FirstClass {\n" +
                "    public String a() {\n" +
                "        return \"x\";\n" +
                "  }\n" +
                "}";
        String sourceSecondClass = "package com.sb.demo;\n" +
                "\n" +
                "public class SecondClass {\n" +
                "    public String a() {\n" +
                "        return new FirstClass().a();\n" +
                "  }\n" +
                "}";
        var compiler = Compiler.java()
                .from("com.sb.demo.FirstClass", sourceFirstClass)
                .compile().load();
        Class<?> firstClass = compiler.get("com.sb.demo.FirstClass");
        var newCompiler = compiler.reset();
        Class<?> secondClass = newCompiler
                .from("com.sb.demo.SecondClass", sourceSecondClass)
                .compile()
                .load().get("com.sb.demo.SecondClass");
        Object second = secondClass.getConstructor().newInstance();
        Method a = secondClass.getDeclaredMethod("a");
        String s = (String) a.invoke(second);
        Assertions.assertEquals("x", s);
        //end snippet

    }

    @Test
    void fluentApiDoc() throws Exception {
        // snippet api_doc
        String sourceFirstClass = "package com.sb.demo;\n" +
                "\n" +
                "public class FirstClass {\n" +
                "    public String a() {\n" +
                "        return \"x\";\n" +
                "  }\n" +
                "}";
        final var compiled = Compiler.java()
                .from("com.sb.demo.FirstClass", sourceFirstClass)
                .from(Paths.get("src/test/java"))
                .compile();
        compiled.saveTo(Paths.get("./target/generated_classes"));
        compiled.stream().forEach(bc -> System.out.println(Compiler.getBinaryName(bc)));
        final var loaded = compiled.load();
        Class<?> firstClassClass = loaded.get("com.sb.demo.FirstClass");
        loaded.stream().forEach( klass -> System.out.println(klass.getSimpleName()));
        final var compiler = loaded.reset();
        final var sameCompiler = compiled.reset();
        // end snippet
    }

}
