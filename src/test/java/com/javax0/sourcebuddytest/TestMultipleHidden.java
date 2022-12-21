package com.javax0.sourcebuddytest;

import com.javax0.sourcebuddy.Compiler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class TestMultipleHidden {
    public interface IfX {
        void X();
    }
    @Test
    @DisplayName("Test that hidden class can see the other class")
    void testHiddenSeeOther() throws Exception {
        final var x = Compiler.java().from("""
                package com.javax0.myPackage;
                public class A {
                  public void sayHello(){
                      System.out.println("hello");
                  }
                }
                """).from("""
                package com.javax0.myPackage;
                public class B implements com.javax0.sourcebuddytest.TestMultipleHidden.IfX {
                    public void X(){
                        new A().sayHello();
                    }
                }
                """).hidden().compile().load().newInstance("B",IfX.class);
        x.X();
    }
}
