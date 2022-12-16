package com.javax0.sourcebuddytest;

import com.javax0.sourcebuddy.Compiler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class TestCrossPackageCreation {
    public interface Hello {
        void hello();
    }
    private static final String CODE = """
            package com.javax0.sourcebuddytest;
                        
            public class MySpecialClass implements TestCrossPackageCreation.Hello {
                        
                @Override
                public void hello() {
                    System.out.println("Hello, from the hidden class.");
                }
            }
            """;
    public static final String CLASS_NAME = "com.javax0.sourcebuddytest.MySpecialClass";

    @Test
    @DisplayName("Invoke the hidden class and say hello using SourceBuddy")
    void sayHellotoSB() throws Exception {
        // snippet sayHelloSB
        final var hello = Compiler.java().from(CLASS_NAME, CODE).compile().loadHidden().newInstance(CLASS_NAME, Hello.class);
        hello.hello();
        //end snippet
    }
}
