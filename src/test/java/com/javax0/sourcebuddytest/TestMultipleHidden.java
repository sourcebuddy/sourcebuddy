package com.javax0.sourcebuddytest;

import com.javax0.sourcebuddy.Compiler;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

public class TestMultipleHidden {
    public interface IfX {
        void X();
    }

    @Test
    @DisplayName("Test that hidden class can see the other class")
    void testHiddenSeeOther() throws Exception {
        final var myOut = System.out;
        try (final var baos = new ByteArrayOutputStream()) {
            System.setOut(new PrintStream(baos));
            final var x = Compiler.java().from("""
                    package com.javax0.myPackage;
                    public class A {
                      public void sayHello(){
                          System.out.print("hello");
                      }
                    }
                    """).from("""
                    package com.javax0.myPackage;
                    public class B implements com.javax0.sourcebuddytest.TestMultipleHidden.IfX {
                        public void X(){
                            new A().sayHello();
                        }
                    }
                    """).hidden().compile().load().newInstance("B", IfX.class);
            x.X();
            Assertions.assertEquals("hello",new String(baos.toByteArray(), StandardCharsets.UTF_8));
        } finally {
            System.setOut(myOut);
        }
    }
}